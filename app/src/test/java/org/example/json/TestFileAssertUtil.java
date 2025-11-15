package org.example.json;

import org.example.core.IndexBuffer;
import static org.junit.jupiter.api.Assertions.*;

public class TestFileAssertUtil {

    public static char[] mediumFile() {

        String file =
                "{ \"key\"   : \"value\",\n" +
                        "  \"key2\"  : 12345,\n" +
                        "  \"key3\"  : false,\n" +
                        "\n" +
                        "  \"stringArray\" : [ \"one\", \"two\", \"three\", \"four\", \"five\", \"six\", \"seven\", \"eight\", \"nine\", \"ten\"],\n" +
                        "  \"numberArray\" : [ 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345],\n" +
                        "  \"booleanArray\" : [ true, false, true, false, true, false, true, false, true, false],\n" +
                        "\n" +
                        "  \"sub\"  : {\n" +
                        "             \"key\"  : \"value\",\n" +
                        "             \"key2\" : 12345,\n" +
                        "             \"key3\" : false,\n" +
                        "             \"stringArray\" : [ \"one\", \"two\", \"three\", \"four\", \"five\", \"six\", \"seven\", \"eight\", \"nine\", \"ten\"],\n" +
                        "             \"numberArray\" : [ 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345],\n" +
                        "             \"booleanArray\" : [ true, false, true, false, true, false, true, false, true, false]\n" +
                        "           }\n" +
                        "}";

        return file.toCharArray();
    }

