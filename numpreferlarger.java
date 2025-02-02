/**
 * This method selects the condition that allows a larger value to pass.
 * It prefers less restrictive conditions (e.g., ">" over ">=") and, for identical conditions,
 * selects the one with the larger value.
 *
 * @param condition1 The first condition to compare
 * @param condition2 The second condition to compare
 * @return The preferred condition that allows a larger value to pass
 */
public Condition preferConditionForLargerValue(final Condition condition1, final Condition condition2) {
    final String operator1 = condition1.extractOperatorFromCriteria();
    final String operator2 = condition2.extractOperatorFromCriteria();

    final double value1 = parseAdjustedValue(condition1.extractValueFromCriteria(), operator1);
    final double value2 = parseAdjustedValue(condition2.extractValueFromCriteria(), operator2);

    boolean secondIsPreferred = shouldPreferSecondCondition(value1, value2, operator1, operator2);
    return secondIsPreferred ? condition2 : condition1;
}

/**
 * Parses the numeric value from the condition and adjusts it if necessary.
 * The adjustment is made for "<=", where the value is increased by 1 to reflect 
 * looser constraint enforcement in comparisons.
 *
 * @param valueStr The string representation of the value
 * @param operator The operator associated with the value
 * @return The adjusted numeric value
 */
private double parseAdjustedValue(String valueStr, String operator) {
    double value = Double.parseDouble(valueStr);
    return "<=".equals(operator) ? value + 1 : value;  // Adjusting <= condition
}

/**
 * Determines whether the second condition should be preferred over the first.
 * It compares operators based on looseness and values for identical operators.
 *
 * @param value1    The numeric value of the first condition
 * @param value2    The numeric value of the second condition
 * @param operator1 The operator of the first condition
 * @param operator2 The operator of the second condition
 * @return True if condition2 should be preferred, otherwise false
 */
private boolean shouldPreferSecondCondition(double value1, double value2, String operator1, String operator2) {
    // Get operator precedence for comparison
    int precedence1 = getOperatorPrecedence(operator1);
    int precedence2 = getOperatorPrecedence(operator2);

    // If both conditions use the same operator, prefer the one with the larger value
    if (operator1.equals(operator2)) {
        return value1 > value2;
    }

    // Prefer looser conditions first (higher precedence number is looser)
    if (precedence1 > precedence2) {
        return true;
    } else if (precedence1 < precedence2) {
        return false;
    } else {
        // Special handling for <= to prefer smaller values
        return operator1.equals("<=") ? value1 < value2 : value1 > value2;
    }
}

/**
 * Returns the precedence of an operator, where higher numbers indicate looser conditions.
 *
 * Operator precedence order:
 * - ">="  (Most permissive)
 * - "=="
 * - ">"
 * - "<="  (Most restrictive)
 *
 * @param operator The operator whose precedence is to be determined
 * @return The precedence of the operator
 * @throws IllegalArgumentException if an unknown operator is encountered
 */
private int getOperatorPrecedence(String operator) {
    switch (operator) {
        case ">=":
            return 4;
        case "==":
            return 3;
        case ">":
            return 2;
        case "<=":
            return 1;
        default:
            throw new IllegalArgumentException("Unknown operator: " + operator);
    }
}



.....

/**
 * Selects the condition that allows a larger value to pass.
 * Prefers looser conditions (e.g., ">" over ">=") and, for identical conditions,
 * selects the one with the larger value.
 *
 * @param condition1 The first condition to compare
 * @param condition2 The second condition to compare
 * @return The preferred condition that allows a larger value to pass
 */
public Condition preferConditionForLargerValue(final Condition condition1, final Condition condition2) {
    final String operator1 = condition1.extractOperatorFromCriteria();
    final String operator2 = condition2.extractOperatorFromCriteria();

    final double value1 = parseAdjustedValue(condition1.extractValueFromCriteria(), operator1);
    final double value2 = parseAdjustedValue(condition2.extractValueFromCriteria(), operator2);

    return shouldPreferSecondCondition(value1, value2, operator1, operator2) ? condition2 : condition1;
}

/**
 * Parses and adjusts values for conditions.
 * Adjusts "<=" values by increasing by 1, since "<=" allows larger values.
 *
 * @param valueStr The value as a string
 * @param operator The operator associated with the value
 * @return The adjusted value
 */
private double parseAdjustedValue(String valueStr, String operator) {
    double value = Double.parseDouble(valueStr);
    return "<=".equals(operator) ? value + 1 : value;  // Adjust "<=" logic
}

/**
 * Determines whether the second condition should be preferred.
 * Looser conditions are preferred, and for identical operators, larger values are chosen.
 *
 * @param value1    Value of the first condition
 * @param value2    Value of the second condition
 * @param operator1 Operator of the first condition
 * @param operator2 Operator of the second condition
 * @return True if condition2 should be preferred, otherwise false
 */
private boolean shouldPreferSecondCondition(double value1, double value2, String operator1, String operator2) {
    int precedence1 = getOperatorPrecedence(operator1);
    int precedence2 = getOperatorPrecedence(operator2);

    // If both conditions use the same operator, prefer the one with the larger value
    if (operator1.equals(operator2)) return value2 > value1;

    // Explicit prioritization rules
    if (operator1.equals(">") && operator2.equals(">=")) return false;
    if (operator1.equals(">=") && operator2.equals(">")) return true;
    if (operator1.equals(">") && operator2.equals("==")) return true;
    if (operator1.equals("==") && operator2.equals(">=")) return true;
    if (operator1.equals("<=") && operator2.equals(">")) return true;

    // Default: Use precedence ranking
    return precedence1 < precedence2;
}

/**
 * Defines operator precedence: Higher numbers indicate looser conditions.
 *
 * @param operator The operator to evaluate
 * @return The precedence of the operator
 */
private int getOperatorPrecedence(String operator) {
    return switch (operator) {
        case ">=" -> 4;
        case "==" -> 3;
        case ">"  -> 2;
        case "<=" -> 1;
        default -> throw new IllegalArgumentException("Unknown operator: " + operator);
    };
}