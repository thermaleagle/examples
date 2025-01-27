@Override
/**
 * Selects the preferred condition by choosing the smaller or more restrictive version constraint.
 * <p>
 * This method is the inverse of {@code preferLarger}. It ensures:
 * <ul>
 *     <li>Proper comparison of Semantic Versioning values.</li>
 *     <li>Pre-release versions (e.g., {@code beta}) are considered smaller than stable versions.</li>
 *     <li>Handling of the {@code <=} operator by converting stable versions into an equivalent pre-release format
 *         (e.g., appending {@code -0} if the version has no pre-release).</li>
 *     <li>Comparison of different operators such as {@code <}, {@code <=}, {@code ==}, and {@code !=}.</li>
 * </ul>
 * </p>
 *
 * @param condition1 The first condition to compare.
 * @param condition2 The second condition to compare.
 * @return The condition that represents the smaller or more restrictive constraint.
 */
public Condition preferSmaller(final Condition condition1, final Condition condition2) {
    final String operator1 = extractOperator(condition1.criteria());
    final String operator2 = extractOperator(condition2.criteria());
    final String value1Str = extractValue(condition1.criteria());
    final String value2Str = extractValue(condition2.criteria());

    // Convert values to SemanticVersion objects
    SemanticVersion value1 = new SemanticVersion(value1Str);
    SemanticVersion value2 = new SemanticVersion(value2Str);

    /**
     * Handling "<=" Operator:
     * Since "less than or equal to" (<=) allows the same version,
     * we ensure that if the version is stable (no pre-release), we convert it into a pre-release format.
     * This ensures "<=" behaves similarly to "<" in comparisons while keeping the logic intact.
     */
    if (operator1.equals("<=") && !value1Str.contains("-")) {
        value1 = new SemanticVersion(value1Str + "-0"); // Adding "-0" ensures it remains smaller than stable versions
    }
    if (operator2.equals("<=") && !value2Str.contains("-")) {
        value2 = new SemanticVersion(value2Str + "-0");
    }

    boolean secondIsSmaller = false;

    /**
     * Handling different operator combinations:
     * - "<" vs "<" → Compare versions directly
     * - "<" vs "<=" → Treat "<=" as slightly smaller
     * - "<=" vs "<" → Similar handling as above
     * - "<=" vs "<=" → Compare versions normally
     * - "==" vs "<"  → "<" wins if its version is smaller
     * - "==" vs "==" → Compare equality
     * - "!=" vs "!=" → Check if versions are different
     */
    if (operator1.equals("<") && operator2.equals("<")) {
        secondIsSmaller = value1.compareTo(value2) > 0;
    } else if (operator1.equals("<") && operator2.equals("<=")) {
        secondIsSmaller = value1.compareTo(value2) > 0;
    } else if (operator1.equals("<=") && operator2.equals("<")) {
        secondIsSmaller = value1.compareTo(value2) > 0;
    } else if (operator1.equals("<=") && operator2.equals("<=")) {
        secondIsSmaller = value1.compareTo(value2) > 0;
    } else if (operator1.equals("==") && operator2.equals("<")) {
        secondIsSmaller = value1.compareTo(value2) > 0;
    } else if (operator1.equals("==") && operator2.equals("==")) {
        secondIsSmaller = value1.compareTo(value2) > 0;
    } else if (operator1.equals("!=") && operator2.equals("!=")) {
        secondIsSmaller = value1.compareTo(value2) != 0;
    } else {
        secondIsSmaller = false;
    }

    /**
     * Return the condition with the "smaller" constraint.
     * If the second condition is smaller, return condition2, otherwise return condition1.
     */
    return secondIsSmaller ? condition2 : condition1;
}
