"""
서버 엔드포인트 검증 — 가짜 Claude 클라이언트로 돈다(네트워크·키·비용 없음).

main.py 와 claude_analyzer.py 를 실제로 엮어서, 요청이 들어와 응답이 나가기까지를 확인한다.

실행: cd backend && python -m pytest -q
"""

import logging
from types import SimpleNamespace

import pytest
from fastapi.testclient import TestClient

import main

CONVERSATION = "검찰청 수사관입니다 계좌가 범죄에 연루돼 지금 바로 안전계좌로 옮기셔야 합니다"

BODY = {
    "session_id": "s-1",
    "masked_text": CONVERSATION,
    "recent_turns": [CONVERSATION],
    "device_base_score": 38.0,
    "device_matched_ids": ["VP-1-1-003", "VP-1-3-007"],
    "device_applied_combo_ids": ["CB-04"],
    "category_hint": "보이스피싱",
}

http = TestClient(main.app)


class _FakeMessages:
    def __init__(self, response):
        self.response = response
        self.calls = 0

    def parse(self, **kwargs):
        self.calls += 1
        return self.response


def _use_fake_claude(monkeypatch, response):
    monkeypatch.setenv("SAFELINK_AI_MODE", "claude")
    messages = _FakeMessages(response)
    monkeypatch.setattr(main, "get_client", lambda: SimpleNamespace(messages=messages))
    return messages


def _judgement(**overrides):
    base = dict(
        context_score_adjustment=12.0,
        context_analysis_summary="기관을 사칭해 송금을 서두르게 하는 흐름입니다.",
        context_detected_pattern="기관 사칭 후 송금 유도",
        confirmed_keyword_ids=["VP-1-1-003"],
    )
    base.update(overrides)
    return main.ca.ClaudeJudgement(**base)


def _ok(judgement):
    return SimpleNamespace(stop_reason="end_turn", stop_details=None, parsed_output=judgement)


# --- 모드 결정 ---------------------------------------------------------------


def test_키가_없으면_mock(monkeypatch):
    monkeypatch.delenv("SAFELINK_AI_MODE", raising=False)
    monkeypatch.delenv("ANTHROPIC_API_KEY", raising=False)
    assert main.resolve_mode() == "mock"


def test_키가_있으면_claude(monkeypatch):
    monkeypatch.delenv("SAFELINK_AI_MODE", raising=False)
    monkeypatch.setenv("ANTHROPIC_API_KEY", "sk-ant-test")
    assert main.resolve_mode() == "claude"


def test_명시한_모드가_우선(monkeypatch):
    monkeypatch.setenv("ANTHROPIC_API_KEY", "sk-ant-test")
    monkeypatch.setenv("SAFELINK_AI_MODE", "mock")
    assert main.resolve_mode() == "mock"


# --- claude 모드 -------------------------------------------------------------


def test_정상_판정은_계약_그대로_나간다(monkeypatch):
    _use_fake_claude(monkeypatch, _ok(_judgement()))
    r = http.post("/analyze", json=BODY)
    assert r.status_code == 200
    data = r.json()
    assert data["context_score_adjustment"] == 12.0
    assert data["context_analysis_summary"] == "기관을 사칭해 송금을 서두르게 하는 흐름입니다."
    assert data["context_detected_pattern"] == "기관 사칭 후 송금 유도"
    assert data["matched_keyword_ids"] == ["VP-1-1-003"]
    # 서버는 최종 위험도를 정하지 않는다
    assert data["recommended_level_override"] is None
    assert data["recommended_institutions"] == []


def test_모델이_과한_값을_줘도_서버가_자른다(monkeypatch):
    judgement = _judgement(context_score_adjustment=-95.0, confirmed_keyword_ids=["MADE-UP-ID", "VP-1-3-007"])
    _use_fake_claude(monkeypatch, _ok(judgement))
    data = http.post("/analyze", json=BODY).json()
    assert data["context_score_adjustment"] == -30.0
    assert data["matched_keyword_ids"] == ["VP-1-3-007"]


def test_거절되면_503이고_대화내용은_어디에도_남지_않는다(monkeypatch, caplog):
    refusal = SimpleNamespace(stop_reason="refusal", stop_details=SimpleNamespace(category="cyber"), parsed_output=None)
    _use_fake_claude(monkeypatch, refusal)
    with caplog.at_level(logging.WARNING, logger="safelink"):
        r = http.post("/analyze", json=BODY)
    assert r.status_code == 503
    assert CONVERSATION not in r.text
    assert "refusal:cyber" in caplog.text
    assert CONVERSATION not in caplog.text
    assert "s-1" not in caplog.text


def test_빈_텍스트는_Claude를_부르지_않고_400(monkeypatch):
    messages = _use_fake_claude(monkeypatch, _ok(_judgement()))
    r = http.post("/analyze", json={**BODY, "masked_text": "   "})
    assert r.status_code == 400
    assert messages.calls == 0


def test_health_claude_모드(monkeypatch):
    monkeypatch.setenv("SAFELINK_AI_MODE", "claude")
    monkeypatch.delenv("SAFELINK_AI_EFFORT", raising=False)
    data = http.get("/health").json()
    assert data["mode"] == "claude"
    assert data["is_mock"] is False
    assert data["model"] == "claude-sonnet-5"
    assert data["effort"] == "high"


# --- mock 모드 ---------------------------------------------------------------


def test_mock_모드는_Claude없이_응답한다(monkeypatch):
    monkeypatch.setenv("SAFELINK_AI_MODE", "mock")

    def _no_claude():
        raise AssertionError("mock 모드에서 Claude 클라이언트를 만들면 안 된다")

    monkeypatch.setattr(main, "get_client", _no_claude)
    r = http.post("/analyze", json=BODY)
    assert r.status_code == 200
    assert "목 서버" in r.json()["context_analysis_summary"]
    assert http.get("/health").json()["is_mock"] is True


@pytest.fixture(autouse=True)
def _reset_client():
    main._client = None
    yield
    main._client = None
