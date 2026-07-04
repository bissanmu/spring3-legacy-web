(function () {
    var form = document.getElementById("chatForm");
    var promptInput = document.getElementById("prompt");
    var result = document.getElementById("result");
    var submitButton = document.getElementById("submitButton");
    var requestStatus = document.getElementById("requestStatus");
    var streamState = document.getElementById("streamState");

    if (!form || !promptInput || !result || !submitButton) {
        return;
    }

    form.addEventListener("submit", function (event) {
        event.preventDefault();

        var prompt = promptInput.value.replace(/^\s+|\s+$/g, "");
        if (!prompt) {
            requestStatus.textContent = "프롬프트를 입력하세요";
            promptInput.focus();
            return;
        }

        result.textContent = "";
        submitButton.disabled = true;
        requestStatus.textContent = "요청 중";
        streamState.textContent = "streaming";

        var body = "prompt=" + encodeURIComponent(prompt);

        fetch(form.getAttribute("data-chat-url"), {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8"
            },
            body: body
        }).then(function (response) {
            if (!response.ok) {
                throw new Error("HTTP " + response.status);
            }
            if (!response.body || !window.TextDecoder) {
                return response.text().then(function (text) {
                    appendChunk(text);
                });
            }
            return readStream(response.body.getReader());
        }).then(function () {
            requestStatus.textContent = "완료";
            streamState.textContent = "done";
        }).catch(function (error) {
            requestStatus.textContent = "오류";
            streamState.textContent = "error";
            appendChunk("\n[브라우저 오류] " + error.message);
        }).then(function () {
            submitButton.disabled = false;
        });
    });

    function readStream(reader) {
        var decoder = new TextDecoder("utf-8");

        function pump() {
            return reader.read().then(function (resultChunk) {
                if (resultChunk.done) {
                    var tail = decoder.decode();
                    if (tail) {
                        appendChunk(tail);
                    }
                    return;
                }

                appendChunk(decoder.decode(resultChunk.value, { stream: true }));
                return pump();
            });
        }

        return pump();
    }

    function appendChunk(text) {
        result.textContent += text;
        result.scrollTop = result.scrollHeight;
    }
}());
