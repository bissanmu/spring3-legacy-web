package com.example.legacy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.springframework.ui.ExtendedModelMap;

import com.example.legacy.repair.RepairAnalysisService;
import com.example.legacy.repair.RepairMappingCache;

public class HomeControllerTest {

    @Test
    public void homeReturnsHomeView() {
        HomeController controller = newController();
        ExtendedModelMap model = new ExtendedModelMap();

        String viewName = controller.home(model);

        assertEquals("home", viewName);
        assertNotNull(model.get("serverTime"));
        assertNotNull(model.get("mappingCount"));
    }

    @Test
    public void healthReturnsOk() {
        HomeController controller = newController();

        assertEquals("OK", controller.health());
    }

    @Test
    public void accidentBriefingPromptContainsInstructionsAndRepairHistory() {
        HomeController controller = newController();

        String prompt = controller.buildAccidentBriefingPrompt("리어범퍼(교환)\n백패널(판금)");

        assertEquals(
                "당신은 20년 경력의 자동차 사고 분석 및 정비 전문가입니다.\n"
                + "제공된 수리 내역 목록을 정밀하게 분석하여 사고 당시의 정황을 추론하십시오.\n"
                + "충격의 방향, 강도, 주요 파손 부위, 그리고 사고의 유형(예: 후방 추돌, 측면 충돌 등)을 논리적으로 설명하십시오.\n\n"
                + "응답은 반드시 아래 세 개 항목만 사용하고, 제목의 문구와 순서를 그대로 유지하십시오.\n"
                + "## 1. 종합 분석 요약 (결론)\n"
                + "전체 분석 결과와 핵심 근거를 요약하십시오.\n\n"
                + "## 2. 충격의 방향 및 강도 추론\n"
                + "충격 방향, 예상 강도, 주요 파손 부위와 판단 근거를 설명하십시오.\n\n"
                + "## 3. 사고 유형 최종 추론\n"
                + "가장 가능성 높은 사고 유형과 그렇게 판단한 논리적 근거를 설명하십시오.\n\n"
                + "수리 내역 목록:\n리어범퍼(교환)\n백패널(판금)",
                prompt);
    }

    private HomeController newController() {
        return new HomeController(new LlmStreamClient(), new RepairAnalysisService(new RepairMappingCache()));
    }
}
