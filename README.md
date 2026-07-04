# Spring 3 Legacy Web

Spring `3.1.1.RELEASE` 기반 XML MVC 레거시 웹 프로젝트입니다.

## 구성

- Maven `war` 패키징
- Servlet `2.5` `web.xml`
- Spring `DispatcherServlet`
- XML 기반 MVC 설정
- XML bean 기반 컨트롤러 등록
- JSP ViewResolver
- JSTL
- SLF4J + Logback

## 실행

```bash
mvn clean package
```

생성된 `target/spring3-legacy-web.war` 파일을 Tomcat 7/8/8.5 같은 `javax.servlet` 기반 WAS에 배포하면 됩니다. Tomcat 10 이상은 `jakarta.servlet` 네임스페이스라서 이 레거시 프로젝트와 바로 호환되지 않습니다.

현재 `pom.xml`은 최신 JDK에서도 컴파일하기 쉽도록 Java 8 타깃으로 설정되어 있습니다. 운영 환경을 더 보수적으로 맞춰야 한다면 JDK 8에서 `<java.version>1.7</java.version>`로 낮춰 빌드하세요.

로컬 Maven Tomcat 플러그인으로 실행하려면 다음 명령을 사용할 수 있습니다.

```bash
mvn tomcat7:run
```

브라우저에서 `http://localhost:8080/`로 접속합니다.

## LLM 연동

기본값은 OpenAI 호환 API입니다.

- `LLM_API_URL`: 기본 `http://localhost:8000/v1/chat/completions`
- `MODEL_NAME`: 기본 `cyankiwi/gemma-4-E4B-it-AWQ-INT4`
- `LLM_API_KEY`: 필요한 경우 Bearer 토큰

웹 화면에서 프롬프트를 입력하면 Spring MVC가 Docker의 LLM API를 호출하고, 응답 토큰을 브라우저에 스트리밍합니다.

## 주요 파일

- `pom.xml`: Spring `3.1.1.RELEASE` 및 웹 의존성
- `src/main/webapp/WEB-INF/web.xml`: 레거시 웹 애플리케이션 진입점
- `src/main/webapp/WEB-INF/spring/appServlet/servlet-context.xml`: Spring MVC 설정
- `src/main/webapp/WEB-INF/views/home.jsp`: JSP 화면
- `src/main/java/com/example/legacy/HomeController.java`: 기본 컨트롤러
