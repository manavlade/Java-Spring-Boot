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
import com.example.demo.models.EmployeeUpload;

public class ExcelParser {

    private ExcelParser() {
        throw new IllegalStateException("Utility class");
    }

    private static final Logger logger = LoggerFactory.getLogger(ExcelParser.class);

    private static final long MAX_FILE_SERVER = 5 * 1024 * 1024;

    private static final long MAX_ROW_COUNT = 10000;

    private static final int MAX_TYPO_ALLOWED = 2;

    private static final List<String> REQUIRED_COLUMNS = List.of("name", "email", "password", "age", "salary");

    // ← ADD these 3 lines at class level alongside your other constants
    private static final Pattern SCRIPT_PATTERN = Pattern.compile("(?i)<script[^>]*>.*?</script>");

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");

    private static final Pattern SQL_PATTERN = Pattern.compile("(?i).*(DROP|DELETE|INSERT|UPDATE|SELECT|UNION|--|;).*");

    public static class RowResult {
        public final int rowNum;
        public final boolean isBlank;
        public final boolean isPassed;
        public final List<String> errors;
        public final String name;
        public final String email;
        public final String password;
        public final int age;
        public final double salary;
        public final boolean ageIsNumeric;
        public final boolean salaryIsNumeric;

        public RowResult(int rowNum, boolean isBlank, String name, String email,
                String password, int age, boolean ageIsNumeric,
                double salary, boolean salaryIsNumeric, List<String> errors) {
            this.rowNum = rowNum;
            this.isBlank = isBlank;
            this.name = name;
            this.email = email;
            this.password = password;
            this.age = age;
            this.ageIsNumeric = ageIsNumeric;
            this.salary = salary;
            this.salaryIsNumeric = salaryIsNumeric;
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

    // @SuppressWarnings("unchecked")
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

            if (actualHeader.isBlank())
                continue;

            String matched = fuzzyMatch(actualHeader);

            if (matched == null) {
                logger.warn("Column {} — '{}' does not match any required column, ignoring",
                        position + 1, actualHeader);
                continue;
            }

            columnsIndex.put(matched, position);
            found.put(matched, true);

            if (!actualHeader.toLowerCase().equals(matched)) {
                warnings.add("Column " + (position + 1) + " _ " + actualHeader + " was interpreted as " + matched + "");
                logger.warn("Column {} -'{}' interpreted as '{}", position + 1, actualHeader, matched);
            } else {
                logger.info("Column {} - '{}' matched exactly", position + 1, matched);
            }
        }

