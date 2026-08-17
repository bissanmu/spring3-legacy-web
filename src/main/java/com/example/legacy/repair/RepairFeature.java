package com.example.legacy.repair;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RepairFeature {

    private String damageDirection;
    private String damageSide;
    private Map<String, Integer> repairActions = new LinkedHashMap<String, Integer>();
    private Map<String, Integer> categoryCounts = new LinkedHashMap<String, Integer>();
    private List<String> majorParts = Collections.emptyList();
    private boolean structuralSignal;
    private String severityHint;
    private List<String> evidence = Collections.emptyList();

    public String getDamageDirection() {
        return damageDirection;
    }

    public void setDamageDirection(String damageDirection) {
        this.damageDirection = damageDirection;
    }

    public String getDamageSide() {
        return damageSide;
    }

    public void setDamageSide(String damageSide) {
        this.damageSide = damageSide;
    }

    public Map<String, Integer> getRepairActions() {
        return repairActions;
    }

    public void setRepairActions(Map<String, Integer> repairActions) {
        this.repairActions = repairActions;
    }

    public Map<String, Integer> getCategoryCounts() {
        return categoryCounts;
    }

    public void setCategoryCounts(Map<String, Integer> categoryCounts) {
        this.categoryCounts = categoryCounts;
    }

    public List<String> getMajorParts() {
        return majorParts;
    }

    public void setMajorParts(List<String> majorParts) {
        this.majorParts = majorParts;
    }

    public boolean isStructuralSignal() {
        return structuralSignal;
    }

    public void setStructuralSignal(boolean structuralSignal) {
        this.structuralSignal = structuralSignal;
    }

    public String getSeverityHint() {
        return severityHint;
    }

    public void setSeverityHint(String severityHint) {
        this.severityHint = severityHint;
    }

    public List<String> getEvidence() {
        return evidence;
    }

    public void setEvidence(List<String> evidence) {
        this.evidence = evidence;
    }
}
