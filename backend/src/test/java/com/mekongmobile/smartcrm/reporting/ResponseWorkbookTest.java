package com.mekongmobile.smartcrm.reporting;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipInputStream;
import static org.junit.jupiter.api.Assertions.*;

class ResponseWorkbookTest {
    @Test
    void exportsValidXmlAndTreatsCustomerFormulaAsText() throws Exception {
        var row = new HashMap<String, Object>();
        row.put("ticket_code", "BH-1"); row.put("center_name", "Tân Bình & <Trung tâm>");
        row.put("technician_name", null); row.put("score", 5);
        row.put("comment", "=HYPERLINK(\"unsafe\")\u0000"); row.put("customer_phone", "090****567");
        byte[] bytes = ResponseWorkbook.write(java.util.List.of(row));
        var documents = new HashMap<String, Document>();
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        try (var zip = new ZipInputStream(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8)) {
            java.util.zip.ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                documents.put(entry.getName(), factory.newDocumentBuilder().parse(new ByteArrayInputStream(zip.readAllBytes())));
            }
        }
        assertEquals(5, documents.size());
        Document sheet = documents.get("xl/worksheets/sheet1.xml");
        assertEquals(2, sheet.getElementsByTagName("row").getLength());
        assertEquals(0, sheet.getElementsByTagName("f").getLength());
        assertTrue(sheet.getDocumentElement().getTextContent().contains("=HYPERLINK(\"unsafe\")"));
        assertTrue(sheet.getDocumentElement().getTextContent().contains("Tân Bình & <Trung tâm>"));
        assertTrue(sheet.getDocumentElement().getTextContent().contains("090****567"));
        assertEquals("5", sheet.getElementsByTagName("v").item(0).getTextContent());
    }
}
