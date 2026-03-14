package ruiseki.omoshiroikamo.api.recipe.expression;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;

import com.google.gson.JsonObject;

import ruiseki.omoshiroikamo.api.condition.ConditionContext;

/**
 * Expression that assigns a value to an NBT key.
 * Supports assignment operators: =, +=, -=, *=, /=
 */
public class NBTAssignmentExpression implements INBTWriteExpression {

    private final String nbtKey;
    private final IExpression valueExpression;
    private final String operation;

    public NBTAssignmentExpression(String nbtKey, IExpression valueExpression, String operation) {
        this.nbtKey = nbtKey;
        this.valueExpression = valueExpression;
        this.operation = operation;
    }

    @Override
    public double evaluate(ConditionContext context) {
        // For numeric evaluation, return the value that would be written
        return valueExpression.evaluate(context);
    }

    @Override
    public void applyToNBT(NBTTagCompound nbt, ConditionContext context) {
        if (nbt == null) return;

        Object value = getValueToWrite(context);

        // Determine NBT type and write
        if (value instanceof String) {
            String stringValue = (String) value;
            NBTBase nbtValue = NBTTypeInference.parseValue(stringValue);
            nbt.setTag(nbtKey, nbtValue);
        } else if (value instanceof Double || value instanceof Float) {
            double numValue = ((Number) value).doubleValue();

            // For compound operations (+=, -=, *=, /=), get current value
            if (!operation.equals("=")) {
                double current = nbt.hasKey(nbtKey) ? nbt.getDouble(nbtKey) : 0.0;
                switch (operation) {
                    case "+=":
                        numValue = current + numValue;
                        break;
                    case "-=":
                        numValue = current - numValue;
                        break;
                    case "*=":
                        numValue = current * numValue;
                        break;
                    case "/=":
                        numValue = numValue != 0 ? current / numValue : current;
                        break;
                }
            }

            // Write as appropriate type
            NBTBase nbtValue = NBTTypeInference.parseNumeric(numValue);
            nbt.setTag(nbtKey, nbtValue);
        }
    }

    /**
     * Get the value to write, either as a number or string.
     */
    private Object getValueToWrite(ConditionContext context) {
        if (valueExpression instanceof StringLiteralExpression) {
            return ((StringLiteralExpression) valueExpression).getStringValue();
        } else {
            return valueExpression.evaluate(context);
        }
    }

    @Override
    public String getNBTKey() {
        return nbtKey;
    }

    @Override
    public String getOperation() {
        return operation;
    }

    @Override
    public String toString() {
        return "nbt('" + nbtKey + "') " + operation + " " + valueExpression;
    }

    public static NBTAssignmentExpression fromJson(JsonObject json) {
        String key = json.get("key")
            .getAsString();
        String op = json.get("operation")
            .getAsString();
        IExpression value = ExpressionsParser.parse(json.get("value"));
        return new NBTAssignmentExpression(key, value, op);
    }
}
