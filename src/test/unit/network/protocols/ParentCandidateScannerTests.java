package test.unit.network.protocols;

import com.google.common.collect.ImmutableMap;
import main.network.protocols.kary_tree.ParentCandidateScanner;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Tests the ParentCandidateScanner as used in the K-ary tree network protocol.
 */
public class ParentCandidateScannerTests {
    private String getNextChild(ParentCandidateScanner<String> scanner) {
        String next = scanner.findNewParent();

        if (next != null)
            return next;

        scanner.disconnect();
        return scanner.findNewParent();
    }

    // Each test is merely an input to this function.
    // This input acts as if all connections are failing; it repeatedly asks
    // for a new parent until the scanner can give no more.
    // expectedOrdering is the ENTIRE list--the ordering the scanner would
    // suggest if it were called until it had no more nodes to suggest.
    private void test(int k,
                      String broadcaster,
                      String node,
                      String parent,
                      Map<String, List<String>> nodeToChildren,
                      List<String> expectedOrdering) {
        ParentCandidateScanner<String> scanner = new ParentCandidateScanner<>(
                k, broadcaster, node, parent, nodeToChildren);

        for (int i = 0; i < expectedOrdering.size(); i++) {
            String expected = expectedOrdering.get(i);
            String actual = getNextChild(scanner);
            assertEquals(
                    String.format("Failed at index %s; expected %s got %s\n",
                            i, expected, actual),
                    expected, actual);
        }

        assertEquals(null, getNextChild(scanner));
    }

    @Test
    public void testBroadcasterHasNoSuggestions() {
        test(3, "bc", "bc", null,
            ImmutableMap.of("bc", Arrays.asList()),
            Arrays.asList());
    }

    @Test
    public void testNodeAndBroadcasterHasNoSuggestions() {
        test(3, "bc", "a", "bc",
                ImmutableMap.of(
                        "bc", Arrays.asList("a"),
                        "a", Arrays.asList()),
                Arrays.asList());
    }

    @Test
    public void testNodeWithSibling() {
        test(3, "bc", "a", "bc",
                ImmutableMap.of(
                        "bc", Arrays.asList("a", "b"),
                        "a", Arrays.asList(),
                        "b", Arrays.asList()),
                Arrays.asList("b"));
    }

    @Test
    public void testNodeWithNonBroadcasterParent() {
        test(3, "bc", "a", "b",
                ImmutableMap.of(
                        "bc", Arrays.asList("b"),
                        "b", Arrays.asList("a"),
                        "a", Arrays.asList()),
                Arrays.asList("bc"));
    }

    @Test
    public void testNodeSiblingChildBroadcaster() {
        test(3, "bc", "a", "bc",
                ImmutableMap.of(
                        "bc", Arrays.asList("a", "b"),
                        "a", Arrays.asList("c"),
                        "b", Arrays.asList(),
                        "c", Arrays.asList()),
                Arrays.asList("b", "c"));
    }

    @Test
    public void testBinaryChain() {
        test(2, "bc", "a", "c",
                ImmutableMap.of(
                        "bc", Arrays.asList("b"),
                        "b", Arrays.asList("c"),
                        "c", Arrays.asList("a"),
                        "a", Arrays.asList()),
                Arrays.asList("bc", "b"));
    }

    @Test
    public void testBinaryBroadcasterSkippedFirst() {
        test(2, "bc", "a", "d",
                ImmutableMap.of(
                        "bc", Arrays.asList("b", "c"),
                        "b", Arrays.asList(),
                        "c", Arrays.asList("d"),
                        "d", Arrays.asList("a"),
                        "a", Arrays.asList()),
                Arrays.asList("b", "c", "bc"));
    }

    @Test
    public void testBinaryWithChild() {
        test(2, "bc", "a", "c",
                ImmutableMap.of(
                        "bc", Arrays.asList("b", "c"),
                        "b", Arrays.asList(),
                        "c", Arrays.asList("a"),
                        "a", Arrays.asList("d"),
                        "d", Arrays.asList()),
                Arrays.asList("b", "bc", "d"));
    }
}
