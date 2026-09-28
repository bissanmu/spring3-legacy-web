package com.example.legacy;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LlmStreamClient {

    private static final String DEFAULT_API_URL = "http://localhost:8000/v1/chat/completions";
    private static final String DEFAULT_MODEL_NAME = "/models/gemma-4-E4B-it-AWQ-INT4";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String apiUrl;
    private final String modelName;
    private final String apiKey;

    public LlmStreamClient() {
        this.apiUrl = readConfig("llm.api.url", "LLM_API_URL", DEFAULT_API_URL);
        this.modelName = readConfig("model.name", "MODEL_NAME", DEFAULT_MODEL_NAME);
        this.apiKey = readConfig("llm.api.key", "LLM_API_KEY", "");
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public String getModelName() {
        return modelName;
    }

    public void streamChat(String prompt, ChunkConsumer consumer) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(0);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
        connection.setRequestProperty("Accept", "text/event-stream");
        if (apiKey.length() > 0) {
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        }

        byte[] requestBody = objectMapper.writeValueAsBytes(createRequestBody(prompt));
        connection.setRequestProperty("Content-Length", String.valueOf(requestBody.length));

        OutputStream outputStream = connection.getOutputStream();
        try {
            outputStream.write(requestBody);
        } finally {
            outputStream.close();
        }

        int statusCode = connection.getResponseCode();
        if (statusCode >= 400) {
            throw new IOException("HTTP " + statusCode + ": " + readError(connection));
        }

        InputStream inputStream = connection.getInputStream();
        try {
            readServerSentEvents(inputStream, consumer);
        } finally {
            inputStream.close();
            connection.disconnect();
        }
    }

    /** Gemma server must be read as SSE even when the caller wants a complete JSON result. */
    public String completeJson(final String system, final String user) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(180000);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
        connection.setRequestProperty("Accept", "text/event-stream");
        if (apiKey.length() > 0) connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        List<Map<String, String>> messages = new ArrayList<Map<String, String>>();
        Map<String, String> systemMessage = new LinkedHashMap<String, String>();
        systemMessage.put("role", "system"); systemMessage.put("content", system);
        messages.add(systemMessage);
        Map<String, String> userMessage = new LinkedHashMap<String, String>();
        userMessage.put("role", "user"); userMessage.put("content", user);
        messages.add(userMessage);
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("model", modelName);
        body.put("messages", messages);
        body.put("temperature", 0.3);
        body.put("max_tokens", 1200);
        body.put("stream", true);
        Map<String, Object> thinking = new LinkedHashMap<String, Object>();
        thinking.put("enable_thinking", false);
        body.put("chat_template_kwargs", thinking);
        Map<String, String> responseFormat = new LinkedHashMap<String, String>();
        responseFormat.put("type", "json_object");
        body.put("response_format", responseFormat);
        byte[] bytes = objectMapper.writeValueAsBytes(body);
        connection.setRequestProperty("Content-Length", String.valueOf(bytes.length));
        OutputStream output = connection.getOutputStream();
        try { output.write(bytes); } finally { output.close(); }
        int status = connection.getResponseCode();
        if (status >= 400) {
            try { throw new IOException("Gemma HTTP " + status + ": " + readError(connection)); }
            finally { connection.disconnect(); }
        }
        final StringBuilder result = new StringBuilder();
        InputStream input = connection.getInputStream();
        try {
            readServerSentEvents(input, new ChunkConsumer() {
                @Override public void onChunk(String chunk) { result.append(chunk); }
            });
        } finally { input.close(); connection.disconnect(); }
        String text = result.toString().trim();
        if (text.startsWith("```")) {
            int firstBreak = text.indexOf('\n');
            int lastFence = text.lastIndexOf("```");
            if (firstBreak >= 0 && lastFence > firstBreak) text = text.substring(firstBreak + 1, lastFence).trim();
        }
        if (text.length() == 0) throw new IOException("Gemma 응답이 비어 있습니다.");
        return text;
    }
    private Map<String, Object> createRequestBody(String prompt) {
        Map<String, Object> userMessage = new LinkedHashMap<String, Object>();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);

        List<Map<String, Object>> messages = new ArrayList<Map<String, Object>>();
        messages.add(userMessage);

        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("model", modelName);
        body.put("messages", messages);
        body.put("temperature", 0.7);
        body.put("max_tokens", 1024);
        body.put("stream", true);
        return body;
    }

    private void readServerSentEvents(InputStream inputStream, ChunkConsumer consumer) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.length() == 0 || line.startsWith(":")) {
                continue;
            }

            String payload = line.startsWith("data:") ? line.substring(5).trim() : line.trim();
            if (payload.length() == 0) {
                continue;
            }
            if ("[DONE]".equals(payload)) {
                break;
            }

            String chunk = extractChunk(payload);
            if (chunk.length() > 0) {
                consumer.onChunk(chunk);
            }
        }
    }

    private String extractChunk(String payload) throws IOException {
        JsonNode root = objectMapper.readTree(payload);
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.size() == 0) {
            return "";
        }

        JsonNode choice = choices.get(0);
        JsonNode content = choice.path("delta").path("content");
        if (!content.isMissingNode() && !content.isNull()) {
            return content.asText();
        }

        JsonNode text = choice.path("text");
        if (!text.isMissingNode() && !text.isNull()) {
            return text.asText();
        }

        return "";
    }

    private String readError(HttpURLConnection connection) throws IOException {
        InputStream stream = connection.getErrorStream();
        if (stream == null) {
            return "";
        }
        try {
            return readAll(stream);
        } finally {
            stream.close();
        }
    }

    private String readAll(InputStream stream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int read;
        while ((read = stream.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }

    private String readConfig(String systemPropertyName, String environmentName, String defaultValue) {
        String value = System.getProperty(systemPropertyName);
        if (value == null || value.trim().length() == 0) {
            value = System.getenv(environmentName);
        }
        if (value == null || value.trim().length() == 0) {
            return defaultValue;
        }
        return value.trim();
    }

    public interface ChunkConsumer {
        void onChunk(String chunk) throws IOException;
    }
}
