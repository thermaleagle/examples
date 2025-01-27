import java.util.*;
import java.util.regex.*;

/**
 * A class for parsing and comparing Semantic Versions based on the SemVer 2.0 specification.
 * <p>
 * Semantic versioning follows the format: {@code MAJOR.MINOR.PATCH-PRERELEASE+BUILD}.
 * </p>
 * <p>Comparison rules:
 * <ul>
 *     <li>MAJOR, MINOR, and PATCH numbers are compared numerically.</li>
 *     <li>Pre-release versions (e.g., "alpha", "beta") are considered lower than stable versions.</li>
 *     <li>Build metadata (e.g., "+build.1") is ignored when comparing versions.</li>
 * </ul>
 * </p>
 */
public class SemanticVersion implements Comparable<SemanticVersion> {
    private final Integer major, minor, patch;
    private final String preRelease;
    private final String buildMetadata;

    private static final Pattern SEMVER_PATTERN = Pattern.compile(
        "^(\\d+|\\*)(?:\\.(\\d+|\\*))?(?:\\.(\\d+|\\*))?(?:-([0-9A-Za-z.-]+))?(?:\\+([0-9A-Za-z.-]+))?$"
    );

    /**
     * Constructs a {@code SemanticVersion} object by parsing a version string.
     *
     * @param version The semantic version string to be parsed.
     * @throws IllegalArgumentException if the input version string is not in valid SemVer format.
     */
    public SemanticVersion(String version) {
        Matcher matcher = SEMVER_PATTERN.matcher(version);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid SemVer format: " + version);
        }

        this.major = parsePart(matcher.group(1));
        this.minor = parsePart(matcher.group(2));
        this.patch = parsePart(matcher.group(3));
        this.preRelease = matcher.group(4);  // Can be null if absent
        this.buildMetadata = matcher.group(5);  // Can be null if absent
    }

    private Integer parsePart(String part) {
        return (part == null || part.equals("*")) ? null : Integer.parseInt(part);
    }

    @Override
    public int compareTo(SemanticVersion other) {
        return compareVersions(this, other);
    }

    /**
     * Determines if this version matches a wildcard version pattern.
     * <p>
     * Example:
     * <ul>
     *     <li>{@code 1.2.3} matches {@code 1.2.*}</li>
     *     <li>{@code 1.5.7} matches {@code 1.*}</li>
     *     <li>{@code 2.0.0} does not match {@code 1.*}</li>
     * </ul>
     * </p>
     *
     * @param pattern A version pattern with wildcards.
     * @return {@code true} if this version matches the pattern, otherwise {@code false}.
     */
    public boolean matchesWildcard(SemanticVersion pattern) {
        if (pattern.major != null && !pattern.major.equals(this.major)) return false;
        if (pattern.minor != null && !pattern.minor.equals(this.minor)) return false;
        if (pattern.patch != null && !pattern.patch.equals(this.patch)) return false;
        return true;
    }

    /**
     * Compares two semantic versions based on major, minor, patch, and pre-release precedence.
     *
     * @param v1 First version.
     * @param v2 Second version.
     * @return Comparison result (-1 if v1 < v2, 0 if equal, 1 if v1 > v2).
     */
    public static int compareVersions(SemanticVersion v1, SemanticVersion v2) {
        int result = Integer.compare(v1.major, v2.major);
        if (result != 0) return result;

        result = Integer.compare(v1.minor, v2.minor);
        if (result != 0) return result;

        result = Integer.compare(v1.patch, v2.patch);
        if (result != 0) return result;

        return comparePreRelease(v1.preRelease, v2.preRelease);
    }

    private static int comparePreRelease(String pr1, String pr2) {
        if (pr1 == null && pr2 == null) return 0; // Both are stable
        if (pr1 == null) return 1;  // Stable > Pre-release
        if (pr2 == null) return -1; // Pre-release < Stable

        String[] parts1 = pr1.split("\\.");
        String[] parts2 = pr2.split("\\.");

        for (int i = 0; i < Math.max(parts1.length, parts2.length); i++) {
            if (i >= parts1.length) return -1;
            if (i >= parts2.length) return 1;

            boolean isNumeric1 = parts1[i].matches("\\d+");
            boolean isNumeric2 = parts2[i].matches("\\d+");

            if (isNumeric1 && isNumeric2) {
                int num1 = Integer.parseInt(parts1[i]);
                int num2 = Integer.parseInt(parts2[i]);
                if (num1 != num2) return Integer.compare(num1, num2);
            } else {
                int cmp = parts1[i].compareTo(parts2[i]);
                if (cmp != 0) return cmp;
            }
        }
        return 0;
    }

    @Override
    public String toString() {
        return (major != null ? major : "*") + "." +
               (minor != null ? minor : "*") + "." +
               (patch != null ? patch : "*") +
               (preRelease != null ? "-" + preRelease : "") +
               (buildMetadata != null ? "+" + buildMetadata : "");
    }
}
