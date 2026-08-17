# 수리내역 기반 사고정황 추론 구현 계획

## 1. 프로젝트 목적

이 프로젝트의 목적은 차량 수리내역을 바탕으로 사고정황을 추론하는 것이다.

카히스토리 사고이력조회 서비스의 부가서비스로, 사용자가 확인하기 어려운 수리내역 목록을 분석해 다음과 같은 정보를 설명한다.

- 사고 충격 방향 추정
- 주요 손상 부위 추정
- 사고 강도 가능성 설명
- 외판 손상인지 골격/내부 영향 가능성이 있는지 구분
- 수리내역에 근거한 자연어 사고정황 요약

LLM은 최종 설명을 생성하는 역할로 사용하되, 원문 수리내역만 그대로 전달하지 않고 정규화된 수리 신호와 함께 전달한다.

## 2. 입력 데이터 특성

프롬프트에 들어올 수 있는 전체 수리내역의 범위는 `작업내역.md`에 담겨 있다.

이 수리내역과 연관된 공임(작업)은 다음의 항목이 올 수 있다.

- 1/2 OH(오버홀)
- 1/3 OH(오버홀)
- 1/4 OH(오버홀)
- 오버홀
- 수리
- 조정
- 도장
- 탈착
- 교환
- 판금

실제 추론 요청에서는 이 전체 범위 중 랜덤하게 1개에서 300개 사이의 수리내역이 입력될 수 있다.

수리내역은 비정형 데이터이므로 표현 방식이 일정하지 않을 수 있다.

예상되는 비정형 요소:

- 같은 부품의 여러 표현
- 좌우 표현 차이: RH, LH, 우측, 좌측, 운전석, 조수석
- 작업 표현 차이: 교환, 교체, 판금, 도장, 탈착, 수정, 조정
- 공백, 괄호, 특수문자, 약어
- 부품명과 작업명이 붙어 있는 표현
- 동일 의미지만 다른 표기

## 3. 구조화 및 정규화 필요성

수리내역만으로 사고정황을 추론하려면 단순 텍스트 검색보다 구조화된 사고 신호가 필요하다.

예를 들어 다음 수리내역은 단순 문자열이 아니라 전방 충격 가능성을 나타내는 신호로 해석되어야 한다.

```text
프론트범퍼 교환
후드 교환
헤드램프 RH 교환
라디에이터 서포트 판금
```

정규화 후에는 다음과 같은 신호로 바꿀 수 있다.

```text
손상 방향: 전방
세부 방향: 우측 전방 가능성
작업 유형: 교환 3건, 판금 1건
주요 부위: 범퍼, 후드, 헤드램프, 라디에이터 서포트
사고 강도 신호: 중간 이상 가능성
골격/내부 영향: 라디에이터 서포트 포함
```

LLM에 원문만 전달하면 결과가 그럴듯할 수는 있지만, 표현 차이에 따라 품질이 흔들릴 수 있다. 서비스 품질을 위해서는 정규화 계층이 필요하다.

## 4. 추천 방식

추천 방식은 1번과 2번 중 하나만 선택하는 것이 아니라 하이브리드 방식이다.

```text
비정형 수리내역
-> 프로그램 로직으로 1차 정제
-> 매핑 테이블/표준 사전으로 구조화 및 정규화
-> 정규화 결과와 원문 일부를 함께 LLM에 전달
-> 사고정황 추론
```

즉, 프로그램 로직으로 입력을 정제하고, 매핑 테이블 기반 표준 사전으로 품질을 보정한다.

## 5. 방식 비교

### 5.1 매핑 테이블 기반 구조화

장점:

- 표준화 품질이 좋다.
- 운영 중 매핑 보정이 쉽다.
- 결과 설명과 근거 추적이 쉽다.
- 같은 입력에 대해 일관된 결과를 만들기 좋다.

단점:

- 매핑 테이블 관리 비용이 있다.
- 요청마다 DB를 직접 조회하면 속도와 부하 문제가 생길 수 있다.
- 신규 표현, 오타, 약어는 지속적으로 보정해야 한다.

### 5.2 프로그램 로직 기반 정제

장점:

- 처리 속도가 빠르다.
- DB 부하가 적다.
- 초기 구현이 빠르다.
- 공백, 특수문자, 좌우 표기 같은 단순 변형에 유연하게 대응할 수 있다.

단점:

- 룰이 많아질수록 유지보수가 어려워질 수 있다.
- 정규화 품질을 보장하기 어렵다.
- 예외 케이스가 계속 늘어날 수 있다.

