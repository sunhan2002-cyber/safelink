"""
SafeLink AI 보조분석 검증 — 위험 문장 테스트 케이스 43개에 Claude 보정을 실제로 적용해 본다.

질문은 두 가지다.
1. 일상 대화(negative_cases, verdict 허용)를 AI 가 위험 쪽으로 올리는가  → 오탐 증가
2. 사기 문구(positive / edge / combo)를 AI 가 안전 쪽으로 내리는가     → 놓침 증가

주의: 테스트 세트의 expected_level 은 "규칙 엔진이 내야 할 점수 구간"이지 실제 위험의 정답이 아니다.
그래서 여기서는 등급의 '정답 일치'보다 위 두 방향의 변화를 본다.

앱과 같은 조건으로 호출한다: backend/claude_analyzer.py 규칙(앱 ClaudeAnalysisRules 와 동일),
claude-sonnet-5, effort low, 전화번호·링크 마스킹된 원문, 보정치를 점수에 더해 정수로 자르고 0~100.

실행: python run_ai_eval.py <온디바이스결과.json>
키는 android/local.properties 의 ANTHROPIC_API_KEY 를 읽고, 화면에 출력하지 않는다.
"""

import json
import os
import statistics
import sys
import time
from pathlib import Path

# 저장소 안에서 경로를 잡는다 — 어느 PC 에서 받아도 그대로 돌아가도록(예전에는 절대 경로였다).
REPO_ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(REPO_ROOT / "backend"))
import anthropic  # noqa: E402
import claude_analyzer as ca  # noqa: E402

LOCAL_PROPERTIES = REPO_ROOT / "android" / "local.properties"
BANDS = ["낮음", "중간", "높음"]


def load_key() -> str:
    """키는 환경변수 ANTHROPIC_API_KEY 를 먼저 보고, 없으면 android/local.properties 에서 읽는다(화면에 찍지 않는다)."""
    env_key = os.environ.get("ANTHROPIC_API_KEY", "").strip()
    if env_key:
        return env_key
    if not LOCAL_PROPERTIES.exists():
        raise SystemExit("ANTHROPIC_API_KEY 환경변수도, android/local.properties 도 없습니다.")
    with open(LOCAL_PROPERTIES, encoding="utf-8") as f:
        for line in f:
            if line.startswith("ANTHROPIC_API_KEY="):
                return line.split("=", 1)[1].strip()
    raise SystemExit(
        "ANTHROPIC_API_KEY 를 찾지 못했습니다. android/local.properties 에 넣거나 환경변수로 지정하세요."
    )


def band(score: int) -> str:
    return "높음" if score >= 66 else ("중간" if score >= 31 else "낮음")


def main(in_path: str) -> None:
    cases = json.load(open(in_path, encoding="utf-8"))
    client = anthropic.Anthropic(api_key=load_key(), max_retries=2)

    rows = []
    for i, c in enumerate(cases, 1):
        inp = ca.AnalysisInput(
            masked_text=c["text"],
            recent_turns=[c["text"]],
            device_base_score=c["score"],
            device_matched_ids=c["matched_ids"],
            device_applied_combo_ids=c["combo_ids"],
            category_hint=c["category"] or None,
        )
        started = time.time()
        try:
            v = ca.analyze_with_claude(inp, client, effort="low", timeout=60)
            adj, summary, pattern, error = v.score_adjustment, v.summary, v.detected_pattern, None
        except ca.AnalysisUnavailable as e:
            adj, summary, pattern, error = None, None, None, e.reason
        elapsed = time.time() - started

        after = None if adj is None else max(0, min(100, int(c["score"] + adj)))
        rows.append({**c, "adjustment": adj, "after": after, "summary": summary,
                     "pattern": pattern, "error": error, "elapsed": round(elapsed, 2)})
        mark = "실패 " + error if error else f"{c['score']:>3} → {after:>3} ({adj:+.0f})"
        print(f"[{i:2}/{len(cases)}] {c['id']:<16} {c['truth']:<4} {mark}  {elapsed:.1f}초", flush=True)

    json.dump(rows, open(in_path.replace(".json", "_ai.json"), "w", encoding="utf-8"), ensure_ascii=False, indent=2)
    report(rows, in_path.replace(".json", "_report.md"))


def summarize(rows, title):
    ok = [r for r in rows if r["error"] is None]
    benign = [r for r in ok if r["truth"] == "일상"]
    scam = [r for r in ok if r["truth"] == "사기"]

    def moved(rs, up):
        return [r for r in rs if (BANDS.index(band(r["after"])) - BANDS.index(band(r["score"]))) * (1 if up else -1) > 0]

    lines = [f"### {title}", "",
             f"- 호출 {len(rows)}건 중 성공 {len(ok)}건, 실패 {len(rows) - len(ok)}건",
             f"- **일상 대화 {len(benign)}건**: AI 가 등급을 올림 **{len(moved(benign, True))}건** / 내림 {len(moved(benign, False))}건 / 유지 {len(benign) - len(moved(benign, True)) - len(moved(benign, False))}건",
             f"- **사기 문구 {len(scam)}건**: AI 가 등급을 내림 **{len(moved(scam, False))}건** / 올림 {len(moved(scam, True))}건 / 유지 {len(scam) - len(moved(scam, True)) - len(moved(scam, False))}건",
             ""]
    risky = moved(benign, True) + moved(scam, False)
    if risky:
        lines += ["주의가 필요한 변화:", "", "| 케이스 | 구분 | 점수 | AI 설명 |", "|---|---|---|---|"]
        for r in risky:
            lines.append(f"| {r['id']} | {r['truth']} | {r['score']}({band(r['score'])}) → {r['after']}({band(r['after'])}) | {r['summary']} |")
        lines.append("")
    return lines, len(moved(benign, True)), len(moved(scam, False))


def report(rows, out_path):
    ok = [r for r in rows if r["error"] is None]
    all_lines, _, _ = summarize(rows, "전체 43건에 AI 보정을 적용했을 때")
    esc = [r for r in rows if r["should_escalate"]]
    esc_lines, _, _ = summarize(esc, f"실제 앱에서 AI 가 호출되는 케이스만 ({len(esc)}건)")
    times = [r["elapsed"] for r in ok]
    lines = ["# AI 보조분석 검증 결과", "",
             f"- 모델 claude-sonnet-5 / effort low / 앱과 같은 규칙(보정치 ±30)",
             f"- 응답 시간: 평균 {statistics.mean(times):.1f}초, 최대 {max(times):.1f}초" if times else "- 응답 시간: 측정 불가",
             "", *esc_lines, *all_lines,
             "## 케이스별", "", "| 케이스 | 구분 | 호출조건 | 점수 | 보정 | AI 수법 |", "|---|---|---|---|---|---|"]
    for r in rows:
        adj = "실패" if r["error"] else f"{r['adjustment']:+.0f}"
        after = "-" if r["after"] is None else f"{r['after']}({band(r['after'])})"
        lines.append(f"| {r['id']} | {r['truth']} | {'O' if r['should_escalate'] else '-'} | {r['score']}({band(r['score'])}) → {after} | {adj} | {r['pattern'] or ''} |")
    open(out_path, "w", encoding="utf-8").write("\n".join(lines) + "\n")
    print()
    print("\n".join(lines[: 6 + len(esc_lines) + len(all_lines)]))
    print(f"\n보고서: {out_path}")


if __name__ == "__main__":
    # 인자를 안 주면 저장소에 들어 있는 기본 케이스 파일을 쓴다.
    main(sys.argv[1] if len(sys.argv) > 1 else str(Path(__file__).with_name("cases.json")))
