package com.example.demo.parser;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
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
                                    dp[i][j - 1] 
                            ));
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

        for(Cell cell : headerRow) {
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

    @SuppressWarnings("unchecked")
    public static Map<String, Object> excelParser(MultipartFile file) {

        validateFile(file);

        List<EmployeeUpload> employee = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();

        List<String> error = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Set<String> fileExistingEmail = new HashSet<>();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            Map<String, Object> structureResult = validateStruture(workbook, sheet);
            warnings = (List<String>) structureResult.get("warnings");

            Map<String, Integer> columnIndex = (Map<String, Integer>) structureResult.get("columnIndexMap");

            logger.info("Reading Excel File: {}", file.getOriginalFilename());

            int dataRowCount = 0;

            for (Row row : sheet) {

                if (row.getRowNum() == 0) {
                    logger.info("Skipping the header for now");
                    continue;
                }

                int displayNumber = row.getRowNum() + 1;

                String name = formatter.formatCellValue(row.getCell(columnIndex.get("name")));
                String email = formatter.formatCellValue(row.getCell(columnIndex.get("email")));
                String password = formatter.formatCellValue(row.getCell(columnIndex.get("password")));
                String ageVal = formatter.formatCellValue(row.getCell(columnIndex.get("age")));
                String salaryVal = formatter.formatCellValue(row.getCell(columnIndex.get("salary")));

                if (name.isBlank() && email.isBlank() && password.isBlank() && ageVal.isBlank() && salaryVal.isBlank()) {
                    logger.info("Skipping blank row {}", displayNumber);
                    continue;
                }

                dataRowCount++;
                if (dataRowCount > MAX_ROW_COUNT) {
                    throw new ExcelValidationException(
                            "File exceeds maximum allowed limit of 10,000 rows. "
                                    + "please split the file and upload in batches");
                }

                int age = 0;
                boolean ageIsNumeric = false;
                Cell ageCell = row.getCell(columnIndex.get("age"));
                if (ageCell != null && ageCell.getCellType() == CellType.NUMERIC) {
                    age = (int) ageCell.getNumericCellValue();
                    ageIsNumeric = true;
                }

                double salary = 0;
                boolean salaryIsDouble = false;
                Cell salCell = row.getCell(columnIndex.get("salary"));
                if (salCell != null && salCell.getCellType() == CellType.NUMERIC) {
                    salary = salCell.getNumericCellValue();
                    salaryIsDouble = true;
                }

                validateFileRow(displayNumber, name, email, password, age, ageIsNumeric, salary, salaryIsDouble, error);

                if (error.isEmpty()) {
                    String emailLower = email.toLowerCase();
                    if(fileExistingEmail.contains(emailLower)){
                        error.add("Row " + displayNumber + " Duplicate email in file: " + email);
                        logger.warn("Row {} has duplicate email in file: {}", displayNumber, email);
                        continue;
                    }
                    EmployeeUpload employeeUpload = new EmployeeUpload();
                    employeeUpload.setName(name);
                    employeeUpload.setEmail(email);
                    employeeUpload.setPassword(password);
                    employeeUpload.setAge(age);
                    employeeUpload.setSalary(salary);

                    employee.add(employeeUpload);

                }

                logger.info("Row {} → name: {}, email: {}, age: {}, salary: {}",
                        row.getRowNum(), name, email, age, salary);

            }

            logger.info("Upload complete total rows read: {}", employee.size());

        } catch (ExcelValidationException | RowValidationException e) {
            throw e;

        } catch (Exception e) {
            throw new ExcelValidationException("Excel parsing failed" + e.getMessage());
        }

        if (!error.isEmpty()) {
            logger.warn(" Validation error found {}", error.size());
            throw new RowValidationException(error);
        }
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("employees", employee);
        result.put("warnings", warnings);
        return result;
    }

}
