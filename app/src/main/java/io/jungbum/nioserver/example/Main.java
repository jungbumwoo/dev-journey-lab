package io.jungbum.nioserver.example;

import io.jungbum.nioserver.IMessageProcessor;
import io.jungbum.nioserver.Message;
import io.jungbum.nioserver.Server;
import io.jungbum.nioserver.http.HttpMessageReaderFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] args) throws IOException {

        IMessageProcessor messageProcessor = (request, writeProxy) -> {
            System.out.println("Message Received from socket: " + request.socketId);

            // 1. 요청 라인 파싱 (예: "GET /?name=Gemini HTTP/1.1")
            String requestLine = new String(request.sharedArray, request.offset, request.length).split("\r\n")[0];
            String[] parts = requestLine.split(" ");
            String name = "World"; // default

            if (parts.length > 1 && parts[1].contains("?name=")) {
                String url = parts[1];
                try {
                    name = url.split("name=")[1].split("&")[0];
                } catch (Exception e) {
                    // 파라미터 파싱 오류 시 기본값 사용
                }
            }

            // 2. 동적 응답 생성
            String body = String.format("<html><body>Hello %s!</body></html>", name);
            String httpResponse = "HTTP/1.1 200 OK\r\n" +
                    "Content-Length: " + body.length() + "\r\n" +
                    "Content-Type: text/html\r\n" +
                    "\r\n" +
                    body;

            byte[] httpResponseBytes = httpResponse.getBytes(StandardCharsets.UTF_8);

            Message response = writeProxy.getMessage();
            response.socketId = request.socketId;
            response.writeToMessage(httpResponseBytes);

            writeProxy.enqueue(response);
        };

        Server server = new Server(9999, new HttpMessageReaderFactory(), messageProcessor);

        server.start();
    }
}