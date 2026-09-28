package com.example.legacy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Creates a model-led briefing from standardized repair items and retains its source evidence. */
public class BriefingService {
    private static final String SYSTEM =
        "보험사고 수리내역을 읽고 소비자를 위한 사고이력 브리핑을 작성하세요.\n" +
        "수리 부위와 작업 내용을 종합해 충격 방향, 피해 규모, 가능성 높은 사고 유형을 직접 추정하세요.\n" +
        "판단의 이유를 쉬운 말로 설명하고, 주요 근거마다 실제 수리항목 ID를 붙이세요.\n" +
        "확실하지 않은 점은 추정이라고 밝히고, 기록에 없는 사실은 만들지 마세요.\n" +
        "탈착만 된 부품을 손상품으로 단정하거나, 기록에 없는 충돌 대상을 특정하지 마세요.\n" +
        "사고 유형과 설명 문장은 한국어로 쓰세요. JSON 필드는 impact_direction(FRONT/REAR/LEFT_SIDE/RIGHT_SIDE/MULTIPLE/UNKNOWN), damage_scope(SMALL/MEDIUM/LARGE/UNKNOWN), accident_type, summary, findings(2~4개: title, description, evidence_ids)입니다.";
    private static final Set<String> DIRECTIONS = new HashSet<String>(Arrays.asList("FRONT", "REAR", "LEFT_SIDE", "RIGHT_SIDE", "MULTIPLE", "UNKNOWN"));
    private static final Set<String> SCOPES = new HashSet<String>(Arrays.asList("SMALL", "MEDIUM", "LARGE", "UNKNOWN"));
    private static final Map<String, String> DIRECTION_LABELS = new HashMap<String, String>();
    private static final Map<String, String> SCOPE_LABELS = new HashMap<String, String>();
    static {
        DIRECTION_LABELS.put("FRONT", "전면"); DIRECTION_LABELS.put("REAR", "후면");
        DIRECTION_LABELS.put("LEFT_SIDE", "좌측면"); DIRECTION_LABELS.put("RIGHT_SIDE", "우측면");
        DIRECTION_LABELS.put("MULTIPLE", "복합"); DIRECTION_LABELS.put("UNKNOWN", "판단 어려움");
        SCOPE_LABELS.put("SMALL", "작은 규모"); SCOPE_LABELS.put("MEDIUM", "중간 규모");
        SCOPE_LABELS.put("LARGE", "큰 규모"); SCOPE_LABELS.put("UNKNOWN", "판단 어려움");
    }

    private final RepairLookup lookup;
    private final LlmStreamClient llm;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, Map<String, Object>> results = new ConcurrentHashMap<String, Map<String, Object>>();

    public BriefingService(RepairLookup lookup, LlmStreamClient llm) { this.lookup = lookup; this.llm = llm; }

