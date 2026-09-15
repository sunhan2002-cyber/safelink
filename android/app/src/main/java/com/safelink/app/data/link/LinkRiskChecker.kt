package com.safelink.app.data.link

import android.content.Context
import android.util.Log
import com.google.android.gms.safebrowsing.SafeBrowsing
import com.google.android.gms.safebrowsing.SafeBrowsingApiOptions
import com.google.android.gms.safebrowsing.SafeBrowsingClient
import com.google.android.gms.safebrowsing.SafeBrowsingResponse
import com.google.android.gms.tasks.Task
import com.safelink.app.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 대화에 섞여 온 링크가 실제로 위험한 주소인지 확인한다.
 *
 * ## 왜 "온디바이스 차단 목록" 방식인가 (설계상 중요한 결정)
 *
 * 링크 검사에는 크게 두 가지 방식이 있다.
 *
 * | | 실시간 조회 방식 | **온디바이스 차단 목록 방식(채택)** |
 * |---|---|---|
 * | 동작 | 검사할 URL 을 서버로 보내 물어봄 | 위험 URL 해시 앞부분 목록을 기기에 내려받아 두고 **기기 안에서** 대조 |
 * | 서버가 아는 것 | **사용자가 어떤 링크를 받았는지 전부** | 아무것도. 목록을 내려받을 뿐 |
 * | 오프라인 | 불가 | 가능 |
 * | 속도 | 네트워크 왕복 필요 | 로컬 조회 (수 ms) |
 *
 * SafeLink 는 **피해자의 대화**를 다루는 앱이다. 링크 하나하나를 외부로 보내면
 * "누가 언제 어떤 사기 링크를 받았는지"가 그대로 외부 기록으로 남는다.
 * 이는 "원문은 서버로 전송하지 않는다"는 앱 전체 원칙과 정면으로 어긋난다.
 * 그래서 성능이 아니라 **개인정보 보호를 이유로** 온디바이스 차단 목록 방식
 * ([SafeBrowsing.Protocol.LOCAL_BLOCK_LIST])을 고정으로 쓴다.
 *
 * 구글 문서도 이 방식에 대해 "검사 중인 URL 을 구글이 알게 되는 시점은 없다"고 명시한다.
 *
 * 구현은 Google Play 서비스가 제공하는 클라이언트를 쓴다. 차단 목록의 다운로드·주기적
 * 갱신·해시 대조를 Play 서비스가 대신 관리하므로, 앱이 직접 목록 DB 를 들고 있을 필요가 없다.
 * (기기 전체가 목록 하나를 공유하므로 앱 용량·트래픽도 늘지 않는다.)
 *
 * ## 검사할 수 없는 경우
 *
 * API 키 미설정, 차단 목록 미준비, Play 서비스 없음 등은 모두 [LinkVerdict.UNCHECKED] 로
 * 돌려준다. **링크 검사 실패가 분석 전체를 막지 않는다** — 키워드 기반 온디바이스 판정은
 * 그대로 나오고, 링크 항목만 "검사하지 못함"으로 표시된다.
 */
class LinkRiskChecker(
    context: Context,
    private val apiKey: String = BuildConfig.SAFE_BROWSING_API_KEY
) {

    private val appContext = context.applicationContext

    /** API 키가 있어야 클라이언트를 만들 수 있다. 키가 없으면 기능만 조용히 비활성. */
    val isConfigured: Boolean get() = apiKey.isNotBlank()

    private val client: SafeBrowsingClient? by lazy {
        if (!isConfigured) return@lazy null
        runCatching {
            SafeBrowsing.getClient(
                appContext,
                SafeBrowsingApiOptions.Builder().setApiKey(apiKey).build()
            )
        }.getOrNull()
    }

    /** [text] 안의 링크를 모두 찾아 검사한다. 링크가 없으면 빈 목록. */
    suspend fun checkText(text: String): List<LinkRiskResult> =
        LinkExtractor.extract(text).map { check(it) }

    suspend fun check(link: ExtractedLink): LinkRiskResult {
        val client = client
            ?: return link.unchecked(
                if (isConfigured) "링크 검사 기능을 사용할 수 없습니다" else "링크 검사가 설정되지 않았습니다"
            )

        val response = try {
            withTimeoutOrNull(LOOKUP_TIMEOUT_MS) {
                // 한글 도메인은 ASCII(퓨니코드)로 바꿔 대조한다 — 위험 주소 목록이 ASCII 호스트 기준이다
                client.lookupUri(LinkExtractor.toAsciiUrl(link.url), THREAT_TYPES, SafeBrowsing.Protocol.LOCAL_BLOCK_LIST).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "링크 검사 실패: ${e.javaClass.simpleName}")
            null
        } ?: return link.unchecked("링크를 검사하지 못했습니다")

        return when (response.status) {
            SafeBrowsingResponse.SafeBrowsingResponseStatus.FAILURE_INVALID_URL ->
                link.unchecked("주소 형식을 확인할 수 없습니다")

            SafeBrowsingResponse.SafeBrowsingResponseStatus.FAILURE_BLOCK_LIST_UNAVAILABLE ->
                link.unchecked("위험 주소 목록이 아직 준비되지 않았습니다")

            SafeBrowsingResponse.SafeBrowsingResponseStatus.FAILURE_NETWORK_UNAVAILABLE ->
                link.unchecked("네트워크에 연결되어 있지 않습니다")

            else ->
                if (response.threatDetected()) {
                    LinkRiskResult(link, LinkVerdict.DANGEROUS, response.detectedThreatType.toThreat())
                } else {
                    LinkRiskResult(link, LinkVerdict.NO_MATCH)
                }
        }
    }

    private fun ExtractedLink.unchecked(reason: String) =
        LinkRiskResult(this, LinkVerdict.UNCHECKED, uncheckedReason = reason)

    private fun Int.toThreat(): LinkThreat? = when (this) {
        SafeBrowsing.ThreatType.TYPE_MALWARE -> LinkThreat.MALWARE
        SafeBrowsing.ThreatType.TYPE_SOCIAL_ENGINEERING -> LinkThreat.SOCIAL_ENGINEERING
        SafeBrowsing.ThreatType.TYPE_UNWANTED_SOFTWARE -> LinkThreat.UNWANTED_SOFTWARE
        SafeBrowsing.ThreatType.TYPE_POTENTIALLY_HARMFUL_APPLICATION -> LinkThreat.HARMFUL_APP
        else -> null
    }

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resumeWithException(it) }
    }

    private companion object {
        const val TAG = "LinkRiskChecker"

        /**
         * 로컬 목록 조회라 보통 수 ms 안에 끝난다. 그럼에도 상한을 두는 건 Play 서비스가
         * 처음 목록을 준비하는 중이거나 프로세스 바인딩이 늦어질 수 있어서다.
         * 백그라운드 감지는 "빨리 알려주는 것"이 핵심이므로 기다리지 않고 넘어간다.
         */
        const val LOOKUP_TIMEOUT_MS = 2_000L

        val THREAT_TYPES = listOf(
            SafeBrowsing.ThreatType.TYPE_SOCIAL_ENGINEERING,
            SafeBrowsing.ThreatType.TYPE_MALWARE,
            SafeBrowsing.ThreatType.TYPE_UNWANTED_SOFTWARE,
            SafeBrowsing.ThreatType.TYPE_POTENTIALLY_HARMFUL_APPLICATION
        )
    }
}
