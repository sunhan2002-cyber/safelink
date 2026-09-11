"""
SafeLink 문맥 분석 서버 — data/API 입출력 .json 스키마 그대로 구현.

두 가지 모드로 돈다.
- claude : Claude Sonnet 5 로 실제 문맥 분석 (claude_analyzer.py)
- mock   : LLM 없이 규칙 기반 가짜 응답. 키 없이 Android <-> 서버 배선만 확인할 때 쓴다.

모드는 SAFELINK_AI_MODE 로 고른다. 지정하지 않으면 ANTHROPIC_API_KEY 가 있을 때 claude,
없을 때 mock 이다. 지금 어느 모드인지는 GET /health 의 "mode" 로 확인한다.

아키텍처 원칙(CLAUDE.md) 준수:
- 서버는 최종 위험도를 결정하지 않는다 - context_score_adjustment(보정치)만 반환하고,
  recommended_level_override 는 항상 null 이다.
- 원문을 저장하지 않는다 - masked_text/recent_turns 는 분석 호출에만 쓰고 버린다.
  DB·파일 저장 코드가 없고, 로그에는 실패 사유 코드만 남긴다(대화 내용·세션 id 미기록).
- AI 분석을 못 받으면 503 을 준다. 앱은 실패 응답을 받으면 온디바이스 결과를 그대로 쓴다.
  그럴듯한 가짜 판정으로 채우지 않는다.

요청/응답 스키마(AnalyzeRequest/AnalyzeResponse)는 그대로라 Android 코드는 바뀌지 않는다.

실행: uvicorn main:app --host 0.0.0.0 --port 8000
Android 에뮬레이터에서는 10.0.2.2:8000 으로 접근 (localhost의 에뮬레이터 별칭).
"""

import logging
import os
from datetime import datetime, timezone, timedelta
from typing import List, Optional

import anthropic
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field

import claude_analyzer as ca

logger = logging.getLogger("safelink")

app = FastAPI(
    title="SafeLink Analyze API",
    description="온디바이스 판정이 애매한 건에 한해 문맥 보정치를 돌려준다. 모드는 GET /health 참고.",
    version="0.2.0",
)

# 로컬 시연용 - 실제 배포 시에는 허용 origin을 좁혀야 함
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

KST = timezone(timedelta(hours=9))


class RecommendedInstitution(BaseModel):
    institution_id: str
    rank: int
    reason: str
    matched_risk_type: str
    matched_subcategory_id: str


class AnalyzeRequest(BaseModel):
    session_id: str
    masked_text: str
    recent_turns: List[str]
    device_base_score: float
    device_matched_ids: List[str]
    device_applied_combo_ids: Optional[List[str]] = Field(default_factory=list)
    category_hint: Optional[str] = None


class AnalyzeResponse(BaseModel):
    context_score_adjustment: float
    context_analysis_summary: str
    context_detected_pattern: Optional[str]
    recommended_level_override: Optional[str]
    guide_reference_id: Optional[str] = None
    matched_keyword_ids: List[str]
    recommended_institutions: List[RecommendedInstitution]
    analysis_timestamp: str


def resolve_mode() -> str:
    """claude 또는 mock. 명시값이 우선이고, 없으면 API 키 유무로 정한다."""
    explicit = os.environ.get("SAFELINK_AI_MODE", "").strip().lower()
    if explicit in ("claude", "mock"):
        return explicit
    return "claude" if os.environ.get("ANTHROPIC_API_KEY") else "mock"


_client: Optional[anthropic.Anthropic] = None


def get_client() -> anthropic.Anthropic:
    """Claude 클라이언트를 처음 쓸 때 한 번만 만든다.

    재시도는 끈다. 앱이 5초 안에 답을 못 받으면 이미 버리므로, 재시도는 대기 시간과 비용만 늘린다.
    """
    global _client
    if _client is None:
        _client = anthropic.Anthropic(max_retries=0)
    return _client


