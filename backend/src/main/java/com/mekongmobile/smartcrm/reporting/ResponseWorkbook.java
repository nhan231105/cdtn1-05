package com.mekongmobile.smartcrm.reporting;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Minimal Office Open XML workbook with inline strings (customer text is never a formula). */
final class ResponseWorkbook {
    private ResponseWorkbook() { }

    static byte[] write(List<Map<String, Object>> responses) {
        try {
            var bytes = new ByteArrayOutputStream();
            try (var zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
                entry(zip, "[Content_Types].xml", """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                        <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                        <Default Extension="xml" ContentType="application/xml"/>
                        <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                        <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                        </Types>""");
                entry(zip, "_rels/.rels", """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                        <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                        </Relationships>""");
                entry(zip, "xl/workbook.xml", """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                        <sheets><sheet name="Phản hồi khảo sát" sheetId="1" r:id="rId1"/></sheets></workbook>""");
                entry(zip, "xl/_rels/workbook.xml.rels", """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                        <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                        </Relationships>""");
                var sheet = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
                row(sheet, 1, List.of("Mã phiếu", "Trung tâm", "Kỹ thuật viên", "Điểm", "Nhận xét", "Số điện thoại (che)", "Thời điểm phản hồi"));
                int index = 2;
                for (Map<String, Object> response : responses) {
                    row(sheet, index++, java.util.Arrays.asList(response.get("ticket_code"), response.get("center_name"),
                            response.get("technician_name"), response.get("score"), response.get("comment"),
                            response.get("customer_phone"), response.get("responded_at")));
                }
                sheet.append("</sheetData></worksheet>");
                entry(zip, "xl/worksheets/sheet1.xml", sheet.toString());
            }
            return bytes.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot create workbook", exception);
        }
    }

    private static void entry(ZipOutputStream zip, String name, String xml) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(xml.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static void row(StringBuilder xml, int number, List<?> cells) {
        xml.append("<row r=\"").append(number).append("\">");
        for (int column = 0; column < cells.size(); column++) {
            Object value = cells.get(column);
            xml.append("<c r=\"").append((char) ('A' + column)).append(number).append('"');
            if (value instanceof Number) xml.append("><v>").append(value).append("</v></c>");
            else xml.append(" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
                    .append(escape(value == null ? "" : value.toString())).append("</t></is></c>");
        }
        xml.append("</row>");
    }

    private static String escape(String value) {
        StringBuilder safe = new StringBuilder();
        value.codePoints().filter(c -> c == 9 || c == 10 || c == 13 || (c >= 32 && c <= 0xD7FF)
                || (c >= 0xE000 && c <= 0xFFFD) || (c >= 0x10000 && c <= 0x10FFFF)).forEach(safe::appendCodePoint);
        return safe.toString().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
