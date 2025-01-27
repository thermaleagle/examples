@Override
public Condition preferLarger(final Condition condition1, final Condition condition2) {
    final String operator1 = extractOperator(condition1.criteria());
    final String operator2 = extractOperator(condition2.criteria());
    final String value1Str = extractValue(condition1.criteria());
    final String value2Str = extractValue(condition2.criteria());

    // Convert values to SemanticVersion objects
    SemanticVersion value1 = new SemanticVersion(value1Str);
    SemanticVersion value2 = new SemanticVersion(value2Str);

    // Adjusting versions for ">=" comparisons to treat it similarly to ">"
    if (operator1.equals(">=")) {
        value1 = new SemanticVersion(value1Str + "-zzzz"); // Fake suffix ensures it's always treated as a higher version
    }
    if (operator2.equals(">=")) {
        value2 = new SemanticVersion(value2Str + "-zzzz");
    }

    boolean secondIsLarger = false;

    if (operator1.equals(">") && operator2.equals(">")) {
        secondIsLarger = value1.compareTo(value2) < 0;
    } else if (operator1.equals(">") && operator2.equals(">=")) {
        secondIsLarger = value1.compareTo(value2) < 0;
    } else if (operator1.equals(">=") && operator2.equals(">")) {
        secondIsLarger = value1.compareTo(value2) < 0;
    } else if (operator1.equals(">=") && operator2.equals(">=")) {
        secondIsLarger = value1.compareTo(value2) < 0;
    } else if (operator1.equals("==") && operator2.equals(">")) {
        secondIsLarger = value1.compareTo(value2) < 0;
    } else if (operator1.equals("==") && operator2.equals("==")) {
        secondIsLarger = value1.compareTo(value2) < 0;
    } else if (operator1.equals("!=") && operator2.equals("!=")) {
        secondIsLarger = value1.compareTo(value2) != 0;
    } else {
        secondIsLarger = false;
    }

    return secondIsLarger ? condition2 : condition1;
}
