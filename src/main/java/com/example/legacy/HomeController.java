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

@Controller
public class HomeController {

    private final LlmStreamClient llmStreamClient = new LlmStreamClient();

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String home(Model model) {
        String serverTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        model.addAttribute("serverTime", serverTime);
        model.addAttribute("modelName", llmStreamClient.getModelName());
        model.addAttribute("apiUrl", llmStreamClient.getApiUrl());
        return "home";
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

        try {
            llmStreamClient.streamChat(prompt, new LlmStreamClient.ChunkConsumer() {
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
}
