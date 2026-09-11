# GitHub 레포 정리 기준

## 1. 정리 목표

GitHub 프로필을 봤을 때 다음 인상이 나도록 정리한다.

- 공학경진대회 본선 진출 프로젝트가 가장 먼저 보임
- Android, Kotlin, AI/OCR, 백그라운드 감지, 데이터 분석 역량이 드러남
- 학교 실습용 임시 레포가 프로필의 전문성을 흐리지 않음
- 삭제보다 비공개 전환을 우선해 과거 작업물을 보존함

---

## 2. 레포 분류 기준

| 분류 | 기준 | 처리 |
|---|---|---|
| 대표 프로젝트 | SafeLink, Sound-learn처럼 설명 가능한 완성형 프로젝트 | Public 유지, README 보강, pinned 처리 |
| 포트폴리오 후보 | 기능 설명 가능, 실행 방법 있음, 기술스택이 명확함 | Public 유지 또는 README 보강 후 공개 |
| 학교 실습 | 과제 제출용, 실습 코드만 있음, README 없음 | Private 전환 권장 |
| 테스트/임시 | 연습용, 빈 레포, 의미 없는 이름, 커밋만 있음 | 삭제 또는 Private 전환 |
| 민감정보 가능 | API key, 개인정보, 학교 내부자료 포함 가능 | 즉시 Private 전환 후 점검 |

---

## 3. 우선 정리 순서

1. SafeLink 레포를 가장 먼저 정리한다.
2. Sound-learn 레포가 있다면 README를 보강하고 pinned 후보로 둔다.
3. 학교 실습용 레포는 이름과 README를 보고 Private 전환 후보로 분류한다.
4. 빈 레포, 테스트 레포, 중복 레포는 삭제 후보로 분류한다.
5. 프로필 pinned repository는 3개 정도만 남긴다.

---

## 4. SafeLink 레포 정리 기준

SafeLink 레포는 다음 항목이 보이면 전문적으로 보인다.

- 프로젝트 한 줄 소개
- 핵심 기능
- 기술스택 표
- 아키텍처 구조
- 실행 방법
- 개인정보 보호 설계
- 현재 구현 상태
- 본선/대회 프로젝트임을 알 수 있는 설명

현재 반영한 README는 위 기준에 맞춰 정리되어 있다.

---

## 5. GitHub에서 직접 설정할 항목

### SafeLink 레포 About 설정

Description:

```text
Android safety-support app for detecting risky messenger conversations with OCR, background detection, and on-device rule-based analysis.
```

Topics:

```text
android
kotlin
jetpack-compose
mlkit
ocr
accessibility-service
room-database
retrofit
digital-safety
engineering-competition
```

Website:

```text
비워두거나 발표/배포 링크가 생긴 뒤 추가
```

### 추천 pinned repository

- `safelink`
- `sound-learn`
- 가장 완성도 높은 개인 프로젝트 1개

---

## 6. 학교 실습 레포 처리 기준

아래에 해당하면 Public으로 둘 이유가 약하다.

- 이름이 `test`, `practice`, `assignment`, `week1`처럼 의미가 약함
- README가 없거나 실행 방법이 없음
- 단일 실습 코드만 있고 프로젝트 설명이 없음
- 오래된 과제 코드라 현재 역량을 보여주기 어려움
- 커밋이 1~2개뿐이고 완성 프로젝트처럼 보이지 않음

권장 처리:

```text
삭제보다 Private 전환 우선
정말 빈 레포이거나 중복 레포만 삭제
```

---

## 7. 삭제 전 체크리스트

- 이 레포가 수업 제출 증빙으로 필요하지 않은가?
- 나중에 코드 참고할 가능성이 없는가?
- 개인 정보나 API key가 들어 있지 않은가?
- 같은 내용의 더 나은 레포가 이미 있는가?
- Public으로 남겼을 때 내 역량을 보여주는가?

하나라도 애매하면 삭제하지 말고 Private으로 전환한다.

---

## 8. 프로필 정리 원칙

```text
대표 프로젝트는 적게, 설명은 명확하게, 실습 레포는 숨기고, 완성 프로젝트를 앞으로 보이게 둔다.
```

