package com.apex.payroll.service.froms;

import com.apex.payroll.dto.forms.CreateFormDefinitionRequest;
import com.apex.payroll.exception.BadRequestException;
import com.apex.payroll.util.JsonHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

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

    private void validateComponentsRecursive(List<Map<String, Object>> components,
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

            Object nested = comp.get("components");
            boolean hasChildren = nested instanceof List<?>;

            // Determine current value from data scope
            Object value = (key != null && dataScope != null) ? dataScope.get(key) : null;

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
                }
            }
        }
    }

    private static boolean isValueTypeValid(String type, Object value) {
        return switch (type) {
            case "textfield", "text", "email", "password", "select", "radio", "time", "datetime", "day", "signature" ->
                    value instanceof String;
            case "currency", "number" -> value instanceof Number;
            case "selectboxes" -> value instanceof Map;
            case "tags" -> (value instanceof List) || (value instanceof String);
            case "file" -> (value instanceof Map) || (value instanceof List) || (value instanceof String);
            case "address" -> value instanceof Map; // container value expected as object
            default ->
                    true;
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
}


