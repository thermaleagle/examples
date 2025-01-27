import java.util.*;
import java.util.regex.*;

/**
 * A class for parsing and comparing Semantic Versions based on the SemVer 2.0 specification.
 * 
 * <p>Semantic versioning follows the format: {@code MAJOR.MINOR.PATCH-PRERELEASE+BUILD}.</p>
 * <p>Comparison rules:
 * <ul>
 *     <li>MAJOR, MINOR, and PATCH are compared numerically.</li>
 *     <li>Pre-release versions (e.g., "alpha", "beta") are lower than stable versions.</li>
 *     <li>Build metadata (e.g., "+build.1") is ignored in comparisons.</li>
 * </ul>
 * </p>
 */
public class SemanticVersion implements Comparable<SemanticVersion> {
    private Integer major, minor, patch;
    private String preRelease;
    private String buildMetadata;

    private static final Pattern SEMVER_PATTERN = Pattern.compile(
        "^(\\d+|\\*)(?:\\.(\\d+|\\*))?(?:\\.(\\d+|\\*))?(?:-([0-9A-Za-z.-]+))?(?:\\+([0-9A-Za-z.-]+))?$"
    );

    /**
     * Constructs a SemanticVersion from a string.
     *
     * @param version the version string to parse
     * @throws IllegalArgumentException if the version format is invalid
     */
    public SemanticVersion(String version) {
        Matcher matcher = SEMVER_PATTERN.matcher(version);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid SemVer format: " + version);
        }

        this.major = parsePart(matcher.group(1));
        this.minor = parsePart(matcher.group(2));
        this.patch = parsePart(matcher.group(3));
        this.preRelease = matcher.group(4);
        this.buildMetadata = matcher.group(5);
    }

    private Integer parsePart(String part) {
        return (part == null || part.equals("*")) ? null : Integer.parseInt(part);
    }

    @Override
    public int compareTo(SemanticVersion other) {
        return compareVersions(this, other);
    }

    /**
     * Compares two semantic versions.
     * 
     * <p>Major, minor, and patch numbers are compared first. If these are equal, pre-release versions are compared lexically.</p>
     *
     * @param v1 the first version
     * @param v2 the second version
     * @return a negative integer if v1 < v2, zero if equal, a positive integer if v1 > v2
     */
    public static int compareVersions(SemanticVersion v1, SemanticVersion v2) {
        Integer[] parts1 = {v1.major, v1.minor, v1.patch};
        Integer[] parts2 = {v2.major, v2.minor, v2.patch};

        // Compare major, minor, and patch versions
        for (int i = 0; i < 3; i++) {
            if (parts1[i] == null || parts2[i] == null) continue;
            if (!parts1[i].equals(parts2[i])) return Integer.compare(parts1[i], parts2[i]);
        }

        // Pre-release handling: Pre-release versions are lower than stable versions
        if (v1.preRelease == null && v2.preRelease != null) return 1;
        if (v1.preRelease != null && v2.preRelease == null) return -1;
        if (v1.preRelease != null && v2.preRelease != null) {
            return comparePreRelease(v1.preRelease, v2.preRelease);
        }

        return 0;
    }

    private static int comparePreRelease(String pr1, String pr2) {
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
