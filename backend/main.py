"""
SafeLink 목(mock) 분석 서버 - data/API 입출력 .json 스키마 그대로 구현.

실제 LLM을 호출하지 않는 규칙 기반 목 서버다. 목적은 "Android <-> 서버 연동 배선"을
실제로 검증/시연하기 위함이지, 문맥 분석 품질을 보장하지 않는다. 진짜 AI 분석으로
교체할 때는 analyze_context() 함수 내부만 갈아끼우면 되고, 요청/응답 스키마와
FastAPI 라우팅은 그대로 유지된다.

아키텍처 원칙(CLAUDE.md) 준수:
- 서버는 최종 위험도를 결정하지 않는다 - context_score_adjustment(보정치)만 반환.
- 원문을 저장하지 않는다 - 요청 처리 후 masked_text/recent_turns는 응답 생성에만 쓰고 버림
  (이 목 서버는 DB/파일 저장 코드 자체가 없음 - 요청 핸들러 스코프를 벗어나면 자동 소멸).

실행: uvicorn main:app --reload --host 0.0.0.0 --port 8000
Android 에뮬레이터에서는 10.0.2.2:8000 으로 접근 (localhost의 에뮬레이터 별칭).
"""

from datetime import datetime, timezone, timedelta
from typing import List, Optional

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field

app = FastAPI(title="SafeLink Mock Analyze API", version="0.1.0-mock")

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


def analyze_context(req: AnalyzeRequest) -> AnalyzeResponse:
    """
    목(mock) 문맥 분석 - 실제 LLM 호출부. 지금은 단순 규칙으로 대체:
    매칭된 키워드가 3개 이상이면(다단계 패턴 가능성) 소폭 가산, 그 외엔 소폭 감산.
    recommended_level_override는 항상 null - 서버가 최종 위험도를 결정하지 않는다는
    원칙을 목 서버에서도 지킴(클라이언트 판단 존중).
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
    return analyze_context(req)


@app.get("/health")
def health():
    return {"status": "ok", "note": "SafeLink mock analyze server"}
