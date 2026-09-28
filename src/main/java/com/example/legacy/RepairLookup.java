package com.example.legacy;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Maps a repair line to the standardized part and work fields from the 2-b output. */
public class RepairLookup {
    private static final Pattern WORK = Pattern.compile("\\((교환|탈착|도장|판금|조정|수리|오버홀|1/2OH|1/3OH|1/4OH)\\)\\s*$", Pattern.CASE_INSENSITIVE);
    private final JsonNode exact;
    private final JsonNode derived;

    public RepairLookup() throws Exception {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("briefing/repair-lookup.json");
        if (stream == null) throw new IllegalStateException("briefing/repair-lookup.json 파일이 없습니다.");
        try {
            JsonNode root = new ObjectMapper().readTree(stream);
            exact = root.path("exact");
            derived = root.path("derived");
        } finally { stream.close(); }
    }

    public List<Map<String, Object>> parse(String history) {
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        if (history == null) return items;
        for (String line : history.split("\\r?\\n")) {
            String raw = line.trim();
            if (raw.length() == 0) continue;
            if (items.size() >= 300) throw new IllegalArgumentException("수리항목은 최대 300줄까지 입력할 수 있습니다.");
            String lookupText = raw.replaceFirst("^R\\d{1,4}\\s*[:.)-]\\s*", "").trim();
            JsonNode match = exact.get(lookupText);
            if (match == null) match = derived.get(normalize(lookupText));
            Matcher workMatcher = WORK.matcher(lookupText);
            String work = workMatcher.find() ? workMatcher.group(1) : "미확인";
            String part = workMatcher.find(0) ? lookupText.substring(0, workMatcher.start()).trim() : lookupText;
            boolean mapped = match != null;
            if (mapped) {
                part = match.path("part").asText(part);
                work = match.path("work").asText(work);
                // The 2-b master stores the generic direction token. Recover the actual side from this line.
                if (lookupText.contains("(좌)")) part = part.replace("(방향)", "(좌)");
                if (lookupText.contains("(우)")) part = part.replace("(방향)", "(우)");
            }
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("id", String.format("R%03d", items.size() + 1));
            item.put("source", raw);
            item.put("part", part);
            item.put("work", work);
            item.put("mapped", mapped);
            items.add(item);
        }
        return items;
    }

    private String normalize(String text) {
        return text.trim().replaceAll("\\s+", "")
                .replace("(좌)", "(방향)").replace("(우)", "(방향)")
                .replaceAll("\\|(?:좌|우)(?=\\))", "|방향")
                .replaceAll("(?i)후론트|프론트", "프런트")
                .replaceAll("(?i)휀다|휀더", "펜더")
                .replaceAll("(?i)쇽업소버|쇼크업소버|쇼버", "쇼크업소버")
                .replaceAll("(?i)어셈블리|앗세이|앗셈블리|ASS'?Y", "ASSY")
                .replaceAll("(?i)에어콘", "에어컨")
                .replaceAll("(?i)밧데리", "배터리")
                .replaceAll("^문짝(?=\\(|$)", "도어");
    }
}