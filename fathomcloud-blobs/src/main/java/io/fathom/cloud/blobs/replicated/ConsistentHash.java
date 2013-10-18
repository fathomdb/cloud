package io.fathom.cloud.blobs.replicated;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.fathomdb.SimpleIterator;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.protobuf.ByteString;

/**
 * A now-traditional ConsistentHash ring.
 * 
 * Note that this data structure is immutable. When the ring changes, build a
 * new one.
 */
public class ConsistentHash<T> {
    private final HashFunction hashFunction;
    private final int numberOfReplicas;
    private final TreeMap<Integer, T> circle = new TreeMap<Integer, T>();
    private final Map<String, T> nodeMap;

    public ConsistentHash(int numberOfReplicas, Map<String, T> nodeMap) {
        this.numberOfReplicas = numberOfReplicas;
        this.nodeMap = nodeMap;
        this.hashFunction = Hashing.murmur3_32();

        for (Entry<String, T> entry : nodeMap.entrySet()) {
            add(entry.getKey(), entry.getValue());
        }
    }

    private void add(String key, T node) {
        for (int i = 0; i < numberOfReplicas; i++) {
            circle.put(hashFunction.hashString(key + "_@_" + i).asInt(), node);
        }
    }

    // public void remove(String key, T node) {
    // for (int i = 0; i < numberOfReplicas; i++) {
    // circle.remove(hashFunction.hashString(key + "_@_" + i).asInt());
    // }
    // }

    // public T get(HashCode hash) {
    // int hashValue = hash.asInt();
    // return get(hashValue);
    // }
    //
    // public T get(int hashValue) {
    // if (circle.isEmpty()) {
    // return null;
    // }
    //
    // Entry<Integer, T> entry = circle.ceilingEntry(hashValue);
    // if (entry == null) {
    // entry = circle.firstEntry();
    // }
    //
    // return entry.getValue();
    // }

    class RingIterator extends SimpleIterator<T> {
        // final TreeMap<Integer, T> circle;
        final int start;
        // final Set<T> done = Sets.newCopyOnWriteArraySet();

        Iterator<T> tail;
        Iterator<T> head;

        public RingIterator(
        // TreeMap<Integer, T> circle,
                int start) {
            // this.circle = circle;
            this.start = start;

            this.tail = circle.tailMap(start, true).values().iterator();
        }

        @Override
        protected T getNext(T current) {
            while (true) {
                if (this.tail != null) {
                    if (tail.hasNext()) {
                        T next = tail.next();
                        // if (done.contains(next)) {
                        // done.add(next);
                        // return next;
                        // } else {
                        // continue;
                        // }
                        return next;
                    } else {
                        this.tail = null;
                        this.head = circle.headMap(start, false).values().iterator();
                    }
                }

                if (this.head != null) {
                    if (head.hasNext()) {
                        T next = head.next();
                        // if (done.contains(next)) {
                        // done.add(next);
                        // return next;
                        // } else {
                        // continue;
                        // }
                        return next;
                    } else {
                        this.head = null;
                    }
                }

                return null;
            }
        }
    }

    private Iterator<T> walkRing(int hashValue) {
        return new RingIterator(hashValue);
    }

    public Iterable<T> all() {
        return nodeMap.values();
    }

    public Iterator<T> walkRing(ByteString key) {
        return walkRing(key.hashCode());
    }
}