# 신기훈 4주차 결과물 02 - DetectionViewModel 연동 및 샘플 검증

> 01번 문서(`01_키워드매칭_위험도계산_추천기관_정리.md`)의 "0. 현재 상태: 엔진 구현 완료 /
> 앱 연동 전"에서 이어지는 작업. 이번 문서로 **앱 연동까지 완료**됨.

---

## 1. 제출 요약

- **작업명**: `DetectionViewModel`의 임시 매칭 로직을 `DetectionRepository`/`DetectionEngine`(v1)으로 교체
- **작업 목적**: 김재겸이 4주차에 만든 `DetectionViewModel`은 반복감쇠·콤보 보너스·추천기관
  게이팅이 없는 "1차 단순 매칭"이었고, 코드에 `TODO(신기훈 로직으로 교체)`가 명시돼 있었음.
  이번 작업으로 그 TODO를 실제로 해소함.
- **수정 파일**: `android/app/src/main/java/com/safelink/app/ui/screens/detection/DetectionViewModel.kt`
- **반영 내용**:
  - 자체 `KeywordEntry`/`loadKeywords()`/`recommendInstitutions()` 전부 제거
  - `private val repository by lazy { DetectionRepository(getApplication()) }` 추가
  - `analyze()`를 `result = repository.analyze(originalText)` 한 줄로 교체
  - Hilt를 아직 안 쓰는 `AndroidViewModel`이라 `DetectionRepository`(`@Inject constructor`)를
    생성자 직접 호출로 사용 (Hilt 마이그레이션은 이번 범위 밖)
- **이번 주 목표와 연결되는 이유**: `DetectionInput -> DetectionResult -> ResponseGuide` 흐름이
  이제 실제로 반복감쇠/콤보/게이팅까지 반영된 값으로 동작한다. `analyze()` 호출 시점·방식(동기,
  버튼 onClick)은 기존과 동일해서 `DetectionInputScreen.kt`는 수정 불필요.
- **남은 작업**: 4장 참고.

---

## 2. 샘플 입력 5개 + 실제 검증된 결과값

**검증 방법**: 이 저장소 환경엔 Android SDK가 없어 에뮬레이터로 직접 못 띄워봤다. 대신
`DetectionEngine`(엔진 본체, Android 의존성 없음)을 Android 밖에서 별도 컴파일해서 실제 실행—
`DetectionRepository`는 이 엔진에 assets 로딩만 얹은 얇은 래퍼라 결과는 동일하다. 5개 샘플을
**각각 2회씩** 돌려서 완전히 같은 결과(`DetectionResult` 전체 값 `==` 비교)가 나오는지도 같이
확인했다 → **5개 전부 결정론 통과** (동일 입력 → 동일 출력).

샘플 1~4는 이미 팀이 검증해둔 `DetectionResultDummyData`의 원문을 그대로 재사용했다 (새 문장
대신 검증된 문장을 씀 — 01번 문서에서 발견한 "키워드 부분 문자열 충돌" 같은 새로운 데이터
이슈를 피하기 위함). 샘플 5는 콤보 2개가 동시에 발동하는 경우를 보여주기 위해 추가.

### 샘플 1 — 보이스피싱 (`DetectionResultDummyData.vpCritical` 원문)

> "택배기사인데요, 배송 중 확인 차 연락드렸습니다. 그럼 명의 도용 우려가 있어서 확인이
> 필요합니다. 지금 당장 확인 안 하시면 계좌가 압류될 수 있습니다."

| 항목 | 값 |
|---|---|
| score | **82** (기존 더미 `vpCritical.score=82`와 정확히 일치) |
| riskLevel | CRITICAL |
| category | 보이스피싱 |
| appliedComboIds | [COMBO-GENERAL-3CAT] |
| matchedKeywords | 5건 (VP-1-1-001, VP-1-1-002, VP-1-2-002, VP-1-4-001, VP-1-4-004) |
| recommendedInstitutions | 5건 — rank1 경찰청(협박), 2 대한법률구조공단(협박), 3 검찰청(협박), 4 금융감독원(기관사칭), 5 개인정보보호위원회(개인정보탈취) |

### 샘플 2 — 로맨스스캠 (`DetectionResultDummyData.rsWarning` 원문)

> "지방 파견 나와 있어요, 무역회사 대표입니다. 진심으로 마음이 가요, 당신 생각을 안 할 수가
> 없어요. 이건 아무한테도 말하지 말아주세요, 우리 둘만 아는 걸로 해요."

