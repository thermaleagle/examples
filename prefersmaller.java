/**
 * This method selects the condition that allows a smaller value to pass.
 * It prefers stricter conditions (e.g., "<" over "<=") and, for identical conditions,
 * selects the one with the smaller value.
 *
 * @param condition1 The first condition to compare
 * @param condition2 The second condition to compare
 * @return The preferred condition that allows a smaller value to pass
 */
@Override
public Condition preferConditionForSmallerValue(final Condition condition1, final Condition condition2) {
    final String operator1 = condition1.extractOperatorFromCriteria();
    final String operator2 = condition2.extractOperatorFromCriteria();

    final double value1 = parseAdjustedValue(condition1.extractValueFromCriteria(), operator1);
    final double value2 = parseAdjustedValue(condition2.extractValueFromCriteria(), operator2);

    boolean firstIsPreferred = shouldPreferFirstCondition(value1, value2, operator1, operator2);
    return firstIsPreferred ? condition1 : condition2;
}

/**
 * Parses the numeric value from the condition and adjusts it if necessary.
 * The adjustment is made for ">=," where the value is reduced by 1 to reflect 
 * stricter constraint enforcement in comparisons.
 *
 * @param valueStr The string representation of the value
 * @param operator The operator associated with the value
 * @return The adjusted numeric value
 */
private double parseAdjustedValue(String valueStr, String operator) {
    double value = Double.parseDouble(valueStr);
    return ">=".equals(operator) ? value - 1 : value;  // Adjusting >= condition
}

/**
 * Determines whether the first condition should be preferred over the second.
 * It compares operators based on restrictiveness and values for identical operators.
 *
 * @param value1    The numeric value of the first condition
 * @param value2    The numeric value of the second condition
 * @param operator1 The operator of the first condition
 * @param operator2 The operator of the second condition
 * @return True if condition1 should be preferred, otherwise false
 */
private boolean shouldPreferFirstCondition(double value1, double value2, String operator1, String operator2) {
    // Get operator precedence for comparison
    int precedence1 = getOperatorPrecedence(operator1);
    int precedence2 = getOperatorPrecedence(operator2);

    // If both conditions use the same operator, prefer the one with the smaller value
    if (operator1.equals(operator2)) {
        return value1 < value2;
    }

    // Prefer stricter conditions first (lower precedence number is stricter)
    if (precedence1 < precedence2) {
        return true;
    } else if (precedence1 > precedence2) {
        return false;
    } else {
        // Special handling for >= to prefer larger values
        return operator1.equals(">=") ? value1 > value2 : value1 < value2;
    }
}

/**
 * Returns the precedence of an operator, where lower numbers indicate stricter conditions.
 *
 * Operator precedence order:
 * - "<"  (Most restrictive)
 * - "<="
 * - "=="
 * - ">=" (Least restrictive, but prefers larger values)
 *
 * @param operator The operator whose precedence is to be determined
 * @return The precedence of the operator
 * @throws IllegalArgumentException if an unknown operator is encountered
 */
private int getOperatorPrecedence(String operator) {
    switch (operator) {
        case "<":
            return 1;
        case "<=":
            return 2;
        case "==":
            return 3;
        case ">=":
            return 4;
        default:
            throw new IllegalArgumentException("Unknown operator: " + operator);
    }
}