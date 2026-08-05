# Design.md — Safelink

> 본 문서는 Requirements.md를 기반으로 Safelink의 기술적 구현 방법을 정의합니다.
> 구현 시작 전 기술 검증의 기준이 되며, 변경 시 Requirements.md와 함께 업데이트합니다.

---

## 1. 기술 스택 (Tech Stack)

### Android 앱

| 항목 | 선택 | 이유 |
|------|------|------|
| 언어 | Kotlin | 간결한 문법, Android 공식 권장 언어 |
| UI | Jetpack Compose | 선언형 UI, 상태 기반 화면 재구성 용이 |
| 로컬 DB | Room DB | 메모·감지 기록 구조화 저장 |
| 설정 저장 | SharedPreferences (EncryptedSharedPreferences) | PIN·생체인증·설정값 등 단순 키-값 저장 |
| 네트워크 | Retrofit2 + OkHttp | 위험 감지 API 통신 |
| 직렬화 | Gson | JSON 파싱 (지원 기관 DB, API 응답) |
| 비동기 | Kotlin Coroutines + Flow | 비동기 처리, 실시간 DB 관찰 |
| 네비게이션 | Navigation Compose | 화면 간 이동 관리 |
| 생체인증 | BiometricPrompt API | 지문·얼굴 인증 |
| 알림 | NotificationManager + WorkManager | 배너 알림, 백그라운드 작업 |
| DI | Hilt | 의존성 주입 |

### Python 백엔드

| 항목 | 선택 | 이유 |
|------|------|------|
| 언어 | Python 3.11 | 텍스트 처리·정규식 생태계 풍부 |
| 프레임워크 | FastAPI | 비동기 처리, 자동 문서화(Swagger), 빠른 개발 |
| 서버 | Uvicorn | ASGI 서버, FastAPI 공식 권장 |
| 위험 감지 | 키워드 목록 + 정규식(re 모듈) | 대회 일정 내 구현 가능한 경량 방식 |
| 데이터 | JSON 파일 (keywords.json, institutions.json) | 팀 직접 구축, 별도 DB 서버 불필요 |

---

## 2. 전체 아키텍처 (Architecture Overview)

```
┌─────────────────────────────────────────┐
│              Android App                │
│                                         │
│  UI Layer (Jetpack Compose Screens)     │
│       ↕                                 │
│  ViewModel Layer (상태 관리)            │
│       ↕                                 │
│  Repository Layer (데이터 중개)         │
│       ↕              ↕                  │
│  Room DB         Retrofit API Client   │
│  (로컬 기록)      (위험 감지 요청)      │
└──────────────────────┬──────────────────┘
                       │ HTTP (JSON)
            ┌──────────▼──────────┐
            │   FastAPI 서버      │
            │                     │
            │  /analyze 엔드포인트│
            │  키워드·정규식 엔진  │
            │  keywords.json 로드  │
            └─────────────────────┘
```

**계층 설명**
- **UI Layer**: Compose Screen 함수들. ViewModel의 상태(State)를 관찰하여 화면 렌더링
- **ViewModel Layer**: 비즈니스 로직 처리, UI 상태 보관. Repository를 통해 데이터 접근
- **Repository Layer**: Room DB와 API Client를 통합하여 ViewModel에 단일 인터페이스 제공
- **FastAPI 서버**: 텍스트 분석 전용. 수신 텍스트를 분석 후 즉시 결과 반환, 저장하지 않음

---

## 3. 데이터 모델 (Data Model)

### 3.1 Room DB 엔티티

#### DiagnosisRecord — 자가진단 기록

```kotlin
@Entity(tableName = "diagnosis_records")
data class DiagnosisRecord(
    @PrimaryKey val id: String,          // UUID
    val timestamp: Long,                 // 진단 일시 (Unix timestamp)
    val riskLevel: RiskLevel,            // CAUTION / WARNING / CRITICAL
    val score: Int,                      // 위험 점수 (0~100)
    val checkedCount: Int                // 체크된 항목 수
    // 원문 항목 내용은 저장하지 않음 (최소 수집 원칙)
)
```

