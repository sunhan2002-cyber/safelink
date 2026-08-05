# 신기훈 4주차 결과물 07 - AI API 실제 연동 + 목 서버

> 06번 문서까지는 "언제 AI를 불러야 하는가"(`shouldEscalateToAI`)만 구현돼 있었고, 실제
> 네트워크 호출은 없었음. 이번 작업으로 **호출 자체**까지 연결했다 — 실제 LLM은 아직
> 없어서 목(mock) 서버로 대체했지만, Android ↔ 서버 배선 자체는 실제로 동작한다.
>
> **v1.1 갱신**: 1차 리뷰 피드백 6건 반영 — 목서버 표시 강화, AI 응답 반영 범위 확정
> (요약/패턴/추천기관까지 병합), recentTurns 한계 명시, 실패 폴백 3경로 명확화, 문서
> 인코딩 재확인(UTF-8 정상 — 뷰어 쪽 문제였던 것으로 보임).

---

## 0. ⚠️ 이 문서가 설명하는 서버는 목(MOCK) 서버입니다

**실제 LLM을 호출하지 않습니다.** `backend/main.py`의 `analyze_context()` 함수는 "매칭된
키워드 개수가 3개 이상이면 +10점, 아니면 -5점"이라는 단순 규칙일 뿐, 문맥이나 의미를
전혀 분석하지 않습니다. 목적은 AI 분석 품질이 아니라 **Android ↔ 서버 연동 배선이 실제로
동작하는지 검증/시연**하는 것입니다.

**실제 AI로 교체할 때 갈아끼울 지점은 `analyze_context()` 함수 하나뿐**입니다. 함수
시그니처(`AnalyzeRequest -> AnalyzeResponse`)와 FastAPI 라우팅(`@app.post("/analyze")`)만
유지하면 Android 쪽 코드는 한 줄도 안 고쳐도 됩니다. `main.py` 상단과 `/health` 응답에도
`is_mock: true`로 명시해뒀습니다.

---

## 1. 제출 요약

- **작업명**: AI API 실제 연동 (Android 클라이언트 + 목 FastAPI 서버)
- **작업 목적**: "AI API 진입 조건은 됐는데 실제 호출은 안 붙어있다"는 공백을 메움 —
  월요일 체크리스트 "AI API 보조 분석을 붙일 인터페이스가 정리됐는지"를 실제 동작으로 증명
- **추가/수정 파일**:
  - `backend/main.py`, `requirements.txt`, `README.md` (목 FastAPI 서버, 목서버 표시 강화)
  - `android/app/src/main/java/.../data/remote/dto/AnalyzeDto.kt`
  - `android/app/src/main/java/.../data/remote/AnalyzeApiService.kt` (Retrofit)
  - `android/app/src/main/java/.../data/model/DetectionResult.kt` — `aiSummary`/`aiDetectedPattern` 필드 추가
  - `android/app/src/main/java/.../data/repository/DetectionEngine.kt` — `maskSensitiveInfo()`, `mergeAiResponse()` 추가
  - `android/app/src/main/java/.../data/repository/DetectionRepository.kt` — `escalateToAI()` (병합은 engine에 위임)
  - `android/app/src/main/java/.../ui/screens/detection/DetectionViewModel.kt` — 연결, recentTurns 한계 주석
  - `android/app/build.gradle.kts`, `libs.versions.toml` — Retrofit/OkHttp/coroutines 의존성
  - `android/app/src/main/AndroidManifest.xml` — INTERNET 권한, cleartext 허용(로컬 목서버용)
- **완료 기준과 연결**: `shouldEscalateToAI()`가 true를 반환한 뒤 실제로 서버를 호출하고,
  응답을 온디바이스 결과에 병합해서 결과를 갱신하는 것까지 **실제로 실행해서 확인**함 (5장).

---

## 2. 목 서버 (`backend/`)

`data/API 입출력 .json` 스키마를 FastAPI로 그대로 구현. 0장 참고 — 실제 LLM 아님.

```bash
cd backend
pip install -r requirements.txt
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

---

## 3. Android 쪽 연결 구조

```
DetectionViewModel.analyze()
  1. repository.analyze(originalText)          # 온디바이스, 항상 동기
  2. repository.shouldEscalateToAI(result, ...) # 06번 문서 5개 조건
  3. true면 viewModelScope.launch {
       repository.escalateToAI(result, sessionId, recentTurns)
     }
```

`escalateToAI()` 내부:

```
DetectionEngine.maskSensitiveInfo(text)   # keyword.json의 전화번호/URL regex 재사용
  -> AnalyzeRequestDto 조립
  -> AnalyzeApiService.analyze() 호출 (Retrofit, baseUrl 기본값 10.0.2.2:8000 = 에뮬레이터의 localhost)
  -> 성공: DetectionEngine.mergeAiResponse(온디바이스결과, 응답) 위임 (4장)
  -> 실패: 온디바이스 result 그대로 반환 (6장)
