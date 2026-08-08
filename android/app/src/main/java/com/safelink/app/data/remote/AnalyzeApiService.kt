package com.safelink.app.data.remote

import com.google.gson.Gson
import com.safelink.app.data.remote.dto.AnalyzeRequestDto
import com.safelink.app.data.remote.dto.AnalyzeResponseDto
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

/** data/API 입출력 .json의 POST /analyze 하나만 있는 서버. */
interface AnalyzeApiService {
    @POST("analyze")
    suspend fun analyze(@Body request: AnalyzeRequestDto): Response<AnalyzeResponseDto>
}

/**
 * Retrofit 인스턴스 생성. Hilt DI 그래프에 아직 안 태우고(DetectionRepository와 동일하게
 * 생성자 직접 호출 방식 유지) 필요한 곳에서 바로 만들어 쓴다.
 *
 * 기본 baseUrl은 Android 에뮬레이터에서 호스트 PC의 localhost(목 서버)를 가리키는
 * 10.0.2.2. 실제 배포 서버가 생기면 이 값만 바꾸면 됨(요청/응답 계약은 그대로).
 */
object AnalyzeApiClient {
    private const val DEFAULT_BASE_URL = "http://10.0.2.2:8000/"

    fun create(baseUrl: String = DEFAULT_BASE_URL): AnalyzeApiService {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(Gson()))
            .build()
            .create(AnalyzeApiService::class.java)
    }
}