#### DetectionRecord — 위험 대화 감지 기록

```kotlin
@Entity(tableName = "detection_records")
data class DetectionRecord(
    @PrimaryKey val id: String,          // UUID
    val timestamp: Long,                 // 감지 일시
    val riskLevel: RiskLevel,            // CAUTION / WARNING / CRITICAL
    val detectedKeywordCount: Int,       // 탐지된 위험 키워드 수
    val detectionCategories: String      // 감지 유형 (JSON 직렬화, 예: ["협박","통제"])
    // 원문 텍스트는 저장하지 않음
)
```

#### Memo — 상황 기록 메모

```kotlin
@Entity(tableName = "memos")
data class Memo(
    @PrimaryKey val id: String,          // UUID
    val timestamp: Long,                 // 작성 일시
    val content: String                  // 메모 본문
)
```

#### RiskLevel — 위험도 공통 열거형

```kotlin
enum class RiskLevel {
    CAUTION,   // 주의
    WARNING,   // 경고
    CRITICAL   // 긴급
}
```

---

### 3.2 SharedPreferences 키 목록

> `EncryptedSharedPreferences` 사용. 민감 데이터는 암호화 저장.

| 키 | 타입 | 설명 | 기본값 |
|----|------|------|--------|
| `app_lock_enabled` | Boolean | 앱 잠금 활성화 여부 | false |
| `biometric_enabled` | Boolean | 생체인증 활성화 여부 | false |
| `pin_hash` | String | PIN 해시값 (SHA-256) | "" |
| `pin_fail_count` | Int | 연속 PIN 오류 횟수 | 0 |
| `pin_lock_until` | Long | PIN 잠금 해제 시각 (Unix timestamp) | 0 |
| `emergency_contact_name` | String | 긴급 연락처 이름 | "" |
| `emergency_contact_phone` | String | 긴급 연락처 전화번호 (암호화) | "" |
| `emergency_message` | String | 지인 문자 발송 내용 | "" |
| `neutral_notif_title` | String | 중립적 알림 제목 | "알림" |
| `neutral_notif_body` | String | 중립적 알림 본문 | "새로운 알림이 있습니다" |

---

### 3.3 FastAPI 요청/응답 스키마

#### POST /analyze — 텍스트 위험 분석 요청

```python
# Request
class AnalyzeRequest(BaseModel):
    text: str                   # 분석 요청 텍스트 (최대 5,000자)

# Response
class AnalyzeResponse(BaseModel):
    risk_level: str             # "CAUTION" | "WARNING" | "CRITICAL" | "NONE"
    score: int                  # 위험 점수 (0~100)
    detected_categories: list[str]   # 감지 유형 목록 (예: ["협박", "통제"])
    flagged_phrases: list[FlaggedPhrase]  # 위험 문장 목록

class FlaggedPhrase(BaseModel):
    text: str                   # 위험 문장 원문
    category: str               # 분류 (협박 / 통제 / 반복압박 / 고립)
    risk_level: str             # 해당 문장의 위험도
    start_index: int            # 원문 내 시작 위치 (강조 표시용)
    end_index: int              # 원문 내 종료 위치
```

---

### 3.4 지원 기관 JSON DB 구조 (institutions.json)

```json
{
  "institutions": [
    {
      "id": "inst_001",
      "name": "여성긴급전화",
      "phone": "1366",
      "categories": ["가정폭력", "데이트폭력", "스토킹"],
      "hours": "24시간",
      "target": "여성 피해자",
      "description": "위기 상황 긴급 지원 및 상담",
      "has_application_guide": false
    }
  ]
}
```

---

## 4. 화면 구조 및 네비게이션 (Screen Architecture)

### 4.1 화면 목록

