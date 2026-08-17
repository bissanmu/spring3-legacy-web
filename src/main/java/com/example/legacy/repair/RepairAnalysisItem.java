package com.example.legacy.repair;

import java.util.Locale;

public class RepairAnalysisItem {

    private final String inputName;
    private final String cleanedName;
    private final String normalizedKey;
    private final String standardName;
    private final String sideCode;
    private final String positionCode;
    private final String actionCode;
    private final String categoryCode;
    private final boolean structuralSignal;
    private final String severityHint;
    private final String mappingSource;

    private RepairAnalysisItem(String inputName,
                               String cleanedName,
                               String normalizedKey,
                               String standardName,
                               String sideCode,
                               String positionCode,
                               String actionCode,
                               String categoryCode,
                               boolean structuralSignal,
                               String severityHint,
                               String mappingSource) {
        this.inputName = inputName;
        this.cleanedName = cleanedName;
        this.normalizedKey = normalizedKey;
        this.standardName = standardName;
        this.sideCode = sideCode;
        this.positionCode = positionCode;
        this.actionCode = actionCode;
        this.categoryCode = categoryCode;
        this.structuralSignal = structuralSignal;
        this.severityHint = severityHint;
        this.mappingSource = mappingSource;
    }

    public static RepairAnalysisItem fromMapping(String inputName, String cleanedName, RepairMapping mapping) {
        String text = cleanedName.toUpperCase(Locale.ROOT);
        String mappingSide = valueOrDefault(mapping.getSideCode(), "UNKNOWN");
        String mappingPosition = valueOrDefault(mapping.getPositionCode(), "UNKNOWN");
        String mappingAction = valueOrDefault(mapping.getActionCode(), "NOT_SPECIFIED");
        String mappingCategory = valueOrDefault(mapping.getCategoryCode(), "UNKNOWN");

        String inferredSide = RepairCodeRules.inferSide(text);
        String inferredPosition = RepairCodeRules.inferPosition(text);
        String explicitAction = RepairCodeRules.inferExplicitAction(cleanedName);
        String inferredAction = RepairCodeRules.inferAction(text);
        String inferredCategory = RepairCodeRules.inferCategory(text);

        String sideCode = preferKnown(inferredSide, mappingSide, "UNKNOWN");
        String positionCode = preferKnown(inferredPosition, mappingPosition, "UNKNOWN");
        String actionCode = preferKnown(explicitAction, preferKnown(mappingAction, inferredAction, "NOT_SPECIFIED"), "NOT_SPECIFIED");
        String categoryCode = preferKnown(mappingCategory, inferredCategory, "UNKNOWN");
        boolean structuralSignal = "Y".equalsIgnoreCase(mapping.getStructuralFlag()) || RepairCodeRules.inferStructural(text);
        String severityHint = higherSeverity(
                valueOrDefault(mapping.getSeverityHint(), "LOW"),
                RepairCodeRules.inferSeverity(text, actionCode));

        return new RepairAnalysisItem(
                inputName,
                cleanedName,
                RepairTextNormalizer.normalizeKey(cleanedName),
                valueOrDefault(mapping.getStandardName(), cleanedName),
                sideCode,
                positionCode,
                actionCode,
                categoryCode,
                structuralSignal,
                severityHint,
                "DICTIONARY");
    }

    public static RepairAnalysisItem fromHeuristic(String inputName, String cleanedName) {
        String normalizedKey = RepairTextNormalizer.normalizeKey(cleanedName);
        String text = cleanedName.toUpperCase(Locale.ROOT);
        String actionCode = RepairCodeRules.inferAction(text);
        boolean structural = RepairCodeRules.inferStructural(text);
        return new RepairAnalysisItem(
                inputName,
                cleanedName,
                normalizedKey,
                cleanedName,
                RepairCodeRules.inferSide(text),
                RepairCodeRules.inferPosition(text),
                actionCode,
                RepairCodeRules.inferCategory(text),
                structural,
                RepairCodeRules.inferSeverity(text, actionCode),
                "HEURISTIC");
    }

    private static String valueOrDefault(String value, String defaultValue) {
        return value == null || value.trim().length() == 0 ? defaultValue : value.trim();
    }

    private static String preferKnown(String primary, String fallback, String unknownValue) {
        if (primary != null && primary.length() > 0 && !unknownValue.equals(primary)) {
            return primary;
        }
        return fallback == null || fallback.length() == 0 ? unknownValue : fallback;
    }

    private static String higherSeverity(String left, String right) {
        return severityRank(right) > severityRank(left) ? right : left;
    }

    private static int severityRank(String severity) {
        if ("HIGH".equals(severity)) {
            return 3;
        }
        if ("MEDIUM".equals(severity) || "MEDIUM_OR_HIGH".equals(severity)) {
            return 2;
        }
        return 1;
    }

    public String getInputName() {
        return inputName;
    }

    public String getCleanedName() {
        return cleanedName;
    }

    public String getNormalizedKey() {
        return normalizedKey;
    }

    public String getStandardName() {
        return standardName;
    }

    public String getSideCode() {
        return sideCode;
    }

    public String getPositionCode() {
        return positionCode;
    }

    public String getActionCode() {
        return actionCode;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public boolean isStructuralSignal() {
        return structuralSignal;
    }

    public String getSeverityHint() {
        return severityHint;
    }

    public String getMappingSource() {
        return mappingSource;
    }

    public boolean isDictionaryMatched() {
        return "DICTIONARY".equals(mappingSource);
    }
}
