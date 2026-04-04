package com.example.demo.parser;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.Exception.ExcelValidationException;
import com.example.demo.Exception.RowValidationException;
import com.example.demo.config.EmployeeFieldConfig;
import com.example.demo.models.EmployeeUpload;

public class ExcelParser {

    private ExcelParser() {
        throw new IllegalStateException("Utility class");
    }

    private static final Logger logger = LoggerFactory.getLogger(ExcelParser.class);

    private static final long MAX_FILE_SERVER = 5L * 1024 * 1024;

    private static final long MAX_ROW_COUNT = 10000;

    private static final int MAX_TYPO_ALLOWED = 2;

    private static final List<String> REQUIRED_COLUMNS = EmployeeFieldConfig.getFieldName();

    private static final Pattern SCRIPT_PATTERN = Pattern.compile("(?i)<script[^>]*>.*?</script>");

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");

    private static final Pattern SQL_PATTERN = Pattern.compile("(?i).*(DROP|DELETE|INSERT|UPDATE|SELECT|UNION|--|;).*");

    public static class RowResult {
        public final int rowNum;
        public final boolean isBlank;
        public final boolean isPassed;
        public final List<String> errors;
        public final Map<String, String> stringValues;
        public final Map<String, Object> numericValues;

        public RowResult(int rowNum, boolean isBlank,
                Map<String, String> stringValues,
                Map<String, Object> numericValues,
                List<String> errors) {
            this.rowNum = rowNum;
            this.isBlank = isBlank;
            this.stringValues = stringValues;
            this.numericValues = numericValues;
            this.errors = errors;
            this.isPassed = !isBlank && errors.isEmpty();
        }
    }