| 화면 ID | 화면명 | 주요 기능 |
|---------|--------|-----------|
| `LockScreen` | 잠금 화면 | PIN 입력 / 생체인증 |
| `HomeScreen` | 메인 화면 | 기능 진입점, 최근 기록 요약 |
| `DiagnosisScreen` | 자가진단 화면 | 체크리스트 표시 및 선택 |
| `DiagnosisResultScreen` | 자가진단 결과 화면 | 위험도 결과 표시, 다음 행동 연결 |
| `DetectionInputScreen` | 대화 감지 입력 화면 | 텍스트 입력 / 클립보드 불러오기 |
| `DetectionResultScreen` | 대화 감지 결과 화면 | 위험 문장 강조, 위험도 표시 |
| `ResponseGuideScreen` | 대응 가이드 화면 | 위험도별 행동 지침 |
| `SupportMatchScreen` | 맞춤 지원 매칭 화면 | 유형 선택 후 기관 목록 표시 |
| `SupportDetailScreen` | 기관 상세 화면 | 기관 정보, 전화 연결 |
| `ApplicationGuideScreen` | 신청 동행 화면 | 단계별 신청 절차 안내 |
| `EmergencyScreen` | 긴급 도움 요청 화면 | 공공기관 전화, 지인 문자 |
| `RecordListScreen` | 기록 목록 화면 | 감지·진단 기록 최신순 조회 |
| `MemoEditScreen` | 메모 작성 화면 | 상황 메모 작성·저장 |
| `SettingsScreen` | 설정 화면 | 보안, 긴급 연락처, 알림 문구 설정 |

### 4.2 네비게이션 구조

```
앱 시작
  └─ LockScreen (잠금 활성화 시)
        └─ 인증 성공
              └─ HomeScreen ◀─ Bottom Navigation ─▶ RecordListScreen / SettingsScreen
                    ├─ DiagnosisScreen ──▶ DiagnosisResultScreen
                    │                          ├──▶ ResponseGuideScreen
                    │                          └──▶ SupportMatchScreen
                    ├─ DetectionInputScreen ──▶ DetectionResultScreen
                    │                               └──▶ ResponseGuideScreen
                    ├─ SupportMatchScreen ──▶ SupportDetailScreen
                    │                               └──▶ ApplicationGuideScreen
                    └─ EmergencyScreen  ← (긴급 알림 탭 또는 대응 가이드에서 진입)
```

**Bottom Navigation 탭 3개**
- 홈 (HomeScreen)
- 기록 (RecordListScreen)
- 설정 (SettingsScreen)

> EmergencyScreen은 Bottom Navigation에 포함하지 않음. 항상 빠른 진입 경로(알림, 버튼)로만 접근.

---

## 5. 핵심 비즈니스 로직 (Core Logic)

### 5.1 자가진단 위험도 산출

```
총 문항 수: N개 (체크리스트 항목)
각 항목은 가중치 보유: 일반 항목(1점) / 고위험 항목(2점)
최대 점수 = 전체 가중치 합

위험도 분류:
  score >= 70% → CRITICAL (긴급)
  score >= 40% → WARNING  (경고)
  score >= 10% → CAUTION  (주의)
  score <  10% → 위험 없음
```

### 5.2 위험 대화 감지 파이프라인 (FastAPI)

```
1. 텍스트 수신 (POST /analyze)
2. 텍스트 전처리: 공백 정규화, 길이 검증 (5,000자 초과 시 거부)
3. 카테고리별 키워드 매칭
   - 협박: "죽여버", "가만 안 둬", "후회하게 해줄게" 등
   - 통제: "어디 있어", "누구 만났어", "폰 보여줘" 등
   - 반복압박: 동일 유형 키워드 3회 이상 반복 패턴
   - 고립: "가족한테 말하지 마", "친구 만나지 마" 등
4. 정규식 보조: 패턴 변형 탐지 (예: 띄어쓰기 변형, ㅈㄱ 초성 등)
5. 위험도 점수 산출
   - 각 키워드 카테고리별 가중치 합산
   - CRITICAL: 협박 카테고리 탐지 또는 총점 70 이상
   - WARNING: 통제 + 고립 복합 탐지 또는 총점 40 이상
   - CAUTION: 단일 카테고리 탐지 또는 총점 10 이상
6. 결과 반환 (텍스트 서버 저장 없음, 즉시 응답)
```

### 5.3 배너 알림 발송 로직

