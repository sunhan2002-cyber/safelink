# Tasks.md — Safelink

> 본 문서는 Design.md의 설계를 바탕으로 스프린트별 세부 구현 작업을 정의합니다.
> 각 Task는 1인이 1주 내에 완료할 수 있는 단위로 분리되었습니다.

**역할 태그**
- `[Android]` 김재겸 — Kotlin/Compose 앱 구현
- `[백엔드]` 김재겸 — FastAPI 서버 구현
- `[UI/UX]` 신기훈 — Figma 와이어프레임, 디자인
- `[기획]` 김우영 — 문서, 데이터 수집, 기획

---

## Sprint 1: 기획 확정 | 6/16(화) ~ 6/22(월)

> 목표: 문제 정의 확정, 핵심 사용자·시나리오 정리

### 기획

- [ ] Task 1.1 `[기획]` 사용자 페르소나 2~3개 작성 (나이·상황·목적 포함)
- [ ] Task 1.2 `[기획]` 위험 대화 예시 수집 — 카테고리별(협박/통제/반복압박/고립) 최소 10개씩
- [ ] Task 1.3 `[기획]` 앱 핵심 가치 문장 1~2개 정리 (대회 소개용)
- [ ] Task 1.4 `[기획]` 필수 기능 / 선택 기능 최종 확정 및 문서화
- [ ] Task 1.5 `[기획]` 자가진단 체크리스트 항목 초안 작성 (10개 이상, 가중치 구분)

### 전체

- [ ] Task 1.6 `[전체]` Requirements.md 팀원 전체 검토 및 수정 사항 반영

---

## Sprint 2: 서비스 설계 | 6/23(화) ~ 6/29(월)

> 목표: 앱 구조와 사용자 흐름 확정

### 기획

- [ ] Task 2.1 `[기획]` 전체 사용자 플로우 작성 (정상 흐름 + 긴급 흐름)
- [ ] Task 2.2 `[기획]` 화면 목록 확정 (Design.md 기준 14개 화면 검토)
- [ ] Task 2.3 `[기획]` 위험 감지 → 알림 → 대응 가이드 → 지원 연결 전체 흐름 설계
- [ ] Task 2.4 `[기획]` 배너 알림 3단계 시나리오 설계 (주의/경고/긴급 각 상황 기술)
- [ ] Task 2.5 `[기획]` 중립적 알림 문구 5개 이상 초안 작성

### UI/UX

- [ ] Task 2.6 `[UI/UX]` 앱 전체 색상 시스템 초안 결정 (위험도별 색상 포함)

### 전체

- [ ] Task 2.7 `[전체]` Design.md 팀원 전체 검토 및 수정 사항 반영

---

## Sprint 3: 화면 기획 + 데이터 설계 | 6/30(화) ~ 7/6(월)

> 목표: 구현 직전 필요한 화면·데이터 구조 정리

### UI/UX

- [ ] Task 3.1 `[UI/UX]` 와이어프레임 제작 — HomeScreen, DiagnosisScreen, DiagnosisResultScreen
- [ ] Task 3.2 `[UI/UX]` 와이어프레임 제작 — DetectionInputScreen, DetectionResultScreen, ResponseGuideScreen
- [ ] Task 3.3 `[UI/UX]` 와이어프레임 제작 — SupportMatchScreen, SupportDetailScreen, ApplicationGuideScreen
- [ ] Task 3.4 `[UI/UX]` 와이어프레임 제작 — EmergencyScreen, RecordListScreen, MemoEditScreen, SettingsScreen, LockScreen
- [ ] Task 3.5 `[UI/UX]` Compose Theme 토큰 정의 (색상, 타이포그래피, 간격)

### 기획

- [ ] Task 3.6 `[기획]` 자가진단 체크리스트 항목 확정 + 가중치 수치 확정
- [ ] Task 3.7 `[기획]` institutions.json 초안 작성 — 상담 기관 10개 이상 수집 (이름/전화/운영시간/대상/유형)
- [ ] Task 3.8 `[기획]` keywords.json 초안 작성 — 카테고리별 위험 키워드 목록 + 가중치

### Android / 백엔드

- [ ] Task 3.9 `[Android]` GitHub 레포지토리 Android 프로젝트 초기 구성 (빈 Compose 프로젝트)
- [ ] Task 3.10 `[백엔드]` GitHub 레포지토리 FastAPI 프로젝트 초기 구성 (main.py, requirements.txt)
- [ ] Task 3.11 `[Android]` `[백엔드]` API 엔드포인트 명세 최종 확인 (Design.md 스키마 검토)

### 전체

- [ ] Task 3.12 `[전체]` Tasks.md 팀원 전체 검토 및 수정 사항 반영

