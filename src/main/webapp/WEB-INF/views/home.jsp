<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!doctype html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Spring 3 LLM Chat</title>
    <link rel="stylesheet" href="<c:url value='/resources/css/main.css'/>">
</head>
<body>
<main class="page">
    <section class="workspace">
        <header class="topbar">
            <div>
                <p class="eyebrow">Spring 3.1.1.RELEASE</p>
                <h1>로컬 LLM 스트리밍 채팅</h1>
            </div>
            <dl class="status-list">
                <dt>Model</dt>
                <dd><c:out value="${modelName}"/></dd>
                <dt>API</dt>
                <dd><c:out value="${apiUrl}"/></dd>
            </dl>
        </header>

        <form id="chatForm" class="chat-form" data-chat-url="<c:url value='/api/chat'/>">
            <label class="field-label" for="prompt">Prompt</label>
            <textarea id="prompt" name="prompt" rows="7" placeholder="모델에게 보낼 내용을 입력하세요."></textarea>
            <div class="actions">
                <span id="requestStatus" class="request-status" aria-live="polite">대기 중</span>
                <button id="submitButton" type="submit">생성</button>
            </div>
        </form>

        <section class="result-shell" aria-live="polite">
            <div class="result-header">
                <h2>Response</h2>
                <span id="streamState" class="stream-state">idle</span>
            </div>
            <pre id="result" class="result-output"></pre>
        </section>
    </section>
</main>
<script src="<c:url value='/resources/js/chat.js'/>"></script>
</body>
</html>
