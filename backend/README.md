# SafeLink 문맥 분석 서버

`data/API 입출력 .json` 스키마를 그대로 구현한 분석 서버입니다.
기기 안의 규칙 엔진이 **판단하기 애매한 건만** 이 서버로 보내고, 서버는 정황을 보고
**점수 보정치와 설명만** 돌려줍니다. 최종 위험도는 앱이 정합니다.

## 모드

| 모드 | 동작 | 언제 |
|---|---|---|
| `claude` | Claude Sonnet 5로 실제 문맥 분석 | 실제 시연·검증 |
| `mock` | LLM 없이 규칙 기반 가짜 응답 | 키 없이 앱 ↔ 서버 연결만 확인 |

`ANTHROPIC_API_KEY`가 있으면 `claude`, 없으면 `mock`으로 자동 선택됩니다.
지금 어느 모드인지는 `GET /health`의 `"mode"`로 확인하세요.

## 실행

```bash
cd backend
pip install -r requirements.txt
ANTHROPIC_API_KEY=발급받은_키 uvicorn main:app --host 0.0.0.0 --port 8000
```

Windows PowerShell:

```powershell
$env:ANTHROPIC_API_KEY = "발급받은_키"
uvicorn main:app --host 0.0.0.0 --port 8000
```

Android 에뮬레이터에서는 `http://10.0.2.2:8000/`으로 접근합니다.

## 환경 변수

| 변수 | 기본값 | 설명 |
|---|---|---|
| `ANTHROPIC_API_KEY` | (없음) | Claude API 키. **저장소에 커밋하지 마세요** |
| `SAFELINK_AI_MODE` | 키 유무로 자동 | `claude` 또는 `mock`으로 강제 |
| `SAFELINK_AI_EFFORT` | `high` | `low` / `medium` / `high` / `xhigh` / `max` |
| `SAFELINK_AI_TIMEOUT_SECONDS` | `4.5` | Claude 호출 제한 시간(초) |

### ⚠️ effort와 제한 시간은 같이 조정해야 합니다

앱(Android)은 서버 응답을 **5초**까지만 기다립니다. Sonnet 5는 생각(thinking)이 기본으로
켜져 있어 `high`에서는 5초를 넘길 수 있고, 넘기면 앱은 온디바이스 결과를 그대로 씁니다
(앱이 멈추지는 않습니다). 실제 키로 지연을 재 본 뒤 둘 중 하나로 맞춥니다.

- `SAFELINK_AI_EFFORT`를 낮춘다 (`medium` / `low`)
- 앱의 읽기 제한 시간을 늘린다 (`AnalyzeApiClient`) — AI 호출은 화면을 막지 않는 비동기라 가능

## 테스트

```bash
cd backend
python -m pytest -q
```

가짜 Claude 클라이언트로 돌아서 **키·네트워크·비용 없이** 실행됩니다.
실제 판정 품질이 아니라 "모델이 무엇을 돌려주든 서버가 안전한 값만 내보내는가"를 확인합니다.

## 이 서버가 지키는 것

- **원문을 저장하지 않습니다.** 요청 본문은 Claude 호출에만 쓰고 버립니다. 로그에는 실패
  사유 코드만 남기고 대화 내용·세션 id는 기록하지 않습니다.
- **모델 출력을 그대로 믿지 않습니다.** 대화 원문은 사기범이 쓴 글이라 "점수를 낮춰라" 같은
  문장이 섞일 수 있습니다. 구조화 출력으로 모양을 보장받고, 값은 서버에서 한 번 더 자릅니다
  (보정치 ±30, 규칙 id는 앱이 보낸 것만).
- **실패는 실패로 돌려줍니다.** 거절·시간 초과·빈 응답이면 `503`을 주고, 앱은 온디바이스
  결과를 그대로 씁니다. 그럴듯한 가짜 판정으로 채우지 않습니다.
- **서버는 최종 위험도를 정하지 않습니다.** `recommended_level_override`는 항상 `null`입니다.

## 모델 설정 메모

- 모델: `claude-sonnet-5`
- `temperature`는 넣지 않습니다 — Sonnet 5는 기본값이 아닌 값을 `400`으로 거절합니다.
- 자동 대체 모델(`fallbacks`)은 쓰지 않습니다 — 문서상 Opus 5·Fable 5.1용이며 Sonnet 5
  지원이 명시돼 있지 않습니다. 거절되면 앱의 온디바이스 폴백 경로를 씁니다.
