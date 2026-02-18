package io.jungbum.nioserver.http;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HttpUtilTest {

    private HttpHeaders headers;

    @BeforeEach
    void setUp() {
        headers = new HttpHeaders();
    }

    @Test
    @DisplayName("GET 요청(Body 없음) 파싱 테스트")
    void testParseGetRequest() {
        String raw = "GET /index.html HTTP/1.1\r\nHost: localhost\r\n\r\n";
        byte[] src = raw.getBytes();

        int result = HttpUtil.parseHttpRequest(src, 0, src.length, headers);

        assertEquals(src.length, result, "메시지 전체가 파싱되어야 함");
        assertEquals(0, headers.contentLength);
    }

    @Test
    @DisplayName("POST 요청(Body 있음) 파싱 테스트")
    void testParsePostRequest() {
        String body = "name=jungbum";
        String raw = "POST /submit HTTP/1.1\r\n" +
                "Content-Length: " + body.length() + "\r\n" +
                "\r\n" +
                body;
        byte[] src = raw.getBytes();

        int result = HttpUtil.parseHttpRequest(src, 0, src.length, headers);

        assertEquals(src.length, result);
        assertEquals(body.length(), headers.contentLength);

        // Body 영역 확인
        String extractedBody = new String(src, headers.bodyStartIndex, headers.bodyEndIndex - headers.bodyStartIndex);
        assertEquals(body, extractedBody);
    }

    @Test
    @DisplayName("데이터가 중간에 잘린 경우 -1을 반환해야 함")
    void testIncompleteRequest() {
        String incomplete = "GET /index.html HTTP/1.1\r\nHost: local"; // \r\n\r\n 없음
        byte[] src = incomplete.getBytes();

        int result = HttpUtil.parseHttpRequest(src, 0, src.length, headers);

        assertEquals(-1, result, "미완성 요청은 -1을 반환해야 함");
    }
}