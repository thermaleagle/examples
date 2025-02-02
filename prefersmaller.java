@Override
public Condition preferConditionForSmallerValue(final Condition condition1, final Condition condition2) {
    final String operator1 = condition1.extractOperatorFromCriteria();
    final String operator2 = condition2.extractOperatorFromCriteria();

    final double value1 = parseAdjustedValue(condition1.extractValueFromCriteria(), operator1);
    final double value2 = parseAdjustedValue(condition2.extractValueFromCriteria(), operator2);

    boolean firstIsPreferred = shouldPreferFirstCondition(value1, value2, operator1, operator2);
    return firstIsPreferred ? condition1 : condition2;
}

private double parseAdjustedValue(String valueStr, String operator) {
    double value = Double.parseDouble(valueStr);
    return ">=".equals(operator) ? value - 1 : value;  // Adjusting for >= as per original logic
}

private boolean shouldPreferFirstCondition(double value1, double value2, String operator1, String operator2) {
    // Define strictness order: More restrictive conditions are preferred
    int precedence1 = getOperatorPrecedence(operator1);
    int precedence2 = getOperatorPrecedence(operator2);

    // If operators are the same, prefer the one with the smaller value
    if (operator1.equals(operator2)) {
        return value1 < value2;
    }

    // Explicit rule-based comparison
    // Prefer stricter conditions that allow fewer values to pass
    if (precedence1 < precedence2) {
        return true;
    } else if (precedence1 > precedence2) {
        return false;
    } else {
        // Special handling for >= to prefer larger values
        return operator1.equals(">=") ? value1 > value2 : value1 < value2;
    }
}

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