---

## Sprint 4: 기본 화면 개발 | 7/7(화) ~ 7/13(월)

> 목표: 앱 기본 구조와 자가진단 핵심 화면 구현

### 환경 설정

- [ ] Task 4.1 `[Android]` build.gradle 라이브러리 의존성 추가
  - Hilt, Room, Compose Navigation, Retrofit2, Gson, Coroutines, BiometricPrompt
- [ ] Task 4.2 `[Android]` Hilt Application 클래스 및 기본 모듈 구성
- [ ] Task 4.3 `[Android]` AppDatabase 설정 (DiagnosisRecord, DetectionRecord, Memo 엔티티 등록)
- [ ] Task 4.4 `[Android]` RiskLevel enum 및 공통 도메인 모델 정의
- [ ] Task 4.5 `[Android]` Compose Theme 적용 (Task 3.5 토큰 기반 — 색상, 타이포그래피)
- [ ] Task 4.6 `[Android]` NavGraph 초안 구성 (전체 화면 라우트 등록, 빈 화면으로 연결)

### 자가진단 기능 (F-01)

- [ ] Task 4.7 `[Android]` DiagnosisScreen 구현
  - Task 3.6에서 확정한 체크리스트 항목 렌더링, 체크박스 선택 상태 관리
- [ ] Task 4.8 `[Android]` DiagnosisResultScreen 구현
  - 위험도(주의/경고/긴급) 결과 표시, 위험도별 색상 적용
  - 경고/긴급 시 대응 가이드, 지원 매칭 이동 버튼 표시
- [ ] Task 4.9 `[Android]` 자가진단 위험도 산출 로직 구현 (Design.md 5.1 기준)
  - 체크 항목 가중치 합산 → 점수 비율 → RiskLevel 분류
- [ ] Task 4.10 `[Android]` DiagnosisDAO 구현 (insert, getAll, deleteById)
- [ ] Task 4.11 `[Android]` DiagnosisRepository 구현
- [ ] Task 4.12 `[Android]` DiagnosisViewModel 구현 (체크 상태, 결과, 저장 흐름)
- [ ] Task 4.13 `[Android]` 자가진단 완료 시 DiagnosisRecord 로컬 저장 구현
  - 저장 항목: id, timestamp, riskLevel, score, checkedCount (원문 항목 저장 금지)

### 메인 화면

- [ ] Task 4.14 `[Android]` HomeScreen 구현
  - 기능 진입 버튼 (자가진단, 대화 감지, 지원 매칭, 긴급 요청)
  - 최근 기록 2~3개 요약 표시 (Room DB 조회)
- [ ] Task 4.15 `[Android]` Bottom Navigation 구성 (홈 / 기록 / 설정 탭)

---

## Sprint 5: 지원 연결 + 보안 기능 개발 | 7/14(화) ~ 7/20(월)

> 목표: 지원 매칭·신청 동행·긴급 요청·보안 기능 구현

### 지원 기관 데이터 로드

- [ ] Task 5.1 `[Android]` assets/ 폴더에 institutions.json 추가
- [ ] Task 5.2 `[Android]` InstitutionRepository 구현 — JSON 파일 파싱 (Gson), 유형별 필터링

### 맞춤 지원 매칭 (F-05)

- [ ] Task 5.3 `[Android]` SupportMatchScreen 구현
  - 위험 유형 선택 UI (가정폭력/데이트폭력/스토킹/기타)
  - 선택 유형에 맞는 기관 목록 렌더링
- [ ] Task 5.4 `[Android]` SupportDetailScreen 구현
  - 기관 이름, 전화번호, 운영시간, 지원 대상 표시
  - 전화번호 탭 → 전화 앱 Intent 실행

### 신청 동행 (F-06)

- [ ] Task 5.5 `[Android]` ApplicationGuideScreen 구현
  - 단계별 절차 목록 (1단계, 2단계…) 렌더링
  - 단계별 완료 체크 상태 관리 (세션 내 유지)
  - 필요 서류 목록 표시

### 긴급 도움 요청 (F-07)

- [ ] Task 5.6 `[Android]` EmergencyScreen 구현
  - 공공기관 전화 버튼 2개 (1366 여성긴급전화, 112 경찰)
  - 지인 문자 버튼 (등록 연락처 없으면 등록 안내 팝업)
- [ ] Task 5.7 `[Android]` 공공기관 전화 Intent 구현 (ACTION_DIAL)
- [ ] Task 5.8 `[Android]` 지인 문자 Intent 구현 (ACTION_SENDTO + 사전 설정 본문)
- [ ] Task 5.9 `[Android]` 긴급 연락처 미등록 시 SettingsScreen 이동 안내 팝업 구현

