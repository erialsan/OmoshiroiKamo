package ruiseki.omoshiroikamo.api.recipe.expression;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Represents a pattern for matching and modifying NBT compound tags in a list.
 * Supports operation auto-inference based on value format:
 * - Comparison operators (>=, <=, >, <, ==, !=): REQUIRE
 * - Zero value (0): REMOVE
 * - Relative operators (+, -): MODIFY
 * - Absolute value: SET
 */
public class NBTPattern {

    private final Map<String, ValuePattern> patterns;
    private final OperationType primaryOperation;

    public enum OperationType {
        REQUIRE, // Condition check - item must match
        REMOVE, // Delete matching items
        MODIFY, // Relative modification
        SET // Absolute value setting
    }

    public NBTPattern(Map<String, ValuePattern> patterns, OperationType primaryOperation) {
        this.patterns = patterns;
        this.primaryOperation = primaryOperation;
    }

    /**
     * Check if this pattern matches the given NBT compound.
     *
     * @param item The NBT compound to check
     * @return True if all pattern conditions are met
     */
    public boolean matches(NBTTagCompound item) {
        if (item == null) return false;

        for (Map.Entry<String, ValuePattern> entry : patterns.entrySet()) {
            String key = entry.getKey();
            ValuePattern pattern = entry.getValue();

            if (!item.hasKey(key)) {
                // Missing key - only matches if pattern allows it
                if (pattern.operation != OperationType.REMOVE) {
                    return false;
                }
            } else {
                if (!pattern.matches(item, key)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Apply this pattern's modifications to the given NBT compound.
     * Only applies if the pattern matches.
     *
     * @param item The NBT compound to modify
     * @return True if the pattern was applied
     */
    public boolean apply(NBTTagCompound item) {
        if (item == null || !matches(item)) return false;

        for (Map.Entry<String, ValuePattern> entry : patterns.entrySet()) {
            String key = entry.getKey();
            ValuePattern pattern = entry.getValue();
            pattern.apply(item, key);
        }

        return true;
    }

    /**
     * Get the primary operation type for this pattern.
     */
    public OperationType getOperationType() {
        return primaryOperation;
    }

    /**
     * Check if this is a removal pattern.
     */
    public boolean isRemove() {
        return primaryOperation == OperationType.REMOVE;
    }

    /**
     * Parse a pattern from JSON.
     *
     * @param json The JSON object defining the pattern
     * @return A new NBTPattern
     */
    public static NBTPattern fromJson(JsonObject json) {
        Map<String, ValuePattern> patterns = new HashMap<>();
        OperationType primaryOp = OperationType.SET; // Default

        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();

            ValuePattern pattern = ValuePattern.parse(value);
            patterns.put(key, pattern);

            // Determine primary operation from patterns
            if (pattern.operation == OperationType.REQUIRE) {
                primaryOp = OperationType.REQUIRE;
            } else if (pattern.operation == OperationType.REMOVE && primaryOp != OperationType.REQUIRE) {
                primaryOp = OperationType.REMOVE;
            } else if (pattern.operation == OperationType.MODIFY && primaryOp == OperationType.SET) {
                primaryOp = OperationType.MODIFY;
            }
        }

        return new NBTPattern(patterns, primaryOp);
    }

    /**
     * Represents a value pattern for a single NBT key.
     */
    public static class ValuePattern {

        final OperationType operation;
        final String operator; // For REQUIRE: >=, <=, >, <, ==, !=
        final Object value;

        public ValuePattern(OperationType operation, String operator, Object value) {
            this.operation = operation;
            this.operator = operator;
            this.value = value;
        }

        /**
         * Check if the value in the NBT matches this pattern.
         */
        public boolean matches(NBTTagCompound nbt, String key) {
            if (operation == OperationType.REQUIRE) {
                return checkCondition(nbt, key);
            }
            // For non-require operations, any value matches
            return true;
        }

        /**
         * Apply this pattern to the NBT.
         */
        public void apply(NBTTagCompound nbt, String key) {
            switch (operation) {
                case REMOVE:
                    nbt.removeTag(key);
                    break;
                case MODIFY:
                    modifyValue(nbt, key);
                    break;
                case SET:
                    setValue(nbt, key);
                    break;
                case REQUIRE:
                    // No modification for require
                    break;
            }
        }

        private boolean checkCondition(NBTTagCompound nbt, String key) {
            if (!nbt.hasKey(key)) return false;

            double current = nbt.getDouble(key);
            double target = toDouble(value);

            switch (operator) {
                case ">=":
                    return current >= target;
                case "<=":
                    return current <= target;
                case ">":
                    return current > target;
                case "<":
                    return current < target;
                case "==":
                    return current == target;
                case "!=":
                    return current != target;
                default:
                    return false;
            }
        }

        private void modifyValue(NBTTagCompound nbt, String key) {
            double current = nbt.hasKey(key) ? nbt.getDouble(key) : 0.0;
            double modifier = toDouble(value);

            String op = operator != null ? operator : "+";
            double result;
            if (op.equals("-")) {
                result = current - modifier;
            } else {
                result = current + modifier;
            }

            NBTBase nbtValue = NBTTypeInference.parseNumeric(result);
            nbt.setTag(key, nbtValue);
        }

        private void setValue(NBTTagCompound nbt, String key) {
            NBTBase nbtValue;
            if (value instanceof String) {
                nbtValue = NBTTypeInference.parseValue((String) value);
            } else {
                nbtValue = NBTTypeInference.parseNumeric(toDouble(value));
            }
            nbt.setTag(key, nbtValue);
        }

        private double toDouble(Object obj) {
            if (obj instanceof Number) {
                return ((Number) obj).doubleValue();
            } else if (obj instanceof String) {
                try {
                    String str = (String) obj;
                    // Remove operator prefix if present
                    if (str.startsWith("+") || str.startsWith("-")) {
                        return Double.parseDouble(str.substring(1));
                    }
                    return Double.parseDouble(str);
                } catch (NumberFormatException e) {
                    return 0.0;
                }
            }
            return 0.0;
        }

        /**
         * Parse a value pattern from JSON element.
         */
        public static ValuePattern parse(JsonElement element) {
            if (element.isJsonPrimitive()) {
                if (element.getAsJsonPrimitive()
                    .isNumber()) {
                    double num = element.getAsDouble();
                    if (num == 0) {
                        return new ValuePattern(OperationType.REMOVE, null, num);
                    } else {
                        return new ValuePattern(OperationType.SET, null, num);
                    }
                } else if (element.getAsJsonPrimitive()
                    .isString()) {
                        String str = element.getAsString();
                        return parseString(str);
                    }
            }

            // Default to SET with the value
            return new ValuePattern(OperationType.SET, null, element.toString());
        }

        private static ValuePattern parseString(String str) {
            // Check for comparison operators
            if (str.startsWith(">=")) {
                return new ValuePattern(OperationType.REQUIRE, ">=", str.substring(2));
            } else if (str.startsWith("<=")) {
                return new ValuePattern(OperationType.REQUIRE, "<=", str.substring(2));
            } else if (str.startsWith("==")) {
                return new ValuePattern(OperationType.REQUIRE, "==", str.substring(2));
            } else if (str.startsWith("!=")) {
                return new ValuePattern(OperationType.REQUIRE, "!=", str.substring(2));
            } else if (str.startsWith(">")) {
                return new ValuePattern(OperationType.REQUIRE, ">", str.substring(1));
            } else if (str.startsWith("<")) {
                return new ValuePattern(OperationType.REQUIRE, "<", str.substring(1));
            }

            // Check for relative operators
            if (str.startsWith("+")) {
                return new ValuePattern(OperationType.MODIFY, "+", str.substring(1));
            } else if (str.startsWith("-")) {
                return new ValuePattern(OperationType.MODIFY, "-", str.substring(1));
            }

            // Absolute value
            return new ValuePattern(OperationType.SET, null, str);
        }
    }
}
