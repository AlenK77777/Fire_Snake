// Jenkinsfile для проекта Fire Snake
// Декларативный pipeline (современная строгая структура)

// pipeline - обязательный корневой блок декларативного синтаксиса
pipeline {
    
    // agent - указывает, где выполнять pipeline
    // any - на любом доступном агенте
    agent any
    
    // environment - блок для определения переменных окружения
    // Доступны во всех этапах pipeline
    environment {
        APP_NAME = 'fire-snake-game'
        APP_VERSION = '1.0.0'
    }
    
    // options - настройки pipeline
    options {
        // timestamps - добавляет временные метки в лог
        timestamps()
        // buildDiscarder - автоматическое удаление старых сборок
        buildDiscarder(logRotator(numToKeepStr: '10'))
        // timeout - максимальное время выполнения pipeline
        timeout(time: 30, unit: 'MINUTES')
    }
    
    // stages - обязательный блок, содержащий все этапы сборки
    stages {
        
        // stage - этап сборки, отображается в интерфейсе Jenkins
        stage('Проверка окружения') {
            // steps - обязательный блок внутри stage, содержит шаги
            steps {
                // echo - выводит сообщение в лог сборки
                echo 'Проверка версий инструментов...'
                
                // bat - выполняет команду Windows Batch (cmd.exe)
                // Проверяем версию Java
                bat 'java -version'
                
                // Проверяем версию Maven
                bat 'mvn -version'
            }
        }
        
        // Этап: Получение кода из репозитория
        stage('Checkout') {
            steps {
                // checkout scm - получает код из репозитория, указанного в настройках job
                checkout scm
                
                echo 'Код получен из репозитория'
            }
        }
        
        // Этап: Очистка проекта
        stage('Очистка') {
            steps {
                echo 'Очистка проекта от предыдущей сборки...'
                
                // mvn clean - удаляет папку target со старыми файлами
                bat 'mvn clean'
            }
        }
        
        // Этап: Компиляция исходного кода
        stage('Компиляция') {
            steps {
                echo 'Компиляция исходного кода...'
                
                // mvn compile - компилирует .java файлы в .class
                // -DskipTests - пропускает тесты (запустим отдельно)
                bat 'mvn compile -DskipTests'
            }
        }
        
        // Этап: Запуск тестов
        stage('Тестирование') {
            steps {
                echo 'Запуск тестов...'
                
                // catchError - ловит ошибки и позволяет продолжить pipeline
                // buildResult: 'UNSTABLE' - устанавливает статус сборки
                // stageResult: 'FAILURE' - устанавливает статус этапа
                catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                    // mvn test - запускает юнит-тесты
                    bat 'mvn test'
                }
            }
            // post - блок действий после выполнения этапа
            post {
                // always - выполняется всегда, независимо от результата
                always {
                    // junit - публикует результаты тестов в Jenkins
                    // allowEmptyResults: true - не падать если тестов нет
                    junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
                }
            }
        }
        
        // Этап: Создание JAR-файла
        stage('Упаковка') {
            steps {
                echo 'Создание JAR-файла...'
                
                // mvn package - создаёт JAR в папке target
                // -DskipTests - тесты уже запускали выше
                bat 'mvn package -DskipTests'
            }
        }
        
        // Этап: Сохранение артефактов
        stage('Архивация') {
            steps {
                echo 'Сохранение артефактов сборки...'
                
                // archiveArtifacts - сохраняет файлы как артефакты в Jenkins
                // artifacts: - какие файлы сохранить (можно использовать *)
                // fingerprint: true - создаёт уникальный "отпечаток" файла
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                
                echo "Артефакт сохранён: ${APP_NAME}-${APP_VERSION}.jar"
            }
        }
    }
    
    // post - блок действий после завершения всего pipeline
    post {
        // success - выполняется только при успешной сборке
        success {
            echo '=========================================='
            echo 'СБОРКА УСПЕШНО ЗАВЕРШЕНА!'
            echo '=========================================='
        }
        
        // unstable - выполняется при нестабильной сборке (например, упавшие тесты)
        unstable {
            echo '=========================================='
            echo 'Сборка завершена со статусом: UNSTABLE'
            echo '=========================================='
        }
        
        // failure - выполняется при неудачной сборке
        failure {
            echo '=========================================='
            echo 'СБОРКА ЗАВЕРШИЛАСЬ С ОШИБКОЙ!'
            echo '=========================================='
        }
        
        // always - выполняется всегда, независимо от результата
        always {
            echo 'Pipeline завершён'
        }
    }
}
