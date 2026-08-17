package com.example.legacy.repair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class RepairAnalysisService {

    private static final int MAX_REPAIR_ITEMS = 300;

    private final RepairMappingCache repairMappingCache;

    public RepairAnalysisService(RepairMappingCache repairMappingCache) {
        this.repairMappingCache = repairMappingCache;
    }

    public static RepairAnalysisService createDefault() {
        RepairMappingCache cache = new RepairMappingCache();
        try {
            cache.reload();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load repair mapping dictionary", ex);
        }
        return new RepairAnalysisService(cache);
    }

    public RepairAnalysisResult analyze(String inputText) {
        List<String> parsedItems = RepairTextNormalizer.splitItems(inputText);
        boolean truncated = parsedItems.size() > MAX_REPAIR_ITEMS;
        int truncatedItemCount = truncated ? parsedItems.size() - MAX_REPAIR_ITEMS : 0;
        if (truncated) {
            parsedItems = new ArrayList<String>(parsedItems.subList(0, MAX_REPAIR_ITEMS));
        }

        List<RepairAnalysisItem> items = new ArrayList<RepairAnalysisItem>();
        List<String> unmappedItems = new ArrayList<String>();
        for (String item : parsedItems) {
            String cleaned = RepairTextNormalizer.cleanText(item);
            RepairMapping mapping = repairMappingCache.find(cleaned);
            RepairAnalysisItem analysisItem;
            if (mapping != null) {
                analysisItem = RepairAnalysisItem.fromMapping(item, cleaned, mapping);
            } else {
                analysisItem = RepairAnalysisItem.fromHeuristic(item, cleaned);
                unmappedItems.add(cleaned);
            }
            items.add(analysisItem);
        }

        return new RepairAnalysisResult(
                inputText,
                parsedItems,
                items,
                unmappedItems,
                buildFeature(items),
                repairMappingCache.size(),
                truncated,
                truncatedItemCount);
    }

    public String buildLlmPrompt(String rawInput, RepairAnalysisResult analysisResult) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("차량 수리내역만 근거로 사고정황을 추론한다.\n");
        prompt.append("단정 표현은 피하고, 가능성 중심으로 설명한다.\n");
        prompt.append("수리내역과 정규화 Feature에 없는 사실은 만들지 않는다.\n\n");

        prompt.append("[원문 수리내역]\n");
        appendList(prompt, analysisResult.getOriginalItems(), 300);
        if (analysisResult.isTruncated()) {
            prompt.append("- 입력이 300건을 초과해 ")
                    .append(analysisResult.getTruncatedItemCount())
                    .append("건은 이번 분석에서 제외됨\n");
        }
        prompt.append("\n");

        RepairFeature feature = analysisResult.getFeature();
        prompt.append("[정규화 Feature]\n");
        prompt.append("- damageDirection: ").append(feature.getDamageDirection()).append(" (")
                .append(label(feature.getDamageDirection())).append(")\n");
        prompt.append("- damageSide: ").append(feature.getDamageSide()).append(" (")
                .append(label(feature.getDamageSide())).append(")\n");
        prompt.append("- repairActions: ").append(formatCounts(feature.getRepairActions())).append("\n");
        prompt.append("- majorParts: ").append(feature.getMajorParts()).append("\n");
        prompt.append("- structuralSignal: ").append(feature.isStructuralSignal()).append("\n");
        prompt.append("- severityHint: ").append(feature.getSeverityHint()).append(" (")
                .append(label(feature.getSeverityHint())).append(")\n");
        prompt.append("- evidence: ").append(feature.getEvidence()).append("\n\n");

        prompt.append("[정규화 항목]\n");
        for (RepairAnalysisItem item : analysisResult.getItems()) {
            prompt.append("- ")
                    .append(item.getCleanedName())
                    .append(" => source=").append(item.getMappingSource())
                    .append(", standard=").append(item.getStandardName())
                    .append(", category=").append(item.getCategoryCode())
                    .append(", action=").append(item.getActionCode())
                    .append(", position=").append(item.getPositionCode())
                    .append(", side=").append(item.getSideCode())
                    .append(", structural=").append(item.isStructuralSignal())
                    .append(", severity=").append(item.getSeverityHint())
                    .append("\n");
        }
        prompt.append("\n");

        prompt.append("[미매핑 수리내역]\n");
        if (analysisResult.getUnmappedItems().isEmpty()) {
            prompt.append("- 없음\n");
        } else {
            appendList(prompt, analysisResult.getUnmappedItems(), 80);
        }
        prompt.append("\n");

        prompt.append("[응답 형식]\n");
        prompt.append("1. 사고정황 요약\n");
        prompt.append("2. 충격 방향 및 손상 영역 추정\n");
        prompt.append("3. 주요 근거\n");
        prompt.append("4. 골격/내부 영향 가능성\n");
        prompt.append("5. 유의사항\n");
        return prompt.toString();
    }

    public int getDictionarySize() {
        return repairMappingCache.size();
    }

    private RepairFeature buildFeature(List<RepairAnalysisItem> items) {
        Map<String, Integer> positionCounts = new LinkedHashMap<String, Integer>();
        Map<String, Integer> sideCounts = new LinkedHashMap<String, Integer>();
        Map<String, Integer> actionCounts = new LinkedHashMap<String, Integer>();
        Map<String, Integer> categoryCounts = new LinkedHashMap<String, Integer>();
        boolean structuralSignal = false;
        String highestSeverity = "LOW";

        for (RepairAnalysisItem item : items) {
            count(positionCounts, item.getPositionCode(), "UNKNOWN");
            count(sideCounts, item.getSideCode(), "UNKNOWN");
            count(actionCounts, item.getActionCode(), "NOT_SPECIFIED");
            count(categoryCounts, item.getCategoryCode(), "UNKNOWN");
            structuralSignal = structuralSignal || item.isStructuralSignal();
            highestSeverity = higherSeverity(highestSeverity, item.getSeverityHint());
        }

        RepairFeature feature = new RepairFeature();
        feature.setDamageDirection(dominantCode(positionCounts, "UNKNOWN"));
        feature.setDamageSide(dominantCode(sideCounts, "UNKNOWN"));
        feature.setRepairActions(actionCounts);
        feature.setCategoryCounts(categoryCounts);
        feature.setMajorParts(topCodes(categoryCounts, 8));
        feature.setStructuralSignal(structuralSignal);
        feature.setSeverityHint(toFeatureSeverity(highestSeverity));
        feature.setEvidence(buildEvidence(items));
        return feature;
    }

    private void count(Map<String, Integer> counts, String code, String ignoredCode) {
        if (code == null || code.length() == 0 || ignoredCode.equals(code)) {
            return;
        }
        Integer current = counts.get(code);
        counts.put(code, current == null ? 1 : current + 1);
    }

    private String dominantCode(Map<String, Integer> counts, String defaultValue) {
        String winner = defaultValue;
        int winnerCount = 0;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > winnerCount) {
                winner = entry.getKey();
                winnerCount = entry.getValue();
            }
        }
        return winner;
    }

    private List<String> topCodes(Map<String, Integer> counts, int maxCount) {
        List<String> results = new ArrayList<String>();
        Map<String, Integer> remaining = new LinkedHashMap<String, Integer>(counts);
        while (!remaining.isEmpty() && results.size() < maxCount) {
            String winner = dominantCode(remaining, null);
            if (winner == null) {
                break;
            }
            results.add(winner);
            remaining.remove(winner);
        }
        return results;
    }

    private String higherSeverity(String current, String candidate) {
        return severityRank(candidate) > severityRank(current) ? candidate : current;
    }

    private int severityRank(String severity) {
        if ("HIGH".equals(severity)) {
            return 3;
        }
        if ("MEDIUM".equals(severity) || "MEDIUM_OR_HIGH".equals(severity)) {
            return 2;
        }
        return 1;
    }

    private String toFeatureSeverity(String severity) {
        if ("HIGH".equals(severity)) {
            return "HIGH";
        }
        if ("MEDIUM".equals(severity)) {
            return "MEDIUM_OR_HIGH";
        }
        return "LOW";
    }

    private List<String> buildEvidence(List<RepairAnalysisItem> items) {
        LinkedHashSet<String> evidence = new LinkedHashSet<String>();
        for (RepairAnalysisItem item : items) {
            if (item.isStructuralSignal() || "HIGH".equals(item.getSeverityHint())) {
                evidence.add(item.getCleanedName());
            }
        }
        for (RepairAnalysisItem item : items) {
            if ("REPLACE".equals(item.getActionCode()) || "PANEL_BEATING".equals(item.getActionCode())) {
                evidence.add(item.getCleanedName());
            }
            if (evidence.size() >= 8) {
                break;
            }
        }
        for (RepairAnalysisItem item : items) {
            if (evidence.size() >= 8) {
                break;
            }
            evidence.add(item.getCleanedName());
        }
        return new ArrayList<String>(evidence);
    }

    private void appendList(StringBuilder builder, List<String> values, int maxCount) {
        int count = Math.min(values.size(), maxCount);
        for (int i = 0; i < count; i++) {
            builder.append("- ").append(values.get(i)).append("\n");
        }
        if (values.size() > maxCount) {
            builder.append("- ... 외 ").append(values.size() - maxCount).append("건\n");
        }
    }

    private String formatCounts(Map<String, Integer> counts) {
        if (counts.isEmpty()) {
            return "없음";
        }
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(entry.getKey()).append(": ").append(entry.getValue());
        }
        return builder.toString();
    }

    private String label(String code) {
        if ("FRONT".equals(code)) {
            return "전방";
        }
        if ("REAR".equals(code)) {
            return "후방";
        }
        if ("SIDE".equals(code)) {
            return "측면";
        }
        if ("UNDER".equals(code)) {
            return "하부";
        }
        if ("UPPER".equals(code)) {
            return "상부";
        }
        if ("LEFT".equals(code)) {
            return "좌측";
        }
        if ("RIGHT".equals(code)) {
            return "우측";
        }
        if ("BOTH".equals(code)) {
            return "좌우";
        }
        if ("OVERHAUL".equals(code)) {
            return "오버홀";
        }
        if ("HIGH".equals(code)) {
            return "높음";
        }
        if ("MEDIUM_OR_HIGH".equals(code)) {
            return "중간 이상 가능성";
        }
        if ("LOW".equals(code)) {
            return "낮음";
        }
        return "알 수 없음";
    }
}
