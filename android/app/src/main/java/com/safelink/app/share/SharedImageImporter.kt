package com.safelink.app.share

import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import java.io.File

/**
 * 다른 앱에서 "공유 → SafeLink" 로 넘어온 스크린샷을 받는다.
 *
 * ── 흐름 ─────────────────────────────────────────────────────────────
 * 스크린샷 미리보기(또는 갤러리·카톡 사진)에서 공유 → SafeLink 를 고르면 MainActivity 가 이미지 주소를 받는다.
 * 여기서 앱 전용 캐시로 **복사해 둔 뒤** 스크린샷 분석으로 넘긴다.
 *
 * ── 왜 바로 쓰지 않고 복사하는가 ─────────────────────────────────────
 * 공유받은 주소(content://)의 읽기 권한은 그 인텐트를 받은 화면에만 잠깐 주어진다. 앱 잠금·온보딩을 거치는 사이
 * 화면이 다시 만들어지면 권한이 사라져 "이미지를 읽을 수 없음"이 된다. 받자마자 복사하면 이후 흐름과 무관하다.
 * 복사본은 다음 공유가 들어오거나 새 분석을 시작할 때 지운다([clear]).
 *
 * ── 무엇을 거절하는가 ────────────────────────────────────────────────
 * 공유 인텐트는 **어느 앱이든** 보낼 수 있다. 그래서
 * - file:// 주소는 받지 않는다 — 우리 앱 내부 파일(기록 DB 등)을 가리키게 해서 읽히려는 시도를 막는다.
 * - content:// 라도 제공자가 우리 앱이면 받지 않는다(같은 이유).
 * - 이미지가 아니거나 너무 큰 파일은 건너뛴다.
 */
object SharedImageImporter {

    private const val TAG = "SharedImageImporter"
    private const val DIR = "shared_screenshots"

    /** 한 장당 최대 크기 — 스크린샷은 보통 수 MB 이하다. 이보다 크면 스크린샷이 아닐 가능성이 높다. */
    private const val MAX_BYTES = 20L * 1024 * 1024

    /** 이 인텐트가 공유로 들어온 이미지인지 */
    fun isImageShare(intent: Intent?): Boolean {
        if (intent == null) return false
        val action = intent.action
        if (action != Intent.ACTION_SEND && action != Intent.ACTION_SEND_MULTIPLE) return false
        return intent.type?.startsWith("image/") == true
    }

    /** 인텐트에 담긴 이미지 주소들 (SEND 는 한 장, SEND_MULTIPLE 은 여러 장) */
    fun streamsOf(intent: Intent): List<Uri> = when (intent.action) {
        Intent.ACTION_SEND -> listOfNotNull(intent.parcelable(Intent.EXTRA_STREAM))
        Intent.ACTION_SEND_MULTIPLE -> intent.parcelableList(Intent.EXTRA_STREAM)
        else -> emptyList()
    }

    /**
     * 공유받은 이미지를 앱 캐시로 복사하고, 복사본 주소를 돌려준다. 이전 공유 복사본은 먼저 지운다.
     * 디스크 작업이므로 메인 스레드에서 부르지 않는다.
     */
    fun import(context: Context, uris: List<Uri>, maxImages: Int): List<Uri> {
        val dir = clear(context)
        dir.mkdirs()
        return uris.asSequence()
            .filter { isAcceptable(context, it) }
            .take(maxImages)
            .mapIndexedNotNull { index, uri -> copy(context, uri, File(dir, "shared_$index.img")) }
            .toList()
    }

    /** 공유 복사본을 모두 지운다. 지운 뒤의(비어 있는) 폴더를 돌려준다. */
    fun clear(context: Context): File {
        val dir = File(context.cacheDir, DIR)
        dir.listFiles()?.forEach { it.delete() }
        return dir
    }

    private fun isAcceptable(context: Context, uri: Uri): Boolean {
        if (uri.scheme != ContentResolver.SCHEME_CONTENT) {
            Log.w(TAG, "content:// 가 아닌 공유 주소는 받지 않음: ${uri.scheme}")
            return false
        }
        val authority = uri.authority ?: return false
        val provider = runCatching {
            context.packageManager.resolveContentProvider(authority, 0)
        }.getOrNull()
        if (provider?.packageName == context.packageName) {
            Log.w(TAG, "앱 자신의 제공자를 가리키는 공유 주소는 받지 않음")
            return false
        }
        val type = runCatching { context.contentResolver.getType(uri) }.getOrNull()
        // 형식을 알려주지 않는 제공자도 있어(null) 그때는 통과시키고, 실제로 이미지가 아니면 OCR 단계에서 걸러진다
        return type == null || type.startsWith("image/")
    }

    private fun copy(context: Context, source: Uri, target: File): Uri? = runCatching {
        context.contentResolver.openInputStream(source)?.use { input ->
            target.outputStream().use { output ->
                val buffer = ByteArray(64 * 1024)
                var total = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > MAX_BYTES) error("공유 이미지가 너무 큼")
                    output.write(buffer, 0, read)
                }
            }
        } ?: error("공유 이미지를 열 수 없음")
        Uri.fromFile(target)
    }.onFailure {
        target.delete()
        Log.w(TAG, "공유 이미지 복사 실패(건너뜀)", it)
    }.getOrNull()

    /**
     * 공유받기 입구(activity-alias)를 켜고 끈다.
     * 설정의 "스크린샷 분석 사용"을 끄면 공유 목록에서도 SafeLink 가 사라지게 한다 —
     * 공유 목록에는 보이는데 눌러 보면 꺼져 있는 기능이면 설정으로서 성립하지 않는다.
     */
    fun setShareTargetEnabled(context: Context, enabled: Boolean) {
        val component = ComponentName(context, "${context.packageName}.ShareScreenshotAlias")
        val state = if (enabled) PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
        else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        runCatching {
            if (context.packageManager.getComponentEnabledSetting(component) != state) {
                context.packageManager.setComponentEnabledSetting(component, state, PackageManager.DONT_KILL_APP)
            }
        }.onFailure { Log.w(TAG, "공유 입구 상태 변경 실패", it) }
    }

    @Suppress("DEPRECATION")
    private fun Intent.parcelable(key: String): Uri? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) getParcelableExtra(key, Uri::class.java)
        else getParcelableExtra(key) as? Uri

    @Suppress("DEPRECATION")
    private fun Intent.parcelableList(key: String): List<Uri> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) getParcelableArrayListExtra(key, Uri::class.java).orEmpty()
        else getParcelableArrayListExtra<Uri>(key).orEmpty()
}
