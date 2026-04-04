package com.example.demo.config;

import java.util.List;
import java.util.Map;

import java.util.regex.Pattern;

public class EmployeeFieldConfig {

    private EmployeeFieldConfig() {
        throw new IllegalStateException("Config class");
    }

    public enum FieldType {
        STRING, INTEGER, DOUBLE
    }

    public static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
            Pattern.CASE_INSENSITIVE);

    public record FieldDef(
            String employeeName,
            FieldType type,
            boolean required,
            Double minVal,
            Double maxVal,
            String regexPattern,
            String errorMessage) {

        public static FieldDef ofString(String employeeName, boolean required, String regex, String errorMessage) {
            return new FieldDef(employeeName, FieldType.STRING, required, null, null, regex, errorMessage);
        }

        public static FieldDef ofString(String employeeName, boolean required) {
            return new FieldDef(employeeName, FieldType.STRING, required, null, null, null, null);
        }

        public static FieldDef ofInteger(String employeeName, FieldType type, boolean required, double minValue,
                double maxValue, String errorMessage) {
            return new FieldDef(employeeName, type, required, minValue, maxValue, null, errorMessage);
        }
    }

    public static final List<FieldDef> FIELDS = List.of(
            FieldDef.ofString("name", true),
            FieldDef.ofString("email", true, "^[a-zA-Z0-9_+&*-]+(?:\\\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\\\.)+[a-zA-Z]{2,7}$",
                    "Invalid email format"),
            FieldDef.ofString("password", true, "^.{8,}$", "Password must be at least 8 characters long"),

            FieldDef.ofInteger("age", FieldType.INTEGER, true, 1, 120, "Age must be between 1 and 120"),
            FieldDef.ofInteger("salary", FieldType.DOUBLE, true, 0, Double.MAX_VALUE,
                    "Salary must be a positive number"));

    public static List<String> getFieldName() {
        return FIELDS.stream().map(FieldDef::employeeName).toList();
    }

    public static void validate(int displayRow, Map<String, String> stringValues, Map<String, Object> numericValues,
            List<String> errors) {

        for (FieldDef field : FIELDS) {

            if (field.type == FieldType.STRING) {
                validateStringField(displayRow, field, stringValues, errors);
            } else {
                validateNumericField(displayRow, field, numericValues, errors);
            }
        }
    }

    private static void validateStringField(int displayRow, FieldDef field,
            Map<String, String> stringValues, List<String> errors) {

        String value = stringValues.getOrDefault(field.employeeName(), "");

        if (field.required() && value.isBlank()) {
            errors.add("Row " + displayRow + ": " + field.employeeName() + " is required");
            return;
        }

        if (field.required() && value.isBlank()) {
            errors.add("Row " + displayRow + ": " + field.employeeName() + " is required");
            return;
        }

        if (!value.isBlank()) {
            if ("email".equalsIgnoreCase(field.employeeName())) {
                if (!EMAIL_PATTERN.matcher(value).matches()) {
                    errors.add("Row " + displayRow + ": " + field.errorMessage());
                }
            } else if (field.regexPattern() != null && !value.matches(field.regexPattern())) {
                errors.add("Row " + displayRow + ": " + field.errorMessage());
            }
        }
    }

    private static void validateNumericField(int displayRow, FieldDef field,
            Map<String, Object> numericValues, List<String> errors) {

        boolean isNumeric = Boolean.TRUE.equals(
                numericValues.getOrDefault(field.employeeName() + "_isNumeric", false));

        if (!isNumeric) {
            errors.add("Row " + displayRow + ": " + field.employeeName() + " is not a valid number");
            return;
        }

        double value = (double) numericValues.getOrDefault(field.employeeName(), 0.0);
        if (field.minVal() != null && value < field.minVal()) { 
            errors.add("Row " + displayRow + ": " + field.employeeName() + " must be at least " + field.minVal());
        }
        if (field.maxVal() != null && value > field.maxVal()) {
            errors.add("Row " + displayRow + ": " + field.employeeName() + " must be at most " + field.maxVal());
        }
    }
}
