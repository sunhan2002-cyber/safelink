package com.safelink.app.data.remote

import android.os.SystemClock
import android.util.Log
import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.core.JsonValue
import com.anthropic.models.messages.JsonOutputFormat
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.OutputConfig
import com.anthropic.models.messages.StopReason
import com.anthropic.models.messages.ThinkingConfigAdaptive
import com.anthropic.models.messages.ThinkingConfigDisabled
import com.safelink.app.BuildConfig
import com.safelink.app.data.remote.dto.AnalyzeRequestDto
import com.safelink.app.data.remote.dto.AnalyzeResponseDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * 앱이 Claude 를 직접 호출하는 AI 문맥 분석기 (발표용 구성).
 *
 * ── 왜 서버를 거치지 않는가 ────────────────────────────────────────────
 * 발표 시연에서 폰 ↔ PC 서버 접속(네트워크·방화벽·터널)에 의존하지 않기 위해서다. 팀 결정.
 * 대가로 API 키가 APK 안에 들어간다. 그래서 이 구성의 APK 는 외부에 배포·공유하지 않으며,
 * 키는 local.properties 에만 두고(저장소 미포함) 발표가 끝나면 콘솔에서 폐기한다.
 * 키가 비어 있으면 [isConfigured] 가 false 가 되어 기존 서버 경로로 요청한다.
 *
 * ── 서버가 지키던 규칙은 그대로 지킨다 ─────────────────────────────────
 * 프롬프트, 입력 구성, 출력 검증(보정치 ±30, 지어낸 규칙 id 제거, 원문 속 지시 무력화)은
 * [ClaudeAnalysisRules] 에 있다. 이 클래스는 SDK 호출과 실패 처리만 맡는다.
 *
 * ── 속도 ─────────────────────────────────────────────────────────────
 * effort LOW. 실측(로맨스스캠 샘플, Sonnet 5, PC): high 4.9~6.9초 / medium 3.1~3.8초 / low 3.3~3.4초.
 * 샘플 두 건에서 세 단계의 판정(보정치·수법)은 같았다.
 *
 * 에뮬레이터 실측으로는 앱을 켜고 첫 호출이 13.5초, 이후 호출이 5.8~5.9초였다. 첫 호출의 차이는
 * SDK 준비(JSON 라이브러리 초기화, 서버 연결 수립) 비용이다. 그래서
 * - SDK 클라이언트를 프로세스 전체에서 하나만 쓴다. 분석 화면마다 새로 만들면 준비가 매번 사라진다.
 * - 앱이 시작될 때 [warmUp] 으로 미리 준비해 둔다(SafeLinkApplication).
 *
 * 호출은 결과 화면을 막지 않는 비동기라 제한 시간은 15초로 넉넉히 두어, 가끔 느린 호출이 실패로
 * 끝나지 않게 했다. 실패(거절·시간 초과·빈 응답·네트워크 오류)는 전부 null 로 돌려주고, 호출 측은
 * 온디바이스 결과를 그대로 쓴다. 로그에는 실패 종류와 소요 시간만 남기고 대화 내용은 남기지 않는다.
 */
class ClaudeDirectAnalyzer {

    val isConfigured: Boolean get() = isKeyConfigured

