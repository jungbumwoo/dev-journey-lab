package org.example.json;

import org.example.core.DataCharBuffer;
import org.example.core.IndexBuffer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


public class JsonTokenizerTest {

    @Test
    public void test() {
        DataCharBuffer dataBuffer = new DataCharBuffer();
        dataBuffer.data = "{  \"key\" : \"value\" }".toCharArray();
        dataBuffer.length = dataBuffer.data.length;

        JsonTokenizer tokenizer = new JsonTokenizer(dataBuffer, new IndexBuffer(dataBuffer.data.length, true));

        // "{"
        assertTrue(tokenizer.hasMoreTokens());
        tokenizer.parseToken();
        assertEquals(0, tokenizer.tokenPosition());
        assertEquals(TokenTypes.JSON_CURLY_BRACKET_LEFT, tokenizer.tokenType());

        // "key"
        assertTrue(tokenizer.hasMoreTokens());
        tokenizer.nextToken();
        tokenizer.parseToken();
        assertEquals(4, tokenizer.tokenPosition());
        assertEquals(3, tokenizer.tokenLength());

        // ":"
        assertTrue(tokenizer.hasMoreTokens());
        tokenizer.nextToken();
        tokenizer.parseToken();
        assertEquals(9, tokenizer.tokenPosition());
        assertEquals(TokenTypes.JSON_COLON, tokenizer.tokenType());

        // "value"
        assertTrue(tokenizer.hasMoreTokens());
        tokenizer.nextToken();
        tokenizer.parseToken();
        assertEquals(12, tokenizer.tokenPosition());
        assertEquals(5, tokenizer.tokenLength());

        // "}"
        assertTrue(tokenizer.hasMoreTokens());
        tokenizer.nextToken();
        tokenizer.parseToken();
        assertEquals(19, tokenizer.tokenPosition());
        assertEquals(TokenTypes.JSON_CURLY_BRACKET_RIGHT, tokenizer.tokenType());

        assertFalse(tokenizer.hasMoreTokens());
    }

    @Test
    public void testNumbers() {
        DataCharBuffer dataBuffer = new DataCharBuffer();
        dataBuffer.data = "{  \"key\" : 0.123, \"key2\" : 1234567890.0123456789 }".toCharArray();
        dataBuffer.length = dataBuffer.data.length;

        JsonTokenizer tokenizer = new JsonTokenizer(dataBuffer, new IndexBuffer(dataBuffer.data.length, true));

        assertTrue(tokenizer.hasMoreTokens());
        tokenizer.parseToken();

        assertEquals(0, tokenizer.tokenPosition());
        assertEquals(TokenTypes.JSON_CURLY_BRACKET_LEFT, tokenizer.tokenType());

        tokenizer.nextToken();
        tokenizer.parseToken();
        assertEquals(4, tokenizer.tokenPosition());
        assertEquals(3, tokenizer.tokenLength());
        assertEquals(TokenTypes.JSON_STRING_TOKEN, tokenizer.tokenType());

        tokenizer.nextToken();
        tokenizer.parseToken();
        assertEquals(9, tokenizer.tokenPosition());
        assertEquals(TokenTypes.JSON_COLON, tokenizer.tokenType());

        tokenizer.nextToken();
        tokenizer.parseToken();
        assertEquals(11, tokenizer.tokenPosition());
        assertEquals(5, tokenizer.tokenLength());
        assertEquals(TokenTypes.JSON_NUMBER_TOKEN, tokenizer.tokenType());

        tokenizer.nextToken();
        tokenizer.parseToken();
        assertEquals(16, tokenizer.tokenPosition());
        assertEquals(TokenTypes.JSON_COMMA, tokenizer.tokenType());

        tokenizer.nextToken();
        tokenizer.parseToken();
        assertEquals(19, tokenizer.tokenPosition());
        assertEquals(4, tokenizer.tokenLength());
        assertEquals(TokenTypes.JSON_STRING_TOKEN, tokenizer.tokenType());

        tokenizer.nextToken();
        tokenizer.parseToken();
        assertEquals(25, tokenizer.tokenPosition());
        assertEquals(TokenTypes.JSON_COLON, tokenizer.tokenType());

        tokenizer.nextToken();
        tokenizer.parseToken();
        assertEquals(27, tokenizer.tokenPosition());
        assertEquals(21, tokenizer.tokenLength());
        assertEquals(TokenTypes.JSON_NUMBER_TOKEN, tokenizer.tokenType());

        tokenizer.nextToken();
        tokenizer.parseToken();
        assertEquals(49, tokenizer.tokenPosition());
        assertEquals(TokenTypes.JSON_CURLY_BRACKET_RIGHT, tokenizer.tokenType());
    }
}