## 6. 최종 권장 아키텍처

최종 구조는 다음을 권장한다.

```text
DB 매핑 테이블 또는 수리내역 사전
-> Spring 애플리케이션 기동 시 전체 메모리 캐시 로딩
-> 요청 중에는 DB 조회 없이 메모리 Map 조회
-> 정규화 Feature 생성
-> LLM 프롬프트 생성
-> 스트리밍 응답으로 사고정황 출력
```

핵심은 DB를 요청마다 조회하지 않는 것이다.

DB는 관리 저장소로 사용하고, 실제 요청 처리에서는 JVM 메모리에 올라간 캐시를 사용한다.

## 7. 메모리 캐시 로딩 전략

Spring 3 레거시 웹 프로젝트에서도 메모리 캐시 로딩은 충분히 가능하다.

대표 구현 방식:

- Spring Bean 초기화 시 로딩
- `InitializingBean` 사용
- XML `init-method` 사용
- `@PostConstruct` 사용
- `volatile Map`을 이용한 안전한 캐시 교체
- 관리자 기능 또는 주기 작업을 통한 캐시 리로드

권장 구조:

```text
repair_mapping 테이블
-> 서버 기동 시 전체 조회
-> normalizeKey 기준으로 HashMap 구성
-> 요청 중에는 cache.get(normalizedRepairName)
-> 매핑 변경 시 reload()
```

예시 코드 방향:

```java
public class RepairMappingCache {

    private volatile Map<String, RepairMapping> cache =
        Collections.emptyMap();

    public void reload() {
        List<RepairMapping> rows = repairMappingDao.findAll();

        Map<String, RepairMapping> next = new HashMap<String, RepairMapping>();
        for (RepairMapping row : rows) {
            next.put(normalize(row.getRawName()), row);
        }

        cache = Collections.unmodifiableMap(next);
    }

    public RepairMapping find(String rawName) {
        return cache.get(normalize(rawName));
    }
}
```

이 방식이면 수리내역 300개가 입력되어도 DB 조회 300번이 아니라 JVM 메모리 `HashMap` 조회 300번으로 처리된다.

## 8. 정규화 파이프라인

요청 처리 흐름은 다음과 같이 설계한다.

```text
1. 원문 수리내역 입력
2. 항목 분리
3. 문자열 1차 정제
4. 좌우/방향/작업명 정규화
5. 표준 수리 항목 매칭
6. 사고 추론용 Feature 생성
7. LLM 프롬프트 구성
8. LLM 스트리밍 응답 출력
```

1차 정제 예:

- 앞뒤 공백 제거
- 중복 공백 제거
- 괄호/특수문자 정리
- 영문 대소문자 통일
- RH/LH 변환
- 교체/교환 같은 작업명 통일

정규화 대상 예:

- 부품명
- 작업 유형
- 좌우 방향
- 전후 방향
- 손상 영역
- 외판/골격/내부 부품 여부
- 사고 강도 신호

## 9. Feature 설계 예시

정규화 결과는 LLM에 넣기 쉬운 형태로 만든다.

예시 Feature:

```text
damageDirection: FRONT
damageSide: RIGHT
repairActions:
  - REPLACE: 3
  - PAINT: 1
  - PANEL_BEATING: 1
majorParts:
  - FRONT_BUMPER
  - HOOD
  - HEAD_LAMP_RIGHT
  - RADIATOR_SUPPORT
structuralSignal: true
severityHint: MEDIUM_OR_HIGH
evidence:
  - 라디에이터 서포트 판금
  - 후드 교환
  - 헤드램프 RH 교환
```

LLM은 이 Feature와 원문을 함께 보고 사고정황을 설명한다.

## 10. LLM 프롬프트 구성 방향

LLM에는 원문만 전달하지 않고 다음 정보를 함께 전달한다.

```text
[원문 수리내역]
- 프론트범퍼 교환
- 헤드램프 RH 교환
- 라디에이터 서포트 판금

[정규화 결과]
- 손상 방향: 전방
- 세부 방향: 우측 전방 가능성
- 작업 유형: 교환 2건, 판금 1건
- 골격/내부 영향 신호: 라디에이터 서포트 포함
- 사고 강도: 중간 이상 가능성

[응답 지침]
- 단정하지 말고 가능성 중심으로 설명한다.
- 수리내역에 없는 사실은 만들지 않는다.
- 주요 근거를 함께 제시한다.
- 사용자가 이해하기 쉬운 문장으로 설명한다.
```

