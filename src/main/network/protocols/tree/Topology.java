package main.network.protocols.tree;

import main.util.Serialization;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages the topology for the tree protocol.
 *
 * There are two components to the topology: information the node receives from
 * its descendants, and information it receives from its parent.
 *
 * Nodes forward information about this tree to keep it up-to-date for all
 * nodes. The reason for this is a single method:
 * createParentCandidateScanner(). All other methods are used for keeping the
 * topology up-to-date.
 *
 * Note that this object does not necessarily represent the correct tree at
 * this exact moment--nodes may have changed parents or died between now and
 * the last time we received data.
 *
 * One important subtle point about this class and its serialization and
 * deserialization is that we associate the node-to-parent maps with whom we
 * received the data from. We can't have a single map that represents the
 * entire data structure because if, for example, a descendant disconnected,
 * our child would know that it disconnected and not include it in its message.
 * We would read this message, however, and only update the parents for nodes
 * we saw in the message, leaving the disconnected node in the map.
 *
 * We keep one key in our map per edge connecting us to the tree--a key for the
 * parent and a key for each of our children. The values are maps of nodes to
 * parents for nodes in those portions of the tree.
 *
 * The public methods of this class are synchronized, since a lot of the data
 * is tightly-coupled (e.g. the two maps).
 *
 * TODO(ddoucet): check for cycles; disconnect from children if one is found
 *      (make sure to call disconnect() on ParentCandidateScanner when searching
 *      for a new parent afterward)
 */
public class Topology<TKey> {
    private final TKey broadcasterKey;
    private final TKey currentNodeKey;

    // See description above class
    private final Map<TKey, Map<TKey, TKey>> destToNodeToParent;

    // Maps nodes to their list of children. Recomputed after updating the
    // above map.
    private final Map<TKey, List<TKey>> nodeToChildren;

    private TKey parentKey;

    public Topology(TKey broadcasterKey, TKey currentNodeKey) {
        this.broadcasterKey = broadcasterKey;
        this.currentNodeKey = currentNodeKey;

        destToNodeToParent = new HashMap<>();
        nodeToChildren = new HashMap<>();
        parentKey = null;
    }

    public synchronized ParentCandidateScanner<TKey> createParentCandidateScanner() {
        Map<TKey, List<TKey>> nodeToChildrenCopy = new HashMap<>();
        for (Map.Entry<TKey, List<TKey>> entry : nodeToChildren.entrySet())
            nodeToChildrenCopy.put(entry.getKey(), new ArrayList<>(entry.getValue()));

        return new ParentCandidateScanner<>(
                broadcasterKey,
                currentNodeKey,
                parentKey,
                nodeToChildrenCopy);
    }

    // Should only be called while this is locked.
    private void computeNodeToChildrenMapLocked() throws Exception {
        nodeToChildren.clear();

        for (Map.Entry<TKey, Map<TKey, TKey>> edgeMap : destToNodeToParent.entrySet()) {
            for (Map.Entry<TKey, TKey> nodeParent : edgeMap.getValue().entrySet()) {
                TKey node = nodeParent.getKey(),
                        parent = nodeParent.getValue();

                if (!nodeToChildren.containsKey(parent))
                    nodeToChildren.put(parent, new ArrayList<>());

                if (!nodeToChildren.containsKey(node))
                    nodeToChildren.put(node, new ArrayList<>());

                nodeToChildren.get(parent).add(node);
            }
        }
        // Adding in cycle check
        checkForCycles();
    }

    private void checkForCycles() throws Exception {
        checkForCycles(broadcasterKey, new HashSet<>());
    }

    private void checkForCycles(TKey node, Set<TKey> visited) throws Exception {
        if (visited.contains(node)) {
            System.out.println("cycle!");
            throw new Exception("Cycle detected in topology");
        }

        if (nodeToChildren.get(node) == null)
            return;

        visited.add(node);
        for (TKey child : nodeToChildren.get(node))
            checkForCycles(child, visited);
        visited.remove(node);
    }

    public synchronized void updateChildInfo(TKey child, InputStream stream)
            throws Exception {
        updateEdgeLocked(child, stream);
    }

    public synchronized void updateNonDescendantInfo(InputStream stream)
            throws Exception {
        updateEdgeLocked(parentKey, stream);
    }

    // Should only be called while this is locked.
    private void updateEdgeLocked(TKey edge, InputStream stream) throws Exception {
        Map<TKey, TKey> nodeToParent = new HashMap<>();

        long numNodes = Serialization.readLong(stream);
        for (int i = 0; i < numNodes; i++) {
            // This really blows
            TKey node = Serialization.deserialize(stream, (Class<TKey>)edge.getClass());
            TKey parent = Serialization.deserialize(stream, (Class<TKey>)edge.getClass());
            nodeToParent.put(node, parent);
        }

        destToNodeToParent.put(edge, nodeToParent);
        computeNodeToChildrenMapLocked();
    }

    private static <TKey> int totalNodes(Collection<Map<TKey, TKey>> maps) {
        return maps.stream()
                .mapToInt(Map::size).sum();
    }

    private static <TKey> void writeMapToStream(OutputStream stream, Map<TKey, TKey> nodeToParent)
            throws Exception {
        for (TKey node : nodeToParent.keySet()) {
            stream.write(Serialization.serialize(node));
            stream.write(Serialization.serialize(nodeToParent.get(node)));
        }
    }

    private void writeMapsToStream(OutputStream stream, Collection<Map<TKey, TKey>> maps)
            throws Exception {
        if (broadcasterKey.equals(currentNodeKey))
            Serialization.writeLong(stream, totalNodes(maps));
        else {
            Serialization.writeLong(stream, totalNodes(maps) + 1);
            stream.write(Serialization.serialize(currentNodeKey));
            stream.write(Serialization.serialize(parentKey));
        }

        for (Map<TKey, TKey> map : maps)
            writeMapToStream(stream, map);
    }

    // Should only be called while this is locked.
    private byte[] serializeExceptEdgeLocked(byte prefix, TKey edge) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(prefix);

        List<Map<TKey, TKey>> maps = destToNodeToParent.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(edge))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
        writeMapsToStream(baos, maps);

        return baos.toByteArray();
    }

    public synchronized byte[] serializeDescendantInfo(byte prefix)
            throws Exception {
        return serializeExceptEdgeLocked(prefix, parentKey);
    }

    public synchronized byte[] serializeExceptChild(byte prefix, TKey child)
            throws Exception {
        return serializeExceptEdgeLocked(prefix, child);
    }

    // Removes a child and all of its descendants from the map. This is called
    // when a child disconnects (thus we've lost access to it and all of its
    // descendants).
    public synchronized void removeChild(TKey key) {
        removeChildLocked(key);
    }

    // Should only be called while this is locked.
    private void removeChildLocked(TKey key) {
        removeDescendantLocked(key);
        destToNodeToParent.remove(key);
    }

    // Should only be called while this is locked.
    private void removeDescendantLocked(TKey key) {
        if (!nodeToChildren.containsKey(key))
            return;

        nodeToChildren.get(key).forEach(this::removeDescendantLocked);
        nodeToChildren.remove(key);
    }

    public synchronized void setParent(TKey key) {
        parentKey = key;
    }
}
