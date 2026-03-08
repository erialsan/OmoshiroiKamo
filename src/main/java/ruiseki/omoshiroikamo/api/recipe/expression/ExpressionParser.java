package ruiseki.omoshiroikamo.api.recipe.expression;

import java.util.ArrayList;
import java.util.List;

import ruiseki.omoshiroikamo.api.condition.ComparisonCondition;
import ruiseki.omoshiroikamo.api.condition.ICondition;

/**
 * A simple recursive descent parser for expressions and conditions.
 * Supports arithmetic, comparison, variables (day, time, moon), and nbt('key')
 * function.
 */
public class ExpressionParser {

    private final String input;
    private int pos = -1, ch;

    public ExpressionParser(String input) {
        this.input = input;
    }

    private void nextChar() {
        ch = (++pos < input.length()) ? input.charAt(pos) : -1;
    }

    private boolean eat(int charToEat) {
        while (ch == ' ') nextChar();
        if (ch == charToEat) {
            nextChar();
            return true;
        }
        return false;
    }

    public Object parse() {
        nextChar();
        Object x = parseComparison();
        if (pos < input.length()) throw new RuntimeException("Unexpected: " + (char) ch);
        return x;
    }

    // comparison = expression ( ( "==" | "!=" | ">" | ">=" | "<" | "<=" )
    // expression )*
    private Object parseComparison() {
        Object x = parseExpression();
        while (true) {
            String op = "";
            if (eat('=')) {
                if (eat('=')) op = "==";
                else throw new RuntimeException("Expected '=='");
            } else if (eat('!')) {
                if (eat('=')) op = "!=";
                else throw new RuntimeException("Expected '!='");
            } else if (eat('>')) {
                if (eat('=')) op = ">=";
                else op = ">";
            } else if (eat('<')) {
                if (eat('=')) op = "<=";
                else op = "<";
            } else {
                return x;
            }

            Object y = parseExpression();
            if (!(x instanceof IExpression) || !(y instanceof IExpression)) {
                throw new RuntimeException("Comparison requires numeric expressions");
            }
            x = new ComparisonCondition((IExpression) x, (IExpression) y, op);
        }
    }

    // expression = term ( ( "+" | "-" ) term )*
    private IExpression parseExpression() {
        IExpression x = parseTerm();
        for (;;) {
            if (eat('+')) x = new ArithmeticExpression(x, parseTerm(), "+");
            else if (eat('-')) x = new ArithmeticExpression(x, parseTerm(), "-");
            else return x;
        }
    }

    // term = factor ( ( "*" | "/" | "%" ) factor )*
    private IExpression parseTerm() {
        IExpression x = parseFactor();
        for (;;) {
            if (eat('*')) x = new ArithmeticExpression(x, parseFactor(), "*");
            else if (eat('/')) x = new ArithmeticExpression(x, parseFactor(), "/");
            else if (eat('%')) x = new ArithmeticExpression(x, parseFactor(), "%");
            else return x;
        }
    }

    // factor = ( "+" | "-" ) factor | "(" comparison ")" | number | function |
    // variable
    private IExpression parseFactor() {
        if (eat('+')) return parseFactor(); // unary plus
        if (eat('-')) return new ArithmeticExpression(new ConstantExpression(0), parseFactor(), "-"); // unary minus

        IExpression x;
        int startPos = this.pos;
        if (eat('(')) { // parentheses
            Object res = parseComparison();
            if (res instanceof IExpression) {
                x = (IExpression) res;
            } else {
                // Wrapper might be needed if Condition is used inside Expression (not supported
                // yet)
                throw new RuntimeException("Conditions not supported inside expressions");
            }
            eat(')');
        } else if ((ch >= '0' && ch <= '9') || ch == '.') { // numbers
            while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
            x = new ConstantExpression(Double.parseDouble(input.substring(startPos, this.pos)));
        } else if (ch >= 'a' && ch <= 'z') { // variables or functions
            while (ch >= 'a' && ch <= 'z' || ch == '_') nextChar();
            String name = input.substring(startPos, this.pos);
            if (eat('(')) { // function
                List<String> args = new ArrayList<>();
                while (ch != ')' && ch != -1) {
                    if (eat('\'')) {
                        int s = pos;
                        while (ch != '\'' && ch != -1) nextChar();
                        args.add(input.substring(s, pos));
                        eat('\'');
                    } else {
                        nextChar();
                    }
                    eat(',');
                }
                eat(')');
                if (name.equals("nbt") && !args.isEmpty()) {
                    x = new NbtExpression(args.get(0), 0);
                } else {
                    throw new RuntimeException("Unknown function: " + name);
                }
            } else {
                // variable
                if (name.equals("day") || name.equals("total_days")
                    || name.equals("time")
                    || name.equals("moon_phase")
                    || name.equals("moon")) {
                    x = new WorldPropertyExpression(name.equals("moon") ? "moon_phase" : name);
                } else {
                    throw new RuntimeException("Unknown variable: " + name);
                }
            }
        } else {
            throw new RuntimeException("Unexpected: " + (char) ch);
        }

        return x;
    }

    public static IExpression parseExpression(String input) {
        Object res = new ExpressionParser(input).parse();
        if (res instanceof IExpression) return (IExpression) res;
        throw new RuntimeException("Input is not a numeric expression: " + input);
    }

    public static ICondition parseCondition(String input) {
        Object res = new ExpressionParser(input).parse();
        if (res instanceof ICondition) return (ICondition) res;
        throw new RuntimeException("Input is not a condition: " + input);
    }
}
