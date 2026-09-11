package com.safelink.app.ui.theme

import androidx.compose.ui.graphics.Color

// 브랜드 (9주차 - Figma "SafeLink Concept B" 리디자인 시안 기준 민트/그린 계열로 전환.
// 변수 이름은 그대로 유지 — 이 4개 토큰을 참조하는 다른 화면 코드를 안 건드리기 위해 값만
// 교체함. 실제 값은 시안 스크린샷 픽셀 샘플링으로 추출(#16C79A 계열이 반복적으로 등장).
val BrandBlue = Color(0xFF16C79A)
val BrandBlueDark = Color(0xFF0B8F6E)
val BrandBlueLight = Color(0xFFDCF6EE)

// 배경·표면
val BackgroundGray = Color(0xFFF5F6F8)
val SurfaceWhite = Color(0xFFFFFFFF)

// 위험도 색상 (안전=초록, 주의=주황, 경고=주황(진하게), 긴급=빨강) — 9주차: Figma 시안 색으로 갱신.
// 시안에서 "안전"은 브랜드 그린과 같은 색을 씀(=신뢰감 있는 기본색이 곧 안전 상태).
val RiskSafe = Color(0xFF16C79A)
val RiskCaution = Color(0xFFF5A524)
val RiskWarning = Color(0xFFE0801A)
val RiskCritical = Color(0xFFF25C54)

val RiskSafeContainer = Color(0xFFDCF6EE)
val RiskCautionContainer = Color(0xFFFEF2DE)
val RiskWarningContainer = Color(0xFFFCE8D6)
val RiskCriticalContainer = Color(0xFFFDE7E5)

val TextPrimary = Color(0xFF111827)
val TextSecondary = Color(0xFF6B7280)

// 분석 근거 유형 구분 색상 (7주차 수정 - 문장/상황/AI 근거가 키워드 카드와 시각적으로
// 구분 안 되던 문제 대응). 위험도 색상(Risk*)과 겹치지 않는 별도 팔레트 - 위험도와
// "이 근거가 어느 층에서 왔는지"는 서로 다른 축이라 혼동 없게 분리.
val RuleSentenceAccent = Color(0xFF0891B2)          // 문장 규칙 - 청록
val RuleSentenceContainer = Color(0xFFCFFAFE)
val RuleSituationalAccent = Color(0xFF7C3AED)       // 상황 규칙 - 보라
val RuleSituationalContainer = Color(0xFFEDE9FE)
val RuleAiAccent = Color(0xFF4338CA)                // AI 보조분석 - 남색(브랜드블루보다 진하게, "보조"라는 인상 유지)
val RuleAiContainer = Color(0xFFE0E7FF)

// "오늘의 안전 팁" 카드 전용 파란 톤 — Figma B02 픽셀 샘플링 값(사용자 요청, 9주차)
val TipBlue = Color(0xFF2766F2)
val TipBlueContainer = Color(0xFFDBEEFF)