    public Map<String, Object> create(String vehicleName, String accidentDate, String repairHistory) throws IOException {
        List<Map<String, Object>> items = lookup.parse(repairHistory);
        if (items.isEmpty()) throw new IllegalArgumentException("수리항목을 한 줄 이상 입력해 주세요.");
        Map<String, Object> input = new LinkedHashMap<String, Object>();
        List<List<String>> repairItems = new ArrayList<List<String>>();
        Map<String, Integer> workCounts = new LinkedHashMap<String, Integer>();
        List<String> unmappedIds = new ArrayList<String>();
        Map<String, String> sources = new LinkedHashMap<String, String>();
        int mappedCount = 0;
        for (Map<String, Object> item : items) {
            String id = (String) item.get("id");
            String work = (String) item.get("work");
            repairItems.add(Arrays.asList(id, (String) item.get("part"), work));
            Integer count = workCounts.get(work);
            workCounts.put(work, count == null ? 1 : count + 1);
            if (Boolean.TRUE.equals(item.get("mapped"))) mappedCount++; else unmappedIds.add(id);
            sources.put(id, (String) item.get("source"));
        }
        input.put("repair_count", items.size());
        input.put("mapped_count", mappedCount);
        input.put("work_type_counts", workCounts);
        input.put("item_format", Arrays.asList("근거ID", "표준부품명", "작업"));
        input.put("repair_items", repairItems);
        if (!unmappedIds.isEmpty()) input.put("unmapped_ids", unmappedIds);
        String itemJson = mapper.writeValueAsString(input);
        String first = llm.completeJson(SYSTEM, itemJson);
        String review = llm.completeJson(SYSTEM,
            "제공된 표준화 수리항목과 초안을 대조해 판단과 근거 설명을 수정하세요. " +
            "탈착만 된 항목을 손상·교환으로 쓰거나, 근거 ID가 다른 부품을 가리키거나, " +
            "기록에 없는 속도·충돌 대상을 특정한 표현을 바로잡으세요. 같은 JSON 필드로 완성본만 답하세요.\n" +
            "수리항목: " + itemJson + "\n초안: " + first);
        String last = llm.completeJson(SYSTEM,
            "최종 브리핑을 간결하게 다시 쓰세요. 각 근거는 제공된 수리항목으로 확인되는 부품과 작업만 설명하고, " +
            "R번호는 evidence_ids에만 쓰세요. 탈착을 손상으로 표현하거나 기록에 없는 속도·충돌 대상을 특정하지 마세요. " +
            "같은 JSON 필드로 답하세요.\n수리항목: " + itemJson + "\n검토안: " + review);
        JsonNode analysis;
        try { analysis = mapper.readTree(last); }
        catch (Exception ex) { throw new IOException("Gemma가 유효한 JSON을 반환하지 않았습니다.", ex); }
        validate(analysis, sources.keySet());
        String id = UUID.randomUUID().toString();
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("schema_version", "2.0");
        result.put("accident_id", id);
        result.put("accident_date", blankToNull(accidentDate));
        result.put("vehicle_name", blankToNull(vehicleName) == null ? "차량 정보 미입력" : vehicleName.trim());
        result.put("analysis", analysis);
        Map<String, String> labels = new LinkedHashMap<String, String>();
        labels.put("impact_direction", DIRECTION_LABELS.get(analysis.path("impact_direction").asText()));
        labels.put("damage_scope", SCOPE_LABELS.get(analysis.path("damage_scope").asText()));
        labels.put("accident_type", analysis.path("accident_type").asText());
        result.put("display_labels", labels);
        Map<String, Object> quality = new LinkedHashMap<String, Object>();
        quality.put("automatic_status", "passed");
        quality.put("validation_errors", Collections.emptyList());
        quality.put("manual_review", "required");
        quality.put("publishable", false);
        result.put("quality", quality);
        result.put("source_items", sources);
        Map<String, Object> features = new LinkedHashMap<String, Object>();
        features.put("repair_count", items.size());
        features.put("mapped_count", mappedCount);
        features.put("unmapped_ids", unmappedIds);
        features.put("work_type_counts", workCounts);
        result.put("features", features);
        result.put("normalized_items", repairItems);
        if (results.size() >= 100) results.clear();
        results.put(id, result);
        return result;
    }

    public Map<String, Object> get(String id) { return results.get(id); }

    private void validate(JsonNode analysis, Set<String> validIds) throws IOException {
        if (analysis == null || !analysis.isObject()) throw new IOException("Gemma 결과 형식이 잘못되었습니다.");
        if (!DIRECTIONS.contains(analysis.path("impact_direction").asText())) throw new IOException("충격 방향 코드가 잘못되었습니다.");
        if (!SCOPES.contains(analysis.path("damage_scope").asText())) throw new IOException("피해 규모 코드가 잘못되었습니다.");
        if (analysis.path("accident_type").asText().trim().length() == 0 || analysis.path("summary").asText().trim().length() == 0)
            throw new IOException("사고 유형 또는 요약이 비어 있습니다.");
        JsonNode findings = analysis.path("findings");
        if (!findings.isArray() || findings.size() < 2 || findings.size() > 4) throw new IOException("주요 판단은 2~4개여야 합니다.");
        for (JsonNode finding : findings) {
            if (finding.path("title").asText().trim().length() == 0 || finding.path("description").asText().trim().length() == 0)
                throw new IOException("판단 제목 또는 설명이 비어 있습니다.");
            JsonNode ids = finding.path("evidence_ids");
            if (!ids.isArray() || ids.size() == 0) throw new IOException("판단의 근거 ID가 없습니다.");
            for (JsonNode evidence : ids) if (!validIds.contains(evidence.asText())) throw new IOException("존재하지 않는 근거 ID: " + evidence.asText());
        }
    }

    private String blankToNull(String value) { return value == null || value.trim().isEmpty() ? null : value.trim(); }
}