package com.apex.payroll.service.froms;

import com.apex.payroll.dto.forms.CreateFormDefinitionRequest;
import com.apex.payroll.exception.BadRequestException;
import com.apex.payroll.util.JsonHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;

@Service
@Transactional
public class FormValidationService {

    public void validateFormDefinition(Object formDefinition) {
        List<String> errors = new ArrayList<>();

        if (formDefinition instanceof CreateFormDefinitionRequest req) {
            if (req.getFormName() == null || req.getFormName().isBlank()) {
                errors.add("'formName' is required");
            }
            if (req.getComponents() == null || req.getComponents().isEmpty()) {
                errors.add("'components' must be a non-empty array");
            } else {
                validateComponentsRecursive(req.getComponents(), errors, null);
            }
        } else if (formDefinition instanceof Map) {
            Object comps = ((Map<?, ?>) formDefinition).get("components");
            if (!(comps instanceof List<?> compsList) || compsList.isEmpty()) {
                errors.add("'components' must be a non-empty array");
            } else {
                //noinspection unchecked
                validateComponentsRecursive((List<Map<String, Object>>) (List<?>) compsList, errors, null);
            }
        } else {
            errors.add("Unsupported form definition format");
        }

        if (!errors.isEmpty()) {
            throw new BadRequestException("Form definition invalid: " + String.join("; ", errors));
        }
    }

    public void validateFormSubmission(String formDefinitionJson, Map<String, Object> formData) {
        Map<String, Object> schema = JsonHelper.fromJson(formDefinitionJson, Map.class);
        List<String> errors = new ArrayList<>();

        Object comps = schema.get("components");
        if (comps instanceof List<?> compList) {
            //noinspection unchecked
            validateSubmissionRecursive((List<Map<String, Object>>) (List<?>) compList, formData, errors, null);
        }

        if (!errors.isEmpty()) {
            throw new BadRequestException("Form submission invalid: " + String.join("; ", errors));
        }
    }

    private void validateComponentsRecursive(
            List<Map<String, Object>> components,
            List<String> errors,
            String parentKey) {
        int index = 0;
        for (Map<String, Object> comp : components) {
            index++;
            String label = asString(comp.get("label"));
            String key = asString(comp.get("key"));
            String type = asString(comp.get("type"));

            if (isBlank(label)) errors.add(ctx(parentKey, key, index) + "'label' is required");
            if (isBlank(key)) errors.add(ctx(parentKey, key, index) + "'key' is required");
            if (isBlank(type)) errors.add(ctx(parentKey, key, index) + "'type' is required");

            Object nested = comp.get("components");
            if (nested instanceof List<?> nestedList) {
                //noinspection unchecked
                validateComponentsRecursive((List<Map<String, Object>>) (List<?>) nestedList,
                        errors,
                        key != null ? key : parentKey);
            }
        }
    }

    private void validateSubmissionRecursive(List<Map<String, Object>> components,
                                             Map<String, Object> dataScope,
                                             List<String> errors,
                                             String parentKey) {
        for (Map<String, Object> comp : components) {
            String key = asString(comp.get("key"));
            String type = asString(comp.get("type"));
            Map<String, Object> validate = asMap(comp.get("validate"));
            boolean required = validate != null && Boolean.TRUE.equals(validate.get("required"));
            boolean validateWhenHidden = Boolean.TRUE.equals(comp.get("validateWhenHidden"));

            Object nested = comp.get("components");
            boolean hasChildren = nested instanceof List<?>;

            // Determine current value from data scope
            Object value = (key != null && dataScope != null) ? dataScope.get(key) : null;

            // Visibility check: skip validations if hidden and validateWhenHidden=false
            boolean visible = isVisible(comp, dataScope);
            if (!visible && !validateWhenHidden) {
                continue;
            }

            if (hasChildren) {
                // For containers, if there's data under the key, dive into it as the new scope
                Map<String, Object> nextScope = (value instanceof Map) ? (Map<String, Object>) value : dataScope;
                //noinspection unchecked
                validateSubmissionRecursive((List<Map<String, Object>>) nested, nextScope, errors, path(parentKey, key));
                // Also, if container itself is marked required, ensure the object is present and non-empty
                if (required && (value == null || isEmptyValue(value))) {
                    errors.add(fieldPath(parentKey, key) + " is required");
                }
                continue;
            }

            // Skip non-input elements like buttons
            String compType = type != null ? type.toLowerCase() : "";
            if (compType.equals("button")) {
                continue;
            }

            // Required check
            if (required && (value == null || isEmptyValue(value))) {
                errors.add(fieldPath(parentKey, key) + " is required");
                continue; // no need to type-check null
            }

            // Type check (only if a value is present)
            if (value != null) {
                if (!isValueTypeValid(compType, value)) {
                    errors.add(fieldPath(parentKey, key) + " has invalid type for '" + compType + "'");
                } else {
                    // String rules: minLength, maxLength, pattern
                    if (validate != null && value instanceof String) {
                        applyStringRules(fieldPath(parentKey, key), compType, (String) value, validate, errors);
                    }

                    // Specialized validations (email, datetime, time, day, file)
                    applySpecializedValidations(compType, value, comp, fieldPath(parentKey, key), errors);

                    // Option membership for select, radio, selectboxes
                    validateOptions(compType, value, comp, fieldPath(parentKey, key), errors);
                }
            }
        }
    }