이렇게 하면 LLM은 판단의 전부를 담당하지 않고, 정규화된 사고 신호를 바탕으로 자연어 설명을 생성한다.

## 11. 운영 성능 전략

DB 부하를 줄이기 위한 원칙:

- 요청마다 매핑 테이블을 조회하지 않는다.
- 전체 매핑 테이블을 애플리케이션 메모리에 로딩한다.
- `HashMap` 또는 `ConcurrentHashMap` 기반으로 조회한다.
- 매핑 변경 시 캐시를 통째로 교체한다.
- 캐시 리로드는 관리자 기능 또는 배치로 처리한다.

성능 관점에서 요청당 1~300개 수리내역은 메모리 조회로 충분히 처리 가능하다.

주의할 점:

- 캐시 크기가 커질 경우 메모리 사용량 측정 필요
- 다중 WAS 환경에서는 캐시 리로드 동기화 필요
- 매핑 테이블 변경 시 적용 시점 관리 필요
- LLM 호출 시간이 전체 응답 시간의 주요 병목이 될 가능성이 높음

## 12. 단계별 구현 계획

### 1단계: 수리내역 사전 분석

- `수리내역_전체.TXT` 항목 분리
- 중복 제거
- 빈도 분석
- 주요 부품명/작업명/좌우 표현 추출

### 2단계: 정규화 모델 설계

- 표준 부품 코드 정의
- 작업 유형 코드 정의
- 손상 방향 코드 정의
- 사고 강도 신호 정의
- Feature JSON 구조 정의

### 3단계: 메모리 캐시 구현

- `RepairMappingCache` 구현
- 서버 기동 시 로딩
- `find()` 조회 API 구현
- 캐시 리로드 메서드 구현

### 4단계: 정제 파이프라인 구현

- 문자열 정제
- 좌우/방향/작업명 정규화
- 매핑 캐시 조회
- 미매핑 항목 분리
- Feature 생성

### 5단계: LLM 프롬프트 개선

- 원문 수리내역 포함
- 정규화 결과 포함
- 사고정황 응답 포맷 고정
- 단정 금지, 근거 제시 지침 추가

### 6단계: 화면 개선

- 수리내역 입력 영역
- 정규화 결과 확인 영역
- LLM 추론 결과 스트리밍 영역
- 미매핑 수리내역 표시

### 7단계: 운영 보정

- 미매핑 항목 로그 수집
- 매핑 테이블 보강
- 사고정황 품질 샘플링
- 프롬프트와 Feature 가중치 개선

## 13. 결론

이 서비스는 단순히 LLM에 수리내역 원문을 전달하는 방식보다, 정규화 엔진과 LLM을 조합하는 방식이 적합하다.

권장 결론:

```text
프로그램 로직으로 비정형 수리내역을 1차 정제하고,
매핑 테이블을 메모리 캐시로 로딩해 표준화하며,
정규화된 Feature와 원문을 함께 LLM에 전달해 사고정황을 추론한다.
```

이 구조는 속도, 품질, 운영 보정 가능성을 모두 확보할 수 있다.

## 14. 구현 진행 내역

작성 기준: 2026-07-17

### 14.1 완료한 구현

- Spring 3 레거시 XML MVC 구조에 수리내역 분석 계층을 추가했다.
- `com.example.legacy.repair` 패키지를 추가해 다음 책임을 분리했다.
  - `RepairMappingCache`: 매핑 사전을 애플리케이션 기동 시 메모리에 로딩하고 `HashMap`으로 조회한다.
  - `RepairTextNormalizer`: 수리내역 입력 분리, 공백 정리, 매칭용 정규화 키 생성을 담당한다.
  - `RepairCodeRules`: 좌우, 전후 방향, 작업 유형, 부품 카테고리, 골격 신호, 강도 힌트를 휴리스틱으로 추론한다.
  - `RepairAnalysisService`: 사전 매칭, 휴리스틱 fallback, Feature 생성, LLM 프롬프트 생성을 담당한다.
  - `RepairAnalysisResult`, `RepairAnalysisItem`, `RepairFeature`, `RepairMapping`: 분석 결과와 매핑 데이터를 표현한다.
- Spring XML 설정에 `llmStreamClient`, `repairMappingCache`, `repairAnalysisService`, `homeController` Bean을 등록했다.
- `repairMappingCache`는 `init-method="reload"`로 기동 시 사전을 로딩한다.
- `pom.xml`에 `data/repair_mapping_dictionary.json`을 WAR classpath의 `data/` 아래로 포함하도록 리소스 설정을 추가했다.
- Tomcat 7 호환을 위해 Jackson 버전을 `2.9.10.8`로 조정했다.
- `web.xml`에 `metadata-complete="true"`를 추가해 레거시 XML 기반 웹앱 설정을 명시했다.

