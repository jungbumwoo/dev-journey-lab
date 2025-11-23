package org.example.json;

import org.example.core.DataCharBuffer;
import org.example.core.IndexBuffer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class JsonParserTest {
    @Test
    @DisplayName("elementBuffer test")
    public void test() {
        DataCharBuffer dataBuffer = new DataCharBuffer();
        dataBuffer.data = "{  \"key\" : \"value\", \"key2\" : \"value2\" }".toCharArray();
        dataBuffer.length = dataBuffer.data.length;

        IndexBuffer tokenBuffer = new IndexBuffer(dataBuffer.data.length, true);
        IndexBuffer elementBuffer = new IndexBuffer(dataBuffer.data.length, true);

        JsonParser parser = new JsonParser(tokenBuffer, elementBuffer);

        parser.parse(dataBuffer);

        assertEquals(TokenTypes.JSON_CURLY_BRACKET_LEFT, tokenBuffer.type[0]); // {
        assertEquals(TokenTypes.JSON_STRING_TOKEN, tokenBuffer.type[1]);  // key
        assertEquals(TokenTypes.JSON_COLON, tokenBuffer.type[2]);  // :
        assertEquals(TokenTypes.JSON_STRING_TOKEN, tokenBuffer.type[3]);  // value
        assertEquals(TokenTypes.JSON_COMMA, tokenBuffer.type[4]);  // ,
        assertEquals(TokenTypes.JSON_STRING_TOKEN, tokenBuffer.type[5]);  // key2
        assertEquals(TokenTypes.JSON_COLON, tokenBuffer.type[6]);  // :
        assertEquals(TokenTypes.JSON_STRING_TOKEN, tokenBuffer.type[7]);  // value2
        assertEquals(TokenTypes.JSON_CURLY_BRACKET_RIGHT, tokenBuffer.type[8]);  // }

        assertEquals(ElementTypes.JSON_OBJECT_START, elementBuffer.type[0]);
        assertEquals(ElementTypes.JSON_PROPERTY_NAME, elementBuffer.type[1]); // key
        assertEquals(ElementTypes.JSON_PROPERTY_VALUE_STRING, elementBuffer.type[2]); // value
        assertEquals(ElementTypes.JSON_PROPERTY_NAME, elementBuffer.type[3]); // key2
        assertEquals(ElementTypes.JSON_PROPERTY_VALUE_STRING, elementBuffer.type[4]); // value2
        assertEquals(ElementTypes.JSON_OBJECT_END, elementBuffer.type[5]);
    }

    @Test
    @DisplayName("elementBuffer array input test")
    public void testArrays() {
        DataCharBuffer dataBuffer = new DataCharBuffer();
        dataBuffer.data = "{  \"key\" : \"value\", \"key2\" : [\"value2\", \"value3\" ], \"key3\": \"value4\" }".toCharArray();
        dataBuffer.length = dataBuffer.data.length;

        IndexBuffer tokenBuffer = new IndexBuffer(dataBuffer.data.length, true);
        IndexBuffer elementBuffer = new IndexBuffer(dataBuffer.data.length, true);

        JsonParser parser = new JsonParser(tokenBuffer, elementBuffer);

        parser.parse(dataBuffer);

        assertEquals(ElementTypes.JSON_OBJECT_START, elementBuffer.type[0]); // {
        assertEquals(ElementTypes.JSON_PROPERTY_NAME, elementBuffer.type[1]); // key
        assertEquals(ElementTypes.JSON_PROPERTY_VALUE_STRING, elementBuffer.type[2]); // value
        assertEquals(ElementTypes.JSON_PROPERTY_NAME, elementBuffer.type[3]); // key2
        assertEquals(ElementTypes.JSON_ARRAY_START, elementBuffer.type[4]); // [
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_STRING, elementBuffer.type[5]); // value2
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_STRING, elementBuffer.type[6]); // value3
        assertEquals(ElementTypes.JSON_ARRAY_END     , elementBuffer.type[7]); // ]
        assertEquals(ElementTypes.JSON_PROPERTY_NAME , elementBuffer.type[8]); // key3
        assertEquals(ElementTypes.JSON_PROPERTY_VALUE_STRING, elementBuffer.type[9]); // value4

        assertEquals(ElementTypes.JSON_OBJECT_END    , elementBuffer.type[10]); // }
    }

    @Test
    @DisplayName("element buffer, array with object test")
    public void testArraysWithObjectsWithArraysWithObjects() {
        DataCharBuffer dataBuffer = new DataCharBuffer();
        dataBuffer.data = "{  \"key\" : \"value\", \"key2\" : [\"value2\", { \"key21\" : \"value21\", \"key22\" : [\"value221\", \"value222\"]} ], \"key3\": \"value4\" }".toCharArray();
        dataBuffer.length = dataBuffer.data.length;

        IndexBuffer tokenBuffer   = new IndexBuffer(dataBuffer.data.length, true);
        IndexBuffer elementBuffer = new IndexBuffer(dataBuffer.data.length, true);

        JsonParser parser = new JsonParser(tokenBuffer, elementBuffer);

        parser.parse(dataBuffer);

        assertEquals(ElementTypes.JSON_OBJECT_START   , elementBuffer.type[0]);  // {
        assertEquals(ElementTypes.JSON_PROPERTY_NAME  , elementBuffer.type[1]);  // key
        assertEquals(ElementTypes.JSON_PROPERTY_VALUE_STRING , elementBuffer.type[2]);  // value
        assertEquals(ElementTypes.JSON_PROPERTY_NAME  , elementBuffer.type[3]);  // key2
        assertEquals(ElementTypes.JSON_ARRAY_START    , elementBuffer.type[4]); // [
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_STRING, elementBuffer.type[5]); // value2
        assertEquals(ElementTypes.JSON_OBJECT_START   , elementBuffer.type[6]);  // {
        assertEquals(ElementTypes.JSON_PROPERTY_NAME  , elementBuffer.type[7]); // key21
        assertEquals(ElementTypes.JSON_PROPERTY_VALUE_STRING , elementBuffer.type[8]); // value21
        assertEquals(ElementTypes.JSON_PROPERTY_NAME  , elementBuffer.type[9]);  // key22
        assertEquals(ElementTypes.JSON_ARRAY_START    , elementBuffer.type[10]);  // [
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_STRING, elementBuffer.type[11]);  // value221
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_STRING, elementBuffer.type[12]);  // value222
        assertEquals(ElementTypes.JSON_ARRAY_END      , elementBuffer.type[13]);  // ]
        assertEquals(ElementTypes.JSON_OBJECT_END     , elementBuffer.type[14]);  // }
        assertEquals(ElementTypes.JSON_ARRAY_END      , elementBuffer.type[15]);  // ]
        assertEquals(ElementTypes.JSON_PROPERTY_NAME  , elementBuffer.type[16]);  // key3
        assertEquals(ElementTypes.JSON_PROPERTY_VALUE_STRING , elementBuffer.type[17]);  // value4

        assertEquals(ElementTypes.JSON_OBJECT_END     , elementBuffer.type[18]);  // }
    }

    @Test
    public void testNumbers () {
        DataCharBuffer dataBuffer = new DataCharBuffer();
        dataBuffer.data = "{  \"key\" : 123.345, \"key2\" : [\"value2\", 0.987 ] }".toCharArray();
        dataBuffer.length = dataBuffer.data.length;

        IndexBuffer tokenBuffer   = new IndexBuffer(dataBuffer.data.length, true);
        IndexBuffer elementBuffer = new IndexBuffer(dataBuffer.data.length, true);

        JsonParser parser = new JsonParser(tokenBuffer, elementBuffer);

        parser.parse(dataBuffer);

        assertEquals(ElementTypes.JSON_OBJECT_START            , elementBuffer.type[0]);  // {
        assertEquals(ElementTypes.JSON_PROPERTY_NAME           , elementBuffer.type[1]);  // key
        assertEquals(ElementTypes.JSON_PROPERTY_VALUE_NUMBER   , elementBuffer.type[2]);  // 123.345
        assertEquals(ElementTypes.JSON_PROPERTY_NAME           , elementBuffer.type[3]);  // key2
        assertEquals(ElementTypes.JSON_ARRAY_START             , elementBuffer.type[4]);  // [
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_STRING      , elementBuffer.type[5]);  // value2
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_NUMBER      , elementBuffer.type[6]);  // 0.987
        assertEquals(ElementTypes.JSON_ARRAY_END               , elementBuffer.type[7]);  // ]
        assertEquals(ElementTypes.JSON_OBJECT_END              , elementBuffer.type[8]);  // }
    }

    @Test
    public void testStringsEncoded() {
        DataCharBuffer dataBuffer = new DataCharBuffer();
        dataBuffer.data = "{  \"key\" : \" \\\" \\t \\n \\r \" }".toCharArray();
        dataBuffer.length = dataBuffer.data.length;

        IndexBuffer tokenBuffer   = new IndexBuffer(dataBuffer.data.length, true);
        IndexBuffer elementBuffer = new IndexBuffer(dataBuffer.data.length, true);

        JsonParser parser = new JsonParser(tokenBuffer, elementBuffer);

        parser.parse(dataBuffer);

        assertEquals(ElementTypes.JSON_OBJECT_START              , elementBuffer.type[0]);  // {
        assertEquals(ElementTypes.JSON_PROPERTY_NAME             , elementBuffer.type[1]);  // key
        assertEquals(ElementTypes.JSON_PROPERTY_VALUE_STRING_ENC , elementBuffer.type[2]);  // " \" \t \n \r "
        assertEquals(ElementTypes.JSON_OBJECT_END                , elementBuffer.type[3]);  // }

    }



    @Test
    public void testBooleans() {
        DataCharBuffer dataBuffer = new DataCharBuffer();
        dataBuffer.data = "{  \"key\" : true, \"key2\" : [\"value2\", 0.987, false, true ], \"key3\" : false }".toCharArray();
        dataBuffer.length = dataBuffer.data.length;

        IndexBuffer tokenBuffer   = new IndexBuffer(dataBuffer.data.length, true);
        IndexBuffer elementBuffer = new IndexBuffer(dataBuffer.data.length, true);

        JsonParser parser = new JsonParser(tokenBuffer, elementBuffer);

        parser.parse(dataBuffer);

        assertEquals(ElementTypes.JSON_OBJECT_START            , elementBuffer.type[0]);
        assertEquals(ElementTypes.JSON_PROPERTY_NAME           , elementBuffer.type[1]);
        assertEquals(ElementTypes.JSON_PROPERTY_VALUE_BOOLEAN  , elementBuffer.type[2]);
        assertEquals(ElementTypes.JSON_PROPERTY_NAME           , elementBuffer.type[3]);
        assertEquals(ElementTypes.JSON_ARRAY_START             , elementBuffer.type[4]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_STRING      , elementBuffer.type[5]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_NUMBER      , elementBuffer.type[6]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_BOOLEAN     , elementBuffer.type[7]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_BOOLEAN     , elementBuffer.type[8]);
        assertEquals(ElementTypes.JSON_ARRAY_END               , elementBuffer.type[9]);
        assertEquals(ElementTypes.JSON_PROPERTY_NAME           , elementBuffer.type[10]);
        assertEquals(ElementTypes.JSON_PROPERTY_VALUE_BOOLEAN  , elementBuffer.type[11]);
        assertEquals(ElementTypes.JSON_OBJECT_END              , elementBuffer.type[12]);
    }


    @Test
    public void testNull() {
        DataCharBuffer dataBuffer = new DataCharBuffer();
        dataBuffer.data = "{  \"key\" : null, \"key2\" : [\"value2\", 0.987, false, true, null ], \"key3\" : false }".toCharArray();
        dataBuffer.length = dataBuffer.data.length;

        IndexBuffer tokenBuffer   = new IndexBuffer(dataBuffer.data.length, true);
        IndexBuffer elementBuffer = new IndexBuffer(dataBuffer.data.length, true);

        JsonParser parser = new JsonParser(tokenBuffer, elementBuffer);

        parser.parse(dataBuffer);

        assertEquals(ElementTypes.JSON_OBJECT_START            , elementBuffer.type[0]);
        assertEquals(ElementTypes.JSON_PROPERTY_NAME           , elementBuffer.type[1]);
        assertEquals(ElementTypes.JSON_PROPERTY_VALUE_NULL     , elementBuffer.type[2]);
        assertEquals(ElementTypes.JSON_PROPERTY_NAME           , elementBuffer.type[3]);
        assertEquals(ElementTypes.JSON_ARRAY_START             , elementBuffer.type[4]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_STRING      , elementBuffer.type[5]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_NUMBER      , elementBuffer.type[6]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_BOOLEAN     , elementBuffer.type[7]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_BOOLEAN     , elementBuffer.type[8]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_NULL        , elementBuffer.type[9]);
        assertEquals(ElementTypes.JSON_ARRAY_END               , elementBuffer.type[10]);
        assertEquals(ElementTypes.JSON_PROPERTY_NAME           , elementBuffer.type[11]);
        assertEquals(ElementTypes.JSON_PROPERTY_VALUE_BOOLEAN  , elementBuffer.type[12]);
        assertEquals(ElementTypes.JSON_OBJECT_END              , elementBuffer.type[13]);

    }



    @Test
    public void testMediumFile() {
        DataCharBuffer dataBuffer = new DataCharBuffer();
        dataBuffer.data = TestFileAssertUtil.mediumFile();
        dataBuffer.length = dataBuffer.data.length;

        IndexBuffer tokenBuffer   = new IndexBuffer(dataBuffer.data.length, true);
        IndexBuffer elementBuffer = new IndexBuffer(dataBuffer.data.length, true);

        JsonParser parser = new JsonParser(tokenBuffer, elementBuffer);

        parser.parse(dataBuffer);

        TestFileAssertUtil.assertsMediumFile(elementBuffer);
    }
}
