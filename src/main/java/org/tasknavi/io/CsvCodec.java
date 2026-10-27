package org.tasknavi.io;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * CSV の文字コード判定・分解・組み立て（RFC 4180 準拠）。
 * Excel で保存した CSV を読めるように、BOM なし UTF-8 と Shift_JIS（Windows-31J）も受け付ける。
 */
public final class CsvCodec {

    public static final Charset WINDOWS_31J = Charset.forName("windows-31j");
    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    /** @param lineNumber 行が始まる物理行（1 始まり）。値の中に改行があっても数え間違えない */
    public record Row(int lineNumber, List<String> fields) {
    }

    private CsvCodec() {
    }

    /** BOM 付き → UTF-8、正しい UTF-8 として読める → UTF-8、それ以外 → Windows-31J。 */
    public static String decode(byte[] bytes) {
        if (startsWithUtf8Bom(bytes)) {
            return new String(bytes, UTF8_BOM.length, bytes.length - UTF8_BOM.length, StandardCharsets.UTF_8);
        }
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException e) {
            return new String(bytes, WINDOWS_31J);
        }
    }

    /** Excel が文字化けせずに開ける形（BOM 付き UTF-8）。 */
    public static byte[] encodeForExcel(String text) {
        byte[] body = text.getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[UTF8_BOM.length + body.length];
        System.arraycopy(UTF8_BOM, 0, out, 0, UTF8_BOM.length);
        System.arraycopy(body, 0, out, UTF8_BOM.length, body.length);
        return out;
    }

    /** 1 行目にタブがあってカンマがなければタブ区切りとみなす。 */
    public static char detectDelimiter(String text) {
        int end = text.indexOf('\n');
        String firstLine = end < 0 ? text : text.substring(0, end);
        return firstLine.indexOf('\t') >= 0 && firstLine.indexOf(',') < 0 ? '\t' : ',';
    }

    /** 空行は飛ばす。引用符の中の改行・区切り文字・二重引用符（""）はそのまま値に入る。 */
    public static List<Row> parse(String text, char delimiter) {
        List<Row> rows = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        int line = 1;
        int rowStartLine = 1;

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else if (ch == '\r' || ch == '\n') {
                    if (ch == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                        i++;
                    }
                    field.append('\n');
                    line++;
                } else {
                    field.append(ch);
                }
            } else if (ch == '"' && field.isEmpty()) {
                inQuotes = true;
            } else if (ch == delimiter) {
                fields.add(field.toString());
                field.setLength(0);
            } else if (ch == '\r' || ch == '\n') {
                if (ch == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
                fields.add(field.toString());
                field.setLength(0);
                addIfNotBlank(rows, rowStartLine, fields);
                fields = new ArrayList<>();
                line++;
                rowStartLine = line;
            } else {
                field.append(ch);
            }
        }
        fields.add(field.toString());
        addIfNotBlank(rows, rowStartLine, fields);
        return rows;
    }

    /** 行の区切りは CRLF（Excel と同じ）。 */
    public static String format(List<List<String>> rows) {
        StringBuilder out = new StringBuilder();
        for (List<String> row : rows) {
            for (int i = 0; i < row.size(); i++) {
                if (i > 0) {
                    out.append(',');
                }
                out.append(quote(row.get(i)));
            }
            out.append("\r\n");
        }
        return out.toString();
    }

    static String quote(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String normalized = value.replace("\r\n", "\n").replace('\r', '\n');
        if (normalized.indexOf(',') >= 0 || normalized.indexOf('"') >= 0 || normalized.indexOf('\n') >= 0
                || normalized.startsWith(" ") || normalized.endsWith(" ")) {
            return "\"" + normalized.replace("\"", "\"\"") + "\"";
        }
        return normalized;
    }

    private static void addIfNotBlank(List<Row> rows, int lineNumber, List<String> fields) {
        for (String value : fields) {
            if (!value.isBlank()) {
                rows.add(new Row(lineNumber, List.copyOf(fields)));
                return;
            }
        }
    }

    private static boolean startsWithUtf8Bom(byte[] bytes) {
        return bytes.length >= UTF8_BOM.length
                && bytes[0] == UTF8_BOM[0] && bytes[1] == UTF8_BOM[1] && bytes[2] == UTF8_BOM[2];
    }
}
