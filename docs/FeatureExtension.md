# 기능 확장 구조 — 스크린샷 분석 · 백그라운드 감지 · 알림

> 작성: 김재겸 | 브랜치: `feature/screenshot-background`
> 목적: 스크린샷 분석 흐름을 앱에 붙이고, 백그라운드 감지·알림이 **어디에 들어갈지** 코드 구조를 확정.

---

## 1. 스크린샷 분석 흐름 (초안 구현 완료)

기존 "대화 분석" 화면의 **스크린샷 업로드 모드가 실제로 동작**하도록 구현했다.

```
DetectionInput(스크린샷 모드)
  └ 갤러리에서 스크린샷 선택 (Android PhotoPicker, 권한 불필요)
  └ 선택 썸네일 표시 + 개별 삭제
  └ [분석 시작하기]
        └ OcrService.extractText()  ← OCR (지금은 임시 구조)
        └ 추출 텍스트 → originalText
  ↓
Analyzing → DetectionResult → ResponseGuide  (기존 텍스트 흐름과 동일하게 합류)
```

**핵심: 텍스트 입력과 스크린샷 입력이 `originalText` 한 점으로 합류**한다. 그래서 OCR만 붙이면 스크린샷도 텍스트와 똑같이 기존 분석 엔진을 탄다.

### OCR 임시 구조 → 실제 교체 지점

| 구분 | 파일 | 역할 |
|---|---|---|
| 인터페이스 | `data/ocr/OcrService.kt` | 스크린샷 → 텍스트 추상화 |
| 임시 구현 | `data/ocr/StubOcrService.kt` | 지금: 예시 텍스트 반환 (흐름 시연용) |
| 실제 구현 (예정) | `MlKitOcrService` | ML Kit 한국어 OCR |

**OCR 연결 방법 (다음 단계):**
1. `build.gradle`: `com.google.mlkit:text-recognition-korean` 추가
2. `MlKitOcrService` 구현 — 각 이미지 URI → `InputImage` → `recognizer.process()` → 텍스트 병합
3. `DetectionViewModel.ocrService` 를 교체 (한 줄)
4. 비동기라 `runOcrOnSelectedImages()` 를 `suspend` + `viewModelScope` + 로딩 상태로 변경

> 즉 **화면·분석 코드는 그대로 두고 `OcrService` 구현만 갈아끼우면** 실제 OCR로 전환된다.

---

## 2. 백그라운드 감지 구조 (자리 확보 완료)

### 왜 AccessibilityService인가
안드로이드에서 **다른 앱(카카오톡·문자)의 화면 텍스트를 백그라운드로 읽는 표준·합법 경로**는 접근성 서비스뿐이다. 사용자가 [설정 > 접근성]에서 직접 켜야 동작하므로 프라이버시·정책상 안전하다.

### 감지 → 분석 → 알림 흐름과 각 코드 위치

```
[다른 앱: 카카오톡에 새 메시지]
   ↓ 화면 변화 이벤트
MessageDetectionService.onAccessibilityEvent()      background/MessageDetectionService.kt
   ├ 1. 감지 대상 앱인지 필터 (MONITORED_PACKAGES)
   ├ 2. extractVisibleText()  ── 화면 텍스트 추출        [TODO]
   ├ 3. DetectionRepository.analyze(text)  ── 온디바이스 분석  [기존 엔진 재사용]
   └ 4. 경고 이상이면 RiskNotifier.notifyRisk()          notification/RiskNotifier.kt
           ↓ 배너 알림
        [알림 탭] → 대응 가이드 / 긴급 화면 딥링크          [TODO: Task 6.15]
```

### 이번 주에 잡은 것 (스켈레톤)

| 파일 | 상태 | 내용 |
|---|---|---|
| `background/MessageDetectionService.kt` | 스켈레톤 | 접근성 서비스, 감지→분석→알림 골격. 텍스트 추출은 TODO |
| `notification/RiskNotifier.kt` | 동작 | 위험도별 배너 알림 발송 (중립 문구, 우선순위 분기) |
| `res/xml/accessibility_service_config.xml` | 완료 | 서비스 설정 |
| `AndroidManifest.xml` | 완료 | 서비스 선언 + POST_NOTIFICATIONS 권한 |

### 남은 구현 (다음 단계 TODO)
- `extractVisibleText()`: `rootInActiveWindow` 노드 트리 순회 → 대화 텍스트 수집
- 중복 감지 억제(디바운스), 세션 누적 점수
- 접근성 권한 온보딩 UI (설정으로 유도)
- 알림 딥링크 (위험도별 화면 이동, Task 6.15)
- POST_NOTIFICATIONS 런타임 권한 요청 UI (Android 13+)

---

## 3. 완료 기준 대비

| 완료 기준 | 상태 |
|---|---|
| 스크린샷 분석 기능 흐름이 앱 안에서 보이기 시작 | ✅ 갤러리 선택→썸네일→분석→결과 동작 (OCR 임시) |
| 백그라운드 감지가 어디에 들어갈지 코드 구조 확보 | ✅ 서비스·알림 스켈레톤 + 흐름 문서화 |

## 4. 브랜치 운영
- 작업 브랜치: `feature/screenshot-background`
- `main`(발표 기준 안정본)과 분리 → 확장 기능이 데모 흐름을 깨지 않도록
- 검증 후 팀 리뷰 거쳐 `main` 병합 예정
