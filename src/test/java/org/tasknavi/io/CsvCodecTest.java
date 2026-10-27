package org.tasknavi.io;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CsvCodecTest {

    @Test
    void decodesUtf8WithAndWithoutBomAndShiftJis() {
        String text = "タスク名,担当者\r\n要件定義,山田\r\n";
        byte[] withBom = CsvCodec.encodeForExcel(text);
        assertArrayEquals(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}, java.util.Arrays.copyOf(withBom, 3));

        assertEquals(text, CsvCodec.decode(withBom));
        assertEquals(text, CsvCodec.decode(text.getBytes(StandardCharsets.UTF_8)));
        assertEquals(text, CsvCodec.decode(text.getBytes(CsvCodec.WINDOWS_31J)));
    }

    @Test
    void parsesQuotedDelimitersQuotesAndLineBreaksWithStartLineNumbers() {
        String text = "a,b,c\r\n"
                + "1,\"x,y\",\"say \"\"hi\"\"\"\r\n"
                + "\r\n"
                + "2,\"line1\r\nline2\",z\n"
                + "3,last,\n";

        List<CsvCodec.Row> rows = CsvCodec.parse(text, ',');

        assertEquals(4, rows.size());
        assertEquals(List.of("1", "x,y", "say \"hi\""), rows.get(1).fields());
        assertEquals(2, rows.get(1).lineNumber());
        assertEquals(List.of("2", "line1\nline2", "z"), rows.get(2).fields());
        assertEquals(4, rows.get(2).lineNumber());
        assertEquals(List.of("3", "last", ""), rows.get(3).fields());
        assertEquals(6, rows.get(3).lineNumber());
    }

    @Test
    void formatQuotesOnlyWhenNeededAndRoundTrips() {
        List<List<String>> rows = List.of(
                List.of("ID", "名前"),
                List.of("1", "a,b"),
                List.of("2", "改行\nあり"),
                List.of("3", "\"引用\""));

        String csv = CsvCodec.format(rows);

        assertEquals("ID,名前\r\n1,\"a,b\"\r\n2,\"改行\nあり\"\r\n3,\"\"\"引用\"\"\"\r\n", csv);
        List<CsvCodec.Row> parsed = CsvCodec.parse(csv, ',');
        for (int i = 0; i < rows.size(); i++) {
            assertEquals(rows.get(i), parsed.get(i).fields());
        }
    }

    @Test
    void detectsTabSeparatedText() {
        assertEquals('\t', CsvCodec.detectDelimiter("タスク名\t担当者\nA\tB"));
        assertEquals(',', CsvCodec.detectDelimiter("タスク名,担当者\nA,B"));
    }
}
