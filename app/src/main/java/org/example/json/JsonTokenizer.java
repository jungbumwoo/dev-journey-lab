package org.example.json;

import org.example.core.DataCharBuffer;
import org.example.core.IndexBuffer;

public class JsonTokenizer {

    private DataCharBuffer dataBuffer = null;
    private IndexBuffer tokenBuffer = null;

    private int tokenIndex = 0;
    private int dataPosition = 0;  // like cursor . 데이터 위치. 현 위치.
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
         skipWhiteSpace();
        // this.tokenLength = 0;

        this.tokenBuffer.position[this.tokenIndex] = this.dataPosition;  // 아직 여기 이해 덜 됨 -> tokenBuffer position에는 해당 토큰의 data 위치를 가리키는 듯?
        char nextChar = this.dataBuffer.data[this.dataPosition];

        switch (nextChar) {
            case '{' -> this.tokenBuffer.type[this.tokenIndex] = TokenTypes.JSON_CURLY_BRACKET_LEFT;
            case '}' -> this.tokenBuffer.type[this.tokenIndex] = TokenTypes.JSON_CURLY_BRACKET_RIGHT;
            case '[' -> this.tokenBuffer.type[this.tokenIndex] = TokenTypes.JSON_SQUARE_BRACKET_LEFT;
            case ']' -> this.tokenBuffer.type[this.tokenIndex] = TokenTypes.JSON_SQUARE_BRACKET_RIGHT;
            case ',' -> this.tokenBuffer.type[this.tokenIndex] = TokenTypes.JSON_COMMA;
            case ':' -> this.tokenBuffer.type[this.tokenIndex] = TokenTypes.JSON_COLON;

            case '"' -> parseStringToken();
            case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
                    -> {
                parseNumberToken(); this.tokenBuffer.type[this.tokenIndex] = TokenTypes.JSON_NUMBER_TOKEN;
            }
            // if 문 실패하면 어떻게 되는거임?
            case 'f' -> { if(parseFalse()) {this.tokenBuffer.type[this.tokenIndex] = TokenTypes.JSON_BOOLEAN_TOKEN;}}
            case 't' -> { if(parseTrue()) {this.tokenBuffer.type[this.tokenIndex] = TokenTypes.JSON_BOOLEAN_TOKEN;}}
            case 'n' -> { if(parseNull()) {this.tokenBuffer.type[this.tokenIndex] = TokenTypes.JSON_NULL_TOKEN;}}

            //default    :  { parseStringToken(); this.tokenBuffer.type[this.tokenIndex] = TokenTypes.JSON_STRING_TOKEN; }
        }

        this.tokenBuffer.length[this.tokenIndex] = this.tokenLength;
    }

    private boolean parseNull() {
        // this.tokenLength = 4; <- 이거 설정 안해줘도 되나?
        return this.dataBuffer.data[this.dataPosition + 1] == 'u'
                && this.dataBuffer.data[this.dataPosition + 2] == 'l'
                && this.dataBuffer.data[this.dataPosition + 3] == 'l';
    }

    private boolean parseTrue() {
        if(
                this.dataBuffer.data[this.dataPosition + 1] == 'r' &&
                        this.dataBuffer.data[this.dataPosition + 2] == 'u' &&
                        this.dataBuffer.data[this.dataPosition + 3] == 'e' )
        {
            this.tokenLength = 4;
            return true;
        }
        return false;
    }

    private boolean parseFalse() {
        if(
                this.dataBuffer.data[this.dataPosition + 1] == 'a' &&
                        this.dataBuffer.data[this.dataPosition + 2] == 'l' &&
                        this.dataBuffer.data[this.dataPosition + 3] == 's' &&
                        this.dataBuffer.data[this.dataPosition + 4] == 'e'
        )  {
            this.tokenLength = 5;
            return true;
        }
        return false;
    }

    private void parseStringToken() {
        int tempPos = this.dataPosition;
        boolean containsEncodedChars = false;
        boolean endOfStringFound = false;

        while (!endOfStringFound) {
            tempPos++;
            switch (this.dataBuffer.data[tempPos]) {
                case '"' -> endOfStringFound = this.dataBuffer.data[tempPos - 1] != '\\';
                case '\\' -> containsEncodedChars = true;
            }
        }
        if (containsEncodedChars) {
            this.tokenBuffer.type[this.tokenIndex] = TokenTypes.JSON_STRING_ENC_TOKEN;
        } else {
            this.tokenBuffer.type[this.tokenIndex] = TokenTypes.JSON_STRING_TOKEN;
        }

        this.tokenBuffer.position[this.tokenIndex] = this.dataPosition + 1; // +1 to skip over the beginning quote char (")
        this.tokenLength = (tempPos - this.dataPosition - 1); // +2 to include the enclosing quote chars ("").
    }

    private void parseNumberToken() {
        this.tokenLength = 1;

        boolean isEndOfNumberFound = false;
        while (!isEndOfNumberFound) {
            switch (this.dataBuffer.data[this.dataPosition + this.tokenLength]) {
                case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.' -> this.tokenLength++;
                default -> isEndOfNumberFound = true;
            }
        }
    }

    private void skipWhiteSpace() {
        boolean isWhiteSpace = true;
        while (isWhiteSpace) {
            switch (this.dataBuffer.data[this.dataPosition]) {
                case ' ', '\r', '\n', '\t' -> this.dataPosition++;
                default -> isWhiteSpace = false;
            }
        }
    }

    public void nextToken() {
        switch (this.tokenBuffer.type[this.tokenIndex]) {
            case TokenTypes.JSON_STRING_TOKEN, TokenTypes.JSON_STRING_ENC_TOKEN
                    -> this.dataPosition += this.tokenBuffer.length[this.tokenIndex] + 2;  // "string" 이라 " " 때문에 +2
            case TokenTypes.JSON_CURLY_BRACKET_LEFT,
                 TokenTypes.JSON_CURLY_BRACKET_RIGHT,
                 TokenTypes.JSON_SQUARE_BRACKET_LEFT,
                 TokenTypes.JSON_SQUARE_BRACKET_RIGHT,
                 TokenTypes.JSON_COLON,
                 TokenTypes.JSON_COMMA
                    -> this.dataPosition++;
            case TokenTypes.JSON_NULL_TOKEN
                    -> this.dataPosition += 4; // parsing 할 때 tokenLength 4로 잡아두면 되는거 아닌가?
            default
                    -> this.dataPosition += this.tokenLength;
        }

        this.tokenIndex++; // point to next token index array cell.
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
