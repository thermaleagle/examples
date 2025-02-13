import java.util.List;
import java.util.StringJoiner;

public class Condition {
    private Parameter parameter;
    private Criteria criteria;
    private List<Condition> andConditions;
    private List<Condition> orConditions;
    private List<Condition> notConditions;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        // Add the main condition
        if (criteria != null) {
            sb.append(criteria.toString());
        }

        // Process andConditions
        if (andConditions != null && !andConditions.isEmpty()) {
            sb.append(" AND (").append(joinConditions(andConditions)).append(")");
        }

        // Process orConditions
        if (orConditions != null && !orConditions.isEmpty()) {
            sb.append(" OR (").append(joinConditions(orConditions)).append(")");
        }

        // Process notConditions
        if (notConditions != null && !notConditions.isEmpty()) {
            sb.append(" NOT (").append(joinConditions(notConditions)).append(")");
        }

        return sb.toString();
    }

    private String joinConditions(List<Condition> conditions) {
        StringJoiner joiner = new StringJoiner(", ");
        for (Condition condition : conditions) {
            joiner.add(condition.toString());
        }
        return joiner.toString();
    }

    // Assume Parameter and Criteria classes are defined elsewhere
}