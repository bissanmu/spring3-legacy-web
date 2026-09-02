package com.example.legacy;

import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HomeController {

    private static final String ACCIDENT_BRIEFING_PROMPT =
            "당신은 20년 경력의 자동차 사고 분석 및 정비 전문가입니다.\n"
            + "제공된 수리 내역 목록을 정밀하게 분석하여 사고 당시의 정황을 추론하십시오.\n"
            + "충격의 방향, 강도, 주요 파손 부위, 그리고 사고의 유형(예: 후방 추돌, 측면 충돌 등)을 논리적으로 설명하십시오.\n\n"
            + "응답은 반드시 아래 세 개 항목만 사용하고, 제목의 문구와 순서를 그대로 유지하십시오.\n"
            + "## 1. 종합 분석 요약 (결론)\n"
            + "전체 분석 결과와 핵심 근거를 요약하십시오.\n\n"
            + "## 2. 충격의 방향 및 강도 추론\n"
            + "충격 방향, 예상 강도, 주요 파손 부위와 판단 근거를 설명하십시오.\n\n"
            + "## 3. 사고 유형 최종 추론\n"
            + "가장 가능성 높은 사고 유형과 그렇게 판단한 논리적 근거를 설명하십시오.";

    private final LlmStreamClient llmStreamClient;

    public HomeController() {
        this(new LlmStreamClient());
    }

    public HomeController(LlmStreamClient llmStreamClient) {
        this.llmStreamClient = llmStreamClient;
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String home() {
        return "redirect:/sample.html";
    }

    @ResponseBody
    @RequestMapping(value = "/health", method = RequestMethod.GET)
    public String health() {
        return "OK";
    }

    @RequestMapping(value = "/api/accident-briefing", method = RequestMethod.POST)
    public void accidentBriefing(@RequestParam(value = "repairHistory", required = false) String repairHistory,
                                 HttpServletResponse response) throws Exception {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/plain;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");

        final java.io.PrintWriter writer = response.getWriter();

        if (repairHistory == null || repairHistory.trim().length() == 0) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writer.write("분석할 수리내역이 없습니다.");
            writer.flush();
            return;
        }

        try {
            llmStreamClient.streamChat(buildAccidentBriefingPrompt(repairHistory),
                    new LlmStreamClient.ChunkConsumer() {
                        public void onChunk(String chunk) throws java.io.IOException {
                            writer.write(chunk);
                            writer.flush();
                            response.flushBuffer();
                        }
                    });
        } catch (Exception ex) {
            if (!response.isCommitted()) {
                response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
            }
            writer.write("\n\n[LLM 호출 오류] " + ex.getMessage());
            writer.flush();
        }
    }

    String buildAccidentBriefingPrompt(String repairHistory) {
        return ACCIDENT_BRIEFING_PROMPT + "\n\n수리 내역 목록:\n" + repairHistory.trim();
    }
}
