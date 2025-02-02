@Override
public Condition preferSmaller(final Condition condition1, final Condition condition2) {
    final String operator1 = condition1.extractOperatorFromCriteria();
    final String operator2 = condition2.extractOperatorFromCriteria();

    final double value1 = parseAdjustedValue(condition1.extractValueFromCriteria(), operator1);
    final double value2 = parseAdjustedValue(condition2.extractValueFromCriteria(), operator2);

    boolean firstIsLarger = compareConditions(value1, value2, operator1, operator2);
    return firstIsLarger ? condition1 : condition2;
}

private double parseAdjustedValue(String valueStr, String operator) {
    double value = Double.parseDouble(valueStr);
    return ">=".equals(operator) ? value - 1 : value;
}

private boolean compareConditions(double value1, double value2, String operator1, String operator2) {
    if ("<".equals(operator1)) {
        return "<".equals(operator2) ? value1 > value2 : value1 >= value2;
    } 
    if ("<=".equals(operator1)) {
        return "<".equals(operator2) ? value1 > value2 : value1 >= value2;
    }
    if ("==".equals(operator1) && "==".equals(operator2)) {
        return Double.compare(value1, value2) == 0;
    }
    return false;
}