| 항목 | 값 |
|---|---|
| score | **47** (기존 더미 `rsWarning.score=47`와 정확히 일치) |
| riskLevel | WARNING |
| category | 로맨스스캠 |
| appliedComboIds | [COMBO-GENERAL-3CAT] |
| matchedKeywords | 4건 (RS-2-1-001, RS-2-1-002, RS-2-2-002, RS-2-3-002) |
| recommendedInstitutions | 6건 — rank1 정신건강복지센터(심리조작), 2 여성인권진흥원(심리조작), 3 민간상담센터(심리조작), 4 금융감독원(금융사기), 5 경찰청(금융사기), 6 대한법률구조공단(금융사기) |

### 샘플 3 — 가스라이팅 (`DetectionResultDummyData.glCaution` 원문)

> "내가 언제 그렇게 말했어? 너는 항상 왜곡해서 기억하더라. 다 너를 위해서 하는 말이야, 진심이야."

| 항목 | 값 |
|---|---|
| score | **24** (기존 더미 `glCaution.score=24`와 정확히 일치) |
| riskLevel | CAUTION |
| category | 가스라이팅 |
| appliedComboIds | [] |
| matchedKeywords | 2건 (GL-3-2-001, GL-3-4-001) |
| recommendedInstitutions | 0건 — standalone_recommend=false(3-2, 3-4)이고 콤보 미발동·점수 24점(31점 미만)이라 게이팅 통과 못 함. **추천기관 없음이 정상 동작.** |

### 샘플 4 — 안전 (`DetectionResultDummyData.safeEmpty` 원문)

> "오늘 저녁에 뭐 먹을까? 나는 파스타 먹고 싶어."

| 항목 | 값 |
|---|---|
| score | **0** |
| riskLevel | SAFE |
| category | "" |
| isSafeAndEmpty | true |
| matchedKeywords / recommendedInstitutions | 0건 / 0건 |

### 샘플 5 — 콤보 2개 동시 발동 (다단계 사칭, 신규)

> "택배기사인데요, 배송 중 확인 차 연락드렸습니다. 그럼 명의 도용 우려가 있어서 확인이
> 필요합니다. 이 번호로 전화해보세요: 010-1234-5678"

| 항목 | 값 |
|---|---|
| score | **77** |
| riskLevel | CRITICAL |
| category | 보이스피싱 |
| appliedComboIds | [COMBO-GENERAL-3CAT, COMBO-VP-PHONE-VERIFY] (콤보 2개 동시 발동 확인용) |
| matchedKeywords | 5건 (VP-1-1-001, VP-1-1-002, VP-1-2-002, VP-1-3-002, VP-1-3-003) |
| recommendedInstitutions | 5건 — rank1 경찰청(기관사칭), 2 금융감독원(기관사칭), 3 검찰청(기관사칭), 4 개인정보보호위원회(개인정보탈취), 5 대한법률구조공단(협박) |

---

## 3. 참고: 기존 더미데이터와 달라지는 부분

샘플 1·2의 **`score`/`riskLevel`/`category`는 기존 `DetectionResultDummyData`와 정확히
일치**하지만, **`recommendedInstitutions`의 개수·구성은 다르다** (기존 더미는 3개로 손으로
추린 예시였고, 실제 엔진은 `subcategory_to_risk_type` → `risk_type_priority` 전체 규칙을
기계적으로 적용해서 5~6개가 나옴). 이는 **버그가 아니라 의도된 동작** — 더미데이터는
Preview용 손 예시였고, 이번 결과가 문서화된 resolve 규칙을 실제로 그대로 실행한 값이다.

---

## 4. 남은 작업

- 실제 기기/에뮬레이터에서 `DetectionInputScreen -> DetectionResultScreen` 눈으로 확인
  (이 환경엔 Android SDK가 없어 못 함 — 로컬 Android Studio에서 확인 필요)
- `institutions.json`이 5~6개까지 늘어난 추천기관을 결과 화면이 스크롤 없이도 자연스럽게
  보여주는지 UI 쪽 확인 필요 (김재겸 — 기존 더미 3개 기준으로 레이아웃 잡았을 수 있음)
- 01번 문서에 적은 데이터 이슈 2건(키워드 부분 문자열 충돌, 픽스처 불일치)은 여전히 팀 논의
  대기 중

---

## 5. 최종 연동 코드 (그대로 반영됨)

```kotlin
class DetectionViewModel(application: Application) : AndroidViewModel(application) {
    var originalText by mutableStateOf("")
    var inputMethod by mutableStateOf("텍스트 입력")
    var result by mutableStateOf<DetectionResult?>(null)
        private set

    private val repository: DetectionRepository by lazy { DetectionRepository(getApplication()) }

    fun analyze() {
        result = repository.analyze(originalText)
    }
}
```
