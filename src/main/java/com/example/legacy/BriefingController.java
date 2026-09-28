package com.example.legacy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
public class BriefingController {
    private final BriefingService service;
    private final ObjectMapper mapper = new ObjectMapper();
    public BriefingController(BriefingService service) { this.service = service; }

    @RequestMapping(value = "/api/briefings", method = RequestMethod.POST)
    public void create(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            if (request.getContentType() == null || !request.getContentType().toLowerCase().startsWith("application/json"))
                throw new IllegalArgumentException("JSON 형식으로 요청해 주세요.");
            Map<String, Object> body;
            InputStream input = request.getInputStream();
            try { body = mapper.readValue(input, new TypeReference<Map<String, Object>>() {}); }
            finally { input.close(); }
            write(response, HttpServletResponse.SC_OK,
                service.create(value(body, "vehicleName"), value(body, "accidentDate"), value(body, "repairHistory")));
        } catch (IllegalArgumentException ex) {
            write(response, HttpServletResponse.SC_BAD_REQUEST, error(ex.getMessage()));
        } catch (IOException ex) {
            String message = ex.getMessage();
            if (message != null && message.contains("Read timed out"))
                message = "Gemma 서버가 180초 동안 결과를 보내지 않았습니다. 모델 서버의 생성 상태를 확인해 주세요.";
            write(response, HttpServletResponse.SC_BAD_GATEWAY, error(message));
        }
    }

    @RequestMapping(value = "/api/briefings/{id}", method = RequestMethod.GET)
    public void get(@PathVariable("id") String id, HttpServletResponse response) throws IOException {
        Map<String, Object> result = service.get(id);
        if (result == null) write(response, HttpServletResponse.SC_NOT_FOUND,
            error("결과를 찾을 수 없습니다. 서버 재시작 후에는 다시 분석해야 합니다."));
        else write(response, HttpServletResponse.SC_OK, result);
    }

    private void write(HttpServletResponse response, int status, Map<String, Object> body) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        byte[] bytes = mapper.writeValueAsBytes(body);
        response.setContentLength(bytes.length);
        OutputStream output = response.getOutputStream();
        output.write(bytes);
        output.flush();
    }
    private String value(Map<String, Object> request, String key) {
        Object value = request.get(key);
        return value == null ? null : String.valueOf(value);
    }
    private Map<String, Object> error(String message) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("error", message);
        return body;
    }
}