    private static boolean isValueTypeValid(String type, Object value) {
        return switch (type) {
            case "textfield", "text", "email", "password", "select", "radio", "time", "datetime", "signature" ->
                    value instanceof String;
            case "day" -> (value instanceof String) || (value instanceof Map);
            case "currency", "number" -> value instanceof Number;
            case "selectboxes" -> value instanceof Map;
            case "tags" -> (value instanceof List) || (value instanceof String);
            case "file" -> (value instanceof Map) || (value instanceof List) || (value instanceof String);
            case "address" -> value instanceof Map; // container value expected as object
            default -> true;
        };
    }

    private static boolean isEmptyValue(Object v) {
        return switch (v) {
            case null -> true;
            case String s -> s.trim().isEmpty();
            case Collection<?> c -> c.isEmpty();
            case Map<?, ?> m -> m.isEmpty();
            default -> false;
        };
    }

    private static String asString(Object o) {
        return (o == null) ? null : Objects.toString(o, null);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o) {
        return (o instanceof Map) ? (Map<String, Object>) o : null;
    }

    private static String path(String parent, String key) {
        return (parent == null || parent.isBlank()) ? key : parent + "." + key;
    }

    private static String fieldPath(String parent, String key) {
        return "field '" + path(parent, key) + "'";
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String ctx(String parentKey, String key, int index) {
        String at = (key != null && !key.isBlank()) ? ("field '" + key + "'") : ("component#" + index);
        return (parentKey == null || parentKey.isBlank()) ? (at + ": ") : ("[" + parentKey + "] " + at + ": ");
    }

    private static boolean isVisible(Map<String, Object> comp, Map<String, Object> data) {
        Map<String, Object> conditional = asMap(comp.get("conditional"));
        if (conditional == null) return true;
        String when = asString(conditional.get("when"));
        Object eq = conditional.get("eq");
        Boolean show = (conditional.get("show") instanceof Boolean b) ? b : Boolean.TRUE;
        if (when == null || when.isBlank()) return true;
        Object actual = (data != null) ? data.get(when) : null;
        boolean conditionMet = Objects.equals(actual, eq);
        return show ? conditionMet : !conditionMet;
    }

    private static void applyStringRules(String path, String compType, String s,
                                         Map<String, Object> validate, List<String> errors) {
        if (validate == null) return;
        Integer min = (validate.get("minLength") instanceof Number n) ? n.intValue() : null;
        Integer max = (validate.get("maxLength") instanceof Number n) ? n.intValue() : null;
        // For password, ignore any frontend-provided regex pattern; enforce our own policy separately
        String pattern = "password".equalsIgnoreCase(compType) ? null : asString(validate.get("pattern"));
        String customMsg = asString(validate.get("customMessage"));

        if (min != null && s.length() < min) {
            errors.add(path + " must be at least " + min + " characters" + (customMsg != null ? " (" + customMsg + ")" : ""));
        }
        if (max != null && s.length() > max) {
            errors.add(path + " must be at most " + max + " characters" + (customMsg != null ? " (" + customMsg + ")" : ""));
        }
        if (pattern != null && !pattern.isBlank()) {
            try {
                if (!Pattern.compile(pattern).matcher(s).matches()) {
                    errors.add(path + " does not match required pattern" + (customMsg != null ? " (" + customMsg + ")" : ""));
                }
            } catch (Exception e) {
                errors.add("Invalid regex pattern for " + path);
            }
        }
    }

    private static void applySpecializedValidations(String compType, Object value,
                                                    Map<String, Object> comp,
                                                    String path,
                                                    List<String> errors) {
        switch (compType) {
            case "email" -> {
                if (value instanceof String s && !isEmail(s)) {
                    errors.add(path + " must be a valid email address");
                }
            }
            case "datetime" -> {
                if (value instanceof String s && !isIsoOffsetDateTime(s)) {
                    errors.add(path + " must be a valid ISO datetime (e.g., 2025-11-12T15:30:00+05:00)");
                }
            }
            case "time" -> {
                if (value instanceof String s && !isTime(s)) {
                    errors.add(path + " must be a valid time (HH:mm[:ss])");
                }
            }
            case "day" -> {
                Integer minYear = getInt(asMap(comp.get("fields")) != null ? asMap(comp.get("fields")).get("year") : null, "minYear");
                Integer maxYear = getInt(asMap(comp.get("fields")) != null ? asMap(comp.get("fields")).get("year") : null, "maxYear");
                if (value instanceof String s) {
                    if (!isDayString(s, minYear, maxYear)) {
                        errors.add(path + " must be a valid date (dd/MM/yyyy)" + yearRangeMsg(minYear, maxYear));
                    }
                } else if (value instanceof Map<?, ?> m) {
                    if (!isDayObject(m, minYear, maxYear)) {
                        errors.add(path + " must contain day/month/year within range" + yearRangeMsg(minYear, maxYear));
                    }
                }
            }
            case "file" -> validateFiles(value, comp, path, errors);
            case "password" -> {
                if (value instanceof String s && !meetsPasswordPolicy(s)) {
                    errors.add(path + " must include upper, lower, number, special");
                }
            }
            default -> {}
        }
    }

    private static boolean meetsPasswordPolicy(String s) {
        // Require at least one lowercase, one uppercase, one digit, and one non-alphanumeric
        String policy = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$";
        return Pattern.compile(policy).matcher(s).matches();
    }

    private static void validateOptions(String compType, Object value, Map<String, Object> comp,
                                        String path, List<String> errors) {
        Set<Object> allowed = extractAllowedValues(compType, comp);
        if (allowed == null) return;
        switch (compType) {
            case "select", "radio" -> {
                if (value != null && !allowed.contains(value)) {
                    errors.add(path + " must be one of: " + allowed);
                }
            }
            case "selectboxes" -> {
                if (value instanceof Map<?, ?> m) {
                    for (Map.Entry<?, ?> e : m.entrySet()) {
                        Object k = e.getKey();
                        Object v = e.getValue();
                        if (!allowed.contains(k)) {
                            errors.add(path + " contains unsupported option '" + k + "'");
                        }
                        if (!(v instanceof Boolean)) {
                            errors.add(path + " option '" + k + "' must be boolean");
                        }
                    }
                }
            }
            default -> {}
        }
    }

    private static Set<Object> extractAllowedValues(String compType, Map<String, Object> comp) {
        List<Map<String, Object>> vals = null;
        if (Objects.equals(compType, "select")) {
            Map<String, Object> data = asMap(comp.get("data"));
            if (data != null && data.get("values") instanceof List<?> l) {
                //noinspection unchecked
                vals = (List<Map<String, Object>>) (List<?>) l;
            }
        } else if (Objects.equals(compType, "radio") || Objects.equals(compType, "selectboxes")) {
            if (comp.get("values") instanceof List<?> l) {
                //noinspection unchecked
                vals = (List<Map<String, Object>>) (List<?>) l;
            }
        }
        if (vals == null) return null;
        Set<Object> allowed = new HashSet<>();
        for (Map<String, Object> v : vals) {
            if (v == null) continue;
            Object val = v.get("value");
            if (val != null) allowed.add(val);
        }
        return allowed;
    }

    @SuppressWarnings("unchecked")
    private static void validateFiles(Object value, Map<String, Object> comp, String path, List<String> errors) {
        List<Map<String, Object>> files = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object o : list) if (o instanceof Map<?, ?> m) files.add((Map<String, Object>) m);
        } else if (value instanceof Map<?, ?> m) {
            files.add((Map<String, Object>) m);
        } else if (value instanceof String) {
            return; // allow raw string without further checks
        }