### 보안 기능 (F-09)

- [ ] Task 5.10 `[Android]` EncryptedSharedPreferences 모듈 구성 (AES-256-GCM)
- [ ] Task 5.11 `[Android]` LockScreen 구현 — PIN 4자리 입력 UI
- [ ] Task 5.12 `[Android]` PIN 잠금 로직 구현 (Design.md 5.4 기준)
  - PIN SHA-256 해시 저장 및 비교
  - 5회 오류 시 30초 차단 + 카운트다운 표시
- [ ] Task 5.13 `[Android]` BiometricPrompt 연동 — 생체인증 성공/실패 처리
- [ ] Task 5.14 `[Android]` 앱 포그라운드 복귀 시 잠금 화면 진입 로직 구현
- [ ] Task 5.15 `[Android]` SettingsScreen 구현
  - 앱 잠금 토글, 생체인증 토글
  - 긴급 연락처 이름·전화번호 입력 및 암호화 저장
  - 긴급 문자 내용 입력 및 저장
  - 중립적 알림 문구 직접 수정 기능

---

## Sprint 6: 위험 감지 + 알림 개발 | 7/21(화) ~ 7/27(월)

> 목표: 핵심 차별점인 실시간 위험 감지 데모 구현

### FastAPI 백엔드 (F-02 서버 측)

- [ ] Task 6.1 `[백엔드]` FastAPI 앱 기본 구성 완성 (main.py, CORS 설정, uvicorn 실행)
- [ ] Task 6.2 `[백엔드]` Pydantic 스키마 구현 (AnalyzeRequest, AnalyzeResponse, FlaggedPhrase)
- [ ] Task 6.3 `[백엔드]` keywords.json 로드 모듈 구현 (카테고리별 키워드, 가중치 포함)
- [ ] Task 6.4 `[백엔드]` 위험 감지 핵심 로직 구현 (detector.py)
  - 카테고리별 키워드 매칭 (협박/통제/반복압박/고립)
  - 정규식 보조 패턴 (초성 변형, 띄어쓰기 변형)
  - 위험도 점수 산출 및 RiskLevel 분류
  - flagged_phrases 위치 인덱스(start_index, end_index) 계산
- [ ] Task 6.5 `[백엔드]` /analyze 엔드포인트 구현
  - 5,000자 초과 입력 거부 (422 반환)
  - 분석 완료 후 수신 텍스트 미저장 검증
- [ ] Task 6.6 `[백엔드]` 로컬 서버 실행 테스트 + ngrok 연결 확인
- [ ] Task 6.7 `[백엔드]` Swagger UI(/docs)에서 /analyze 엔드포인트 동작 검증

### Android 대화 감지 (F-02, F-03)

- [ ] Task 6.8 `[Android]` Retrofit2 API 클라이언트 설정
  - BaseUrl: ngrok URL (로컬 서버 연결)
  - AnalyzeApiService 인터페이스 구현
- [ ] Task 6.9 `[Android]` DetectionInputScreen 구현
  - 텍스트 입력창 (멀티라인)
  - 클립보드 버튼 — ClipboardManager로 텍스트 자동 붙여넣기
  - 분석 버튼 + 로딩 인디케이터
- [ ] Task 6.10 `[Android]` DetectionResultScreen 구현
  - 위험 문장 강조 표시 (AnnotatedString — 주의:노랑/경고:주황/긴급:빨강)
  - 전체 위험도 및 감지된 카테고리 표시
  - 위험 없음 시 안내 문구 표시
- [ ] Task 6.11 `[Android]` DetectionViewModel + DetectionRepository 구현
- [ ] Task 6.12 `[Android]` 감지 완료 후 DetectionRecord 로컬 저장
  - 저장: id, timestamp, riskLevel, detectedKeywordCount, detectionCategories (원문 미저장)

### Android 알림 (F-03)

- [ ] Task 6.13 `[Android]` 알림 채널 생성
  - 채널 ID: `safelink_alert`
  - 채널 표시명: SharedPreferences neutral_notif_title 값 사용
- [ ] Task 6.14 `[Android]` 위험도별 배너 알림 발송 로직 구현 (Design.md 5.3 기준)
  - CAUTION: 무음 (PRIORITY_LOW)
  - WARNING: 진동 (PRIORITY_DEFAULT)
  - CRITICAL: 진동 + 알림음 (PRIORITY_HIGH)
- [ ] Task 6.15 `[Android]` 알림 탭 PendingIntent 설정
  - CAUTION/WARNING → ResponseGuideScreen 이동
  - CRITICAL → EmergencyScreen 직접 이동

### Android 대응 가이드 (F-04)

