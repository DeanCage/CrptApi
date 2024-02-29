package org.example;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class CrptApi {
    // Переменные для хранения промежутка времени и лимита запросов
    private final TimeUnit timeUnit;
    private final int requestLimit;

    // Переменные для отслеживания времени последнего запроса и количества запросов
    private long lastRequestTime = 0;
    private int requestCount = 0;

    // Объект для синхронизации доступа к общим данным
    private final Object lock = new Object();

    // Конструктор класса, принимающий промежуток времени и лимит запросов
    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
    }

    // Метод для создания документа
    public void createDocument(String documentJson, String signature) {
        // Синхронизация доступа к общим данным
        synchronized (lock) {
            // Получение текущего времени
            long currentTime = System.currentTimeMillis();
            // Вычисление времени, прошедшего с последнего запроса
            long timeElapsed = currentTime - lastRequestTime;

            // Проверка, прошло ли уже время промежутка
            if (timeElapsed < timeUnit.toMillis(1)) {
                // Проверка, не превышен ли лимит запросов
                if (requestCount >= requestLimit) {
                    try {
                        // Если лимит превышен, поток ожидает до окончания промежутка времени
                        long waitTime = timeUnit.toMillis(1) - timeElapsed;
                        Thread.sleep(waitTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    // Увеличение счетчика запросов
                    requestCount++;
                }
            } else {
                // Если промежуток времени прошел, счетчик сбрасывается
                lastRequestTime = currentTime;
                requestCount = 1;
            }
        }

        // Отправка POST-запроса на указанный URL
        sendPostRequest(documentJson);
    }

    // Метод для отправки POST-запроса
    private void sendPostRequest(String requestBody) {
        try {
            // Создание объекта URL для отправки запроса
            URL url = new URL("https://ismp.crpt.ru/api/v3/lk/documents/create");
            // Создание соединения
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Установка метода запроса и заголовков
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);

            // Запись данных запроса в поток вывода
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Получение кода ответа от сервера и вывод на консоль
            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            // Закрытие соединения
            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
