package com.example.legacy;

import static org.junit.Assert.*;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class RepairLookupTest {
    @Test public void usesMasterMappingAndPreservesSource() throws Exception {
        RepairLookup lookup = new RepairLookup();
        List<Map<String, Object>> items = lookup.parse("전방감지센서(탈착)\n프런트범퍼 (도장)\n미등록부품(교환)\n헤드램프어셈블리(좌)(탈착)");
        assertEquals(4, items.size());
        assertEquals("R001", items.get(0).get("id"));
        assertEquals("전방감지센서", items.get(0).get("part"));
        assertEquals("탈착", items.get(0).get("work"));
        assertEquals("프런트범퍼 (도장)", items.get(1).get("source"));
        assertEquals("프런트범퍼", items.get(1).get("part"));
        assertEquals("도장", items.get(1).get("work"));
        assertEquals(Boolean.FALSE, items.get(2).get("mapped"));
        assertEquals("미등록부품", items.get(2).get("part"));
        assertEquals("교환", items.get(2).get("work"));
        assertEquals(Boolean.TRUE, items.get(3).get("mapped"));
        assertEquals("헤드램프ASSY(좌)", items.get(3).get("part"));
    }
}