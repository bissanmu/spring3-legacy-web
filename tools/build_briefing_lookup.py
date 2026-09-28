"""Build a conflict-aware Java lookup from the 2-b repair map."""
import csv
import json
import re
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
source = ROOT / "data" / "ai_briefing" / "REPAIR_ITEM_MAP.csv"
target = ROOT / "src" / "main" / "resources" / "briefing" / "repair-lookup.json"


def canonical(text):
    value = re.sub(r"\s+", "", text.strip())
    value = re.sub(r"\((?:좌|우)\)", "(방향)", value)
    value = re.sub(r"\|(?:좌|우)(?=\))", "|방향", value)
    rules = [
        (r"후론트|프론트", "프런트"), (r"휀다|휀더", "펜더"),
        (r"쇽업소버|쇼크업소버|쇼버", "쇼크업소버"),
        (r"어셈블리|앗세이|앗셈블리|ASS'?Y", "ASSY"),
        (r"에어콘", "에어컨"), (r"밧데리", "배터리"),
        (r"^문짝(?=\(|$)", "도어"),
    ]
    for pattern, replacement in rules:
        value = re.sub(pattern, replacement, value, flags=re.IGNORECASE)
    return value


exact = {}
derived_candidates = defaultdict(dict)
with source.open(encoding="utf-8-sig", newline="") as handle:
    for row in csv.DictReader(handle):
        value = {"part": row["standard_part_name"], "work": row["work_type_name"]}
        identity = (row["part_id"], row["work_type_id"])
        if row["original_text"].strip():
            exact[row["original_text"].strip()] = value
        for field in ("original_text", "normalized_key", "standardized_repair_text"):
            if row[field].strip():
                derived_candidates[canonical(row[field])][identity] = value

derived = {key: next(iter(values.values())) for key, values in derived_candidates.items() if len(values) == 1}
target.parent.mkdir(parents=True, exist_ok=True)
target.write_text(json.dumps({"exact": exact, "derived": derived}, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
print(f"{len(exact)} exact, {len(derived)} conflict-free derived mappings -> {target}")