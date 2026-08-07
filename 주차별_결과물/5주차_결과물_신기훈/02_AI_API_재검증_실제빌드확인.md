# 신기훈 5주차 결과물 02 - AI API 흐름 재검증 + 실제 빌드 확인

> 신기훈 담당 항목: "AI API 실제 호출/응답/fallback/결과 병합 완성".
> **완료 기준**: "AI API가 실제 앱 결과에 반영돼야 함".

---

## 1. 왜 다시 검증했나

01번 문서에서 `shouldEscalateToAI()`의 조건을 5개→3개로 줄였다(`sessionTurnCount`
파라미터도 제거). 이건 함수 시그니처가 바뀐 변경이라, "AI 호출 여부 판단 → 실제 호출 →
병합 → 실패 시 폴백"으로 이어지는 전체 흐름이 여전히 정상 동작하는지 처음부터 다시
확인해야 했다.

## 2. 회색지대 케이스 end-to-end 재검증

목 서버(`backend/`)를 실제로 띄우고, 회색지대 점수(TC-VP-EDGE-01과 동일, score=37)로
실제 흐름을 처음부터 끝까지 실행했다:

```
1. 온디바이스 분석: score=37 (회색지대 20-40)
2. shouldEscalateToAI(result)  <- sessionTurnCount 파라미터 없이 호출
   결과: true
3. 마스킹 -> 실제 HTTP POST http://127.0.0.1:8000/analyze -> 200 OK
4. mergeAiResponse(): score 37 -> 47, riskLevel CAUTION -> WARNING,
   aiSummary/aiDetectedPattern 채워짐
```

파라미터 정리 이후에도 회색지대 트리거 → 실제 네트워크 호출 → 병합까지 문제없이 동작.

## 3. 기존 검증 항목 재확인

4주차에 이미 검증했던 항목들도 5주차 코드 변경(shouldEscalateToAI 정리, regex-complex,
numeric_ratio_pattern) 이후 다시 실행해서 전부 이상 없음을 확인:

- 마스킹(`[전화번호]`/`[링크]`) 정상 동작
- 네트워크 실패(존재하지 않는 포트) 시 온디바이스 결과 그대로 유지
- 추천기관 병합(dedup + rank 재부여) 정상 동작

## 4. 처음으로 실제 Android 빌드 확인

01번 문서까지 계속 "이 저장소에 Android SDK가 없어 `./gradlew` 실측을 못 함"이 남은
작업으로 적혀 있었다. 이번에 로컬에 Android SDK가 설치돼 있는 걸 확인해서 **최초로 실제
Gradle 빌드**를 돌렸다.

```
JAVA_HOME=".../Android Studio/jbr" ./gradlew :app:testDebugUnitTest
-> BUILD SUCCESSFUL, 68 tests, 0 failures

JAVA_HOME=".../Android Studio/jbr" ./gradlew :app:assembleDebug
-> BUILD SUCCESSFUL, 디버그 APK 정상 패키징
```

`assembleDebug`는 신기훈 코드뿐 아니라 **김재겸/김선한/김우영이 최근에 반영한 UI·문구
변경분까지 전부 포함된 상태**에서 돌렸다 — 즉 팀 전체 코드가 지금 시점에 실제로 함께
빌드된다는 걸 확인한 것.

### 4-1. 실제 빌드에서만 드러난 문제 2건

스탠드얼론 `kotlinc` 검증(Android SDK 없이 순수 Kotlin만 컴파일하는 방식, 4주차부터 계속
써온 방법)으로는 못 잡았던 문제를 실제 Gradle 빌드가 잡아냈다:

1. `DataIntegrityTest.kt`의 "keyword json 80개 항목 전수 매칭 커버리지" 테스트가 실패함 —
   이 테스트는 `match_type`이 `keyword`가 아닌 항목마다 검증용 샘플 문장을 하드코딩된
   맵(`regexSamples`)에서 찾는데, 이번 주 신설한 `RS-2-9-003`(regex-complex)이 그 맵에
   없어서 "샘플 없음" 실패. 맵에 추가해서 해결.
2. `RegexComplexNumericPatternTest.kt`의 테스트 함수명에 `%` 문자가 들어있어서 Gradle이
   "Windows에서 문제될 수 있는 문자"로 경고. 함수명에서 제거.

두 문제 다 로직 버그는 아니고 테스트 코드 자체의 사소한 정리였지만, **실제 빌드를 돌리지
않았으면 계속 몰랐을 것들**이라 이번에 실측한 의미가 있었다.

## 5. `local.properties`

`android/local.properties`에 로컬 SDK 경로를 추가했다 — `.gitignore`에 이미 등록돼 있어
커밋 대상 아님(팀원마다 SDK 설치 경로가 다르므로 각자 로컬에서 만들어야 함).

## 6. 남은 작업

- 실제 기기/에뮬레이터에서 화면을 띄워서 눈으로 확인하는 것까지는 이번에 안 함(빌드
  성공까지만 확인) — 필요하면 다음 단계
- `429`(호출 한도 초과) 등 목서버에 없는 에러코드 처리, 실제 배포 서버 전환은 여전히 이번
  범위 밖(07번 문서 "남은 작업" 그대로 유효)