```

**마스킹은 새로 안 만들고 keyword.json의 기존 regex(VP-1-3-003 전화번호, VP-1-6-004 URL)를
그대로 재사용**했다 — 마스킹 규칙이 나중에 바뀌어도 keyword.json 하나만 고치면 됨.

---

## 4. AI 응답 반영 범위 (1차 리뷰 피드백 반영 — 확정)

서버 응답(`AnalyzeResponseDto`)의 필드별로 반영 여부를 결정했다. 병합 로직 전체는
`DetectionRepository`가 아니라 **`DetectionEngine.mergeAiResponse()`** 한 곳에 모아뒀다
(기관 조회에 필요한 `institutions.json` 데이터를 이미 갖고 있는 곳이라서).

| 응답 필드 | 반영 여부 | 방식 |
|---|---|---|
| `context_score_adjustment` | ✅ 반영 | `score += adjustment`, 0~100 clamp, `riskLevel`은 항상 `RiskLevel.fromScore(조정된 score)`로 재계산 |
| `context_analysis_summary` | ✅ 반영 | `DetectionResult.aiSummary`(신규 nullable 필드)에 그대로 저장 |
| `context_detected_pattern` | ✅ 반영 | `DetectionResult.aiDetectedPattern`(신규 nullable 필드)에 그대로 저장 |
| `recommended_institutions` | ✅ 반영(병합) | 아래 4-1 참고 |
| `recommended_level_override` | ❌ 반영 안 함 | 서버 3단계(낮음/중간/높음) ↔ 클라이언트 4단계(SAFE~CRITICAL) 자동 매핑이 모호함(예: "높음"이 WARNING인지 CRITICAL인지 점수 없이는 알 수 없음). CLAUDE.md 원칙("서버가 최종 위험도를 결정하면 원칙 위반") 그대로 — 응답에는 담겨오지만 자동 적용 안 함 |
| `matched_keyword_ids` | ❌ 반영 안 함 | 서버가 새로 찾은 키워드 id가 와도, 위치(startIndex/endIndex) 정보가 없어서 `MatchedKeyword`로 재구성 불가 — 이번 범위 밖(9장) |

`DetectionResult`에 추가된 2개 필드는 **기본값 `null`이라 기존 더미데이터·화면 코드는
전혀 안 깨짐** — AI 보조 분석이 실행된 경우에만 채워짐.

### 4-1. 추천기관 병합 방식 (`recommended_institutions`)

서버 응답은 `institution_id`/`rank`/`reason`/`matched_risk_type`만 주고 `name`/`contact`/
`group`은 없다(그건 온디바이스 `institutions.json`에만 있음). 그래서:

```
서버가 준 institution_id를 institutions.json에서 조회해서 RecommendedInstitutionUi로 완성
  -> 온디바이스 목록 + 서버 목록을 합침
  -> institution_id 기준 dedup (겹치면 온디바이스 쪽 reason을 유지 — 이미 화면에 붙어있던 근거이므로)
  -> 최종 배열 인덱스 기준으로 rank 1부터 재부여
```

목서버는 항상 `recommended_institutions: []`를 반환하므로(0장), 실제로 서버가 새 기관을
추천하는 상황은 아직 시연 데이터로는 못 만든다 — 그래서 `mergeAiResponse()`를 가짜 응답
객체로 직접 호출해서 **"온디바이스에 없던 기관이 실제로 추가되고, 중복 기관은 dedup되고,
rank가 1부터 순차로 재부여되는지"**를 별도로 검증했다 (5장).

---

## 5. 실제 실행 검증

이 환경엔 Android SDK가 없어 에뮬레이터로 직접 못 띄웠지만, `AnalyzeApiService`/
`AnalyzeApiClient`/`DetectionEngine`은 전부 Android 의존성이 없는 순수 Kotlin이라 —
**프로덕션 코드 그대로** 별도 컴파일해서, 실제로 띄운 목 서버에 진짜 HTTP 요청을 보내는
것까지 실행 확인했다.

```
1. 온디바이스 분석: score=77, riskLevel=CRITICAL
2. shouldEscalateToAI: false (77점은 회색지대 밖 - "명백히 높음"은 서버 호출 없이
   즉시 경고한다는 기존 원칙대로 정확히 동작)
