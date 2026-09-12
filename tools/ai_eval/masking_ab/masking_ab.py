"""마스킹 A/B — 같은 대화를 두 가지 마스킹으로 각각 AI 에 보내 판정이 달라지는지 비교한다.

A = 예전 마스킹(하이픈 있는 전화번호 + http 링크만), B = 지금 마스킹(PrivacyMasker).
cases_pii.json 의 masked_current / masked_extended 를 그대로 쓴다 —
두 본문은 실제 규칙 엔진과 마스킹 코드로 만들어 둔 값이다.

질문: 개인정보를 더 가리면 AI 문맥 판단이 나빠지는가?
보는 것: 보정치(context_score_adjustment) 차이, 최종 점수 구간 변화, 수법 문구 변화.
"""

import json
import os
import statistics
import sys
import time
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(REPO / "backend"))

import anthropic  # noqa: E402
import claude_analyzer as ca  # noqa: E402

CASES = Path(__file__).with_name("cases_pii.json")
OUT = Path(__file__).with_name("result_latest.json")


def load_key() -> str:
    env = os.environ.get("ANTHROPIC_API_KEY", "").strip()
    if env:
        return env
    with open(REPO / "android" / "local.properties", encoding="utf-8") as f:
        for line in f:
            if line.startswith("ANTHROPIC_API_KEY="):
                return line.split("=", 1)[1].strip()
    raise SystemExit("키를 찾지 못했습니다")


def band(score: int) -> str:
    return "높음" if score >= 66 else ("중간" if score >= 31 else "낮음")


def judge(client, case, turns):
    inp = ca.AnalysisInput(
        masked_text=turns[-1],
        recent_turns=turns,
        device_base_score=case["score"],
        device_matched_ids=case["matched_ids"],
        device_applied_combo_ids=case["combo_ids"],
        category_hint=case["category"] or None,
    )
    started = time.time()
    try:
        v = ca.analyze_with_claude(inp, client, effort="low", timeout=60)
        return {
            "adjustment": v.score_adjustment,
            "summary": v.summary,
            "pattern": v.detected_pattern,
            "error": None,
            "elapsed": round(time.time() - started, 1),
        }
    except ca.AnalysisUnavailable as e:
        return {"adjustment": None, "summary": None, "pattern": None, "error": e.reason,
                "elapsed": round(time.time() - started, 1)}


def main() -> None:
    cases = json.load(open(CASES, encoding="utf-8"))
    client = anthropic.Anthropic(api_key=load_key(), max_retries=2)

    rows = []
    for i, case in enumerate(cases, 1):
        a = judge(client, case, case["masked_current"])
        b = judge(client, case, case["masked_extended"])
        after_a = None if a["adjustment"] is None else max(0, min(100, int(case["score"] + a["adjustment"])))
        after_b = None if b["adjustment"] is None else max(0, min(100, int(case["score"] + b["adjustment"])))
        rows.append({**case, "a": a, "b": b, "after_a": after_a, "after_b": after_b})
        print(
            f"[{i:2}/{len(cases)}] {case['id']:<12} {case['truth']:<3} 규칙 {case['score']:>3} | "
            f"A {a['adjustment']:+.0f}→{after_a:>3}({band(after_a)})  "
            f"B {b['adjustment']:+.0f}→{after_b:>3}({band(after_b)})",
            flush=True,
        )

    json.dump(rows, open(OUT, "w", encoding="utf-8"), ensure_ascii=False, indent=2)

    ok = [r for r in rows if r["a"]["error"] is None and r["b"]["error"] is None]
    diffs = [abs(r["a"]["adjustment"] - r["b"]["adjustment"]) for r in ok]
    band_changed = [r for r in ok if band(r["after_a"]) != band(r["after_b"])]
    worse_scam = [r for r in ok if r["truth"] == "사기" and r["after_b"] < r["after_a"]]
    worse_benign = [r for r in ok if r["truth"] == "일상" and r["after_b"] > r["after_a"]]

    print()
    print("=== 요약 ===")
    print(f"성공 {len(ok)}/{len(rows)}건")
    print(f"보정치 차이: 평균 {statistics.mean(diffs):.1f}점, 최대 {max(diffs):.0f}점")
    print(f"확장 마스킹으로 등급이 바뀐 케이스: {len(band_changed)}건")
    for r in band_changed:
        print(f"  - {r['id']} {r['truth']}: {band(r['after_a'])} → {band(r['after_b'])}")
    print(f"사기인데 점수가 내려간 케이스: {len(worse_scam)}건")
    for r in worse_scam:
        print(f"  - {r['id']}: {r['after_a']} → {r['after_b']}  (B 설명: {r['b']['summary']})")
    print(f"일상인데 점수가 올라간 케이스: {len(worse_benign)}건")
    for r in worse_benign:
        print(f"  - {r['id']}: {r['after_a']} → {r['after_b']}  (B 설명: {r['b']['summary']})")
    print()
    print("=== 수법 문구 비교 ===")
    for r in ok:
        mark = "같음" if (r["a"]["pattern"] or "") == (r["b"]["pattern"] or "") else "다름"
        print(f"  {r['id']:<12} [{mark}] A: {r['a']['pattern']}  |  B: {r['b']['pattern']}")
    print(f"\n결과 저장: {OUT}")


if __name__ == "__main__":
    main()
