pipeline {
    agent any

    triggers {
        githubPush()
    }

    options {
        disableConcurrentBuilds()
        timeout(time: 20, unit: 'MINUTES')
    }

    environment {
        // Jenkins runs under launchd with a minimal PATH; add Homebrew and Docker Desktop
        PATH = "/opt/homebrew/bin:/usr/local/bin:${env.PATH}"
        JAVA_HOME = '/opt/homebrew/opt/openjdk@21'
        IMAGE = 'aaryanantala/calculator'
        // fixed project name so every run updates the same stack
        COMPOSE_PROJECT_NAME = 'calculator'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh 'mvn -B clean package -DskipTests'
            }
        }

        stage('Test') {
            steps {
                sh 'mvn -B test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Docker Build') {
            steps {
                sh 'docker build -t $IMAGE:$BUILD_NUMBER -t $IMAGE:latest .'
            }
        }

        stage('Push') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub-creds',
                                                  usernameVariable: 'DOCKER_USER',
                                                  passwordVariable: 'DOCKER_PASS')]) {
                    sh 'echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin'
                    // multi-arch (Intel + Apple Silicon) so the image runs on any machine
                    sh 'docker buildx build --platform linux/amd64,linux/arm64 -t $IMAGE:$BUILD_NUMBER -t $IMAGE:latest --push .'
                }
            }
        }

        stage('Deploy') {
            steps {
                sh 'IMAGE_TAG=$BUILD_NUMBER docker compose up -d --wait'
                // pick up nginx.conf changes without downtime
                sh 'docker compose exec -T nginx nginx -s reload'
                sh 'curl -fs http://localhost/actuator/health'
            }
        }
    }

    post {
        success {
            emailext to: '$DEFAULT_RECIPIENTS',
                     subject: "SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                     body: "Build #${env.BUILD_NUMBER} passed and image ${env.IMAGE}:${env.BUILD_NUMBER} is deployed.\n\n${env.BUILD_URL}"
        }
        failure {
            emailext to: '$DEFAULT_RECIPIENTS',
                     subject: "FAILURE: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                     body: "Build #${env.BUILD_NUMBER} failed.\n\nConsole output: ${env.BUILD_URL}console"
        }
    }
}
