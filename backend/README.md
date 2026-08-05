# SafeLink 목(mock) 분석 서버

`data/API 입출력 .json` 스키마를 그대로 구현한 **목 서버**입니다. 실제 LLM을 호출하지
않고 규칙 기반으로 그럴듯한 응답을 만들어서, Android ↔ 서버 연동 배선이 실제로
동작하는지 확인/시연하는 용도입니다. 신기훈 4주차 07번 문서 참고.

## 실행

```bash
cd backend
pip install -r requirements.txt
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

## 확인

```bash
curl http://localhost:8000/health
```

Android 에뮬레이터에서는 `http://10.0.2.2:8000/`으로 접근합니다 (에뮬레이터 안에서
호스트 PC의 localhost를 가리키는 특수 주소).

## 실제 AI로 교체하려면

`main.py`의 `analyze_context()` 함수 내부만 실제 LLM 호출로 바꾸면 됩니다. 요청/응답
스키마, FastAPI 라우팅, CORS 설정은 그대로 유지하면 Android 쪽은 코드 변경이 필요
없습니다(같은 계약을 계속 지키는 한).
