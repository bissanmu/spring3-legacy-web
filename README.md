# AI 사고이력 브리핑 · Spring 3

Spring 3.1.1 XML MVC 프로젝트에 보험사고 수리내역 입력과 Gemma 4 결과 화면을 추가했습니다. `http://localhost:8080/` 또는 `/briefing.html`에서 차량명, 사고일자(선택), 수리항목을 입력합니다. 수리항목은 한 줄에 하나씩 넣거나 6건 예시와 같은 TXT 파일을 선택해 불러옵니다. 분석 후 `/briefing-result.html?id=...`에서 6건 미리보기와 같은 3개 판단 카드, 요약, 번호형 근거, 근거 원문, JSON을 볼 수 있습니다. 종전 카히스토리 샘플은 `/sample.html`에 있습니다.

## 실행

Java 8 이상과 Maven이 필요합니다. Gemma 4 호환 서버가 `http://localhost:8000/v1/chat/completions`에서 스트리밍 응답을 제공해야 합니다.

```powershell
mvn test
mvn tomcat7:run
```

브라우저 주소: `http://localhost:8080/briefing.html`

설정: `LLM_API_URL` (기본 `http://localhost:8000/v1/chat/completions`), `MODEL_NAME` (기본 `/models/gemma-4-E4B-it-AWQ-INT4`), `LLM_API_KEY` (필요한 경우). Java 시스템 속성 `llm.api.url`, `model.name`, `llm.api.key`도 지원합니다.

## 처리 흐름

1. 입력 원문을 줄 단위로 분리하고 `R001`부터 근거 ID를 부여합니다.
2. `data/ai_briefing/REPAIR_ITEM_MAP.csv`의 2-b 표준 부품명·작업 유형을 조회합니다. 수리내역 원문은 `source_items`에 그대로 보존합니다. 일치하지 않는 항목은 원문 부품명과 작업 유형을 사용하고 `unmapped_ids`에 표시합니다.
3. Gemma 4에 표준화 수리항목, 근거 ID, 작업 유형 건수를 보내 초안·검토안·최종안을 생성합니다. 충격 방향·피해 규모·사고 유형은 모델이 판단합니다.
4. 최종 JSON 필드와 근거 ID 존재 여부를 검사한 뒤 결과를 화면에 표시합니다. 검사는 내용의 사실 여부를 보증하지 않으므로 `manual_review=required`, `publishable=false`로 표시합니다.

표준화 조회용 `src/main/resources/briefing/repair-lookup.json`은 `python tools/build_briefing_lookup.py`로 다시 생성할 수 있습니다. 실제 모델 호출은 SSE 스트림으로 받습니다.

## API

- `POST /api/briefings`: JSON `{ "vehicleName": "...", "accidentDate": "2026-09-28", "repairHistory": "부품(교환)\n부품(도장)" }` → 결과 JSON
- `GET /api/briefings/{id}`: 생성된 결과 JSON

결과는 서버 메모리에 보관되므로 서버를 재시작하면 조회할 수 없습니다. 최대 100건 보관하며 그 다음 요청 때 이전 결과를 비웁니다. 입력은 최대 300줄입니다.

## 주요 파일

- `src/main/java/com/example/legacy/RepairLookup.java`: 2-b 표준화 데이터 조회
- `src/main/java/com/example/legacy/BriefingService.java`: 모델 입력, 3단계 호출, 결과 검증
- `src/main/java/com/example/legacy/BriefingController.java`: JSON API
- `src/main/java/com/example/legacy/LlmStreamClient.java`: OpenAI 호환 SSE 클라이언트
- `src/main/webapp/WEB-INF/views/briefing.html`: 차량 입력 화면
- `src/main/webapp/WEB-INF/views/briefing-result.html`: 결과 화면
- `src/main/webapp/WEB-INF/views/sample.html`: 종전 카히스토리 샘플