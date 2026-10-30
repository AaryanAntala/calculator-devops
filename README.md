# calculator-devops

A Calculator REST API built with Spring Boot, delivered through a fully automated CI/CD pipeline: a push to GitHub triggers Jenkins, which builds, tests, containerizes, publishes and deploys the application with Docker and Docker Compose.

- Source code: https://github.com/AaryanAntala/calculator-devops
- Docker Hub image: https://hub.docker.com/r/aaryanantala/calculator

## Architecture

```
Developer
    |  git push
    v
GitHub  --- webhook (push event) --->  Jenkins
                                         |  1. Checkout
                                         |  2. Build        (mvn clean package)
                                         |  3. Test         (mvn test, JUnit reports)
                                         |  4. Docker Build (image tagged with build number)
                                         |  5. Push         (Docker Hub, multi-architecture)
                                         |  6. Deploy       (docker compose up -d, health check)
                                         v
                              Docker Compose stack
                         +-------------------------------+
   client --> :80 -->    |  nginx  -->  calculator:8085  |
                         |      (calculator-net network) |
                         +-------------------------------+
                                         |
                                  Email notification (success / failure)
```

## Technology stack

| Area | Technology |
|------|------------|
| Language and framework | Java 21, Spring Boot 4 (Spring Web MVC, Actuator) |
| Build | Maven |
| Testing | JUnit 5, Spring MockMvc |
| Version control | Git, GitHub |
| CI/CD | Jenkins (declarative pipeline) |
| Containers | Docker (multi-stage build), Docker Hub |
| Deployment | Docker Compose, Nginx reverse proxy |

## Repository structure

