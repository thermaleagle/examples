import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@code preferLarger} and {@code preferSmaller} methods
 * in {@code SemanticVersion} comparison.
 * <p>
 * These tests verify:
 * <ul>
 *     <li>Correct handling of Semantic Version comparisons.</li>
 *     <li>Proper selection of the "larger" constraint when using {@code preferLarger}.</li>
 *     <li>Proper selection of the "smaller" constraint when using {@code preferSmaller}.</li>
 *     <li>Handling of operators such as {@code >}, {@code >=}, {@code <}, {@code <=}, {@code ==}, and {@code !=}.</li>
 * </ul>
 * </p>
 */
public class SemanticVersionComparatorTest {

    /**
     * Tests the {@code preferLarger} method.
     * <p>
     * Verifies that {@code preferLarger} correctly returns the preferred condition.
     * This is determined by:
     * <ul>
     *     <li>Comparing versions numerically for {@code >} and {@code >=} cases.</li>
     *     <li>Ensuring {@code >=} is slightly preferred over {@code >}.</li>
     *     <li>Checking equality for {@code ==} and difference for {@code !=}.</li>
     * </ul>
     * </p>
     *
     * @param op1 Operator for the first condition (e.g., {@code >}, {@code >=}, {@code ==}).
     * @param v1 Version string for the first condition.
     * @param op2 Operator for the second condition.
     * @param v2 Version string for the second condition.
     * @param expected Expected operator of the preferred condition.
     */
    @ParameterizedTest
    @CsvSource({
        // Greater Than Cases
        "> , 1.8.0, > , 1.8.1, >",
        "> , 1.8.1, > , 1.8.0, >",

        // Greater Than or Equal Cases
        ">=, 1.8.0, >=, 1.8.1, >=",
        ">=, 1.8.1, >=, 1.8.0, >=",

        // Mixed > and >= cases
        ">=, 1.8.0, > , 1.8.1, >",
        "> , 1.8.0, >=, 1.8.1, >=",

        // Stable vs Pre-release (Stable should be preferred)
        ">=, 1.8.0, > , 1.8.0-beta, >=",
        "> , 1.8.0-beta, > , 1.8.0, >",

        // Equality Cases
        "==, 2.0.0, ==, 2.0.0, ==",
        "==, 2.0.1, ==, 2.0.0, ==",

        // Not Equal Cases
        "!=, 2.0.0, !=, 2.0.1, !=",
        "!=, 2.0.1, !=, 2.0.0, !="
    })
    public void testPreferLarger(String op1, String v1, String op2, String v2, String expected) {
        Condition cond1 = new Condition(op1 + " " + v1);
        Condition cond2 = new Condition(op2 + " " + v2);
        Condition result = preferLarger(cond1, cond2);

        assertEquals(expected, extractOperator(result.criteria()), 
            "Expected the preferred condition to have operator " + expected);
    }

    /**
     * Tests the {@code preferSmaller} method.
     * <p>
     * Ensures that {@code preferSmaller} correctly returns the more restrictive condition.
     * This is determined by:
     * <ul>
     *     <li>Comparing versions numerically for {@code <} and {@code <=} cases.</li>
     *     <li>Ensuring {@code <=} is slightly preferred over {@code <}.</li>
     *     <li>Checking equality for {@code ==} and difference for {@code !=}.</li>
     * </ul>
     * </p>
     *
     * @param op1 Operator for the first condition (e.g., {@code <}, {@code <=}, {@code ==}).
     * @param v1 Version string for the first condition.
     * @param op2 Operator for the second condition.
     * @param v2 Version string for the second condition.
     * @param expected Expected operator of the preferred condition.
     */
    @ParameterizedTest
    @CsvSource({
        // Less Than Cases
        "< , 1.8.0, < , 1.8.1, <",
        "< , 1.8.1, < , 1.8.0, <",

        // Less Than or Equal Cases
        "<=, 1.8.0, <=, 1.8.1, <=",
        "<=, 1.8.1, <=, 1.8.0, <=",

        // Mixed < and <= cases
        "<=, 1.8.0, < , 1.8.1, <",
        "< , 1.8.0, <=, 1.8.1, <=",

        // Stable vs Pre-release (Pre-release should be preferred)
        "<=, 1.8.0-beta, < , 1.8.0, <=",
        "< , 1.8.0, < , 1.8.0-beta, <",

        // Equality Cases
        "==, 2.0.0, ==, 2.0.0, ==",
        "==, 2.0.1, ==, 2.0.0, ==",

        // Not Equal Cases
        "!=, 2.0.0, !=, 2.0.1, !=",
        "!=, 2.0.1, !=, 2.0.0, !="
    })
    public void testPreferSmaller(String op1, String v1, String op2, String v2, String expected) {
        Condition cond1 = new Condition(op1 + " " + v1);
        Condition cond2 = new Condition(op2 + " " + v2);
        Condition result = preferSmaller(cond1, cond2);

        assertEquals(expected, extractOperator(result.criteria()), 
            "Expected the preferred condition to have operator " + expected);
    }
}
