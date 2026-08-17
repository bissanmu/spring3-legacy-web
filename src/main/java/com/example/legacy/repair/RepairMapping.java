package com.example.legacy.repair;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RepairMapping {

    @JsonProperty("mapping_id")
    private long mappingId;

    @JsonProperty("raw_name")
    private String rawName;

    @JsonProperty("normalized_key")
    private String normalizedKey;

    @JsonProperty("standard_name")
    private String standardName;

    @JsonProperty("side_code")
    private String sideCode;

    @JsonProperty("position_code")
    private String positionCode;

    @JsonProperty("action_code")
    private String actionCode;

    @JsonProperty("category_code")
    private String categoryCode;

    @JsonProperty("structural_flag")
    private String structuralFlag;

    @JsonProperty("severity_hint")
    private String severityHint;

    @JsonProperty("active_flag")
    private String activeFlag;

    public RepairMapping() {
    }

    public long getMappingId() {
        return mappingId;
    }

    public void setMappingId(long mappingId) {
        this.mappingId = mappingId;
    }

    public String getRawName() {
        return rawName;
    }

    public void setRawName(String rawName) {
        this.rawName = rawName;
    }

    public String getNormalizedKey() {
        return normalizedKey;
    }

    public void setNormalizedKey(String normalizedKey) {
        this.normalizedKey = normalizedKey;
    }

    public String getStandardName() {
        return standardName;
    }

    public void setStandardName(String standardName) {
        this.standardName = standardName;
    }

    public String getSideCode() {
        return sideCode;
    }

    public void setSideCode(String sideCode) {
        this.sideCode = sideCode;
    }

    public String getPositionCode() {
        return positionCode;
    }

    public void setPositionCode(String positionCode) {
        this.positionCode = positionCode;
    }

    public String getActionCode() {
        return actionCode;
    }

    public void setActionCode(String actionCode) {
        this.actionCode = actionCode;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public void setCategoryCode(String categoryCode) {
        this.categoryCode = categoryCode;
    }

    public String getStructuralFlag() {
        return structuralFlag;
    }

    public void setStructuralFlag(String structuralFlag) {
        this.structuralFlag = structuralFlag;
    }

    public String getSeverityHint() {
        return severityHint;
    }

    public void setSeverityHint(String severityHint) {
        this.severityHint = severityHint;
    }

    public String getActiveFlag() {
        return activeFlag;
    }

    public void setActiveFlag(String activeFlag) {
        this.activeFlag = activeFlag;
    }

    public boolean isActive() {
        return activeFlag == null || "Y".equalsIgnoreCase(activeFlag);
    }
}
