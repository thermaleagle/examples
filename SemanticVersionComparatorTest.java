import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@code SemanticVersion} class.
 *
 * <p>Uses JUnit 5 with parameterized tests to verify version comparisons.</p>
 */
public class SemanticVersionComparatorTest {

    /**
     * Tests semantic version comparison.
     *
     * @param v1 the first version
     * @param v2 the second version
     * @param expected the expected comparison result (-1, 0, 1)
     */
    @ParameterizedTest
    @CsvSource({
        "1.0.0, 1.0.0, 0",
        "1.0.0, 1.0.1, -1",
        "1.0.1, 1.0.0, 1",
        "1.1.0, 1.0.0, 1",
        "1.0.0-alpha, 1.0.0, -1",
        "1.0.0-alpha, 1.0.0-beta, -1",
        "1.0.0-beta, 1.0.0-rc.1, -1",
        "1.0.0-rc.1, 1.0.0, -1",
        "2.0.0, 1.0.0, 1"
    })
    public void testSemanticVersionComparison(String v1, String v2, int expected) {
        SemanticVersion version1 = new SemanticVersion(v1);
        SemanticVersion version2 = new SemanticVersion(v2);
        assertEquals(expected, Integer.signum(version1.compareTo(version2)));
    }

    /**
     * Tests wildcard matching functionality.
     *
     * @param pattern the wildcard pattern
     * @param version the actual version
     * @param expected whether it should match (true/false)
     */
    @ParameterizedTest
    @CsvSource({
        "1.2.*, 1.2.3, true",
        "1.2.*, 1.3.0, false",
        "1.*, 1.5.6, true",
        "2.*, 1.9.9, false",
        "*, 2.3.4, true"
    })
    public void testWildcardMatching(String pattern, String version, boolean expected) {
        SemanticVersion wildcard = new SemanticVersion(pattern);
        SemanticVersion actualVersion = new SemanticVersion(version);
        assertEquals(expected, actualVersion.matchesWildcard(wildcard));
    }
}