    /** 판정을 받아 앱이 쓰는 응답 형태로 돌려준다. 받지 못하면 null. */
    suspend fun analyze(request: AnalyzeRequestDto): AnalyzeResponseDto? = withContext(Dispatchers.IO) {
        val startedAt = SystemClock.elapsedRealtime()
        try {
            val params = MessageCreateParams.builder()
                .model(ClaudeAnalysisRules.MODEL)
                .maxTokens(ClaudeAnalysisRules.MAX_TOKENS)
                .system(ClaudeAnalysisRules.SYSTEM_PROMPT)
                .thinking(ThinkingConfigAdaptive.builder().build())
                .outputConfig(outputConfig)
                .addUserMessage(
                    ClaudeAnalysisRules.buildUserMessage(
                        maskedText = request.maskedText,
                        recentTurns = request.recentTurns,
                        deviceBaseScore = request.deviceBaseScore.toInt(),
                        matchedIds = request.deviceMatchedIds,
                        appliedComboIds = request.deviceAppliedComboIds,
                        categoryHint = request.categoryHint
                    )
                )
                .build()

            val message = client.messages().create(params)
            val elapsed = SystemClock.elapsedRealtime() - startedAt

            // content 를 읽기 전에 stopReason 부터 본다. 거절이면 content 가 비어 있을 수 있다.
            when (message.stopReason().orElse(null)) {
                StopReason.REFUSAL -> {
                    Log.w(TAG, "AI 분석 거절 (${elapsed}ms)")
                    return@withContext null
                }
                StopReason.MAX_TOKENS -> {
                    Log.w(TAG, "AI 분석 출력 한도 도달 (${elapsed}ms)")
                    return@withContext null
                }
                else -> Unit
            }

            val json = message.content().firstNotNullOfOrNull { it.text().orElse(null)?.text() }
            val judgement = json?.let { ClaudeAnalysisRules.parseJudgement(it) }
            val response = judgement?.let {
                ClaudeAnalysisRules.toResponse(
                    judgement = it,
                    allowedKeywordIds = request.deviceMatchedIds,
                    analysisTimestamp = OffsetDateTime.now(KST).toString()
                )
            }
            if (response == null) {
                Log.w(TAG, "AI 분석 응답을 읽지 못함 (${elapsed}ms)")
            } else {
                Log.d(TAG, "AI 분석 완료 (${elapsed}ms)")
            }
            response
        } catch (e: CancellationException) {
            throw e // 구조적 동시성 - 취소는 그대로 전파
        } catch (e: Exception) {
            val elapsed = SystemClock.elapsedRealtime() - startedAt
            Log.w(TAG, "AI 분석 실패: ${e.javaClass.simpleName} (${elapsed}ms)")
            null
        }
    }

    companion object {
        private const val TAG = "ClaudeDirect"
        private const val TIMEOUT_SECONDS = 15L
        private val KST: ZoneOffset = ZoneOffset.ofHours(9)

        private val isKeyConfigured: Boolean = BuildConfig.ANTHROPIC_API_KEY.isNotBlank()

        /** 프로세스 전체에서 하나만 쓰는 클라이언트. 연결·JSON 준비를 호출마다 새로 하지 않기 위해서다. */
        private val client: AnthropicClient by lazy {
            AnthropicOkHttpClient.builder()
                .apiKey(BuildConfig.ANTHROPIC_API_KEY)
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                // 재시도하면 대기 시간이 배로 늘어난다. 실패하면 온디바이스 결과로 충분하다.
                .maxRetries(0)
                .build()
        }

        private val outputConfig: OutputConfig by lazy {
            val schema = JsonOutputFormat.Schema.builder().apply {
                ClaudeAnalysisRules.OUTPUT_SCHEMA.forEach { (key, value) ->
                    putAdditionalProperty(key, JsonValue.from(value))
                }
            }.build()
            OutputConfig.builder()
                .effort(OutputConfig.Effort.LOW)
                .format(JsonOutputFormat.builder().schema(schema).build())
                .build()
        }

        /**
         * 첫 AI 분석이 SDK 준비 때문에 느려지지 않도록 미리 준비한다. 앱 시작 시 백그라운드 스레드에서 부른다.
         *
         * 클라이언트와 요청 설정을 만들어 JSON 라이브러리를 초기화하고, 실제 분석과 같은 경로로 아주 작은
         * 요청("ping", 출력 1토큰)을 한 번 보내 연결과 응답 처리까지 준비해 둔다. 앱을 켤 때마다 1원 미만이
         * 과금된다. 대화 내용은 보내지 않는다. 실패해도 조용히 넘어간다 —
         * 준비가 안 됐으면 첫 분석이 조금 느릴 뿐 동작에는 영향이 없다.
         */
        fun warmUp() {
            if (!isKeyConfigured) return
            val startedAt = SystemClock.elapsedRealtime()
            try {
                outputConfig
                // 실제 분석과 같은 messages 경로로 아주 작은 요청(출력 1토큰, 생각 끔)을 보낸다.
                // 모델 정보 조회만으로는 요청·응답 변환 코드가 준비되지 않아 첫 분석이 여전히 느렸다(6.3초).
                client.messages().create(
                    MessageCreateParams.builder()
                        .model(ClaudeAnalysisRules.MODEL)
                        .maxTokens(1L)
                        .thinking(ThinkingConfigDisabled.builder().build())
                        .addUserMessage("ping")
                        .build()
                )
                Log.d(TAG, "AI 분석 준비 완료 (${SystemClock.elapsedRealtime() - startedAt}ms)")
            } catch (e: Exception) {
                Log.w(TAG, "AI 분석 준비 실패: ${e.javaClass.simpleName}")
            }
        }
    }
}
