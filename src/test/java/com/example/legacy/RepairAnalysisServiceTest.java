package com.example.legacy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.example.legacy.repair.RepairAnalysisResult;
import com.example.legacy.repair.RepairAnalysisService;
import com.example.legacy.repair.RepairMappingCache;

public class RepairAnalysisServiceTest {

    @Test
    public void analyzeBuildsFeatureWithHeuristicFallback() {
        RepairAnalysisService service = new RepairAnalysisService(new RepairMappingCache());

        RepairAnalysisResult result = service.analyze(
                "프론트범퍼 교환\n"
                        + "후드 교환\n"
                        + "헤드램프 RH 교환\n"
                        + "라디에이터 서포트 판금");

        assertEquals(4, result.getItems().size());
        assertEquals(4, result.getUnmappedItems().size());
        assertEquals("FRONT", result.getFeature().getDamageDirection());
        assertEquals("RIGHT", result.getFeature().getDamageSide());
        assertEquals(Integer.valueOf(3), result.getFeature().getRepairActions().get("REPLACE"));
        assertEquals(Integer.valueOf(1), result.getFeature().getRepairActions().get("PANEL_BEATING"));
        assertTrue(result.getFeature().isStructuralSignal());
        assertEquals("HIGH", result.getFeature().getSeverityHint());
        assertTrue(result.getFeature().getMajorParts().contains("RADIATOR_SUPPORT"));
    }

    @Test
    public void buildPromptIncludesNormalizedResultAndResponseRules() {
        RepairAnalysisService service = new RepairAnalysisService(new RepairMappingCache());
        RepairAnalysisResult result = service.analyze("라디에이터 서포트 판금");

        String prompt = service.buildLlmPrompt("라디에이터 서포트 판금", result);

        assertTrue(prompt.contains("[원문 수리내역]"));
        assertTrue(prompt.contains("[정규화 Feature]"));
        assertTrue(prompt.contains("structuralSignal: true"));
        assertTrue(prompt.contains("단정 표현은 피하고"));
        assertFalse(prompt.contains("null"));
    }

    @Test
    public void dictionaryMatchIgnoresTrailingParenthesizedAction() throws Exception {
        RepairMappingCache cache = new RepairMappingCache();
        cache.reload();
        RepairAnalysisService service = new RepairAnalysisService(cache);

        RepairAnalysisResult result = service.analyze("ABS 하이드로릭 유니트(탈착)");

        assertEquals(1, result.getMappedCount());
        assertEquals(0, result.getHeuristicCount());
        assertEquals("REMOVE_INSTALL", result.getItems().get(0).getActionCode());
        assertEquals("ABS 하이드로릭 유니트", result.getItems().get(0).getStandardName());
    }

    @Test
    public void explicitParenthesizedActionOverridesAssyText() throws Exception {
        RepairMappingCache cache = new RepairMappingCache();
        cache.reload();
        RepairAnalysisService service = new RepairAnalysisService(cache);

        RepairAnalysisResult result = service.analyze("엔진 Ass'y(V형)(탈착)");

        assertEquals(1, result.getMappedCount());
        assertEquals("REMOVE_INSTALL", result.getItems().get(0).getActionCode());
    }
}
