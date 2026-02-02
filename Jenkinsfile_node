// Jenkinsfile для проекта Fire Snake
// Скриптовый pipeline с использованием node

// node - выделяет агента (рабочую машину) для выполнения сборки
// Всё внутри node {} выполняется на этом агенте
node {
    
    // Переменные для удобства
    def appName = 'fire-snake-game'
    def appVersion = '1.0.0'
    
    // stage - этап сборки, группирует шаги и отображается в интерфейсе Jenkins
    stage('Проверка окружения') {
        // echo - выводит сообщение в лог сборки
        echo 'Проверка версий инструментов...'
        
        // bat - выполняет команду Windows Batch (cmd.exe)
        // Проверяем версию Java
        bat 'java -version'
        
        // Проверяем версию Maven
        bat 'mvn -version'
    }
    
    // Этап: Получение кода из репозитория
    stage('Checkout') {
        // checkout scm - получает код из репозитория, указанного в настройках job
        // scm = Source Code Management (Git, SVN и т.д.)
        checkout scm
        
        echo 'Код получен из репозитория'
    }
    
    // Этап: Очистка проекта
    stage('Очистка') {
        echo 'Очистка проекта от предыдущей сборки...'
        
        // mvn clean - удаляет папку target со старыми файлами
        bat 'mvn clean'
    }
    
    // Этап: Компиляция исходного кода
    stage('Компиляция') {
        echo 'Компиляция исходного кода...'
        
        // mvn compile - компилирует .java файлы в .class
        // -DskipTests - пропускает тесты (запустим отдельно)
        bat 'mvn compile -DskipTests'
    }
    
    // Этап: Запуск тестов
    stage('Тестирование') {
        echo 'Запуск тестов...'
        
        // try-catch - ловим ошибки, чтобы продолжить даже если тесты упали
        try {
            // mvn test - запускает юнит-тесты
            bat 'mvn test'
        } catch (Exception e) {
            // Если тесты упали, помечаем сборку как нестабильную
            // currentBuild.result - статус текущей сборки
            currentBuild.result = 'UNSTABLE'
            echo "Тесты завершились с ошибками: ${e.message}"
        }
        
        // junit - публикует результаты тестов в Jenkins
        // allowEmptyResults: true - не падать если тестов нет
        junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
    }
    
    // Этап: Создание JAR-файла
    stage('Упаковка') {
        echo 'Создание JAR-файла...'
        
        // mvn package - создаёт JAR в папке target
        // -DskipTests - тесты уже запускали выше
        bat 'mvn package -DskipTests'
    }
    
    // Этап: Сохранение артефактов
    stage('Архивация') {
        echo 'Сохранение артефактов сборки...'
        
        // archiveArtifacts - сохраняет файлы как артефакты в Jenkins
        // artifacts: - какие файлы сохранить (можно использовать *)
        // fingerprint: true - создаёт уникальный "отпечаток" файла
        archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
        
        echo "Артефакт сохранён: ${appName}-${appVersion}.jar"
    }
    
    // Вывод итогового статуса
    stage('Завершение') {
        // currentBuild.result - итоговый статус сборки
        if (currentBuild.result == null || currentBuild.result == 'SUCCESS') {
            echo '=========================================='
            echo 'СБОРКА УСПЕШНО ЗАВЕРШЕНА!'
            echo '=========================================='
        } else {
            echo '=========================================='
            echo "Сборка завершена со статусом: ${currentBuild.result}"
            echo '=========================================='
        }
    }
}
