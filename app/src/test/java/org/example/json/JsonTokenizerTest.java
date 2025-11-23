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

        IndexBuffer tokenBuffer = new IndexBuffer(dataBuffer.data.length, true);
        JsonTokenizer tokenizer = new JsonTokenizer(dataBuffer, tokenBuffer);

        /*
        * dataBuffer.data
        [
          0:'{',   1:' ',   2:' ',   3:'"',   4:'k',   5:'e',   6:'y',   7:'"',
          8:' ',   9:':',  10:' ',  11:'"',  12:'v',  13:'a',  14:'l',  15:'u',
         16:'e',  17:'"',  18:' ',  19:'}'
        ]
        * */
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

        assertEquals(TokenTypes.JSON_CURLY_BRACKET_LEFT, tokenBuffer.type[0]);
        assertEquals(0, tokenBuffer.position[0]);
        assertEquals(0, tokenBuffer.length[0]); // 왜 1이 아니라 0으로 잡을까. {는 토큰이 아니라고 보는건가. 안써서 의미 없으니까 그대로 둔듯

        assertEquals(TokenTypes.JSON_STRING_TOKEN, tokenBuffer.type[1]);
        assertEquals(4, tokenBuffer.position[1]);
        assertEquals(3, tokenBuffer.length[1]);

        assertEquals(TokenTypes.JSON_COLON, tokenBuffer.type[2]);
        assertEquals(9, tokenBuffer.position[2]);
        assertEquals(3, tokenBuffer.length[1]);  // 3이 의미 있는 사이즈가 아니라 의미 없으니 굳이 update를 안해준건가?
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
