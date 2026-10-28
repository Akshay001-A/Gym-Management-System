
pipeline {
    agent any

    options {
        timestamps()
    }

    stages {
        stage('Checkout Source Code') {
            steps {
                checkout scm
            }
        }

        stage('Build React Frontend') {
            steps {
                dir('frontend') {
                    bat 'npm ci'
                    bat 'npm run build'
                }
            }
        }

        stage('Build Auth Service') {
            steps {
                dir('microservices/auth-service') {
                    bat 'call mvnw.cmd -B clean verify'
                }
            }
        }

        stage('Build Member Service') {
            steps {
                dir('microservices/member-service') {
                    bat 'call mvnw.cmd -B clean verify'
                }
            }
        }

        stage('Build Membership Service') {
            steps {
                dir('microservices/membership-service') {
                    bat 'call mvnw.cmd -B clean verify'
                }
            }
        }

        stage('Build Trainer Service') {
            steps {
                dir('microservices/trainer-service') {
                    bat 'call mvnw.cmd -B clean verify'
                }
            }
        }

        stage('Build API Gateway') {
            steps {
                dir('microservices/api-gateway') {
                    bat 'call mvnw.cmd -B clean verify'
                }
            }
        }
    }

    post {
        success {
            echo 'SUCCESS: Frontend and all microservices built and tested.'
        }

        failure {
            echo 'FAILED: Check the Console Output for the first failing stage.'
        }

        always {
            echo 'Jenkins pipeline execution completed.'
        }
    }
}
