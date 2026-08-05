# 신기훈 4주차 결과물 07 - AI API 실제 연동 + 목 서버

> 06번 문서까지는 "언제 AI를 불러야 하는가"(`shouldEscalateToAI`)만 구현돼 있었고, 실제
> 네트워크 호출은 없었음. 이번 작업으로 **호출 자체**까지 연결했다 — 실제 LLM은 아직
> 없어서 목(mock) 서버로 대체했지만, Android ↔ 서버 배선 자체는 실제로 동작한다.

---

## 1. 제출 요약

- **작업명**: AI API 실제 연동 (Android 클라이언트 + 목 FastAPI 서버)
- **작업 목적**: "AI API 진입 조건은 됐는데 실제 호출은 안 붙어있다"는 공백을 메움 —
  월요일 체크리스트 "AI API 보조 분석을 붙일 인터페이스가 정리됐는지"를 실제 동작으로 증명
- **추가 파일**:
  - `backend/main.py`, `requirements.txt`, `README.md` (신규 — 목 FastAPI 서버)
  - `android/app/src/main/java/.../data/remote/dto/AnalyzeDto.kt` (신규)
  - `android/app/src/main/java/.../data/remote/AnalyzeApiService.kt` (신규 — Retrofit)
  - `android/app/src/main/java/.../data/repository/DetectionEngine.kt` — `maskSensitiveInfo()` 추가
  - `android/app/src/main/java/.../data/repository/DetectionRepository.kt` — `escalateToAI()` 추가
  - `android/app/src/main/java/.../ui/screens/detection/DetectionViewModel.kt` — 연결
  - `android/app/build.gradle.kts`, `libs.versions.toml` — Retrofit/OkHttp/coroutines 의존성
  - `android/app/src/main/AndroidManifest.xml` — INTERNET 권한, cleartext 허용(로컬 목서버용)
- **완료 기준과 연결**: `shouldEscalateToAI()`가 true를 반환한 뒤 실제로 서버를 호출하고,
  응답의 `context_score_adjustment`를 온디바이스 점수에 반영해서 결과를 갱신하는 것까지
  **실제로 실행해서 확인**함 (4장).

---

## 2. 목 서버 (`backend/`)

`data/API 입출력 .json` 스키마를 FastAPI로 그대로 구현. 진짜 LLM은 안 부르고, 매칭된
키워드 수 기준 규칙(3개 이상이면 +10, 아니면 -5)으로 그럴듯한 응답을 만든다 — **목적은
문맥 분석 품질이 아니라 "서버가 실제로 응답하고 Android가 그걸 실제로 받아서 반영하는
배선"을 시연 가능하게 만드는 것**. 실제 AI로 바꿀 땐 `main.py`의 `analyze_context()`
함수 내부만 교체하면 되고, 스키마/라우팅은 그대로 유지된다.

```bash
cd backend
pip install -r requirements.txt
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

## 3. Android 쪽 연결 구조

```
DetectionViewModel.analyze()
  1. repository.analyze(originalText)          # 온디바이스, 항상 동기
  2. repository.shouldEscalateToAI(result, ...) # 06번 문서 5개 조건
  3. true면 viewModelScope.launch {
       repository.escalateToAI(result, sessionId, recentTurns)  # 신규
     }
```

`escalateToAI()` 내부:

```
DetectionEngine.maskSensitiveInfo(text)   # keyword.json의 전화번호/URL regex 재사용
  -> AnalyzeRequestDto 조립
  -> AnalyzeApiService.analyze() 호출 (Retrofit, baseUrl 기본값 10.0.2.2:8000 = 에뮬레이터의 localhost)
  -> 성공: score += context_score_adjustment (0~100 clamp), riskLevel 재계산
  -> 실패(네트워크/타임아웃/4xx/5xx): 온디바이스 result 그대로 반환
