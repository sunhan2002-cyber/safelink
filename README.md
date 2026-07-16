# Safelink

Safelink는 위험한 관계의 신호를 빠르게 감지하고, 사용자가 필요한 도움을 안전하게 받을 수 있도록 지원하는 앱 프로젝트입니다.

단순히 위험을 경고하는 데서 끝나지 않고, 상담 기관, 복지 제도, 보호 서비스와 같은 실제 지원으로 연결하고 신청 과정까지 동행하는 것을 목표로 합니다. 메신저 대화나 반복적인 관계 패턴 속에서 나타나는 위험 표현을 감지하고, 즉시 대응할 수 있도록 배너 알림과 대응 가이드를 제공합니다.

또한 가정폭력이나 통제적 관계처럼 앱 사용 사실이 노출되는 것 자체가 위험할 수 있는 상황을 고려해 비밀번호 잠금과 생체인증 같은 보안 기능도 함께 설계하고 있습니다.

## Project Goal

- 위험 신호를 조기에 발견할 수 있는 앱 만들기
- 위험 감지 후 바로 대응 행동으로 이어질 수 있는 흐름 설계
- 상황에 맞는 상담 기관과 복지 제도를 연결
- 사용자의 안전과 프라이버시를 함께 보호

## Core Features

- 위험 관계 자가진단 체크리스트
- 위험 대화 감지 기능
- 백그라운드 감지 및 배너 알림
- 위험도 분류와 대응 가이드 제공
- 맞춤 지원 제도 및 상담 기관 추천
- 신청 동행 기능
- 긴급 도움 요청 기능
- 상황 기록 및 조회 기능
- 비밀번호 및 생체인증 기반 보안 기능

## Tech Stack

### Frontend
- Kotlin
- Jetpack Compose
- Android Studio
- Android Notification
- Foreground Service

### Backend
- Python
- FastAPI or Flask
- REST API
- JSON

### Storage
- MySQL or SQLite
- Room
- SharedPreferences

### Security
- App password lock
- Biometric authentication
- Encrypted local storage

## Project Direction

- 대학 과정에서 익힌 Java와 Python을 기반으로 개발
- AI와 협업하여 앱 구현과 분석 로직 개발 생산성 향상
- 공학경진대회 일정에 맞춰 데모 완성과 제출물 준비를 병행
- 실제 상용 서비스 수준보다 발표와 시연이 가능한 구조를 우선 구현

## Schedule

- 2026.05.26 ~ 2026.06.15: 시험기간으로 프로젝트 진행 중단
- 2026.06.16 ~ 2026.07.06: 기획, 설계, 화면 구조 정리
- 2026.07.07 ~ 2026.08.03: 핵심 기능 구현 및 데모 완성
- 2026.08.04 ~ 2026.08.27: 보고서, 팜플렛, 소개 영상, 최종 점검
- 2026.08.28 16:00: 1차 심사 결과물 제출 마감

## Repository Structure

```
safelink/
├── android/          # Android 앱 (Kotlin + Jetpack Compose)
│                     #   → Android Studio에서 이 폴더를 열어 실행
├── docs/             # 개발 설계 문서
│   ├── Requirements.md      # 요구사항 (F-01~F-09)
│   ├── Design.md            # 기술 설계 (아키텍처·데이터 모델·로직)
│   ├── Tasks.md             # 스프린트별 작업 목록
│   ├── ScreenFlow.md        # 화면 흐름도
│   ├── ScreenUI.md          # 화면별 UI 구성표
│   ├── DevPriority.md       # 우선 개발 화면 목록
│   └── AndroidStructure.md  # 앱 구조 설계
├── data/             # 데이터·API 스펙 (institutions.json, keyword.json, API 입출력)
├── 기획문서/          # 서비스 소개·로드맵·일정·기술스택·명세서·대회 안내
├── 아이디어_초안/     # 시나리오·위험도 계산·카테고리 브레인스토밍
├── 작업노트/          # 팀원 작업 메모 (.wy)
└── 주차별_결과물/     # 주차별 산출물 정리
```

- 디자인(Figma): https://www.figma.com/design/pgz34E5ealhQwQICH9xSTy/safelink

## One-line Summary

Safelink는 위험한 관계를 감지하고, 안전하게 도움을 연결하며, 사용자의 행동까지 이어지도록 돕는 보호형 지원 앱입니다.
