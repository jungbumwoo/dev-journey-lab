package org.example.json;

import org.example.core.DataCharBuffer;
import org.example.core.IndexBuffer;
import org.example.core.ParserException;


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

        this.elementBuffer.count = this.elementIndex;
    }

    private void parseObject(JsonTokenizer tokenizer) {
        assertHasMoreTokens(tokenizer);
        tokenizer.parseToken();
        assertThisTokenType(tokenizer.tokenType(), TokenTypes.JSON_CURLY_BRACKET_LEFT);
        setElementData(tokenizer, ElementTypes.JSON_OBJECT_START);

        tokenizer.nextToken();
        tokenizer.parseToken();
        byte tokenType = tokenizer.tokenType();

        // 중간에 } 나오는건 어떻게 핸들링?
        while (tokenType != TokenTypes.JSON_CURLY_BRACKET_RIGHT) {
            assertThisTokenType(tokenType, TokenTypes.JSON_STRING_TOKEN);
            setElementData(tokenizer, ElementTypes.JSON_PROPERTY_NAME);

            tokenizer.nextToken();
            tokenizer.parseToken();
            tokenType = tokenizer.tokenType();
            assertThisTokenType(tokenType, TokenTypes.JSON_COLON);

            tokenizer.nextToken();
            tokenizer.parseToken();
            tokenType = tokenizer.tokenType();

            switch(tokenType) {
                case TokenTypes.JSON_STRING_TOKEN
                        -> setElementData(tokenizer, ElementTypes.JSON_PROPERTY_VALUE_STRING);
                case TokenTypes.JSON_STRING_ENC_TOKEN
                        -> setElementData(tokenizer, ElementTypes.JSON_PROPERTY_VALUE_STRING_ENC);
                case TokenTypes.JSON_NUMBER_TOKEN
                        -> setElementData(tokenizer, ElementTypes.JSON_PROPERTY_VALUE_NUMBER);
                case TokenTypes.JSON_BOOLEAN_TOKEN
                        -> setElementData(tokenizer, ElementTypes.JSON_PROPERTY_VALUE_BOOLEAN);
                case TokenTypes.JSON_NULL_TOKEN
                        -> setElementData(tokenizer, ElementTypes.JSON_PROPERTY_VALUE_NULL);
                case TokenTypes.JSON_CURLY_BRACKET_LEFT
                        -> parseObject(tokenizer);
                case TokenTypes.JSON_SQUARE_BRACKET_LEFT
                        -> parseArray(tokenizer);
            }

            tokenizer.nextToken();
            tokenizer.parseToken();
            tokenType = tokenizer.tokenType();
            if (tokenType == TokenTypes.JSON_COMMA) {
                tokenizer.nextToken();
                tokenizer.parseToken();
                tokenType = tokenizer.tokenType();
            }
        }
        setElementData(tokenizer, ElementTypes.JSON_OBJECT_END);
    }

    private void parseArray(JsonTokenizer tokenizer) {
        setElementData(tokenizer, ElementTypes.JSON_ARRAY_START);

        tokenizer.nextToken();
        tokenizer.parseToken();

        while (tokenizer.tokenType() != TokenTypes.JSON_SQUARE_BRACKET_RIGHT) {
            byte tokenType = tokenizer.tokenType();

            switch (tokenType) {
                case TokenTypes.JSON_STRING_TOKEN ->
                    setElementData(tokenizer, ElementTypes.JSON_ARRAY_VALUE_STRING);
                case TokenTypes.JSON_STRING_ENC_TOKEN ->
                    setElementData(tokenizer, ElementTypes.JSON_ARRAY_VALUE_STRING_ENC);
                case TokenTypes.JSON_NUMBER_TOKEN ->
                    setElementData(tokenizer, ElementTypes.JSON_ARRAY_VALUE_NUMBER);
                case TokenTypes.JSON_BOOLEAN_TOKEN ->
                    setElementData(tokenizer, ElementTypes.JSON_ARRAY_VALUE_BOOLEAN);
                case TokenTypes.JSON_NULL_TOKEN ->
                    setElementData(tokenizer, ElementTypes.JSON_ARRAY_VALUE_NULL);
                case TokenTypes.JSON_CURLY_BRACKET_LEFT ->
                    parseObject(tokenizer);
            }

            tokenizer.nextToken();
            tokenizer.parseToken();
            tokenType = tokenizer.tokenType();
            if (tokenType == TokenTypes.JSON_COMMA) {
                tokenizer.nextToken();
                tokenizer.parseToken();
                tokenType = tokenizer.tokenType();
            }
        }

        setElementData(tokenizer, ElementTypes.JSON_ARRAY_END);
    }

    private void setElementData(JsonTokenizer tokenizer, byte elementType) {
        this.elementBuffer.position[this.elementIndex] = tokenizer.tokenPosition();
        this.elementBuffer.length[this.elementIndex] = tokenizer.tokenLength();
        this.elementBuffer.type[this.elementIndex] = elementType;
        this.elementIndex++;
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