```

**마스킹은 새로 안 만들고 keyword.json의 기존 regex(VP-1-3-003 전화번호, VP-1-6-004 URL)를
그대로 재사용**했다 — 마스킹 규칙이 나중에 바뀌어도 keyword.json 하나만 고치면 됨.

**`recommended_level_override`는 이번에 자동 반영 안 함**: 서버는 3단계(낮음/중간/높음),
클라이언트 `RiskLevel`은 4단계라 자동 매핑이 모호함(예: "높음"이 WARNING인지 CRITICAL인지
점수 없이는 알 수 없음). CLAUDE.md 원칙("서버가 최종 위험도를 결정하면 원칙 위반") 및
API 문서의 "기본은 null, 클라이언트 판단 존중"과 같은 방향 — 점수 보정치만 자동 반영하고
override는 응답에 담아만 오게 해뒀다(추후 필요시 화면에 참고용으로 노출하는 건 김재겸 쪽 UI 결정).

---

## 4. 실제 실행 검증

이 환경엔 Android SDK가 없어 에뮬레이터로 직접 못 띄웠지만, `AnalyzeApiService`/
`AnalyzeApiClient`/`DetectionEngine`은 전부 Android 의존성이 없는 순수 Kotlin이라 —
**프로덕션 코드 그대로** 별도 컴파일해서, 실제로 띄운 목 서버에 진짜 HTTP 요청을 보내는
것까지 실행 확인했다.

```
1. 온디바이스 분석: score=77, riskLevel=CRITICAL
2. shouldEscalateToAI: false (77점은 회색지대 밖 + 매칭된 subcategory가 게이팅 대상 아님 -
   "명백히 높음"은 서버 호출 없이 즉시 경고한다는 기존 원칙대로 정확히 동작)
3. 마스킹: "010-1234-5678" -> "[전화번호]" (원본 번호 완전히 제거됨 확인)
4. 실제 목서버 호출: HTTP 200, context_score_adjustment=10.0 응답 수신
5. 병합: 77 + 10 = 87, riskLevel 재계산 정상
6. 존재하지 않는 포트로 호출 -> 예외 발생 -> 온디바이스 결과 그대로 유지 (PASS)
```

3~6번은 `shouldEscalateToAI`의 실제 판단과 무관하게 **메커니즘 자체**(마스킹→호출→병합→
폴백)가 맞는지 독립적으로 확인한 것이고, 게이팅 판단 자체는 06번 문서의
`ShouldEscalateToAITest`(11개 케이스)로 이미 따로 검증돼 있음 — 두 검증을 합치면 실제
앱에서 두 로직이 이어졌을 때의 전체 흐름도 안전하다고 볼 수 있음.

---

## 5. 남은 작업 / 알아둘 것

- **실제 배포 서버 없음** — `backend/`는 로컬 실행용 목 서버. 실제 AI(LLM) 연동, 배포,
  API 키 관리 등은 이번 범위 밖
- `android:usesCleartextTraffic="true"`는 로컬 HTTP 목서버 테스트용 — 실제 HTTPS 배포
  서버가 생기면 이 설정은 빼거나 network_security_config로 좁혀야 함
- `sessionTurnCount`가 지금은 항상 1로 고정(단일 입력 = 세션 1턴 취급) — 06번 문서에
  이미 남겨둔 이슈와 동일, 실제 다중 턴 세션 추적이 생기면 같이 교체 필요
- `recommended_institutions`(서버가 추천하는 기관)는 이번엔 항상 빈 배열로 두고
  merge 로직에서 다루지 않음 — 온디바이스 추천 기관을 그대로 유지. 서버 쪽 기관 추천을
  실제로 병합하려면 institution_id -> name/contact/group 조회가 필요해서 별도 작업 필요
- `recommended_level_override` 반영 여부는 3장에서 설명한 이유로 보류 — 필요하면 팀 논의
- `429`(호출 한도 초과) 처리는 목서버에 아직 없음 — rate limit(시간당 20회)은 클라이언트
  쪽에서 별도로 세야 함(이번 범위 밖)
