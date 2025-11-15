package org.example.json;

import org.example.core.DataCharBuffer;
import org.example.core.IndexBuffer;
import org.example.core.ParserException;

import java.text.ParseException;

public class JsonParser {
    private IndexBuffer tokenBuffer = null;
    private IndexBuffer elementBuffer = null;
    private int elementIndex = 0;
    private JsonTokenizer jsonTokenizer = null;

    public JsonParser(IndexBuffer tokenBuffer, IndexBuffer elementBuffer) {
        this.tokenBuffer = tokenBuffer;
        this.jsonTokenizer = new JsonTokenizer(this.tokenBuffer);
        this.elementBuffer = elementBuffer;

    }

    public void parse(DataCharBuffer dataBuffer) {
        this.elementIndex = 0;

        this.jsonTokenizer.reinit(dataBuffer, this.tokenBuffer);

        parseObject(this.jsonTokenizer); // 왜 parameter를 넘겨야함?

//        this.jsonTokenizer.re
    }

    private void parseObject(JsonTokenizer tokenizer) {
        assertHasMoreTokens(tokenizer);
        tokenizer.parseToken();
        assertThisTokenType(tokenizer.tokenType(), TokenTypes.JSON_CURLY_BRACKET_LEFT);
        setElementData(tokenizer, ElementTypes.JSON_OBJECT_START);
    }

    private void setElementData(JsonTokenizer tokenizer, byte elementType) {
        this.elementBuffer.position[this.elementIndex] = tokenizer.tokenPosition();

    }

    private final void assertThisTokenType(byte tokenType, byte expectedTokenType) {
        if (tokenType != expectedTokenType) {
            throw new ParserException("Token type mismatch: Expected " + expectedTokenType + " but found " + tokenType);
        }
    }

    private void assertHasMoreTokens(JsonTokenizer tokenizer) {
        if (!tokenizer.hasMoreTokens()) {
            throw new ParserException("Expected more tokens available in the tokenizer");
        }
    }
}
