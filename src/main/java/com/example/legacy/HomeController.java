package com.example.legacy;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.legacy.repair.RepairAnalysisResult;
import com.example.legacy.repair.RepairAnalysisService;
import com.fasterxml.jackson.databind.ObjectMapper;

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
    private final RepairAnalysisService repairAnalysisService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public HomeController() {
        this(new LlmStreamClient(), RepairAnalysisService.createDefault());
    }

    public HomeController(LlmStreamClient llmStreamClient, RepairAnalysisService repairAnalysisService) {
        this.llmStreamClient = llmStreamClient;
        this.repairAnalysisService = repairAnalysisService;
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String home(Model model) {
        String serverTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        model.addAttribute("serverTime", serverTime);
        model.addAttribute("modelName", llmStreamClient.getModelName());
        model.addAttribute("apiUrl", llmStreamClient.getApiUrl());
        model.addAttribute("mappingCount", repairAnalysisService.getDictionarySize());
        return "home";
    }

    @RequestMapping(value = "/api/analyze", method = RequestMethod.POST)
    public void analyze(@RequestParam(value = "prompt", required = false) String prompt,
                        HttpServletResponse response) throws Exception {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");

        if (prompt == null || prompt.trim().length() == 0) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            objectMapper.writeValue(response.getWriter(), error("수리내역을 입력해주세요."));
            return;
        }

        RepairAnalysisResult analysisResult = repairAnalysisService.analyze(prompt);
        objectMapper.writeValue(response.getWriter(), analysisResult);
    }

    @RequestMapping(value = "/api/chat", method = RequestMethod.POST)
    public void chat(@RequestParam(value = "prompt", required = false) String prompt,
                     HttpServletResponse response) throws Exception {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/plain;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");

        final java.io.PrintWriter writer = response.getWriter();

        if (prompt == null || prompt.trim().length() == 0) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writer.write("프롬프트를 입력해주세요.");
            writer.flush();
            return;
        }

        RepairAnalysisResult analysisResult = repairAnalysisService.analyze(prompt);
        String llmPrompt = repairAnalysisService.buildLlmPrompt(prompt, analysisResult);

        try {
            llmStreamClient.streamChat(llmPrompt, new LlmStreamClient.ChunkConsumer() {
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

    private java.util.Map<String, String> error(String message) {
        java.util.Map<String, String> error = new java.util.LinkedHashMap<String, String>();
        error.put("error", message);
        return error;
    }
}
