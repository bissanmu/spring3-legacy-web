# 수리내역 매핑 사전 생성 결과

- 원본 파일: `작업내역.md`
- 원본 인코딩: `UTF-8`
- 전체 라인 수: 60017
- 중복 제거 후 사전 항목 수: 55288
- CSV: `data/repair_mapping_dictionary.csv`
- JSON: `data/repair_mapping_dictionary.json`
- SQL: `data/repair_mapping_seed.sql`

## 컬럼

| 컬럼 | 설명 |
| --- | --- |
| mapping_id | 사전 항목 ID |
| raw_name | 원본 수리내역 정제 문자열 |
| normalized_key | 매칭용 정규화 키 |
| standard_name | 표준 표시명 초기값 |
| side_code | LEFT, RIGHT, BOTH, UNKNOWN |
| position_code | FRONT, REAR, SIDE, UNDER, UPPER, UNKNOWN |
| action_code | REPLACE, PAINT, PANEL_BEATING, REMOVE_INSTALL, ADJUST, NOT_SPECIFIED |
| category_code | 부품/작업 카테고리 |
| structural_flag | 골격/내부 영향 신호 여부 |
| severity_hint | LOW, MEDIUM, HIGH |
| active_flag | 사용 여부 |

## 주의

이 사전은 원본 텍스트에서 휴리스틱으로 생성한 초기 매핑이다. 운영 품질을 위해 미매핑/오분류 항목을 검수하면서 표준명, 카테고리, 사고 강도 신호를 보정해야 한다.
