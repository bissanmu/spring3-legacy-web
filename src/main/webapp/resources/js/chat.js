(function () {
    var form = document.getElementById("chatForm");
    var promptInput = document.getElementById("prompt");
    var result = document.getElementById("result");
    var submitButton = document.getElementById("submitButton");
    var requestStatus = document.getElementById("requestStatus");
    var streamState = document.getElementById("streamState");
    var analysisState = document.getElementById("analysisState");
    var featureSummary = document.getElementById("featureSummary");
    var normalizedItems = document.getElementById("normalizedItems");
    var matchSummary = document.getElementById("matchSummary");
    var unmappedItems = document.getElementById("unmappedItems");
    var unmappedSummary = document.getElementById("unmappedSummary");

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
        clearAnalysis();
        submitButton.disabled = true;
        requestStatus.textContent = "정규화 중";
        streamState.textContent = "idle";
        analysisState.textContent = "running";

        var body = "prompt=" + encodeURIComponent(prompt);

        fetch(form.getAttribute("data-analyze-url"), {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8"
            },
            body: body
        }).then(function (response) {
            if (!response.ok) {
                return response.text().then(function (text) {
                    throw new Error(text || ("HTTP " + response.status));
                });
            }
            return response.json();
        }).then(function (analysis) {
            renderAnalysis(analysis);
            requestStatus.textContent = "LLM 요청 중";
            streamState.textContent = "streaming";
            return fetch(form.getAttribute("data-chat-url"), {
                method: "POST",
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8"
                },
                body: body
            });
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
            analysisState.textContent = "error";
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

    function clearAnalysis() {
        removeChildren(featureSummary);
        removeChildren(normalizedItems);
        removeChildren(unmappedItems);
        analysisState.textContent = "idle";
        matchSummary.textContent = "0 items";
        unmappedSummary.textContent = "0 items";
    }

    function renderAnalysis(analysis) {
        var feature = analysis.feature || {};
        renderFeatureSummary(analysis, feature);
        renderNormalizedItems(analysis.items || []);
        renderUnmappedItems(analysis.unmappedItems || []);
        analysisState.textContent = "done";
        matchSummary.textContent = (analysis.mappedCount || 0) + " mapped / " + (analysis.heuristicCount || 0) + " heuristic";
        unmappedSummary.textContent = (analysis.unmappedItems || []).length + " items";
    }

    function renderFeatureSummary(analysis, feature) {
        removeChildren(featureSummary);
        addFeatureRow("손상 방향", codeLabel(feature.damageDirection));
        addFeatureRow("세부 방향", codeLabel(feature.damageSide));
        addFeatureRow("작업 유형", countText(feature.repairActions));
        addFeatureRow("주요 부위", listText(feature.majorParts));
        addFeatureRow("골격/내부 신호", feature.structuralSignal ? "있음" : "낮음");
        addFeatureRow("사고 강도", codeLabel(feature.severityHint));
        addFeatureRow("근거", listText(feature.evidence));
        addFeatureRow("사전", (analysis.dictionarySize || 0) + " items");
        if (analysis.truncated) {
            addFeatureRow("입력 제한", "300건 분석, " + analysis.truncatedItemCount + "건 제외");
        }
    }

    function addFeatureRow(label, value) {
        var dt = document.createElement("dt");
        dt.textContent = label;
        var dd = document.createElement("dd");
        dd.textContent = value || "-";
        featureSummary.appendChild(dt);
        featureSummary.appendChild(dd);
    }

    function renderNormalizedItems(items) {
        removeChildren(normalizedItems);
        if (!items.length) {
            normalizedItems.appendChild(emptyText("정규화할 수리내역이 없습니다."));
            return;
        }

        var table = document.createElement("table");
        table.className = "items-table";
        var thead = document.createElement("thead");
        var headRow = document.createElement("tr");
        appendCell(headRow, "th", "수리내역");
        appendCell(headRow, "th", "부위");
        appendCell(headRow, "th", "작업");
        appendCell(headRow, "th", "방향");
        appendCell(headRow, "th", "좌우");
        appendCell(headRow, "th", "강도");
        appendCell(headRow, "th", "출처");
        thead.appendChild(headRow);
        table.appendChild(thead);

        var tbody = document.createElement("tbody");
        for (var i = 0; i < items.length; i += 1) {
            var item = items[i];
            var row = document.createElement("tr");
            appendCell(row, "td", item.standardName || item.cleanedName || "-");
            appendCell(row, "td", codeLabel(item.categoryCode));
            appendCell(row, "td", codeLabel(item.actionCode));
            appendCell(row, "td", codeLabel(item.positionCode));
            appendCell(row, "td", codeLabel(item.sideCode));
            appendCell(row, "td", codeLabel(item.severityHint));
            appendCell(row, "td", sourceLabel(item.mappingSource, item.structuralSignal));
            tbody.appendChild(row);
        }
        table.appendChild(tbody);
        normalizedItems.appendChild(table);
    }

    function renderUnmappedItems(items) {
        removeChildren(unmappedItems);
        if (!items.length) {
            var li = document.createElement("li");
            li.textContent = "미매핑 항목 없음";
            unmappedItems.appendChild(li);
            return;
        }
        for (var i = 0; i < items.length; i += 1) {
            var item = document.createElement("li");
            item.textContent = items[i];
            unmappedItems.appendChild(item);
        }
    }

    function appendCell(row, tagName, text) {
        var cell = document.createElement(tagName);
        cell.textContent = text || "-";
        row.appendChild(cell);
    }

    function emptyText(text) {
        var paragraph = document.createElement("p");
        paragraph.className = "empty-text";
        paragraph.textContent = text;
        return paragraph;
    }

    function removeChildren(node) {
        while (node.firstChild) {
            node.removeChild(node.firstChild);
        }
    }

    function countText(counts) {
        var parts = [];
        var key;
        if (!counts) {
            return "-";
        }
        for (key in counts) {
            if (Object.prototype.hasOwnProperty.call(counts, key)) {
                parts.push(codeLabel(key) + " " + counts[key] + "건");
            }
        }
        return parts.length ? parts.join(", ") : "-";
    }

    function listText(values) {
        return values && values.length ? values.map(codeLabel).join(", ") : "-";
    }

    function sourceLabel(source, structuralSignal) {
        var text = source === "DICTIONARY" ? "사전" : "휴리스틱";
        return structuralSignal ? text + " / 골격" : text;
    }

    function codeLabel(code) {
        var labels = {
            FRONT: "전방",
            REAR: "후방",
            SIDE: "측면",
            UNDER: "하부",
            UPPER: "상부",
            LEFT: "좌측",
            RIGHT: "우측",
            BOTH: "좌우",
            UNKNOWN: "미상",
            REPLACE: "교환",
            OVERHAUL: "오버홀",
            PAINT: "도장",
            PANEL_BEATING: "판금",
            REMOVE_INSTALL: "탈착",
            ADJUST: "조정/수리",
            NOT_SPECIFIED: "미지정",
            BUMPER: "범퍼",
            FENDER: "휀더",
            DOOR: "도어",
            HOOD: "후드",
            TRUNK_TAILGATE: "트렁크/테일게이트",
            QUARTER_PANEL: "쿼터패널",
            SIDE_PANEL: "사이드패널",
            PILLAR: "필러",
            LAMP: "램프",
            RADIATOR_SUPPORT: "라디에이터 서포트",
            PANEL_FRAME: "패널/프레임",
            SUSPENSION_STEERING: "서스펜션/조향",
            WHEEL_TIRE: "휠/타이어",
            GLASS: "유리",
            AIRBAG: "에어백",
            SEAT_BELT: "시트벨트",
            SEAT: "시트",
            EMBLEM_STICKER: "엠블렘/스티커",
            COATING_SEALER: "코팅/실러",
            MIRROR: "미러",
            GRILLE: "그릴",
            MOULDING_TRIM: "몰딩/트림",
            COVER_PROTECTOR: "커버/프로텍터",
            WIPER: "와이퍼",
            COOLING_AC: "냉각/A/C",
            BRAKE: "브레이크",
            EXHAUST: "배기",
            BATTERY: "배터리",
            VALVE_HOSE: "밸브/호스",
            BRACKET_MOUNT: "브라켓/마운트",
            POWERTRAIN: "파워트레인",
            ELECTRICAL: "전장",
            FASTENER_SEAL: "체결/실링",
            TOWING: "견인",
            RAIL: "레일",
            HIGH: "높음",
            MEDIUM: "중간",
            MEDIUM_OR_HIGH: "중간 이상",
            LOW: "낮음"
        };
        return labels[code] || code || "-";
    }
}());
