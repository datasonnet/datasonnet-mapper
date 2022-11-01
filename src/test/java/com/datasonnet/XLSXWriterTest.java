package com.datasonnet;

/*-
 * Copyright 2019-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.datasonnet.document.DefaultDocument;
import com.datasonnet.document.Document;
import com.datasonnet.document.MediaTypes;
import com.datasonnet.util.TestResourceReader;

import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class XLSXWriterTest {

    @Test
    void testXLSXWriter() throws URISyntaxException, IOException {

        Document<String> data = new DefaultDocument<>(
                TestResourceReader.readFileAsString("writeCSVTest.json"),
                MediaTypes.APPLICATION_JSON
        );

        Mapper mapper = new Mapper("payload");


        Document<byte[]> mapped = mapper.transform(data, Collections.emptyMap(), MediaTypes.APPLICATION_XLSX, byte[].class);
        assertEquals(MediaTypes.APPLICATION_XLSX, mapped.getMediaType());
        

        Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(mapped.getContent()));
        int activeSheetIndex = workbook.getActiveSheetIndex();
        Sheet sheet = workbook.getSheetAt(activeSheetIndex);

        // Verify header
        assertEquals("First Name", sheet.getRow(0).getCell(0).getStringCellValue());
        assertEquals("Last Name", sheet.getRow(0).getCell(1).getStringCellValue());
        assertEquals("Phone", sheet.getRow(0).getCell(2).getStringCellValue());
        // Verify row 1
        assertEquals("William", sheet.getRow(1).getCell(0).getStringCellValue());
        assertEquals("Shakespeare", sheet.getRow(1).getCell(1).getStringCellValue());
        assertEquals("(123)456-7890", sheet.getRow(1).getCell(2).getStringCellValue());
        // Verify row 2
        assertEquals("Christopher", sheet.getRow(2).getCell(0).getStringCellValue());
        assertEquals("Marlow", sheet.getRow(2).getCell(1).getStringCellValue());
        assertEquals("(987)654-3210", sheet.getRow(2).getCell(2).getStringCellValue());
        
        workbook.close();

    }

    @Test
    void testXLSXWriterExt() throws IOException, URISyntaxException {
        Document<String> data = new DefaultDocument<>(
                TestResourceReader.readFileAsString("writeCSVExtTest.json"),
                MediaTypes.APPLICATION_JSON
        );
        String datasonnet = TestResourceReader.readFileAsString("writeXLSXExtTest.ds");

        Mapper mapper = new Mapper(datasonnet);


        InputStream mapped = mapper.transform(data, Collections.emptyMap(), MediaTypes.APPLICATION_XLSX, InputStream.class).getContent();

        Workbook workbook = new XSSFWorkbook(mapped);
        int activeSheetIndex = workbook.getActiveSheetIndex();
        Sheet sheet = workbook.getSheetAt(activeSheetIndex);

        // Verify row 1
        assertEquals("William", sheet.getRow(0).getCell(0).getStringCellValue());
        assertEquals("Shakespeare", sheet.getRow(0).getCell(1).getStringCellValue());
        assertEquals("(123)456-7890", sheet.getRow(0).getCell(2).getStringCellValue());
        // Verify row 2
        assertEquals("Christopher", sheet.getRow(1).getCell(0).getStringCellValue());
        assertEquals("Marlow", sheet.getRow(1).getCell(1).getStringCellValue());
        assertEquals("(987)654-3210", sheet.getRow(1).getCell(2).getStringCellValue());
        
        workbook.close();
    }

}
