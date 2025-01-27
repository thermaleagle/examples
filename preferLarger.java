@Override
/**
 * Selects the preferred condition by comparing two conditions based on Semantic Versioning.
 * <p>
 * This method determines which condition represents a "larger" or more inclusive constraint.
 * It does this by:
 * <ul>
 *     <li>Parsing the version strings into {@code SemanticVersion} objects.</li>
 *     <li>Using {@code compareTo()} for accurate semantic version comparison.</li>
 *     <li>Handling the {@code >=} operator by artificially increasing its value 
 *         (e.g., appending "-zzzz" to ensure it's treated as a higher version).</li>
 *     <li>Comparing different operator combinations such as {@code >}, {@code >=}, {@code ==}, and {@code !=}.</li>
 * </ul>
 * </p>
 *
 * @param condition1 The first condition to compare.
 * @param condition2 The second condition to compare.
 * @return The condition that represents the "larger" constraint.
 */
public Condition preferLarger(final Condition condition1, final Condition condition2) {
    final String operator1 = extractOperator(condition1.criteria());
    final String operator2 = extractOperator(condition2.criteria());
    final String value1Str = extractValue(condition1.criteria());
    final String value2Str = extractValue(condition2.criteria());

    // Convert values to SemanticVersion objects
    SemanticVersion value1 = new SemanticVersion(value1Str);
    SemanticVersion value2 = new SemanticVersion(value2Str);

    /**
     * Handling ">=" Operator:
     * Since "greater than or equal to" (>=) allows the same version, 
     * we artificially increase its version by appending "-zzzz".
     * This ensures it behaves similarly to ">" in comparisons.
     */
    if (operator1.equals(">=")) {
        value1 = new SemanticVersion(value1Str + "-zzzz"); 
    }
    if (operator2.equals(">=")) {
        value2 = new SemanticVersion(value2Str + "-zzzz");
    }

    boolean secondIsLarger = false;

    /**
     * Handling different operator combinations:
     * - ">" vs ">" → Compare versions directly
     * - ">" vs ">=" → Treat ">=" as slightly larger
     * - ">=" vs ">" → Similar handling as above
     * - ">=" vs ">=" → Compare versions normally
     * - "==" vs ">"  → ">" wins if its version is higher
     * - "==" vs "==" → Compare equality
     * - "!=" vs "!=" → Check if versions are different
     */
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

    /**
     * Return the condition with the "larger" constraint.
     * If second condition is larger, return condition2, otherwise return condition1.
     */
    return secondIsLarger ? condition2 : condition1;
}
