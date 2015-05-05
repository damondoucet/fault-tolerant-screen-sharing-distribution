package main.network.protocols.kary_tree;

import java.util.*;

/**
 * Uses information collected from the Topology class to suggest candidates for
 * new parents when our old one has disconnect bed.
 *
 * Usage of this class is as follows:
 *      - when with the parent is lost, call
 *          toplogy.createParentCandidateScanner()
 *      - while cannot establish connection:
 *            candidate = scanner.findNewParent()
 *            if candidate == null
 *              disconnect children
 *              candidate = scanner.findNewParent()
 *
 *              if candidate == null
 *                  no possible parents
 *
*             attempt to connect to candidate
 *
 * The object can be discarded once a connection is made because the topology
 * will update its data and this object's data will become out of date.
 *
 * TODO(ddoucet): this class has a lot of kludgy logic going on. I'm sure it
 * can be cleaned up somehow.
 *
 * TODO(ddoucet): currently, this doesn't disconnect children until it's
 * searched through ALL non-descendants in the tree. is this the behavior we
 * want?
 */
public class ParentCandidateScanner<TKey> {
    private final int k;
    private final TKey broadcasterKey;
    private final TKey parentKey;
    private final TKey currentNodeKey;

    private final Map<TKey, List<TKey>> nodeToChildren;

    // Queue of nodes that we might suggest. We inspect the tree in a bfs from
    // the root (the broadcaster). Before suggesting a node, we'll enqueue it
    // here.
    private final Queue<TKey> nodesToInspect;

    // Nodes already suggested. We don't want to suggest these again in another
    // pass of the tree.
    private final Set<TKey> suggestedNodes;

    private boolean disconnected;
    private boolean hasSuggestedAllUndersubscribedNodes;

    public ParentCandidateScanner(int k,
                                  TKey broadcasterKey,
                                  TKey currentNodeKey,
                                  TKey parentKey,
                                  Map<TKey, List<TKey>> nodeToChildren) {
        this.k = k;
        this.broadcasterKey = broadcasterKey;
        this.parentKey = parentKey;
        this.currentNodeKey = currentNodeKey;
        this.nodeToChildren = nodeToChildren;

        this.nodesToInspect = new ArrayDeque<>();
        this.suggestedNodes = new HashSet<>();
        this.disconnected = false;
        this.hasSuggestedAllUndersubscribedNodes = false;

        nodesToInspect.add(broadcasterKey);
    }

    public void disconnect() {
        if (!disconnected) {
            nodesToInspect.add(currentNodeKey);
            disconnected = true;
        }
    }

    //
    // The idea here comes in two parts, depending on what the current state
    // is. Either we're in a normal state, or we're disconnected (we've failed
    // to reconnect too many times).
    //
    //     1) Normal -- only consider nodes that we've heard about through our
    //        parent. We don't want to connect to nodes we've heard from our
    //        children because they're our descendants and that would lead to
    //        cycles.
    //
    //     2) Disconnected -- We can consider any node because we will have
    //        disconnected from any children and thus avoid creating a cycle.
    //
    public synchronized TKey findNewParent() {
        // If the topology hasn't been initialized yet, all we can do is return
        // the broadcaster.
        if (nodeToChildren.isEmpty())
            return broadcasterKey;

        TKey node;
        do {
            node = readNextFromQueue();
            if (node == null)
                return null;

            if (!disconnected && node.equals(currentNodeKey))
                continue;

            nodeToChildren.get(node).forEach(nodesToInspect::add);
        } while (!shouldSuggestNode(node));

        suggestedNodes.add(node);
        return node;
    }

    private TKey readNextFromQueue() {
        if (nodesToInspect.isEmpty()) {
            if (hasSuggestedAllUndersubscribedNodes) {
                // Either we're disconnected, and there are no more nodes, or
                // we're not disconnected and they can disconnect to get more
                // nodes to inspect.
                return null;
            }

            hasSuggestedAllUndersubscribedNodes = true;
            // Now look through nodes with more than K nodes
            nodesToInspect.add(broadcasterKey);
        }

        return nodesToInspect.poll();
    }

    private boolean shouldSuggestNode(TKey node) {
        return !node.equals(parentKey) && !node.equals(currentNodeKey) &&
                !suggestedNodes.contains(node) &&
                (hasSuggestedAllUndersubscribedNodes || nodeToChildren.get(node).size() < k);
    }
}
