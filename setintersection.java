import java.util.HashSet;
import java.util.Set;

public class SetIntersection {
    public static <T> Set<T> findIntersection(Set<T> set1, Set<T> set2) {
        if (set1.size() > set2.size()) {  // Optimize by iterating over the smaller set
            return findIntersection(set2, set1);
        }
        Set<T> result = new HashSet<>(set1);
        result.retainAll(set2); // Efficient intersection operation
        return result;
    }

    public static void main(String[] args) {
        Set<Integer> set1 = Set.of(1, 2, 3, 4, 5);
        Set<Integer> set2 = Set.of(3, 4, 5, 6, 7);

        Set<Integer> intersection = findIntersection(set1, set2);
        System.out.println("Intersection: " + intersection); // Output: [3, 4, 5]
    }
}