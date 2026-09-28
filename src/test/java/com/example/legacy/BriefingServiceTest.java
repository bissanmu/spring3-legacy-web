package com.example.legacy;

import static org.junit.Assert.*;
import java.io.IOException;
import java.util.Map;
import org.junit.Test;

public class BriefingServiceTest {
    @Test public void buildsPreviewContractFromMappedAndUnmappedLines() throws Exception {
        FakeGemma gemma = new FakeGemma();
        BriefingService service = new BriefingService(new RepairLookup(), gemma);
        Map<String, Object> result = service.create("테스트 차량", null,
            "전방감지센서(탈착)\n프런트범퍼커버(교환)\n미등록부품(도장)");
        assertEquals(3, gemma.calls);
        assertEquals("테스트 차량", result.get("vehicle_name"));
        Map<?, ?> features = (Map<?, ?>) result.get("features");
        assertEquals(3, features.get("repair_count"));
        assertEquals(2, features.get("mapped_count"));
        Map<?, ?> sources = (Map<?, ?>) result.get("source_items");
        assertEquals("전방감지센서(탈착)", sources.get("R001"));
        assertEquals("미등록부품(도장)", sources.get("R003"));
        Map<?, ?> labels = (Map<?, ?>) result.get("display_labels");
        assertEquals("전면", labels.get("impact_direction"));
        Map<?, ?> quality = (Map<?, ?>) result.get("quality");
        assertEquals(Boolean.FALSE, quality.get("publishable"));
        assertSame(result, service.get((String) result.get("accident_id")));
    }

    private static class FakeGemma extends LlmStreamClient {
        int calls;
        @Override public String completeJson(String system, String user) throws IOException {
            calls++;
            assertTrue(user.contains("R001"));
            return "{\"impact_direction\":\"FRONT\",\"damage_scope\":\"MEDIUM\",\"accident_type\":\"전면부 충돌\",\"summary\":\"전면 수리 기록이 있습니다.\",\"findings\":[{\"title\":\"범퍼 교환\",\"description\":\"범퍼커버가 교환되었습니다.\",\"evidence_ids\":[\"R002\"]},{\"title\":\"센서 작업\",\"description\":\"센서 탈착이 기록되었습니다.\",\"evidence_ids\":[\"R001\"]}]}";
        }
    }
}