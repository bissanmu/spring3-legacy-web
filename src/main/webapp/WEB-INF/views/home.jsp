<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!doctype html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>수리내역 사고정황 추론</title>
    <link rel="stylesheet" href="<c:url value='/resources/css/main.css'/>">
</head>
<body>
<main class="page">
    <section class="workspace">
        <header class="topbar">
            <div>
                <p class="eyebrow">Repair Signal Analyzer</p>
                <h1>수리내역 사고정황 추론</h1>
            </div>
            <dl class="status-list">
                <dt>Model</dt>
                <dd><c:out value="${modelName}"/></dd>
                <dt>API</dt>
                <dd><c:out value="${apiUrl}"/></dd>
                <dt>Dictionary</dt>
                <dd><c:out value="${mappingCount}"/> items</dd>
            </dl>
        </header>

        <form id="chatForm" class="chat-form"
              data-analyze-url="<c:url value='/api/analyze'/>"
              data-chat-url="<c:url value='/api/chat'/>">
            <label class="field-label" for="prompt">수리내역</label>
            <textarea id="prompt" name="prompt" rows="9" placeholder="프론트범퍼 교환&#10;후드 교환&#10;헤드램프 RH 교환&#10;라디에이터 서포트 판금"></textarea>
            <div class="actions">
                <span id="requestStatus" class="request-status" aria-live="polite">대기 중</span>
                <button id="submitButton" type="submit">추론</button>
            </div>
        </form>

        <section class="analysis-grid" aria-live="polite">
            <section class="analysis-panel">
                <div class="result-header">
                    <h2>정규화 Feature</h2>
                    <span id="analysisState" class="stream-state">idle</span>
                </div>
                <dl id="featureSummary" class="feature-summary"></dl>
            </section>

            <section class="analysis-panel">
                <div class="result-header">
                    <h2>정규화 항목</h2>
                    <span id="matchSummary" class="stream-state">0 items</span>
                </div>
                <div id="normalizedItems" class="normalized-items"></div>
            </section>

            <section class="analysis-panel">
                <div class="result-header">
                    <h2>미매핑 수리내역</h2>
                    <span id="unmappedSummary" class="stream-state">0 items</span>
                </div>
                <ul id="unmappedItems" class="unmapped-items"></ul>
            </section>
        </section>

        <section class="result-shell" aria-live="polite">
            <div class="result-header">
                <h2>LLM 사고정황 추론</h2>
                <span id="streamState" class="stream-state">idle</span>
            </div>
            <pre id="result" class="result-output"></pre>
        </section>
    </section>
</main>
<script src="<c:url value='/resources/js/chat.js'/>"></script>
</body>
</html>