        Set<String> allowedExt = new HashSet<>();
        if (comp.get("fileTypes") instanceof List<?> l) {
            for (Object o : l) if (o instanceof Map<?, ?> fm) {
                String ext = asString(((Map<?, ?>) fm).get("value"));
                if (ext != null) allowedExt.add(ext.toLowerCase());
            }
        }

        Long minSize = parseSize(asString(comp.get("fileMinSize")));

        for (Map<String, Object> f : files) {
            String type = asString(f.get("type"));
            String url = asString(f.get("url"));
            Long size = (f.get("size") instanceof Number n) ? n.longValue() : null;

            if (!allowedExt.isEmpty()) {
                boolean ok = false;
                if (type != null) ok = allowedExt.stream().anyMatch(ext -> type.toLowerCase().contains(ext));
                if (!ok && url != null) ok = allowedExt.stream().anyMatch(ext -> url.toLowerCase().endsWith("." + ext));
                if (!ok) errors.add(path + " has file with unsupported type");
            }

            if (minSize != null && size != null && size < minSize) {
                errors.add(path + " has file smaller than minimum size");
            }
        }
    }

    private static Long parseSize(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            String t = s.trim().toUpperCase(Locale.ROOT);
            long mult = 1L;
            if (t.endsWith("KB")) { mult = 1024L; t = t.substring(0, t.length()-2).trim(); }
            else if (t.endsWith("MB")) { mult = 1024L*1024L; t = t.substring(0, t.length()-2).trim(); }
            else if (t.endsWith("B")) { mult = 1L; t = t.substring(0, t.length()-1).trim(); }
            long base = Long.parseLong(t);
            return base * mult;
        } catch (Exception e) { return null; }
    }

    private static boolean isEmail(String s) {
        String regex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return Pattern.compile(regex).matcher(s).matches();
    }

    private static boolean isIsoOffsetDateTime(String s) {
        try { OffsetDateTime.parse(s); return true; } catch (DateTimeParseException e) { return false; }
    }

    private static boolean isTime(String s) {
        try {
            if (s.length() == 5) {
                LocalTime.parse(s, DateTimeFormatter.ofPattern("HH:mm"));
            } else {
                LocalTime.parse(s, DateTimeFormatter.ofPattern("HH:mm[:ss]"));
            }
            return true;
        } catch (DateTimeParseException e) { return false; }
    }

    private static boolean isDayString(String s, Integer minYear, Integer maxYear) {
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            var ld = java.time.LocalDate.parse(s, fmt);
            return withinYear(ld.getYear(), minYear, maxYear);
        } catch (Exception e) { return false; }
    }

    private static boolean isDayObject(Map<?, ?> m, Integer minYear, Integer maxYear) {
        try {
            Integer d = toInt(m.get("day"));
            Integer mo = toInt(m.get("month"));
            Integer y = toInt(m.get("year"));
            if (d == null || mo == null || y == null) return false;
            if (!withinYear(y, minYear, maxYear)) return false;
            java.time.LocalDate.of(y, mo, d);
            return true;
        } catch (Exception e) { return false; }
    }

    private static Integer toInt(Object o) { return (o instanceof Number n) ? n.intValue() : null; }
    private static Integer getInt(Object obj, String key) {
        if (!(obj instanceof Map<?, ?> m)) return null;
        Object v = m.get(key);
        return (v instanceof Number n) ? n.intValue() : null;
    }
    private static boolean withinYear(int y, Integer minY, Integer maxY) {
        if (minY != null && y < minY) return false;
        if (maxY != null && y > maxY) return false;
        return true;
    }
    private static String yearRangeMsg(Integer minY, Integer maxY) {
        if (minY == null && maxY == null) return "";
        if (minY != null && maxY != null) return " (year between " + minY + " and " + maxY + ")";
        if (minY != null) return " (year >= " + minY + ")";
        return " (year <= " + maxY + ")";
    }
}