3. 마스킹: "010-1234-5678" -> "[전화번호]" (원본 번호 완전히 제거됨 확인)
4. 실제 목서버 호출: HTTP 200, context_score_adjustment=10.0 응답 수신
5. mergeAiResponse() 실제 프로덕션 함수 호출:
   - score: 77 + 10 = 87
   - aiSummary/aiDetectedPattern: 응답 값 그대로 채워짐 확인
   - recommendedInstitutions: 서버가 빈 배열 -> 온디바이스 5건 그대로 유지 확인
6. 존재하지 않는 포트로 호출 -> 예외 발생 -> 온디바이스 결과 그대로 유지 (PASS)
7. mergeAiResponse()에 "온디바이스에 없던 기관 1개 + 이미 있는 기관 1개"를 포함한 가짜
   응답을 직접 넣어서: 새 기관 추가됨 / 중복 기관 dedup됨(reason은 온디바이스 값 유지) /
   rank가 1부터 중복 없이 순차 재부여됨 — 전부 확인 (PASS)
```

3~7번은 `shouldEscalateToAI`의 실제 판단과 무관하게 **메커니즘 자체**(마스킹→호출→병합→
폴백→기관병합)가 맞는지 독립적으로 확인한 것이고, 게이팅 판단 자체는 06번 문서의
`ShouldEscalateToAITest`(11개 케이스)로 이미 따로 검증돼 있음.

---

## 6. 실패 시 온디바이스 결과 유지 — 3가지 경로 (1차 리뷰 피드백 반영)

`DetectionRepository.escalateToAI()`는 아래 3가지 경우 전부 **`result`(원본 온디바이스
결과)를 그대로 반환**한다. 어떤 상황에서도 화면에 아무것도 안 뜨는 경우는 없다.

| # | 상황 | 코드에서 걸리는 지점 |
|---|---|---|
| 1 | HTTP 응답 실패 (4xx/5xx, 예: 429 호출한도초과·503 서버오류) | `!response.isSuccessful` |
| 2 | HTTP는 200인데 응답 바디가 비어있음(파싱 실패 등) | `body == null` |
| 3 | 네트워크 자체 실패 (타임아웃 5초·연결거부·DNS 실패 등) | `catch (e: Exception)` |

`CancellationException`만 예외적으로 다시 던진다(구조적 동시성 — 화면이 꺼지면서 코루틴이
정상적으로 취소되는 것까지 "실패"로 취급해서 억지로 결과를 만들면 안 되므로).

---

## 7. recentTurns / sessionTurnCount 한계 (1차 리뷰 피드백 반영)

**지금은 진짜 다중 턴 분석이 아니라 단일 입력 기준이다.** `DetectionViewModel.analyze()`는
`recentTurns`에 `listOf(originalText)`(원문 1개짜리 리스트), `sessionTurnCount`에 항상
`1`을 넘긴다 — 사용자가 대화 전체를 한 번에 붙여넣는 지금 화면 구조상 자연스러운 값이지만,
**"메시지가 하나씩 쌓이는 진짜 대화 세션"을 추적하는 게 아니다.**

이 한계 때문에 실질적으로 영향받는 것: 06번 문서의 AI 진입 조건 3번("FUTURE_FAKE·SUNK_COST
감지 + 세션 15턴 이상")은 **지금 구조에서는 sessionTurnCount가 항상 1이라 사실상 발동하지
않는다.** 실제 다중 턴 세션 추적(예: 대화 기록을 턴 단위로 누적하는 상태)이 생기면
`DetectionViewModel`의 이 두 값을 그 상태로 교체하면 되고, `escalateToAI()`/
`mergeAiResponse()`는 턴 개수와 무관하게 동작하므로 수정 불필요.

---

## 8. 남은 작업 / 알아둘 것

- **실제 배포 서버 없음** — `backend/`는 로컬 실행용 목 서버. 실제 AI(LLM) 연동, 배포,
  API 키 관리 등은 이번 범위 밖
- `android:usesCleartextTraffic="true"`는 로컬 HTTP 목서버 테스트용 — 실제 HTTPS 배포
  서버가 생기면 이 설정은 빼거나 network_security_config로 좁혀야 함
- `recentTurns`/`sessionTurnCount` 한계는 7장 참고 — 다중 턴 세션 추적 자체가 이번 범위 밖
- `recommended_level_override`/`matched_keyword_ids` 미반영 이유는 4장 표 참고 — 필요하면 팀 논의
- `429`(호출 한도 초과) 처리는 목서버에 아직 없음 — rate limit(시간당 20회)은 클라이언트
  쪽에서 별도로 세야 함(이번 범위 밖)
- 결과 화면에 `aiSummary`/`aiDetectedPattern`을 실제로 보여줄지/어떻게 보여줄지는 문구·UI
  결정이라 김우영·김재겸 쪽 판단 필요 (필드는 이미 채워지지만 화면에 아직 안 붙어있음)
