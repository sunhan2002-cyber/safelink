package com.safelink.app.ui.theme

import androidx.compose.ui.graphics.Color

// 브랜드 (Figma 와이어프레임 기준 파랑 계열)
val BrandBlue = Color(0xFF2563EB)
val BrandBlueDark = Color(0xFF1E40AF)
val BrandBlueLight = Color(0xFFDBEAFE)

// 배경·표면
val BackgroundGray = Color(0xFFF5F6F8)
val SurfaceWhite = Color(0xFFFFFFFF)

// 위험도 색상 (안전=초록, 주의=노랑, 경고=주황, 긴급=빨강)
val RiskSafe = Color(0xFF16A34A)
val RiskCaution = Color(0xFFCA8A04)
val RiskWarning = Color(0xFFEA580C)
val RiskCritical = Color(0xFFDC2626)

val RiskSafeContainer = Color(0xFFDCFCE7)
val RiskCautionContainer = Color(0xFFFEF9C3)
val RiskWarningContainer = Color(0xFFFFEDD5)
val RiskCriticalContainer = Color(0xFFFEE2E2)

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
