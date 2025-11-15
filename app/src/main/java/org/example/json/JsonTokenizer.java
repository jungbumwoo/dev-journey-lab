package org.example.json;

import org.example.core.DataCharBuffer;
import org.example.core.IndexBuffer;

public class JsonTokenizer {

    private DataCharBuffer dataBuffer = null;
    private IndexBuffer tokenBuffer = null;

    private int tokenIndex = 0;
    private int dataPosition = 0;
    private int tokenLength = 0;

    public JsonTokenizer(IndexBuffer tokenBuffer) {
        this.tokenBuffer = tokenBuffer;
    }

    public JsonTokenizer(DataCharBuffer dataBuffer, IndexBuffer tokenBuffer) {
        this.dataBuffer = dataBuffer;
        this.tokenBuffer = tokenBuffer;
    }

    public void reinit(DataCharBuffer dataBuffer, IndexBuffer tokenBuffer) {
        this.dataBuffer = dataBuffer;
        this.tokenBuffer = tokenBuffer;
        this.tokenIndex = 0;
        this.dataPosition = 0;
        this.tokenLength = 0;
    }


    public boolean hasMoreTokens() {
        // 마지막 token 이었으면 dataBuffer.length와 동일
        return (this.dataPosition + this.tokenLength) < this.dataBuffer.length;
    }

    public void parseToken() {
        // skipWhiteSpace();
        // this.tokenLength = 0;

        this.tokenBuffer.position[this.tokenIndex] = this.dataPosition;
        char nextChar = this.dataBuffer.data[this.dataPosition];

        switch (nextChar) {
            case '{': {}
            case '}': {}
        }
    }

    public void nextToken() {
        switch (this.tokenBuffer.type[this.tokenIndex]) {
            // why +2?
            case TokenTypes.JSON_STRING_TOKEN, TokenTypes.JSON_STRING_ENC_TOKEN
                    -> this.dataPosition += this.tokenBuffer.length[this.tokenIndex] + 2;

        }
    }

    public int tokenPosition() {
        return this.tokenBuffer.position[this.tokenIndex];
    }

    public int tokenLength() {
        return this.tokenBuffer.length[this.tokenIndex];
    }

    public byte tokenType() {
        return this.tokenBuffer.type[this.tokenIndex];
    }
}