        for (Map.Entry<String, Boolean> entry : found.entrySet()) {
            if (!entry.getValue()) {
                errors.add("Column " + entry.getKey() + " is missing and could not be matched ");
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("columnIndexMap", columnsIndex);
        result.put("warnings", warnings);
        result.put("errors", errors);

        return result;
    }

    // to check for security vulnerability like xss and sql injection in the future
    // we can add more patterns to check for
    private static String sanitize(String input) {
        if (input == null || input.isBlank())
            return input;

        if (input.indexOf('<') == -1
                && input.indexOf('>') == -1
                && input.indexOf('&') == -1
                && input.indexOf('"') == -1
                && input.indexOf('\'') == -1
                && input.indexOf(';') == -1
                && input.indexOf('-') == -1) {
            return input;
        }

        // ← now uses pre-compiled patterns instead of inline regex
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

    // basic level validation like file type and size and content type
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

    // level 2 to check if someone renamed pdf to xl and uploaded
    private static void validateMagicBytes(MultipartFile file) {
        byte[] bytes = new byte[8];

        try (InputStream is = file.getInputStream()) {
            is.read(bytes);

            // .xlsx magic bytes — starts with PK (50 4B 03 04) — it's a ZIP file internally
            boolean isXlsx = bytes[0] == 0x50 && bytes[1] == 0x4B &&
                    bytes[2] == 0x03 && bytes[3] == 0x04;

            // .xls magic bytes — D0 CF 11 E0
            boolean isXls = bytes[0] == (byte) 0xD0 && bytes[1] == (byte) 0xCF &&
                    bytes[2] == 0x11 && bytes[3] == (byte) 0xE0;

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

    public static void validateFileRow(int displayRow, String name, String email, String password, int age,
            boolean ageIsNumeric, double salary, boolean salaryIsDouble, List<String> errors) {
        if (name.isBlank()) {

            errors.add("Row" + displayRow + "-Name missing");
        }

        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        if (email.isBlank()) {
            errors.add("Row" + displayRow + "-Email missing");
        } else if (!email.matches(emailRegex)) {
            errors.add("Row " + displayRow + " Invalid Email format ");
        }

        if (password.isBlank()) {
            errors.add("Row " + displayRow + "-password missing");
        } else if (password.length() < 8) {
            errors.add("Row " + displayRow + " password must be ateast 8 characters");
        }

        if (!ageIsNumeric) {
            errors.add("Row " + displayRow + "-Age is not number");
        } else if (age <= 0 || age > 120) {
            errors.add("Row " + displayRow + "Age must be greater than 0 and less than 120");
        }

        if (!salaryIsDouble) {
            errors.add("Row " + displayRow + "-Salary is not number");
        } else if (salary <= 0) {
            errors.add("Row " + displayRow + "Salary must be greater than 0");
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

    // ← ADD this entire method — completely new
    private static List<RowResult> processRows(Sheet sheet,
            Map<String, Integer> columnIndex, DataFormatter formatter) {

        List<RowResult> results = new ArrayList<>();
        Set<String> seenEmails = new HashSet<>();
        int dataRowCount = 0;

        for (Row row : sheet) {
            if (row.getRowNum() == 0)
                continue;

            int displayNumber = row.getRowNum() + 1;

            String name = sanitize(formatter.formatCellValue(row.getCell(columnIndex.get("name"))));
            String email = sanitize(formatter.formatCellValue(row.getCell(columnIndex.get("email"))));
            String password = sanitize(formatter.formatCellValue(row.getCell(columnIndex.get("password"))));
            String ageRaw = formatter.formatCellValue(row.getCell(columnIndex.get("age")));
            String salRaw = formatter.formatCellValue(row.getCell(columnIndex.get("salary")));

            // blank row
            if (name.isBlank() && email.isBlank() && password.isBlank()
                    && ageRaw.isBlank() && salRaw.isBlank()) {
                results.add(new RowResult(displayNumber, true, "", "", "",
                        0, false, 0, false, new ArrayList<>()));
                continue;
            }

            dataRowCount++;
            if (dataRowCount > MAX_ROW_COUNT) {
                throw new ExcelValidationException(
                        "File exceeds the maximum allowed limit of 10,000 data rows.");
            }

            // parse age
            int age = 0;
            boolean ageIsNumeric = false;
            Cell ageCell = row.getCell(columnIndex.get("age"));
            if (ageCell != null && ageCell.getCellType() == CellType.NUMERIC) {
                age = (int) ageCell.getNumericCellValue();
                ageIsNumeric = true;
            }

            // parse salary
            double salary = 0;
            boolean salaryIsDouble = false;
            Cell salCell = row.getCell(columnIndex.get("salary"));
            if (salCell != null && salCell.getCellType() == CellType.NUMERIC) {
                salary = salCell.getNumericCellValue();
                salaryIsDouble = true;
            }

            List<String> rowErrors = new ArrayList<>();

            validateFileRow(displayNumber, name, email, password,
                    age, ageIsNumeric, salary, salaryIsDouble, rowErrors);

            // within-file duplicate check
            if (rowErrors.isEmpty()) {
                String emailLower = email.toLowerCase();
                if (seenEmails.contains(emailLower)) {
                    rowErrors.add("Row " + displayNumber
                            + " — Duplicate email '" + email
                            + "' already exists in this file");
                } else {
                    seenEmails.add(emailLower);
                }
            }

            results.add(new RowResult(displayNumber, false, name, email, password,
                    age, ageIsNumeric, salary, salaryIsDouble, rowErrors));
        }

        return results;
    }

    // ← ADD this entire method — completely new
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

            // build output workbook
            Workbook outWorkbook = new XSSFWorkbook();
            Sheet outSheet = outWorkbook.createSheet("Upload Report");

            // header style — dark blue
            CellStyle headerStyle = outWorkbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = outWorkbook.createFont();
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // pass style — light green
            CellStyle passStyle = outWorkbook.createCellStyle();
            passStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            passStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // fail style — rose/red
            CellStyle failStyle = outWorkbook.createCellStyle();
            failStyle.setFillForegroundColor(IndexedColors.ROSE.getIndex());
            failStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // header row
            Row headerRow = outSheet.createRow(0);
            String[] headers = { "name", "email", "password", "age", "salary",
                    "Status", "Error Message" };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                outSheet.setColumnWidth(i, i >= 5 ? 8000 : 5000);
            }

            // data rows
            int outputRow = 1;
            for (RowResult result : rowResults) {
                if (result.isBlank)
                    continue;

                Row row = outSheet.createRow(outputRow++);

                row.createCell(0).setCellValue(result.name);
                row.createCell(1).setCellValue(result.email);
                row.createCell(2).setCellValue(result.password);

                if (result.ageIsNumeric) {
                    row.createCell(3).setCellValue(result.age);
                } else {
                    row.createCell(3).setCellValue("");
                }

                if (result.salaryIsNumeric) {
                    row.createCell(4).setCellValue(result.salary);
                } else {
                    row.createCell(4).setCellValue("");
                }

                // Status column
                Cell statusCell = row.createCell(5);
                statusCell.setCellValue(result.isPassed ? "PASS" : "FAIL");
                statusCell.setCellStyle(result.isPassed ? passStyle : failStyle);

                // Error Message column
                Cell errorCell = row.createCell(6);
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

            // warnings sheet if any
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

            // write to bytes
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            outWorkbook.write(baos);
            outWorkbook.close();

            logger.info("Report generated → passed: {}, failed: {}",
                    rowResults.stream().filter(r -> r.isPassed).count(),
                    rowResults.stream().filter(r -> !r.isPassed && !r.isBlank).count());

            return baos.toByteArray();

        } catch (ExcelValidationException | RowValidationException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Report generation failed: {}", e.getMessage(), e);
            throw new ExcelValidationException("Report generation failed: " + e.getMessage());
        }
    }

   @SuppressWarnings("unchecked")
public static Map<String, Object> excelParser(MultipartFile file) {

    validateFile(file);

    List<EmployeeUpload> employees = new ArrayList<>();
    DataFormatter formatter = new DataFormatter();       // ← only ONE formatter
    List<String> allErrors = new ArrayList<>();
    List<String> warnings = new ArrayList<>();

    try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
        Sheet sheet = workbook.getSheetAt(0);

        // ← Fix 3: typo fixed validateStruture → validateStructure
        Map<String, Object> structureResult = validateStruture(workbook, sheet);
        warnings = (List<String>) structureResult.get("warnings");
        Map<String, Integer> columnIndex =
                (Map<String, Integer>) structureResult.get("columnIndexMap");

        logger.info("Reading Excel File: {}", file.getOriginalFilename());

        // ← Fix 1: actually USE processRows result — old loop completely removed
        List<RowResult> results = processRows(sheet, columnIndex, formatter);

        for (RowResult result : results) {
            if (result.isBlank) continue;

            if (!result.errors.isEmpty()) {
                allErrors.addAll(result.errors);
                continue;
            }

            EmployeeUpload emp = new EmployeeUpload();
            emp.setName(result.name);
            emp.setEmail(result.email);
            emp.setPassword(result.password);
            emp.setAge(result.age);
            emp.setSalary(result.salary);
            employees.add(emp);

            logger.info("Row {} → name: {}, email: {}, age: {}, salary: {}",
                    result.rowNum, result.name, result.email, result.age, result.salary);
        }

        logger.info("Upload complete — valid rows: {}", employees.size());

    } catch (ExcelValidationException | RowValidationException e) {
        throw e;
    } catch (Exception e) {
        logger.error("Excel parsing failed: {}", e.getMessage(), e);
        throw new ExcelValidationException("Excel parsing failed: " + e.getMessage());
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

}