```
위험 감지 결과 수신
  └─ CAUTION  → 무음 알림 (NotificationCompat.PRIORITY_LOW)
  └─ WARNING  → 진동 알림 (NotificationCompat.PRIORITY_DEFAULT)
  └─ CRITICAL → 진동 + 알림음 (NotificationCompat.PRIORITY_HIGH)
                + 알림 탭 시 EmergencyScreen 직접 이동

알림 채널 ID: "safelink_alert" (외부 노출명: 중립 문구로 설정)
알림 제목/본문: SharedPreferences neutral_notif_title / neutral_notif_body 값 사용
```

### 5.4 PIN 잠금 로직

```
앱 포그라운드 진입 시:
  └─ app_lock_enabled == true?
        ├─ YES → LockScreen 표시
        │         └─ 입력 PIN hash == pin_hash?
        │               ├─ YES → 홈 진입, pin_fail_count = 0 초기화
        │               └─ NO  → pin_fail_count += 1
        │                         └─ pin_fail_count >= 5?
        │                               └─ YES → pin_lock_until = 현재 + 30초 설정, 입력 차단
        └─ NO  → 홈 진입
```

### 5.5 긴급 도움 요청 로직

```
공공기관 전화 버튼 탭
  └─ Intent(ACTION_DIAL, "tel:1366") 또는 "tel:112" 실행

지인 문자 버튼 탭
  └─ emergency_contact_phone 존재?
        ├─ YES → Intent(ACTION_SENDTO, "smsto:{phone}")
        │         body = emergency_message 값으로 문자 앱 실행
        └─ NO  → 긴급 연락처 등록 안내 팝업 표시 → SettingsScreen 이동
```

---

## 6. 보안 설계 (Security Design)

| 항목 | 설계 |
|------|------|
| 로컬 저장 암호화 | `EncryptedSharedPreferences` (AES-256-GCM) 사용 |
| PIN 저장 | 원문 저장 금지. SHA-256 해시만 저장 |
| 텍스트 미저장 | FastAPI 서버: 수신 텍스트를 DB·로그에 기록하지 않음. 요청 처리 후 메모리 즉시 해제 |
| 앱 아이콘·이름 | 앱 목적 유추 불가한 중립적 이름·아이콘 사용 (추후 팀 결정) |
| 알림 문구 | 위험·폭력·상담 등 민감 단어 포함 금지. 설정 화면에서 사용자가 직접 수정 가능 |
| Room DB 암호화 | SQLCipher 적용 검토 (Sprint 5~6 구현 시 결정) |
| 네트워크 | HTTPS 통신 강제 (HTTP 차단), API 서버 주소 코드 외부 노출 금지 |

---

## 7. 폴더 구조 (Directory Structure)

### Android

```
app/
├── data/
│   ├── local/
│   │   ├── db/          # Room DB (Entities, DAO, Database)
│   │   └── prefs/       # SharedPreferences 접근 클래스
│   ├── remote/
│   │   ├── api/         # Retrofit API 인터페이스
│   │   └── model/       # 요청/응답 데이터 클래스
│   └── repository/      # Repository 구현체
├── domain/
│   ├── model/           # RiskLevel 등 도메인 모델
│   └── usecase/         # 핵심 비즈니스 로직 UseCase
├── ui/
│   ├── screen/          # 화면별 Composable 함수
│   ├── component/       # 공통 UI 컴포넌트
│   ├── theme/           # 색상, 타이포그래피
│   └── navigation/      # NavGraph 정의
├── service/             # 백그라운드 서비스, 알림 관련
└── di/                  # Hilt 모듈
```

### Python 백엔드

```
backend/
├── main.py              # FastAPI 앱 진입점
├── router/
│   └── analyze.py       # /analyze 엔드포인트
├── service/
│   └── detector.py      # 위험 감지 핵심 로직
├── data/
│   ├── keywords.json    # 키워드·규칙 정의
│   └── institutions.json # 지원 기관 DB
└── model/
    └── schema.py        # 요청/응답 Pydantic 스키마
```

---

*문서 버전: v1.0 | 작성일: 2026-05-26 | 작성자: Safelink 팀*