### 14.2 API 변경

- `POST /api/analyze`를 추가했다.
  - 입력: `prompt`
  - 출력: 원문 항목, 정규화 항목, Feature, 미매핑 항목, 사전 크기, 매핑/휴리스틱 개수
- `POST /api/chat` 흐름을 변경했다.
  - 기존: 사용자가 입력한 원문 프롬프트를 그대로 LLM에 전달
  - 변경: 수리내역을 먼저 정규화하고, 원문 수리내역과 Feature, 응답 지침을 함께 포함한 프롬프트를 LLM에 전달

### 14.3 화면 변경

- `home.jsp`를 수리내역 사고정황 추론 화면으로 개편했다.
- 화면에 다음 영역을 추가했다.
  - 수리내역 입력 영역
  - 정규화 Feature 요약
  - 항목별 정규화 결과 표
  - 미매핑 수리내역 목록
  - LLM 사고정황 추론 스트리밍 결과
- `chat.js`는 `/api/analyze`를 먼저 호출해 정규화 결과를 표시한 뒤 `/api/chat` 스트리밍을 호출하도록 변경했다.
- `main.css`에 Feature 요약, 정규화 표, 미매핑 목록 스타일을 추가했다.

### 14.4 매핑 사전 생성 내역

- `tools/RepairDictionaryBuilder.java`로 매핑 사전 생성 도구를 유지한다.
- `.md` 입력 파일은 UTF-8로 읽도록 보정했다.
- `작업내역.md`를 기반으로 사전을 다시 생성했다.
- 재생성 결과:
  - 원본 파일: `작업내역.md`
  - 원본 라인 수: 60,017
  - 중복 제거 후 사전 항목 수: 55,288
  - 생성 파일:
    - `data/repair_mapping_dictionary.json`
    - `data/repair_mapping_dictionary.csv`
    - `data/repair_mapping_seed.sql`
    - `data/repair_mapping_summary.md`
- JSON 유효성 검증과 UTF-8 한글 표시를 확인했다.

### 14.5 현재 Feature 생성 동작

예시 입력:

```text
프론트범퍼 교환
후드 교환
헤드램프 RH 교환
라디에이터 서포트 판금
```

확인된 정규화 결과:

```text
damageDirection: FRONT
damageSide: RIGHT
repairActions:
  REPLACE: 3
  PANEL_BEATING: 1
majorParts:
  BUMPER
  HOOD
  LAMP
  RADIATOR_SUPPORT
structuralSignal: true
severityHint: HIGH
evidence:
  라디에이터 서포트 판금
  프론트범퍼 교환
  후드 교환
  헤드램프 RH 교환
```

정확한 사전 매칭이 없더라도 휴리스틱 fallback으로 전방, 우측, 교환, 판금, 주요 부품, 골격 신호를 추출한다.

### 14.6 검증 내역

- Maven 테스트 및 패키징을 수행했다.
- 검증 명령:

```bash
mvn clean package
```

- 테스트 결과:
  - `HomeControllerTest`: 통과
  - `RepairAnalysisServiceTest`: 통과
  - 전체 4개 테스트 통과
- WAR 생성 확인:
  - `target/spring3-legacy-web.war`
- 로컬 Tomcat 7 플러그인 기동 확인:
  - `http://localhost:8080/health` 응답: `OK`
  - `POST /api/analyze` 예시 입력 응답 정상 확인
- Tomcat 7에서 최신 Jackson JAR의 `module-info.class` 스캔 오류가 발생했으나, Jackson `2.9.10.8`로 조정 후 오류 로그가 사라지는 것을 확인했다.

### 14.7 아직 남은 작업

- DB 기반 `repair_mapping` 테이블 연동은 아직 구현하지 않았다.
  - 현재는 `data/repair_mapping_dictionary.json`을 classpath 리소스로 로딩한다.
- 관리자용 캐시 리로드 API 또는 화면은 아직 구현하지 않았다.
  - 현재는 애플리케이션 재시작 시 새 사전이 반영된다.
- 미매핑 항목 로그 수집 및 운영 보정 루프는 아직 구현하지 않았다.
- 다중 WAS 환경에서 캐시 리로드 동기화 전략은 아직 구현하지 않았다.
- LLM 품질 샘플링과 Feature 가중치 보정은 향후 운영 데이터 기반으로 진행해야 한다.