    private static int patternMatch(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++)
            dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++)
            dp[0][j] = j;

        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                if (a.charAt(i - 1) == b.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(
                            dp[i - 1][j - 1],
                            Math.min(
                                    dp[i - 1][j],
                                    dp[i][j - 1]));
                }
            }
        }
        return dp[a.length()][b.length()];
    }

    private static String fuzzyMatch(String actual) {
        String bestMatch = null;
        int bestDistance = Integer.MAX_VALUE;

        for (String required : REQUIRED_COLUMNS) {
            int distance = patternMatch(actual.toLowerCase(), required);

            if (distance < bestDistance) {
                bestDistance = distance;
                bestMatch = required;
            }
        }

        return bestDistance <= MAX_TYPO_ALLOWED ? bestMatch : null;
    }

    private static void processHeaderCell(int position, String actualHeader,
            Map<String, Integer> columnIndex,
            Map<String, Boolean> found,
            List<String> warnings) {

        String matched = fuzzyMatch(actualHeader);

        if (matched == null) {
            logger.warn("Column {} — '{}' does not match any required column, ignoring",
                    position + 1, actualHeader);
            return;
        }

        columnIndex.put(matched, position);
        found.put(matched, true);

        if (!actualHeader.toLowerCase().equals(matched)) {
            warnings.add("Column{}" + (position + 1) + " _ " + actualHeader // ← Bug 2 fixed
                    + " was interpreted as " + matched);
            logger.warn("Column {} - '{}' interpreted as '{}'", position + 1, actualHeader, matched);
        } else {
            logger.info("Column {} - '{}' matched exactly", position + 1, matched);
        }
    }

    private static Map<String, Object> buildColumnIndexMap(Row headerRow, DataFormatter formatter) {
        Map<String, Integer> columnsIndex = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        Map<String, Boolean> found = new LinkedHashMap<>();
        for (String col : REQUIRED_COLUMNS)
            found.put(col, false);

        for (Cell cell : headerRow) {
            int position = cell.getColumnIndex();
            String actualHeader = formatter.formatCellValue(cell).trim();

            // if row is blank we simply skip and move forward without adding any error
            // because we will check for missing required columns at the end and report all
            // together
            if (!actualHeader.isBlank())
                processHeaderCell(position, actualHeader, columnsIndex, found, warnings);
        }

        for (Map.Entry<String, Boolean> entry : found.entrySet()) {
            if (Boolean.FALSE.equals(entry.getValue())) {
                errors.add("Column " + entry.getKey() + " is missing and could not be matched ");
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("columnIndexMap", columnsIndex);
        result.put("warnings", warnings);
        result.put("errors", errors);

        return result;
    }

    private static String sanitize(String input) {
        if (input == null || input.isBlank())
            return "";

        if (input.indexOf('<') == -1
                && input.indexOf('>') == -1
                && input.indexOf('&') == -1
                && input.indexOf('"') == -1
                && input.indexOf('\'') == -1
                && input.indexOf(';') == -1
                && input.indexOf('-') == -1) {
            return input;
        }

        String cleaned = SCRIPT_PATTERN.matcher(input).replaceAll("");
        cleaned = HTML_TAG_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = cleaned
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;")
                .trim();

        if (SQL_PATTERN.matcher(input).matches()) {
            logger.warn("SQL injection pattern detected: {}", input);
        }

        return cleaned;
    }

    public static void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ExcelValidationException("Please upload a file. No file was received.");
        }

        if (file.getSize() > MAX_FILE_SERVER) {
            throw new ExcelValidationException("Exceeded max size alllowed: File size is" + file.getSize());
        }

        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();

        boolean isValidType = "application/vnd.ms-excel".equals(contentType) ||
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet".equals(contentType);

        boolean isValidExtension = originalFilename != null &&
                (originalFilename.endsWith(".xls") || originalFilename.endsWith(".xlsx"));

        if (!isValidType || !isValidExtension) {
            throw new ExcelValidationException(
                    "Invalid file type. Only .xls and .xlsx files are accepted. Received: " + originalFilename);
        }

        validateMagicBytes(file);

        logger.info("File validation passed → name: {}, size: {}KB, type: {}",
                originalFilename,
                file.getSize() / 1024,
                contentType);

    }

    private static void validateMagicBytes(MultipartFile file) {

        try (InputStream is = file.getInputStream()) {
            byte[] bytes = is.readNBytes(8);

            if (bytes.length < 4) {
                throw new ExcelValidationException("File is too small to be a valid Excel format.");
            }

            boolean isXlsx = (bytes[0] == 0x50 && bytes[1] == 0x4B &&
                    bytes[2] == 0x03 && bytes[3] == 0x04);

            boolean isXls = (bytes[0] == (byte) 0xD0 && bytes[1] == (byte) 0xCF &&
                    bytes[2] == 0x11 && bytes[3] == (byte) 0xE0);

            if (!isXlsx && !isXls) {
                throw new ExcelValidationException(
                        "File content is not a valid Excel format. " +
                                "Renaming a file to .xlsx does not make it an Excel file.");
            }

        } catch (ExcelValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ExcelValidationException("Could not read file content: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> validateStruture(Workbook workbook, Sheet sheet) {
        if (workbook.getNumberOfSheets() <= 0) {
            throw new ExcelValidationException("Uploaded excel file has 0 sheets");
        }

        if (sheet.getPhysicalNumberOfRows() == 0) {
            throw new ExcelValidationException("Excel sheet is empty. No data found");
        }
        if (sheet.getPhysicalNumberOfRows() == 1) {
            throw new ExcelValidationException("No data rows found. File only contains the header row.");
        }

        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            throw new ExcelValidationException("Header row is missing");
        }

        DataFormatter formatter = new DataFormatter();

        Map<String, Object> columnResult = buildColumnIndexMap(headerRow, formatter);

        List<String> errors = (List<String>) columnResult.get("errors");

        if (!errors.isEmpty()) {
            throw new ExcelValidationException("Missing required Columns" + String.join(", ", errors));
        }

        logger.info(" Structure Validation passed -> sheets: {}, data rows: {}",
                workbook.getNumberOfSheets(), sheet.getPhysicalNumberOfRows());

        return columnResult;
    }

    private static List<RowResult> processRows(Sheet sheet,
            Map<String, Integer> columnIndex, DataFormatter formatter) {

        List<RowResult> results = new ArrayList<>();
        Set<String> seenEmails = new HashSet<>();
        int dataRowCount = 0;

        for (Row row : sheet) {
            if (row.getRowNum() == 0)
                continue; 

            int displayNumber = row.getRowNum() + 1;
            RowResult result = processSingleRow(row, columnIndex, formatter, displayNumber, seenEmails);

            if (!result.isBlank) {
                dataRowCount++;
                if (dataRowCount > MAX_ROW_COUNT) {
                    throw new ExcelValidationException(
                            "File exceeds the maximum allowed limit of 10,000 data rows.");
                }
            }
            results.add(result);
        }
        return results;
    }

    private static RowResult processSingleRow(Row row, Map<String, Integer> columnIndex,
            DataFormatter formatter, int displayNumber, Set<String> seenEmails) {

        Map<String, String> stringValues = new LinkedHashMap<>();
        Map<String, Object> numericValues = new LinkedHashMap<>();

        for (EmployeeFieldConfig.FieldDef field : EmployeeFieldConfig.FIELDS) {
            String fieldName = field.employeeName();
            Cell cell = row.getCell(columnIndex.get(fieldName));
            String raw = formatter.formatCellValue(cell).trim();

            if (field.type() == EmployeeFieldConfig.FieldType.STRING) {
                stringValues.put(fieldName, sanitize(raw));
            } else {
                stringValues.put(fieldName, raw);
                if (cell != null && (cell.getCellType() == CellType.NUMERIC || cell.getCellType() == CellType.FORMULA)) {
                    // numerical or formula cell
                    numericValues.put(fieldName, cell.getNumericCellValue());
                    numericValues.put(fieldName + "_isNumeric", true);
                } else {
                    // Try to parse text numbers (e.g. from pasted strings)
                    String normalized = raw.replaceAll("[\\s,]", "");
                    try {
                        double parsedValue = Double.parseDouble(normalized);
                        numericValues.put(fieldName, parsedValue);
                        numericValues.put(fieldName + "_isNumeric", true);
                    } catch (NumberFormatException e) {
                        numericValues.put(fieldName, 0.0);
                        numericValues.put(fieldName + "_isNumeric", false);
                    }
                }
            }
        }

        boolean isBlankRow = stringValues.values().stream().allMatch(String::isBlank);
        if (isBlankRow) {
            return new RowResult(displayNumber, true,
                    new LinkedHashMap<>(), new LinkedHashMap<>(), new ArrayList<>());
        }

        List<String> rowErrors = new ArrayList<>();
        EmployeeFieldConfig.validate(displayNumber, stringValues, numericValues, rowErrors);

        checkDuplicateEmail(displayNumber,
                stringValues.getOrDefault("email", ""), seenEmails, rowErrors);

        return new RowResult(displayNumber, false, stringValues, numericValues, rowErrors);
    }
    private static void checkDuplicateEmail(int displayNumber, String email,
            Set<String> seenEmails, List<String> rowErrors) {
        if (!rowErrors.isEmpty()) {
            return;
        }

        if (email == null || email.isBlank()) {
            return;
        }

        String emailLower = email.toLowerCase();
        if (seenEmails.contains(emailLower)) {
            rowErrors.add("Row " + displayNumber
                    + " — Duplicate email '" + email
                    + "' already exists in this file");
        } else {
            seenEmails.add(emailLower);
        }
    }

    @SuppressWarnings("unchecked")
    public static byte[] generateReport(MultipartFile file) {

        validateFile(file);

        try (Workbook inputWorkbook = WorkbookFactory.create(file.getInputStream())) {

            Sheet inputSheet = inputWorkbook.getSheetAt(0);
            Map<String, Object> structureResult = validateStruture(inputWorkbook, inputSheet);
            Map<String, Integer> columnIndex = (Map<String, Integer>) structureResult.get("columnIndexMap");
            List<String> warnings = (List<String>) structureResult.get("warnings");

            DataFormatter formatter = new DataFormatter();
            List<RowResult> rowResults = processRows(inputSheet, columnIndex, formatter);

            Workbook outWorkbook = new XSSFWorkbook();
            Sheet outSheet = outWorkbook.createSheet("Upload Report");

            CellStyle headerStyle = buildHeaderStyle(outWorkbook);
            CellStyle passStyle = buildColorStyle(outWorkbook, IndexedColors.LIGHT_GREEN);
            CellStyle failStyle = buildColorStyle(outWorkbook, IndexedColors.ROSE);

            // headers built from config — not hardcoded
            List<String> dataHeaders = new ArrayList<>(EmployeeFieldConfig.getFieldName());
            dataHeaders.add("Status");
            dataHeaders.add("Error Message");
            String[] headers = dataHeaders.toArray(String[]::new);

            Row headerRow = outSheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                outSheet.setColumnWidth(i, i >= headers.length - 2 ? 8000 : 5000);
            }

            // cell writing loops through config — not hardcoded
            int outputRow = 1;
            for (RowResult result : rowResults) {
                if (result.isBlank)
                    continue;

                Row row = outSheet.createRow(outputRow++);
                int colIdx = 0;

                for (EmployeeFieldConfig.FieldDef field : EmployeeFieldConfig.FIELDS) {
                    Cell cell = row.createCell(colIdx++);
                    if (field.type() == EmployeeFieldConfig.FieldType.STRING) {
                        cell.setCellValue(result.stringValues.getOrDefault(field.employeeName(), ""));
                    } else {
                        boolean isNumeric = Boolean.TRUE.equals(
                                result.numericValues.get(field.employeeName() + "_isNumeric"));
                        if (isNumeric) {
                            cell.setCellValue((double) result.numericValues.get(field.employeeName()));
                        } else {
                            cell.setCellValue("");
                        }
                    }
                }

                Cell statusCell = row.createCell(colIdx++);
                statusCell.setCellValue(result.isPassed ? "PASS" : "FAIL");
                statusCell.setCellStyle(result.isPassed ? passStyle : failStyle);

                Cell errorCell = row.createCell(colIdx);
                if (result.isPassed) {
                    errorCell.setCellValue("");
                } else {
                    String errorMessage = result.errors.stream()
                            .map(e -> e.replaceAll("Row\\s+\\d+\\s*[—\\-]\\s*", "").trim())
                            .reduce((a, b) -> a + " | " + b)
                            .orElse("");
                    errorCell.setCellValue(errorMessage);
                    errorCell.setCellStyle(failStyle);
                }
            }

            if (!warnings.isEmpty()) {
                Sheet warnSheet = outWorkbook.createSheet("Header Warnings");
                Row warnHeader = warnSheet.createRow(0);
                warnHeader.createCell(0).setCellValue("Warning");
                warnHeader.getCell(0).setCellStyle(headerStyle);
                warnSheet.setColumnWidth(0, 15000);
                for (int i = 0; i < warnings.size(); i++) {
                    warnSheet.createRow(i + 1).createCell(0).setCellValue(warnings.get(i));
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            outWorkbook.write(baos);
            outWorkbook.close();

            logger.info("Report generated → passed: {}, failed: {}",
                    rowResults.stream().filter(r -> r.isPassed).count(),
                    rowResults.stream().filter(r -> !r.isPassed && !r.isBlank).count());

            return baos.toByteArray();

        } catch (ExcelValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ExcelValidationException("Report generation failed: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> excelParser(MultipartFile file) {

        validateFile(file);

        List<EmployeeUpload> employees = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();
        List<String> allErrors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            Map<String, Object> structureResult = validateStruture(workbook, sheet);
            warnings = (List<String>) structureResult.get("warnings");
            Map<String, Integer> columnIndex = (Map<String, Integer>) structureResult.get("columnIndexMap");

            logger.info("Reading Excel File: {}", file.getOriginalFilename());

            List<RowResult> results = processRows(sheet, columnIndex, formatter);

            for (RowResult result : results) {
                if (result.isBlank)
                    continue;

                if (!result.errors.isEmpty()) {
                    allErrors.addAll(result.errors);
                    continue;
                }

                EmployeeUpload emp = new EmployeeUpload();
                emp.setName(result.stringValues.get("name"));
                emp.setEmail(result.stringValues.get("email"));
                emp.setPassword(result.stringValues.get("password"));
                emp.setAge((int) (double) result.numericValues.get("age"));
                emp.setSalary((double) result.numericValues.get("salary"));
                employees.add(emp);

                logger.info("Row {} → name: {}, email: {}, age: {}, salary: {}",
                        result.rowNum,
                        result.stringValues.get("name"),
                        result.stringValues.get("email"),
                        result.numericValues.get("age"),
                        result.numericValues.get("salary"));
            }

            logger.info("Upload complete — valid rows: {}", employees.size());

        } catch (ExcelValidationException | RowValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ExcelValidationException("Excel parsing failed: " + e.getMessage(), e);
        }

        if (!allErrors.isEmpty()) {
            logger.warn("Validation errors found: {}", allErrors.size());
            throw new RowValidationException(allErrors);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("employees", employees);
        result.put("warnings", warnings);
        return result;
    }

    private static CellStyle buildHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private static CellStyle buildColorStyle(Workbook workbook, IndexedColors color) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(color.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

}