    public static void assertsMediumFile(IndexBuffer elementBuffer) {
        assertEquals(ElementTypes.JSON_OBJECT_START, elementBuffer.type[0]);
        assertEquals(ElementTypes.JSON_PROPERTY_NAME           , elementBuffer.type[ 1]);
        assertEquals(ElementTypes.JSON_PROPERTY_VALUE_STRING   , elementBuffer.type[ 2]);

        assertEquals(ElementTypes.JSON_PROPERTY_NAME           , elementBuffer.type[ 3]);
        assertEquals(ElementTypes.JSON_PROPERTY_VALUE_NUMBER   , elementBuffer.type[ 4]);

        assertEquals(ElementTypes.JSON_PROPERTY_NAME           , elementBuffer.type[ 5]);
        assertEquals(ElementTypes.JSON_PROPERTY_VALUE_BOOLEAN  , elementBuffer.type[ 6]);

        assertEquals(ElementTypes.JSON_PROPERTY_NAME           , elementBuffer.type[ 7]);
        assertEquals(ElementTypes.JSON_ARRAY_START             , elementBuffer.type[ 8]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_STRING      , elementBuffer.type[ 9]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_STRING      , elementBuffer.type[ 10]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_STRING      , elementBuffer.type[ 11]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_STRING      , elementBuffer.type[ 12]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_STRING      , elementBuffer.type[ 13]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_STRING      , elementBuffer.type[ 14]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_STRING      , elementBuffer.type[ 15]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_STRING      , elementBuffer.type[ 16]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_STRING      , elementBuffer.type[ 17]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_STRING      , elementBuffer.type[ 18]);
        assertEquals(ElementTypes.JSON_ARRAY_END               , elementBuffer.type[ 19]);

        assertEquals(ElementTypes.JSON_PROPERTY_NAME           , elementBuffer.type[ 20]);
        assertEquals(ElementTypes.JSON_ARRAY_START             , elementBuffer.type[ 21]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_NUMBER      , elementBuffer.type[ 22]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_NUMBER      , elementBuffer.type[ 23]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_NUMBER      , elementBuffer.type[ 24]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_NUMBER      , elementBuffer.type[ 25]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_NUMBER      , elementBuffer.type[ 26]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_NUMBER      , elementBuffer.type[ 27]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_NUMBER      , elementBuffer.type[ 28]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_NUMBER      , elementBuffer.type[ 29]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_NUMBER      , elementBuffer.type[ 30]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_NUMBER      , elementBuffer.type[ 31]);
        assertEquals(ElementTypes.JSON_ARRAY_END               , elementBuffer.type[ 32]);

        assertEquals(ElementTypes.JSON_PROPERTY_NAME           , elementBuffer.type[ 33]);
        assertEquals(ElementTypes.JSON_ARRAY_START             , elementBuffer.type[ 34]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_BOOLEAN     , elementBuffer.type[ 35]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_BOOLEAN     , elementBuffer.type[ 36]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_BOOLEAN     , elementBuffer.type[ 37]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_BOOLEAN     , elementBuffer.type[ 38]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_BOOLEAN     , elementBuffer.type[ 39]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_BOOLEAN     , elementBuffer.type[ 40]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_BOOLEAN     , elementBuffer.type[ 41]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_BOOLEAN     , elementBuffer.type[ 42]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_BOOLEAN     , elementBuffer.type[ 43]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_BOOLEAN     , elementBuffer.type[ 44]);
        assertEquals(ElementTypes.JSON_ARRAY_END               , elementBuffer.type[ 45]);

        assertEquals(ElementTypes.JSON_PROPERTY_NAME           , elementBuffer.type[ 46]);
        assertEquals(ElementTypes.JSON_OBJECT_START            , elementBuffer.type[ 47]);

        assertEquals(ElementTypes.JSON_PROPERTY_NAME           , elementBuffer.type[ 48]);
        assertEquals(ElementTypes.JSON_PROPERTY_VALUE_STRING   , elementBuffer.type[ 49]);

        assertEquals(ElementTypes.JSON_PROPERTY_NAME           , elementBuffer.type[ 50]);
        assertEquals(ElementTypes.JSON_PROPERTY_VALUE_NUMBER   , elementBuffer.type[ 51]);

        assertEquals(ElementTypes.JSON_PROPERTY_NAME           , elementBuffer.type[ 52]);
        assertEquals(ElementTypes.JSON_PROPERTY_VALUE_BOOLEAN  , elementBuffer.type[ 53]);

        assertEquals(ElementTypes.JSON_PROPERTY_NAME           , elementBuffer.type[ 54]);
        assertEquals(ElementTypes.JSON_ARRAY_START             , elementBuffer.type[ 55]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_STRING      , elementBuffer.type[ 56]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_STRING      , elementBuffer.type[ 57]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_STRING      , elementBuffer.type[ 58]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_STRING      , elementBuffer.type[ 59]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_STRING      , elementBuffer.type[ 60]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_STRING      , elementBuffer.type[ 61]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_STRING      , elementBuffer.type[ 62]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_STRING      , elementBuffer.type[ 63]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_STRING      , elementBuffer.type[ 64]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_STRING      , elementBuffer.type[ 65]);
        assertEquals(ElementTypes.JSON_ARRAY_END               , elementBuffer.type[ 66]);

        assertEquals(ElementTypes.JSON_PROPERTY_NAME           , elementBuffer.type[ 67]);
        assertEquals(ElementTypes.JSON_ARRAY_START             , elementBuffer.type[ 68]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_NUMBER      , elementBuffer.type[ 69]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_NUMBER      , elementBuffer.type[ 70]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_NUMBER      , elementBuffer.type[ 71]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_NUMBER      , elementBuffer.type[ 72]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_NUMBER      , elementBuffer.type[ 73]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_NUMBER      , elementBuffer.type[ 74]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_NUMBER      , elementBuffer.type[ 75]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_NUMBER      , elementBuffer.type[ 76]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_NUMBER      , elementBuffer.type[ 77]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_NUMBER      , elementBuffer.type[ 78]);
        assertEquals(ElementTypes.JSON_ARRAY_END               , elementBuffer.type[ 79]);

        assertEquals(ElementTypes.JSON_PROPERTY_NAME           , elementBuffer.type[ 80]);
        assertEquals(ElementTypes.JSON_ARRAY_START             , elementBuffer.type[ 81]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_BOOLEAN     , elementBuffer.type[ 82]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_BOOLEAN     , elementBuffer.type[ 83]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_BOOLEAN     , elementBuffer.type[ 84]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_BOOLEAN     , elementBuffer.type[ 85]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_BOOLEAN     , elementBuffer.type[ 86]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_BOOLEAN     , elementBuffer.type[ 87]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_BOOLEAN     , elementBuffer.type[ 88]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_BOOLEAN     , elementBuffer.type[ 89]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_BOOLEAN     , elementBuffer.type[ 90]);
        assertEquals(ElementTypes.JSON_ARRAY_VALUE_BOOLEAN     , elementBuffer.type[ 91]);
        assertEquals(ElementTypes.JSON_ARRAY_END               , elementBuffer.type[ 92]);

        assertEquals(ElementTypes.JSON_OBJECT_END              , elementBuffer.type[ 93]);

        assertEquals(ElementTypes.JSON_OBJECT_END              , elementBuffer.type[ 94]);
    }

}