- [ ] Task 6.16 `[Android]` ResponseGuideScreen 구현
  - 위험도별 행동 지침 렌더링
  - 긴급 위험도 시 EmergencyScreen 버튼 상단 고정
  - '도움 받기' 버튼 → SupportMatchScreen 이동

---

## Sprint 7: 통합 + 데모 완성 | 7/28(화) ~ 8/3(월)

> 목표: 시연 가능한 전체 앱 흐름 완성

### 기록 기능 (F-08)

- [ ] Task 7.1 `[Android]` RecordListScreen 구현
  - DiagnosisRecord + DetectionRecord 합산 최신순 정렬 목록 표시
  - 위험도 태그, 감지 유형, 일시 표시
- [ ] Task 7.2 `[Android]` MemoEditScreen 구현
  - 텍스트 입력 후 저장 → Memo Room DB insert
- [ ] Task 7.3 `[Android]` 기록 삭제 기능 구현 (확인 팝업 없이 즉시 삭제, Requirements 3.31)
- [ ] Task 7.4 `[Android]` RecordListViewModel + RecordRepository 구현

### 통합 및 안정화

- [ ] Task 7.5 `[Android]` 전체 NavGraph 최종 연결 확인 (14개 화면 이동 흐름 검증)
- [ ] Task 7.6 `[Android]` 사용자 시나리오 1 — 자가진단 → 경고 결과 → 대응 가이드 → 기관 매칭 흐름 검증
- [ ] Task 7.7 `[Android]` 사용자 시나리오 2 — 대화 붙여넣기 → 위험 감지 → 알림 → 긴급 요청 흐름 검증
- [ ] Task 7.8 `[Android]` 사용자 시나리오 3 — 잠금 설정 → 앱 재진입 → PIN 인증 → 홈 진입 흐름 검증
- [ ] Task 7.9 `[Android]` `[백엔드]` 오류 케이스 처리 검증
  - 네트워크 없을 때 감지 화면 에러 처리
  - PIN 5회 오류 시 차단 동작 확인
  - 긴급 연락처 미등록 시 팝업 동작 확인
- [ ] Task 7.10 `[전체]` 발표용 핵심 시연 장면 3개 선정 및 시연 순서 확정

> ⚠️ 여기까지가 개발 마감. Sprint 8부터는 제출물 제작에만 집중.

---

## Sprint 8: 제출물 초안 제작 | 8/4(화) ~ 8/10(월)

- [ ] Task 8.1 `[기획]` 결과 보고서 목차 작성
- [ ] Task 8.2 `[기획]` 결과 보고서 본문 초안 작성 (기능 설명, 기술 구조, 기대 효과)
- [ ] Task 8.3 `[UI/UX]` 팜플렛 문구 초안 작성
- [ ] Task 8.4 `[UI/UX]` 소개 영상 스토리보드 작성
- [ ] Task 8.5 `[Android]` 데모 화면 캡처 및 GIF 정리

---

## Sprint 9: 제출물 보완 | 8/11(화) ~ 8/17(월)

- [ ] Task 9.1 `[기획]` 결과 보고서 내용 보완 및 팀원 검토
- [ ] Task 9.2 `[UI/UX]` 팜플렛 디자인 정리
- [ ] Task 9.3 `[UI/UX]` 소개 영상 대본 및 장면 구성 확정
- [ ] Task 9.4 `[전체]` 발표용 시연 시나리오 최종 리허설

---

## Sprint 10: 영상 제작 + 최종 문서 | 8/18(화) ~ 8/24(월)

- [ ] Task 10.1 `[전체]` 소개 영상 촬영 및 편집 (10분 이내)
- [ ] Task 10.2 `[기획]` 결과 보고서 최종본 완성
- [ ] Task 10.3 `[UI/UX]` 팜플렛 최종본 완성

---

## Sprint 11: 최종 점검 | 8/25(화) ~ 8/27(목)

- [ ] Task 11.1 `[전체]` 제출 파일 3종 파일명·형식·누락 여부 확인
- [ ] Task 11.2 `[Android]` 앱 데모 최종 재점검 (시나리오 3개 재실행)
- [ ] Task 11.3 `[전체]` 최종 신청서 내용 확인
- [ ] Task 11.4 `[전체]` 제출 직전 점검표 작성 및 서명

---

## 제출일 | 8/28(금)

- [ ] 오전 중 제출 완료 목표
- [ ] **16:00 이전** 제출 마감

---

## 추후 추가 예정

> 1차 심사 통과 후 본선 대비(9/4~9/17) Tasks는 결과 발표 후 별도 추가 예정.

---

*문서 버전: v1.0 | 작성일: 2026-05-26 | 작성자: Safelink 팀*
