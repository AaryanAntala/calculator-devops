# calculator-devops

Calculator REST API built with Spring Boot, wrapped in a CI/CD pipeline: GitHub → Jenkins → Docker Hub → Docker Compose.

## Tech stack

- Java 21, Spring Boot 4 (Spring Web MVC + Actuator), Maven
- JUnit 5 + MockMvc for tests

## Build, test and run

```bash
mvn clean package -DskipTests   # build the executable JAR -> target/calculator.jar
mvn test                        # run unit + integration tests (JUnit XML in target/surefire-reports/)
java -jar target/calculator.jar # start on http://localhost:8080 (override with SERVER_PORT)
```

## API

| Operation | Endpoint | Behaviour |
|-----------|----------|-----------|
| Add       | `GET /api/v1/add?a=&b=`      | `a + b` |
| Subtract  | `GET /api/v1/subtract?a=&b=` | `a - b` |
| Multiply  | `GET /api/v1/multiply?a=&b=` | `a * b` |
| Divide    | `GET /api/v1/divide?a=&b=`   | `a / b`; `b=0` returns HTTP 400 |
| Health    | `GET /actuator/health`       | `UP` / `DOWN` |

### Examples

```bash
$ curl "http://localhost:8080/api/v1/add?a=2&b=3"
{"operation":"add","a":2.0,"b":3.0,"result":5.0}

$ curl "http://localhost:8080/api/v1/divide?a=5&b=0"
{"status":400,"error":"Bad Request","message":"Division by zero is not allowed"}

$ curl "http://localhost:8080/api/v1/add?a=abc&b=3"
{"status":400,"error":"Bad Request","message":"Parameter 'a' must be a number"}

$ curl "http://localhost:8080/actuator/health"
{"groups":["liveness","readiness"],"status":"UP"}
```

Missing, non-numeric or non-finite (`NaN`, `Infinity`) parameters return HTTP 400 with the same error structure.