```
calculator-devops/
|-- src/main/java/...        Spring Boot application
|-- src/test/java/...        JUnit unit tests and MockMvc integration tests
|-- pom.xml                  Maven build
|-- Dockerfile               Multi-stage image build
|-- .dockerignore
|-- docker-compose.yml       Calculator service and Nginx proxy
|-- nginx/nginx.conf         Reverse proxy configuration
|-- Jenkinsfile              Declarative CI/CD pipeline
|-- .gitignore
`-- README.md
```

## Prerequisites

- JDK 21 and Maven 3.9 or later (to build and test locally)
- Docker and Docker Compose (to run the containers)

## Build, test and run locally

```bash
mvn clean package -DskipTests    # build the executable JAR: target/calculator.jar
mvn test                         # run all tests; JUnit XML is written to target/surefire-reports/
java -jar target/calculator.jar  # start on http://localhost:8085
```

The port can be changed with the `SERVER_PORT` environment variable. Port 8085 is the default so that the application does not clash with Jenkins, which uses port 8080.

## API reference

| Operation | Endpoint | Behaviour |
|-----------|----------|-----------|
| Add | `GET /api/v1/add?a=&b=` | Returns `a + b` |
| Subtract | `GET /api/v1/subtract?a=&b=` | Returns `a - b` |
| Multiply | `GET /api/v1/multiply?a=&b=` | Returns `a * b` |
| Divide | `GET /api/v1/divide?a=&b=` | Returns `a / b`; `b=0` returns HTTP 400 |
| Health | `GET /actuator/health` | Returns `UP` or `DOWN` |

Successful responses return HTTP 200 and a JSON body:

```json
{"operation":"add","a":2.0,"b":3.0,"result":5.0}
```

Invalid requests return HTTP 400 and a structured error body:

```json
{"status":400,"error":"Bad Request","message":"Division by zero is not allowed"}
```

The following conditions return HTTP 400: division by zero, a missing parameter, a non-numeric parameter, a non-finite value (`NaN`, `Infinity`) and a result that is out of range.

### Examples

```bash
curl "http://localhost:8085/api/v1/add?a=2&b=3"
curl "http://localhost:8085/api/v1/divide?a=5&b=2"
curl -i "http://localhost:8085/api/v1/divide?a=5&b=0"
curl -i "http://localhost:8085/api/v1/add?a=abc&b=3"
curl -i "http://localhost:8085/api/v1/add?a=2"
curl "http://localhost:8085/actuator/health"
```

## Testing

`mvn test` runs 16 tests:

- `CalculatorServiceTest` (5 JUnit 5 unit tests): add, subtract, multiply, divide and the divide-by-zero case.
- `CalculatorControllerIntegrationTest` (11 MockMvc integration tests): every endpoint, plus the missing parameter, non-numeric parameter, divide-by-zero, non-finite value, out-of-range result and health endpoint cases.

Jenkins publishes the XML reports in `target/surefire-reports/` and fails the build if any test fails.

## Docker

The `Dockerfile` uses two stages:

1. A Maven and JDK 21 stage that resolves dependencies (cached in their own layer) and builds the JAR.
2. A slim `eclipse-temurin:21-jre-alpine` runtime stage that copies only the JAR, runs as a non-root user and exposes port 8085.

```bash
docker build -t calculator .
docker run -p 8085:8085 calculator
curl "http://localhost:8085/actuator/health"
```

The published image supports both `linux/amd64` and `linux/arm64`:

```bash
docker run -p 8085:8085 aaryanantala/calculator:latest
```

## Deployment with Docker Compose

`docker-compose.yml` defines two services on a shared network named `calculator-net`:

- `calculator`: the application image, with environment variables, a health check against `/actuator/health` and the restart policy `unless-stopped`. Its port is not published to the host.
- `nginx`: a reverse proxy published on host port 80. It starts only after the calculator reports healthy and forwards all requests to `calculator:8085`.

```bash
docker compose up -d      # start the full stack
docker compose ps         # both services should be running; calculator shows "healthy"
curl "http://localhost/api/v1/add?a=2&b=3"
docker compose down       # stop and remove the stack
```

The image tag is controlled by the `IMAGE_TAG` variable and defaults to `latest`. Jenkins sets it to the build number, for example `IMAGE_TAG=5 docker compose up -d`.

## CI/CD pipeline

The pipeline is defined in the `Jenkinsfile` and is triggered by a GitHub webhook on every push.

| Stage | Action |
|-------|--------|
| Checkout | Pulls the commit that triggered the build |
| Build | `mvn clean package -DskipTests` |
| Test | `mvn test`, publishes the JUnit reports and fails the build on any test failure |
| Docker Build | Builds the image and tags it with the build number and `latest` |
| Push | Logs in with the Jenkins-managed credential and pushes a multi-architecture image to Docker Hub |
| Deploy | `docker compose up -d --wait`, then verifies `/actuator/health` through Nginx |

After the stages finish, an email reporting success or failure is sent. Re-running the pipeline is safe: Compose recreates only the containers whose image or configuration changed, and concurrent builds are disabled.

### Jenkins setup

1. Plugins: Git, GitHub, Pipeline, JUnit, Credentials Binding, Docker Pipeline and Email Extension.
2. Credential: a "Username with password" credential with the ID `dockerhub-creds`, holding the Docker Hub username and an access token. No credentials are stored in the repository.
3. Email: configure Extended E-mail Notification with an SMTP server (for Gmail, `smtp.gmail.com`, port 465, SSL, and an app password stored as a Jenkins credential), and set the default recipients.
4. Job: create a Pipeline job with "Pipeline script from SCM", the repository URL, branch `*/main` and script path `Jenkinsfile`. The `githubPush()` trigger in the `Jenkinsfile` handles automatic builds.
5. Environment: Jenkins started by a service manager may have a minimal `PATH`. The `Jenkinsfile` adds the Homebrew and Docker directories and sets `JAVA_HOME` for this reason. Docker must be running on the Jenkins host.

### GitHub webhook

Jenkins runs on a local machine, so a tunnel such as ngrok exposes it to GitHub:

```bash
ngrok http --url=<your-ngrok-domain> 8080
```

In the repository settings, add a webhook with the payload URL `https://<your-ngrok-domain>/github-webhook/`, content type `application/json` and the push event. Set the Jenkins URL (Manage Jenkins, System) to the same domain. Jenkins must require a login, because the tunnel makes it reachable from the internet.

## Security notes

- No credentials or secrets are committed. The Docker Hub token and the email password exist only in the Jenkins credentials store.
- The container runs as a non-root user.
- The calculator is reachable only through the Nginx proxy; its port is not published.
- Only the `health` Actuator endpoint is exposed.