def analyze_context_claude(req: AnalyzeRequest) -> AnalyzeResponse:
    inp = ca.AnalysisInput(
        masked_text=req.masked_text,
        recent_turns=req.recent_turns,
        device_base_score=req.device_base_score,
        device_matched_ids=req.device_matched_ids,
        device_applied_combo_ids=req.device_applied_combo_ids or [],
        category_hint=req.category_hint,
    )
    try:
        verdict = ca.analyze_with_claude(
            inp,
            get_client(),
            effort=ca.effort_from_env(),
            timeout=ca.timeout_from_env(),
        )
    except ca.AnalysisUnavailable as e:
        # 사유 코드만 남긴다. 대화 내용과 세션 id 는 로그에 쓰지 않는다.
        logger.warning("AI 분석 불가: %s", e.reason)
        raise HTTPException(status_code=503, detail="AI 분석을 사용할 수 없습니다.")

    return AnalyzeResponse(
        context_score_adjustment=verdict.score_adjustment,
        context_analysis_summary=verdict.summary,
        context_detected_pattern=verdict.detected_pattern,
        # 서버는 최종 위험도를 정하지 않는다 — 앱이 보정치를 더해 직접 계산한다.
        recommended_level_override=None,
        guide_reference_id=None,
        # AI가 실제로 위험 신호로 본 규칙 id. 앱 화면은 이 값을 쓰지 않고(mergeAiResponse),
        # 규칙 검증 자료로만 의미가 있다.
        matched_keyword_ids=verdict.confirmed_keyword_ids,
        # 기관 추천은 온디바이스(institutions.json)가 맡는다.
        recommended_institutions=[],
        analysis_timestamp=datetime.now(KST).isoformat(),
    )


def analyze_context_mock(req: AnalyzeRequest) -> AnalyzeResponse:
    """
    ⚠️ MOCK 구현 - 실제 LLM 호출 없음. "매칭된 키워드 개수"만 보는 단순 규칙:
    3개 이상이면(다단계 패턴 가능성) 소폭 가산, 그 외엔 소폭 감산 — 문맥/의미는 전혀
    분석하지 않는다. 키 없이 연동 배선만 확인하는 용도다.
    """
    matched_count = len(req.device_matched_ids)
    if matched_count >= 3:
        adjustment = 10.0
        pattern = "다단계 패턴 감지 (목 서버 - 실제 분석 아님)"
        summary = (
            f"{req.category_hint or '분석 대상'} 관련 신호가 {matched_count}개 감지되어 "
            "여러 단계가 이어지는 전형적 흐름으로 판단됨 (목 서버 임시 규칙)"
        )
    else:
        adjustment = -5.0
        pattern = None
        summary = (
            f"매칭된 신호가 {matched_count}개로 적어 문맥상 단발성 표현일 가능성 있음 "
            "(목 서버 임시 규칙)"
        )

    return AnalyzeResponse(
        context_score_adjustment=adjustment,
        context_analysis_summary=summary,
        context_detected_pattern=pattern,
        recommended_level_override=None,
        guide_reference_id=None,
        matched_keyword_ids=req.device_matched_ids,
        recommended_institutions=[],
        analysis_timestamp=datetime.now(KST).isoformat(),
    )


@app.post("/analyze", response_model=AnalyzeResponse)
def analyze(req: AnalyzeRequest) -> AnalyzeResponse:
    if not req.masked_text or not req.masked_text.strip():
        raise HTTPException(status_code=400, detail="분석할 텍스트가 없습니다.")
    if resolve_mode() == "mock":
        return analyze_context_mock(req)
    return analyze_context_claude(req)


@app.get("/health")
def health():
    mode = resolve_mode()
    info = {"status": "ok", "mode": mode, "is_mock": mode == "mock"}
    if mode == "claude":
        info.update(
            model=ca.MODEL,
            effort=ca.effort_from_env(),
            timeout_seconds=ca.timeout_from_env(),
        )
    else:
        info["note"] = "목 서버 - 실제 LLM 미호출. ANTHROPIC_API_KEY 를 설정하면 claude 모드로 돈다."
    return info
