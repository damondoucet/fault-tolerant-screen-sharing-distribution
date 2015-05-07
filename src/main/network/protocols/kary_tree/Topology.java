package main.network.protocols.kary_tree;

import main.util.Serialization;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages the topology for the K-ary Tree protocol.
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
 */
public class Topology<TKey> {
    private final TKey broadcasterKey;
    private final TKey currentNodeKey;
    private final int k;

    // See description above class
    private final Map<TKey, Map<TKey, TKey>> destToNodeToParent;

    // Maps nodes to their list of children. Recomputed after updating the
    // above map.
    private final Map<TKey, List<TKey>> nodeToChildren;

    private TKey parentKey;

    public Topology(TKey broadcasterKey, TKey currentNodeKey, int k) {
        this.broadcasterKey = broadcasterKey;
        this.currentNodeKey = currentNodeKey;
        this.k = k;

        destToNodeToParent = new HashMap<>();
        nodeToChildren = new HashMap<>();
        parentKey = null;
    }

    public synchronized ParentCandidateScanner<TKey> createParentCandidateScanner() {
        Map<TKey, List<TKey>> nodeToChildrenCopy = new HashMap<>();
        for (Map.Entry<TKey, List<TKey>> entry : nodeToChildren.entrySet())
            nodeToChildrenCopy.put(entry.getKey(), new ArrayList<>(entry.getValue()));

        return new ParentCandidateScanner<>(
                k,
                broadcasterKey,
                currentNodeKey,
                parentKey,
                nodeToChildrenCopy);
    }

    // Should only be called while this is locked.
    private void computeNodeToChildrenMapLocked() {
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
    }

    public synchronized void updateChildInfo(TKey child, InputStream stream)
            throws IOException {
        updateEdgeLocked(child, stream);
    }

    public synchronized void updateNonDescendantInfo(InputStream stream)
            throws IOException {
        System.out.printf("%s reading nondesc info from parent %s\n", currentNodeKey, parentKey);
        updateEdgeLocked(parentKey, stream);
    }

    // Should only be called while this is locked.
    private void updateEdgeLocked(TKey edge, InputStream stream) throws IOException {
        Map<TKey, TKey> nodeToParent = new HashMap<>();

        long numNodes = Serialization.readLong(stream);
        for (int i = 0; i < numNodes; i++) {
            TKey node = Serialization.deserialize(stream);
            TKey parent = Serialization.deserialize(stream);
            nodeToParent.put(node, parent);
        }

        destToNodeToParent.put(edge, nodeToParent);
        computeNodeToChildrenMapLocked();
    }

    private static <TKey> int totalNodes(Collection<Map<TKey, TKey>> maps) {
        return maps.stream()
                .mapToInt(map -> map.size()).sum();
    }

    private static <TKey> void writeMapToStream(OutputStream stream, Map<TKey, TKey> nodeToParent)
            throws IOException {
        for (TKey node : nodeToParent.keySet()) {
            stream.write(Serialization.serialize(node));
            stream.write(Serialization.serialize(nodeToParent.get(node)));
        }
    }

    private static <TKey> void writeMapsToStream(OutputStream stream, Collection<Map<TKey, TKey>> maps)
            throws IOException {
        Serialization.writeLong(stream, totalNodes(maps));
        for (Map<TKey, TKey> map : maps)
            writeMapToStream(stream, map);
    }

    public synchronized byte[] serializeDescendantInfo(byte prefix) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(prefix);

        List<Map<TKey, TKey>> maps = destToNodeToParent.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(parentKey))
                .map(entry -> entry.getValue())
                .collect(Collectors.toList());
        writeMapsToStream(baos, maps);

        return baos.toByteArray();
    }

    public synchronized byte[] serializeTopology(byte prefix) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(prefix);
        writeMapsToStream(baos, destToNodeToParent.values());
        return baos.toByteArray();
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
