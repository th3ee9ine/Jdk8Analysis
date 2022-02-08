/*
 * Copyright (c) 1997, 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package com.th3ee9ine.java.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * TreeMap，带解析。
 *
 * @author th3ee9ine.
 * @date 2022/1/29 13:34.
 */
public class TreeMap<K,V> extends AbstractMap<K,V>
        implements NavigableMap<K,V>, Cloneable, Serializable {
    /**
     * 比较器，如果没有自定义比较器，则为 null。
     * 默认按照 key 的自然顺序进行排序。
     */
    private final Comparator<? super K> comparator;
    /**
     * 树的根节点。
     * 不可被序列化（transient 关键字，这里不展开描述）。
     */
    private transient Entry<K,V> root;
    /**
     * 节点数。
     * 不可被序列化。
     */
    private transient int size = 0;
    /**
     * 对树结构修改（添加、删除）的次数。
     * 不可被序列化。
     */
    private transient int modCount = 0;

    /**
     * 创建一个新的 TreeMap，默认比较器。
     * 插入的所有 key 都必须实现 Comparable 接口。
     */
    public TreeMap() {
        comparator = null;
    }

    /**
     * 创建一个新的 TreeMap，使用自定义比较器。
     */
    public TreeMap(Comparator<? super K> comparator) {
        this.comparator = comparator;
    }

    /**
     * 根据指定的 Map 构造一个 TreeMap，并且按照 key 的自然顺序进行排序。
     * 此方法的时间复杂度为：n*log(n)。
     *
     * @param m 给定的 Map。
     * @throws ClassCastException 如果指定的 Map 中的 key 不可比较，或者不可相互比较。
     * @throws ClassCastException 如果指定的 Map 为空。
     */
    public TreeMap(Map<? extends K, ? extends V> m) {
        comparator = null;
        putAll(m);
    }

    /**
     * 根据指定的 SortedMap 构造一个 TreeMap，并使用相同的顺序。
     * 此方法的时间复杂度为：n。
     *
     * @param  m 已排序的映射，其映射将放置在此映射中，并且其比较器将用于对此映射进行排序
     * @throws NullPointerException 如果指定的 SortedMap 为空。
     */
    public TreeMap(SortedMap<K, ? extends V> m) {
        comparator = m.comparator();
        try {
            buildFromSorted(m.size(), m.entrySet().iterator(), null, null);
        } catch (IOException cannotHappen) {
        } catch (ClassNotFoundException cannotHappen) {
        }
    }

    /**
     * 返回节点数.
     *
     * @return 节点数.
     */
    @Override
    public int size() {
        return size;
    }

    /**
     * 判断 TreeMap 中是否包含指定的 key，如果包含则返回 true。
     *
     * @param key 指定的 key。
     * @return 如果包含指定的 key，则返回 true。
     * @throws ClassCastException 如果指定的 key 无法与 TreeMap 中的 key 进行比较。
     * @throws NullPointerException 如果指定的 key 为 null 并且 TreeMap 使用默认比较器，或者比较器不允许 key 为 null。
     */
    @Override
    public boolean containsKey(Object key) {
        return getEntry(key) != null;
    }

    /**
     * 判断 TreeMap 中是否包含指定的 value，如果包含则返回 true。
     * 更准确的说：当且仅当此 TreeMap 包含至少一个 value 所对应的键值对时，返回 true。
     * 时间复杂度：与节点数成线性关系。
     *
     * @param value 指定的 value。
     * @return 如果存在指定的value，则返回 true。
     */
    @Override
    public boolean containsValue(Object value) {
        // 从小到大遍历整棵树，找到对应值后，立即返回
        // Entry<K,V> e = getFirstEntry() 返回最小的key所对应的节点。
        // e = successor(e) 将下一个节点赋值给 e。
        for (Entry<K,V> e = getFirstEntry(); e != null; e = successor(e)) {
            // 如果值相等，则说明 value 在当前 TreeMap 中存在，返回 true
            if (valEquals(value, e.value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 根据指定的 key 返回对应的值。
     *
     * @param key 指定的 key。
     * @return 返回 key 所对应的值。
     * @throws ClassCastException 如果指定的 key 无法与 TreeMap 中的 key 进行比较。
     * @throws NullPointerException 如果指定的 key 为 null 并且 TreeMap 使用默认比较器，或者比较器不允许 key 为 null。
     */
    @Override
    public V get(Object key) {
        // 根据 key 获取对应的 Entry
        Entry<K,V> p = getEntry(key);
        // 如果 p 不等于 null，则返回 p.value
        return (p==null ? null : p.value);
    }

    /**
     * 返回比较器，如果没有自定义比较器，则返回 null。
     *
     * @return 比较器。
     */
    @Override
    public Comparator<? super K> comparator() {
        return comparator;
    }

    /**
     * 返回第一个节点的 key。
     *
     * 注意：这里的第一个节点，非根节点，而是最左边的那个节点(最小的节点)。
     *
     * @return 第一个节点。
     */
    @Override
    public K firstKey() {
        return key(getFirstEntry());
    }

    /**
     * 返回最后一个节点的 key。
     *
     * 注意：这里的最后一个节点，非根节点，而是最右边的那个节点(最大的节点)。
     *
     * @return 最后一个节点。
     */
    @Override
    public K lastKey() {
        return key(getLastEntry());
    }

    /**
     * 将指定 map 中的所有键值对复制到当前 map。
     * 这些键值对会替换当前 map 中的相同 key 的键值对。
     *
     * @param  map 指定的 map。
     * @throws ClassCastException – 如果指定的 map 中的 key 或 value 不被允许存储在当前 map 中。
     * @throws NullPointerException – 如果指定的 map 为空或指定的 map 包含一个空键并且当前 map 不允许空键。
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        // 1、获取指定的 map 的大小
        int mapSize = map.size();
        // 2、判断此 TreeMap 是否不存在任何节点，且指定的 map 是 SortedMap 的实现类
        if (size==0 && mapSize!=0 && map instanceof SortedMap) {
            // 2.1、将 map 的比较器赋值给 c
            Comparator<?> c = ((SortedMap<?,?>)map).comparator();
            // 2.2、如果 c 与 comparator 相等，则调用 buildFromSorted 方法，构建 TreeMap
            if (c == comparator || (c != null && c.equals(comparator))) {
                // 2.2.1、对树结构修改次数+1
                ++modCount;
                try {
                    buildFromSorted(mapSize, map.entrySet().iterator(),
                                    null, null);
                } catch (IOException | ClassNotFoundException cannotHappen) {
                }
                return;
            }
        }
        // 3、否则调用 AbstractMap.putAll 方法，将 map 中的元素添加到当前 TreeMap 中
        super.putAll(map);
    }

    /**
     * 根据指定的 key 查找对应的 Entry。
     *
     * 更准确的说：
     *      如果此 TreeMap 包含 key 所对应的 Entry(最多只会有一个这样的映射)，
     *      且可以根据比较器(comparator)判断，key 与 Entry.key 相等，
     *      则返回 Entry，否则返回 null。
     * 所以说，返回 null 的情况可以区分为两种：
     *      第一种：key 所对应的 Entry 不存在。
     *      第二种：key 所对应的 Entry 存在，但是根据比较器(comparator)判断，key 与 Entry.key 不相等。
     * TreeMap.containsKey() 方法可以判断 TreeMap 中是否包含 key 所对应的 Entry，可以使用该方法区分上面两种情况。
     *
     * @param key 指定的 key。
     * @return 根据指定的 key 查找对应的 Entry。
     * @throws ClassCastException 如果指定的 key 无法与 TreeMap 中的 key 进行比较。
     * @throws NullPointerException 如果指定的 key 为 null 并且 TreeMap 使用默认比较器，或者比较器不允许 key 为 null。
     */
    final Entry<K,V> getEntry(Object key) {
        // 1、如果比较器不为空，则执行 TreeMap.getEntryUsingComparator() 方法。
        if (comparator != null) {
            return getEntryUsingComparator(key);
        }
        // 2、如果 key 为空 null，则抛出 NullPointerException 异常。
        // 注意：如果自定义比较器(comparator)的情况，记得在比较器中添加 key 判空的逻辑，
        //      因为 TreeMap.getEntryUsingComparator() 方法不会对 key 进行校验。
        if (key == null) {
            throw new NullPointerException();
        }
        // 3、将 key 所对应的比较器赋值给 k
        @SuppressWarnings("unchecked") // 忽略 unchecked 告警
        Comparable<? super K> k = (Comparable<? super K>) key;
        // 4、将根节点赋值给 p
        Entry<K,V> p = root;
        // 5、遍历 p，查找 k.compareTo(p.key) 等于 0 的值
        while (p != null) {
            // k.compareTo(p.key) 的值说明：
            //      等于0 : 说明 k 等于 p.key
            //      大于0 : 说明 k 大于 p.key
            //      小于0 : 说明 k 小于 p.key
            // 5.1、将 k.compareTo(p.key) 的值赋值给 cmp
            int cmp = k.compareTo(p.key);
            // 5.2、如果 cmp 小于 0 则继续遍历左子树
            if (cmp < 0) {
                p = p.left;
            } else if (cmp > 0) {
                // 5.3、如果 cmp 大于 0 则继续遍历右子树
                p = p.right;
            } else {
                // 5.3、如果 cmp 等于 0 则返回 p
                return p;
            }
        }
        return null;
    }

    /**
     * 使用自定义比较器，根据指定的 key 查找对应的 Entry。
     *
     * @param key 指定的 key。
     * @return 根据指定的 key 查找对应的 Entry。
     */
    final Entry<K,V> getEntryUsingComparator(Object key) {
        // 1、将 key 转换成 K 类型的对象，并赋值给 k。
        @SuppressWarnings("unchecked") // 忽略 unchecked 告警
        K k = (K) key;
        // 2、将 comparator 赋值给 cpr。
        Comparator<? super K> cpr = comparator;
        // 3、如果 cpr 不为空，遍历 TreeMap
        if (cpr != null) {
            // 4、将根节点赋值给 p
            Entry<K,V> p = root;
            // 5、遍历 p 的所有节点，找出 key 对应的 Entry。
            while (p != null) {
                // 5.1、使用比较器，比较两个 key
                int cmp = cpr.compare(k, p.key);
                // 5.2、如果结果小于0，则继续比较左节点
                if (cmp < 0) {
                    p = p.left;
                } else if (cmp > 0) {
                    // 5.3、如果结果大于0，则继续比较右节点
                    p = p.right;
                } else {
                    // 5.3、如果结果等于0，则直接返回结果
                    return p;
                }
            }
        }
        return null;
    }

    /**
     * 获取指定 key 对应的 entry；
     * 如果没有这样的 entry 存在，则返回大于指定 key 的最小键的 entry；
     * 如果不存在这样的 entry（即，树中最大的 key 比指定的 key 小），则返回 null。
     */
    final Entry<K,V> getCeilingEntry(K key) {
        // 1、将根节点赋值给 p
        Entry<K,V> p = root;
        // 2、遍历查找是否有符合条件的 entry
        while (p != null) {
            // 2.1、使用比较器，比较两个 key
            int cmp = compare(key, p.key);
            // 2.2、如果结果小于0，则继续比较左节点
            if (cmp < 0) {
                // 2.2.1、如果 p 的左节点不为空，则继续比较左节点
                if (p.left != null) {
                    p = p.left;
                } else {
                    // 2.2.2、如果 p 的左节点为空，则返回 p
                    return p;
                }
            } else if (cmp > 0) {
                // 2.3、如果结果大于0，则继续比较右节点
                // 2.3.2、如果 p 的右节点不为空，则继续比较右节点
                if (p.right != null) {
                    p = p.right;
                } else {
                    // 2.3.2、如果 p 的右节点为空，则遍历 p 的父节点，找出大于指定 key 的最小键的 entry
                    Entry<K,V> parent = p.parent;
                    Entry<K,V> ch = p;
                    while (parent != null && ch == parent.right) {
                        ch = parent;
                        parent = parent.parent;
                    }
                    return parent;
                }
            } else {
                // 2.4、如果结果等于0，则直接返回 p
                return p;
            }
        }
        return null;
    }

    /**
     * 获取指定 key 对应的 entry；
     * 如果没有这样的 entry 存在，则返回小于指定 key 的最大键的 entry；
     * 如果不存在这样的 entry（即，树中最小的 key 比指定的 key 大），则返回 null。
     */
    final Entry<K,V> getFloorEntry(K key) {
        // 1、将根节点赋值给 p
        Entry<K,V> p = root;
        // 2、遍历查找是否有符合条件的 entry
        while (p != null) {
            // 2.1、使用比较器，比较两个 key
            int cmp = compare(key, p.key);
            // 2.2、如果结果大于0，则继续比较右节点
            if (cmp > 0) {
                // 2.2.1、如果 p 的右节点不为空，则继续比较右节点
                if (p.right != null) {
                    p = p.right;
                } else {
                    // 2.2.2、如果 p 的右节点为空，则返回 p
                    return p;
                }
            } else if (cmp < 0) {
                // 2.3、如果结果小于0，则继续比较左节点
                // 2.3.2、如果 p 的左节点不为空，则继续比较左节点
                if (p.left != null) {
                    p = p.left;
                } else {
                    // 2.3.2、如果 p 的左节点为空，则遍历 p 的父节点，找出小于指定 key 的最大键的 entry
                    Entry<K,V> parent = p.parent;
                    Entry<K,V> ch = p;
                    while (parent != null && ch == parent.left) {
                        ch = parent;
                        parent = parent.parent;
                    }
                    return parent;
                }
            } else {
                // 2.4、如果结果等于0，则直接返回 p
                return p;
            }

        }
        return null;
    }

    /**
     * Gets the entry for the least key greater than the specified
     * key; if no such entry exists, returns the entry for the least
     * key greater than the specified key; if no such entry exists
     * returns {@code null}.
     */
    final Entry<K,V> getHigherEntry(K key) {
        Entry<K,V> p = root;
        while (p != null) {
            int cmp = compare(key, p.key);
            if (cmp < 0) {
                if (p.left != null) {
                    p = p.left;
                } else {
                    return p;
                }
            } else {
                if (p.right != null) {
                    p = p.right;
                } else {
                    Entry<K,V> parent = p.parent;
                    Entry<K,V> ch = p;
                    while (parent != null && ch == parent.right) {
                        ch = parent;
                        parent = parent.parent;
                    }
                    return parent;
                }
            }
        }
        return null;
    }

    /**
     * Returns the entry for the greatest key less than the specified key; if
     * no such entry exists (i.e., the least key in the Tree is greater than
     * the specified key), returns {@code null}.
     */
    final Entry<K,V> getLowerEntry(K key) {
        Entry<K,V> p = root;
        while (p != null) {
            int cmp = compare(key, p.key);
            if (cmp > 0) {
                if (p.right != null) {
                    p = p.right;
                } else {
                    return p;
                }
            } else {
                if (p.left != null) {
                    p = p.left;
                } else {
                    Entry<K,V> parent = p.parent;
                    Entry<K,V> ch = p;
                    while (parent != null && ch == parent.left) {
                        ch = parent;
                        parent = parent.parent;
                    }
                    return parent;
                }
            }
        }
        return null;
    }

    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for the key, the old
     * value is replaced.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     *
     * @return the previous value associated with {@code key}, or
     *         {@code null} if there was no mapping for {@code key}.
     *         (A {@code null} return can also indicate that the map
     *         previously associated {@code null} with {@code key}.)
     * @throws ClassCastException if the specified key cannot be compared
     *         with the keys currently in the map
     * @throws NullPointerException if the specified key is null
     *         and this map uses natural ordering, or its comparator
     *         does not permit null keys
     */
    @Override
    public V put(K key, V value) {
        Entry<K,V> t = root;
        if (t == null) {
            compare(key, key); // type (and possibly null) check

            root = new Entry<>(key, value, null);
            size = 1;
            modCount++;
            return null;
        }
        int cmp;
        Entry<K,V> parent;
        // split comparator and comparable paths
        Comparator<? super K> cpr = comparator;
        if (cpr != null) {
            do {
                parent = t;
                cmp = cpr.compare(key, t.key);
                if (cmp < 0) {
                    t = t.left;
                } else if (cmp > 0) {
                    t = t.right;
                } else {
                    return t.setValue(value);
                }
            } while (t != null);
        }
        else {
            if (key == null) {
                throw new NullPointerException();
            }
            @SuppressWarnings("unchecked")
                Comparable<? super K> k = (Comparable<? super K>) key;
            do {
                parent = t;
                cmp = k.compareTo(t.key);
                if (cmp < 0) {
                    t = t.left;
                } else if (cmp > 0) {
                    t = t.right;
                } else {
                    return t.setValue(value);
                }
            } while (t != null);
        }
        Entry<K,V> e = new Entry<>(key, value, parent);
        if (cmp < 0) {
            parent.left = e;
        } else {
            parent.right = e;
        }
        fixAfterInsertion(e);
        size++;
        modCount++;
        return null;
    }

    /**
     * Removes the mapping for this key from this TreeMap if present.
     *
     * @param  key key for which mapping should be removed
     * @return the previous value associated with {@code key}, or
     *         {@code null} if there was no mapping for {@code key}.
     *         (A {@code null} return can also indicate that the map
     *         previously associated {@code null} with {@code key}.)
     * @throws ClassCastException if the specified key cannot be compared
     *         with the keys currently in the map
     * @throws NullPointerException if the specified key is null
     *         and this map uses natural ordering, or its comparator
     *         does not permit null keys
     */
    @Override
    public V remove(Object key) {
        Entry<K,V> p = getEntry(key);
        if (p == null) {
            return null;
        }

        V oldValue = p.value;
        deleteEntry(p);
        return oldValue;
    }

    /**
     * Removes all of the mappings from this map.
     * The map will be empty after this call returns.
     */
    @Override
    public void clear() {
        modCount++;
        size = 0;
        root = null;
    }

    /**
     * Returns a shallow copy of this {@code TreeMap} instance. (The keys and
     * values themselves are not cloned.)
     *
     * @return a shallow copy of this map
     */
    @Override
    public Object clone() {
        TreeMap<?,?> clone;
        try {
            clone = (TreeMap<?,?>) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }

        // Put clone into "virgin" state (except for comparator)
        clone.root = null;
        clone.size = 0;
        clone.modCount = 0;
        clone.entrySet = null;
        clone.navigableKeySet = null;
        clone.descendingMap = null;

        // Initialize clone with our mappings
        try {
            clone.buildFromSorted(size, entrySet().iterator(), null, null);
        } catch (java.io.IOException cannotHappen) {
        } catch (ClassNotFoundException cannotHappen) {
        }

        return clone;
    }

    // NavigableMap API methods

    /**
     * @since 1.6
     */
    @Override
    public Map.Entry<K,V> firstEntry() {
        return exportEntry(getFirstEntry());
    }

    /**
     * @since 1.6
     */
    @Override
    public Map.Entry<K,V> lastEntry() {
        return exportEntry(getLastEntry());
    }

    /**
     * @since 1.6
     */
    @Override
    public Map.Entry<K,V> pollFirstEntry() {
        Entry<K,V> p = getFirstEntry();
        Map.Entry<K,V> result = exportEntry(p);
        if (p != null) {
            deleteEntry(p);
        }
        return result;
    }

    /**
     * @since 1.6
     */
    @Override
    public Map.Entry<K,V> pollLastEntry() {
        Entry<K,V> p = getLastEntry();
        Map.Entry<K,V> result = exportEntry(p);
        if (p != null) {
            deleteEntry(p);
        }
        return result;
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException if the specified key is null
     *         and this map uses natural ordering, or its comparator
     *         does not permit null keys
     * @since 1.6
     */
    @Override
    public Map.Entry<K,V> lowerEntry(K key) {
        return exportEntry(getLowerEntry(key));
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException if the specified key is null
     *         and this map uses natural ordering, or its comparator
     *         does not permit null keys
     * @since 1.6
     */
    @Override
    public K lowerKey(K key) {
        return keyOrNull(getLowerEntry(key));
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException if the specified key is null
     *         and this map uses natural ordering, or its comparator
     *         does not permit null keys
     * @since 1.6
     */
    @Override
    public Map.Entry<K,V> floorEntry(K key) {
        return exportEntry(getFloorEntry(key));
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException if the specified key is null
     *         and this map uses natural ordering, or its comparator
     *         does not permit null keys
     * @since 1.6
     */
    @Override
    public K floorKey(K key) {
        return keyOrNull(getFloorEntry(key));
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException if the specified key is null
     *         and this map uses natural ordering, or its comparator
     *         does not permit null keys
     * @since 1.6
     */
    @Override
    public Map.Entry<K,V> ceilingEntry(K key) {
        return exportEntry(getCeilingEntry(key));
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException if the specified key is null
     *         and this map uses natural ordering, or its comparator
     *         does not permit null keys
     * @since 1.6
     */
    @Override
    public K ceilingKey(K key) {
        return keyOrNull(getCeilingEntry(key));
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException if the specified key is null
     *         and this map uses natural ordering, or its comparator
     *         does not permit null keys
     * @since 1.6
     */
    @Override
    public Map.Entry<K,V> higherEntry(K key) {
        return exportEntry(getHigherEntry(key));
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException if the specified key is null
     *         and this map uses natural ordering, or its comparator
     *         does not permit null keys
     * @since 1.6
     */
    @Override
    public K higherKey(K key) {
        return keyOrNull(getHigherEntry(key));
    }

    // Views

    /**
     * Fields initialized to contain an instance of the entry set view
     * the first time this view is requested.  Views are stateless, so
     * there's no reason to create more than one.
     */
    private transient EntrySet entrySet;
    private transient KeySet<K> navigableKeySet;
    private transient NavigableMap<K,V> descendingMap;

    /**
     * Returns a {@link Set} view of the keys contained in this map.
     *
     * <p>The set's iterator returns the keys in ascending order.
     * The set's spliterator is
     * <em><a href="Spliterator.html#binding">late-binding</a></em>,
     * <em>fail-fast</em>, and additionally reports {@link Spliterator#SORTED}
     * and {@link Spliterator#ORDERED} with an encounter order that is ascending
     * key order.  The spliterator's comparator (see
     * {@link Spliterator#getComparator()}) is {@code null} if
     * the tree map's comparator (see {@link #comparator()}) is {@code null}.
     * Otherwise, the spliterator's comparator is the same as or imposes the
     * same total ordering as the tree map's comparator.
     *
     * <p>The set is backed by the map, so changes to the map are
     * reflected in the set, and vice-versa.  If the map is modified
     * while an iteration over the set is in progress (except through
     * the iterator's own {@code remove} operation), the results of
     * the iteration are undefined.  The set supports element removal,
     * which removes the corresponding mapping from the map, via the
     * {@code Iterator.remove}, {@code Set.remove},
     * {@code removeAll}, {@code retainAll}, and {@code clear}
     * operations.  It does not support the {@code add} or {@code addAll}
     * operations.
     */
    @Override
    public Set<K> keySet() {
        return navigableKeySet();
    }

    /**
     * @since 1.6
     */
    @Override
    public NavigableSet<K> navigableKeySet() {
        KeySet<K> nks = navigableKeySet;
        return (nks != null) ? nks : (navigableKeySet = new KeySet<>(this));
    }

    /**
     * @since 1.6
     */
    @Override
    public NavigableSet<K> descendingKeySet() {
        return descendingMap().navigableKeySet();
    }

    /**
     * Returns a {@link Collection} view of the values contained in this map.
     *
     * <p>The collection's iterator returns the values in ascending order
     * of the corresponding keys. The collection's spliterator is
     * <em><a href="Spliterator.html#binding">late-binding</a></em>,
     * <em>fail-fast</em>, and additionally reports {@link Spliterator#ORDERED}
     * with an encounter order that is ascending order of the corresponding
     * keys.
     *
     * <p>The collection is backed by the map, so changes to the map are
     * reflected in the collection, and vice-versa.  If the map is
     * modified while an iteration over the collection is in progress
     * (except through the iterator's own {@code remove} operation),
     * the results of the iteration are undefined.  The collection
     * supports element removal, which removes the corresponding
     * mapping from the map, via the {@code Iterator.remove},
     * {@code Collection.remove}, {@code removeAll},
     * {@code retainAll} and {@code clear} operations.  It does not
     * support the {@code add} or {@code addAll} operations.
     */
    @Override
    public Collection<V> values() {
        Collection<V> vs = values;
        if (vs == null) {
            vs = new Values();
            values = vs;
        }
        return vs;
    }

    /**
     * Returns a {@link Set} view of the mappings contained in this map.
     *
     * <p>The set's iterator returns the entries in ascending key order. The
     * sets's spliterator is
     * <em><a href="Spliterator.html#binding">late-binding</a></em>,
     * <em>fail-fast</em>, and additionally reports {@link Spliterator#SORTED} and
     * {@link Spliterator#ORDERED} with an encounter order that is ascending key
     * order.
     *
     * <p>The set is backed by the map, so changes to the map are
     * reflected in the set, and vice-versa.  If the map is modified
     * while an iteration over the set is in progress (except through
     * the iterator's own {@code remove} operation, or through the
     * {@code setValue} operation on a map entry returned by the
     * iterator) the results of the iteration are undefined.  The set
     * supports element removal, which removes the corresponding
     * mapping from the map, via the {@code Iterator.remove},
     * {@code Set.remove}, {@code removeAll}, {@code retainAll} and
     * {@code clear} operations.  It does not support the
     * {@code add} or {@code addAll} operations.
     */
    @Override
    public Set<Map.Entry<K,V>> entrySet() {
        EntrySet es = entrySet;
        return (es != null) ? es : (entrySet = new EntrySet());
    }

    /**
     * @since 1.6
     */
    @Override
    public NavigableMap<K, V> descendingMap() {
        NavigableMap<K, V> km = descendingMap;
        return (km != null) ? km :
            (descendingMap = new DescendingSubMap<>(this,
                                                    true, null, true,
                                                    true, null, true));
    }

    /**
     * @throws ClassCastException       {@inheritDoc}
     * @throws NullPointerException if {@code fromKey} or {@code toKey} is
     *         null and this map uses natural ordering, or its comparator
     *         does not permit null keys
     * @throws IllegalArgumentException {@inheritDoc}
     * @since 1.6
     */
    @Override
    public NavigableMap<K,V> subMap(K fromKey, boolean fromInclusive,
                                    K toKey, boolean toInclusive) {
        return new AscendingSubMap<>(this,
                                     false, fromKey, fromInclusive,
                                     false, toKey,   toInclusive);
    }

    /**
     * @throws ClassCastException       {@inheritDoc}
     * @throws NullPointerException if {@code toKey} is null
     *         and this map uses natural ordering, or its comparator
     *         does not permit null keys
     * @throws IllegalArgumentException {@inheritDoc}
     * @since 1.6
     */
    @Override
    public NavigableMap<K,V> headMap(K toKey, boolean inclusive) {
        return new AscendingSubMap<>(this,
                                     true,  null,  true,
                                     false, toKey, inclusive);
    }

    /**
     * @throws ClassCastException       {@inheritDoc}
     * @throws NullPointerException if {@code fromKey} is null
     *         and this map uses natural ordering, or its comparator
     *         does not permit null keys
     * @throws IllegalArgumentException {@inheritDoc}
     * @since 1.6
     */
    @Override
    public NavigableMap<K,V> tailMap(K fromKey, boolean inclusive) {
        return new AscendingSubMap<>(this,
                                     false, fromKey, inclusive,
                                     true,  null,    true);
    }

    /**
     * @throws ClassCastException       {@inheritDoc}
     * @throws NullPointerException if {@code fromKey} or {@code toKey} is
     *         null and this map uses natural ordering, or its comparator
     *         does not permit null keys
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public SortedMap<K,V> subMap(K fromKey, K toKey) {
        return subMap(fromKey, true, toKey, false);
    }

    /**
     * @throws ClassCastException       {@inheritDoc}
     * @throws NullPointerException if {@code toKey} is null
     *         and this map uses natural ordering, or its comparator
     *         does not permit null keys
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public SortedMap<K,V> headMap(K toKey) {
        return headMap(toKey, false);
    }

    /**
     * @throws ClassCastException       {@inheritDoc}
     * @throws NullPointerException if {@code fromKey} is null
     *         and this map uses natural ordering, or its comparator
     *         does not permit null keys
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public SortedMap<K,V> tailMap(K fromKey) {
        return tailMap(fromKey, true);
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        Entry<K,V> p = getEntry(key);
        if (p!=null && Objects.equals(oldValue, p.value)) {
            p.value = newValue;
            return true;
        }
        return false;
    }

    @Override
    public V replace(K key, V value) {
        Entry<K,V> p = getEntry(key);
        if (p!=null) {
            V oldValue = p.value;
            p.value = value;
            return oldValue;
        }
        return null;
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        Objects.requireNonNull(action);
        int expectedModCount = modCount;
        for (Entry<K, V> e = getFirstEntry(); e != null; e = successor(e)) {
            action.accept(e.key, e.value);

            if (expectedModCount != modCount) {
                throw new ConcurrentModificationException();
            }
        }
    }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        Objects.requireNonNull(function);
        int expectedModCount = modCount;

        for (Entry<K, V> e = getFirstEntry(); e != null; e = successor(e)) {
            e.value = function.apply(e.key, e.value);

            if (expectedModCount != modCount) {
                throw new ConcurrentModificationException();
            }
        }
    }

    // View class support

    class Values extends AbstractCollection<V> {
        @Override
        public Iterator<V> iterator() {
            return new ValueIterator(getFirstEntry());
        }

        @Override
        public int size() {
            return TreeMap.this.size();
        }

        @Override
        public boolean contains(Object o) {
            return TreeMap.this.containsValue(o);
        }

        @Override
        public boolean remove(Object o) {
            for (Entry<K,V> e = getFirstEntry(); e != null; e = successor(e)) {
                if (valEquals(e.getValue(), o)) {
                    deleteEntry(e);
                    return true;
                }
            }
            return false;
        }

        @Override
        public void clear() {
            TreeMap.this.clear();
        }

        @Override
        public Spliterator<V> spliterator() {
            return new ValueSpliterator<K,V>(TreeMap.this, null, null, 0, -1, 0);
        }
    }

    class EntrySet extends AbstractSet<Map.Entry<K,V>> {
        @Override
        public Iterator<Map.Entry<K,V>> iterator() {
            return new EntryIterator(getFirstEntry());
        }

        @Override
        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            Map.Entry<?,?> entry = (Map.Entry<?,?>) o;
            Object value = entry.getValue();
            Entry<K,V> p = getEntry(entry.getKey());
            return p != null && valEquals(p.getValue(), value);
        }

        @Override
        public boolean remove(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            Map.Entry<?,?> entry = (Map.Entry<?,?>) o;
            Object value = entry.getValue();
            Entry<K,V> p = getEntry(entry.getKey());
            if (p != null && valEquals(p.getValue(), value)) {
                deleteEntry(p);
                return true;
            }
            return false;
        }

        @Override
        public int size() {
            return TreeMap.this.size();
        }

        @Override
        public void clear() {
            TreeMap.this.clear();
        }

        @Override
        public Spliterator<Map.Entry<K,V>> spliterator() {
            return new EntrySpliterator<K,V>(TreeMap.this, null, null, 0, -1, 0);
        }
    }

    /*
     * Unlike Values and EntrySet, the KeySet class is static,
     * delegating to a NavigableMap to allow use by SubMaps, which
     * outweighs the ugliness of needing type-tests for the following
     * Iterator methods that are defined appropriately in main versus
     * submap classes.
     */

    Iterator<K> keyIterator() {
        return new KeyIterator(getFirstEntry());
    }

    Iterator<K> descendingKeyIterator() {
        return new DescendingKeyIterator(getLastEntry());
    }

    static final class KeySet<E> extends AbstractSet<E> implements NavigableSet<E> {
        private final NavigableMap<E, ?> m;
        KeySet(NavigableMap<E,?> map) { m = map; }

        @Override
        public Iterator<E> iterator() {
            if (m instanceof TreeMap) {
                return ((TreeMap<E,?>)m).keyIterator();
            } else {
                return ((NavigableSubMap<E,?>)m).keyIterator();
            }
        }

        @Override
        public Iterator<E> descendingIterator() {
            if (m instanceof TreeMap) {
                return ((TreeMap<E,?>)m).descendingKeyIterator();
            } else {
                return ((NavigableSubMap<E,?>)m).descendingKeyIterator();
            }
        }

        @Override
        public int size() { return m.size(); }
        @Override
        public boolean isEmpty() { return m.isEmpty(); }
        @Override
        public boolean contains(Object o) { return m.containsKey(o); }
        @Override
        public void clear() { m.clear(); }
        @Override
        public E lower(E e) { return m.lowerKey(e); }
        @Override
        public E floor(E e) { return m.floorKey(e); }
        @Override
        public E ceiling(E e) { return m.ceilingKey(e); }
        @Override
        public E higher(E e) { return m.higherKey(e); }
        @Override
        public E first() { return m.firstKey(); }
        @Override
        public E last() { return m.lastKey(); }
        @Override
        public Comparator<? super E> comparator() { return m.comparator(); }
        @Override
        public E pollFirst() {
            Map.Entry<E,?> e = m.pollFirstEntry();
            return (e == null) ? null : e.getKey();
        }
        @Override
        public E pollLast() {
            Map.Entry<E,?> e = m.pollLastEntry();
            return (e == null) ? null : e.getKey();
        }
        @Override
        public boolean remove(Object o) {
            int oldSize = size();
            m.remove(o);
            return size() != oldSize;
        }
        @Override
        public NavigableSet<E> subSet(E fromElement, boolean fromInclusive,
                                      E toElement, boolean toInclusive) {
            return new KeySet<>(m.subMap(fromElement, fromInclusive,
                                          toElement,   toInclusive));
        }
        @Override
        public NavigableSet<E> headSet(E toElement, boolean inclusive) {
            return new KeySet<>(m.headMap(toElement, inclusive));
        }
        @Override
        public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
            return new KeySet<>(m.tailMap(fromElement, inclusive));
        }
        @Override
        public SortedSet<E> subSet(E fromElement, E toElement) {
            return subSet(fromElement, true, toElement, false);
        }
        @Override
        public SortedSet<E> headSet(E toElement) {
            return headSet(toElement, false);
        }
        @Override
        public SortedSet<E> tailSet(E fromElement) {
            return tailSet(fromElement, true);
        }
        @Override
        public NavigableSet<E> descendingSet() {
            return new KeySet<>(m.descendingMap());
        }

        @Override
        public Spliterator<E> spliterator() {
            return keySpliteratorFor(m);
        }
    }

    /**
     * Base class for TreeMap Iterators
     */
    abstract class PrivateEntryIterator<T> implements Iterator<T> {
        Entry<K,V> next;
        Entry<K,V> lastReturned;
        int expectedModCount;

        PrivateEntryIterator(Entry<K,V> first) {
            expectedModCount = modCount;
            lastReturned = null;
            next = first;
        }

        @Override
        public final boolean hasNext() {
            return next != null;
        }

        final Entry<K,V> nextEntry() {
            Entry<K,V> e = next;
            if (e == null) {
                throw new NoSuchElementException();
            }
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            next = successor(e);
            lastReturned = e;
            return e;
        }

        final Entry<K,V> prevEntry() {
            Entry<K,V> e = next;
            if (e == null) {
                throw new NoSuchElementException();
            }
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            next = predecessor(e);
            lastReturned = e;
            return e;
        }

        @Override
        public void remove() {
            if (lastReturned == null) {
                throw new IllegalStateException();
            }
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            // deleted entries are replaced by their successors
            if (lastReturned.left != null && lastReturned.right != null) {
                next = lastReturned;
            }
            deleteEntry(lastReturned);
            expectedModCount = modCount;
            lastReturned = null;
        }
    }

    final class EntryIterator extends PrivateEntryIterator<Map.Entry<K,V>> {
        EntryIterator(Entry<K,V> first) {
            super(first);
        }
        @Override
        public Map.Entry<K,V> next() {
            return nextEntry();
        }
    }

    final class ValueIterator extends PrivateEntryIterator<V> {
        ValueIterator(Entry<K,V> first) {
            super(first);
        }
        @Override
        public V next() {
            return nextEntry().value;
        }
    }

    final class KeyIterator extends PrivateEntryIterator<K> {
        KeyIterator(Entry<K,V> first) {
            super(first);
        }
        @Override
        public K next() {
            return nextEntry().key;
        }
    }

    final class DescendingKeyIterator extends PrivateEntryIterator<K> {
        DescendingKeyIterator(Entry<K,V> first) {
            super(first);
        }
        @Override
        public K next() {
            return prevEntry().key;
        }
        @Override
        public void remove() {
            if (lastReturned == null) {
                throw new IllegalStateException();
            }
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            deleteEntry(lastReturned);
            lastReturned = null;
            expectedModCount = modCount;
        }
    }

    // Little utilities

    /**
     * Compares two keys using the correct comparison method for this TreeMap.
     */
    @SuppressWarnings("unchecked")
    final int compare(Object k1, Object k2) {
        return comparator==null ? ((Comparable<? super K>)k1).compareTo((K)k2)
            : comparator.compare((K)k1, (K)k2);
    }

    /**
     * 判断两个值是否相等。
     * 与 equals 的不同之处在于，valEquals 可以处理 o1 为 null 的情况。
     *
     * @param o1 o1.
     * @param o2 o2.
     * @return o1 等于 o2 返回 true。
     */
    static final boolean valEquals(Object o1, Object o2) {
        return (o1==null ? o2==null : o1.equals(o2));
    }

    /**
     * Return SimpleImmutableEntry for entry, or null if null
     */
    static <K,V> Map.Entry<K,V> exportEntry(Entry<K,V> e) {
        return (e == null) ? null :
            new SimpleImmutableEntry<>(e);
    }

    /**
     * Return key for entry, or null if null
     */
    static <K,V> K keyOrNull(Entry<K,V> e) {
        return (e == null) ? null : e.key;
    }

    /**
     * 返回指定 Entry 的 key。
     *
     * @param e 指定的 Entry。
     * @param <K> Entry.key 对应的类型。
     * @return 指定 Entry 的 key。
     *
     * @throws NoSuchElementException 如果 Entry 为空。
     */
    static <K> K key(Entry<K,?> e) {
        if (e==null) {
            throw new NoSuchElementException();
        }
        return e.key;
    }


    // SubMaps

    /**
     * Dummy value serving as unmatchable fence key for unbounded
     * SubMapIterators
     */
    private static final Object UNBOUNDED = new Object();

    /**
     * @serial include
     */
    abstract static class NavigableSubMap<K,V> extends AbstractMap<K,V>
        implements NavigableMap<K,V>, Serializable {
        private static final long serialVersionUID = -2102997345730753016L;
        /**
         * The backing map.
         */
        final TreeMap<K,V> m;

        /**
         * Endpoints are represented as triples (fromStart, lo,
         * loInclusive) and (toEnd, hi, hiInclusive). If fromStart is
         * true, then the low (absolute) bound is the start of the
         * backing map, and the other values are ignored. Otherwise,
         * if loInclusive is true, lo is the inclusive bound, else lo
         * is the exclusive bound. Similarly for the upper bound.
         */
        final K lo, hi;
        final boolean fromStart, toEnd;
        final boolean loInclusive, hiInclusive;

        NavigableSubMap(TreeMap<K,V> m,
                        boolean fromStart, K lo, boolean loInclusive,
                        boolean toEnd,     K hi, boolean hiInclusive) {
            if (!fromStart && !toEnd) {
                if (m.compare(lo, hi) > 0) {
                    throw new IllegalArgumentException("fromKey > toKey");
                }
            } else {
                if (!fromStart) {
                    m.compare(lo, lo); // type check
                }
                if (!toEnd) {
                    m.compare(hi, hi);
                }
            }

            this.m = m;
            this.fromStart = fromStart;
            this.lo = lo;
            this.loInclusive = loInclusive;
            this.toEnd = toEnd;
            this.hi = hi;
            this.hiInclusive = hiInclusive;
        }

        // internal utilities

        final boolean tooLow(Object key) {
            if (!fromStart) {
                int c = m.compare(key, lo);
                return c < 0 || (c == 0 && !loInclusive);
            }
            return false;
        }

        final boolean tooHigh(Object key) {
            if (!toEnd) {
                int c = m.compare(key, hi);
                return c > 0 || (c == 0 && !hiInclusive);
            }
            return false;
        }

        final boolean inRange(Object key) {
            return !tooLow(key) && !tooHigh(key);
        }

        final boolean inClosedRange(Object key) {
            return (fromStart || m.compare(key, lo) >= 0)
                && (toEnd || m.compare(hi, key) >= 0);
        }

        final boolean inRange(Object key, boolean inclusive) {
            return inclusive ? inRange(key) : inClosedRange(key);
        }

        /*
         * Absolute versions of relation operations.
         * Subclasses map to these using like-named "sub"
         * versions that invert senses for descending maps
         */

        final TreeMap.Entry<K,V> absLowest() {
            TreeMap.Entry<K,V> e =
                (fromStart ?  m.getFirstEntry() :
                 (loInclusive ? m.getCeilingEntry(lo) :
                                m.getHigherEntry(lo)));
            return (e == null || tooHigh(e.key)) ? null : e;
        }

        final TreeMap.Entry<K,V> absHighest() {
            TreeMap.Entry<K,V> e =
                (toEnd ?  m.getLastEntry() :
                 (hiInclusive ?  m.getFloorEntry(hi) :
                                 m.getLowerEntry(hi)));
            return (e == null || tooLow(e.key)) ? null : e;
        }

        final TreeMap.Entry<K,V> absCeiling(K key) {
            if (tooLow(key)) {
                return absLowest();
            }
            TreeMap.Entry<K,V> e = m.getCeilingEntry(key);
            return (e == null || tooHigh(e.key)) ? null : e;
        }

        final TreeMap.Entry<K,V> absHigher(K key) {
            if (tooLow(key)) {
                return absLowest();
            }
            TreeMap.Entry<K,V> e = m.getHigherEntry(key);
            return (e == null || tooHigh(e.key)) ? null : e;
        }

        final TreeMap.Entry<K,V> absFloor(K key) {
            if (tooHigh(key)) {
                return absHighest();
            }
            TreeMap.Entry<K,V> e = m.getFloorEntry(key);
            return (e == null || tooLow(e.key)) ? null : e;
        }

        final TreeMap.Entry<K,V> absLower(K key) {
            if (tooHigh(key)) {
                return absHighest();
            }
            TreeMap.Entry<K,V> e = m.getLowerEntry(key);
            return (e == null || tooLow(e.key)) ? null : e;
        }

        /** Returns the absolute high fence for ascending traversal */
        final TreeMap.Entry<K,V> absHighFence() {
            return (toEnd ? null : (hiInclusive ?
                                    m.getHigherEntry(hi) :
                                    m.getCeilingEntry(hi)));
        }

        /** Return the absolute low fence for descending traversal  */
        final TreeMap.Entry<K,V> absLowFence() {
            return (fromStart ? null : (loInclusive ?
                                        m.getLowerEntry(lo) :
                                        m.getFloorEntry(lo)));
        }

        // Abstract methods defined in ascending vs descending classes
        // These relay to the appropriate absolute versions

        abstract TreeMap.Entry<K,V> subLowest();
        abstract TreeMap.Entry<K,V> subHighest();
        abstract TreeMap.Entry<K,V> subCeiling(K key);
        abstract TreeMap.Entry<K,V> subHigher(K key);
        abstract TreeMap.Entry<K,V> subFloor(K key);
        abstract TreeMap.Entry<K,V> subLower(K key);

        /** Returns ascending iterator from the perspective of this submap */
        abstract Iterator<K> keyIterator();

        abstract Spliterator<K> keySpliterator();

        /** Returns descending iterator from the perspective of this submap */
        abstract Iterator<K> descendingKeyIterator();

        // public methods

        @Override
        public boolean isEmpty() {
            return (fromStart && toEnd) ? m.isEmpty() : entrySet().isEmpty();
        }

        @Override
        public int size() {
            return (fromStart && toEnd) ? m.size() : entrySet().size();
        }

        @Override
        public final boolean containsKey(Object key) {
            return inRange(key) && m.containsKey(key);
        }

        @Override
        public final V put(K key, V value) {
            if (!inRange(key)) {
                throw new IllegalArgumentException("key out of range");
            }
            return m.put(key, value);
        }

        @Override
        public final V get(Object key) {
            return !inRange(key) ? null :  m.get(key);
        }

        @Override
        public final V remove(Object key) {
            return !inRange(key) ? null : m.remove(key);
        }

        @Override
        public final Entry<K,V> ceilingEntry(K key) {
            return exportEntry(subCeiling(key));
        }

        @Override
        public final K ceilingKey(K key) {
            return keyOrNull(subCeiling(key));
        }

        @Override
        public final Entry<K,V> higherEntry(K key) {
            return exportEntry(subHigher(key));
        }

        @Override
        public final K higherKey(K key) {
            return keyOrNull(subHigher(key));
        }

        @Override
        public final Entry<K,V> floorEntry(K key) {
            return exportEntry(subFloor(key));
        }

        @Override
        public final K floorKey(K key) {
            return keyOrNull(subFloor(key));
        }

        @Override
        public final Entry<K,V> lowerEntry(K key) {
            return exportEntry(subLower(key));
        }

        @Override
        public final K lowerKey(K key) {
            return keyOrNull(subLower(key));
        }

        @Override
        public final K firstKey() {
            return key(subLowest());
        }

        @Override
        public final K lastKey() {
            return key(subHighest());
        }

        @Override
        public final Entry<K,V> firstEntry() {
            return exportEntry(subLowest());
        }

        @Override
        public final Entry<K,V> lastEntry() {
            return exportEntry(subHighest());
        }

        @Override
        public final Entry<K,V> pollFirstEntry() {
            TreeMap.Entry<K,V> e = subLowest();
            Entry<K,V> result = exportEntry(e);
            if (e != null) {
                m.deleteEntry(e);
            }
            return result;
        }

        @Override
        public final Entry<K,V> pollLastEntry() {
            TreeMap.Entry<K,V> e = subHighest();
            Entry<K,V> result = exportEntry(e);
            if (e != null) {
                m.deleteEntry(e);
            }
            return result;
        }

        // Views
        transient NavigableMap<K,V> descendingMapView;
        transient EntrySetView entrySetView;
        transient KeySet<K> navigableKeySetView;

        @Override
        public final NavigableSet<K> navigableKeySet() {
            KeySet<K> nksv = navigableKeySetView;
            return (nksv != null) ? nksv :
                (navigableKeySetView = new KeySet<>(this));
        }

        @Override
        public final Set<K> keySet() {
            return navigableKeySet();
        }

        @Override
        public NavigableSet<K> descendingKeySet() {
            return descendingMap().navigableKeySet();
        }

        @Override
        public final SortedMap<K,V> subMap(K fromKey, K toKey) {
            return subMap(fromKey, true, toKey, false);
        }

        @Override
        public final SortedMap<K,V> headMap(K toKey) {
            return headMap(toKey, false);
        }

        @Override
        public final SortedMap<K,V> tailMap(K fromKey) {
            return tailMap(fromKey, true);
        }

        // View classes

        abstract class EntrySetView extends AbstractSet<Entry<K,V>> {
            private transient int size = -1, sizeModCount;

            @Override
            public int size() {
                if (fromStart && toEnd) {
                    return m.size();
                }
                if (size == -1 || sizeModCount != m.modCount) {
                    sizeModCount = m.modCount;
                    size = 0;
                    Iterator<?> i = iterator();
                    while (i.hasNext()) {
                        size++;
                        i.next();
                    }
                }
                return size;
            }

            @Override
            public boolean isEmpty() {
                TreeMap.Entry<K,V> n = absLowest();
                return n == null || tooHigh(n.key);
            }

            @Override
            public boolean contains(Object o) {
                if (!(o instanceof Map.Entry)) {
                    return false;
                }
                Entry<?,?> entry = (Entry<?,?>) o;
                Object key = entry.getKey();
                if (!inRange(key)) {
                    return false;
                }
                TreeMap.Entry<?,?> node = m.getEntry(key);
                return node != null &&
                    valEquals(node.getValue(), entry.getValue());
            }

            @Override
            public boolean remove(Object o) {
                if (!(o instanceof Map.Entry)) {
                    return false;
                }
                Entry<?,?> entry = (Entry<?,?>) o;
                Object key = entry.getKey();
                if (!inRange(key)) {
                    return false;
                }
                TreeMap.Entry<K,V> node = m.getEntry(key);
                if (node!=null && valEquals(node.getValue(),
                                            entry.getValue())) {
                    m.deleteEntry(node);
                    return true;
                }
                return false;
            }
        }

        /**
         * Iterators for SubMaps
         */
        abstract class SubMapIterator<T> implements Iterator<T> {
            TreeMap.Entry<K,V> lastReturned;
            TreeMap.Entry<K,V> next;
            final Object fenceKey;
            int expectedModCount;

            SubMapIterator(TreeMap.Entry<K,V> first,
                           TreeMap.Entry<K,V> fence) {
                expectedModCount = m.modCount;
                lastReturned = null;
                next = first;
                fenceKey = fence == null ? UNBOUNDED : fence.key;
            }

            @Override
            public final boolean hasNext() {
                return next != null && next.key != fenceKey;
            }

            final TreeMap.Entry<K,V> nextEntry() {
                TreeMap.Entry<K,V> e = next;
                if (e == null || e.key == fenceKey) {
                    throw new NoSuchElementException();
                }
                if (m.modCount != expectedModCount) {
                    throw new ConcurrentModificationException();
                }
                next = successor(e);
                lastReturned = e;
                return e;
            }

            final TreeMap.Entry<K,V> prevEntry() {
                TreeMap.Entry<K,V> e = next;
                if (e == null || e.key == fenceKey) {
                    throw new NoSuchElementException();
                }
                if (m.modCount != expectedModCount) {
                    throw new ConcurrentModificationException();
                }
                next = predecessor(e);
                lastReturned = e;
                return e;
            }

            final void removeAscending() {
                if (lastReturned == null) {
                    throw new IllegalStateException();
                }
                if (m.modCount != expectedModCount) {
                    throw new ConcurrentModificationException();
                }
                // deleted entries are replaced by their successors
                if (lastReturned.left != null && lastReturned.right != null) {
                    next = lastReturned;
                }
                m.deleteEntry(lastReturned);
                lastReturned = null;
                expectedModCount = m.modCount;
            }

            final void removeDescending() {
                if (lastReturned == null) {
                    throw new IllegalStateException();
                }
                if (m.modCount != expectedModCount) {
                    throw new ConcurrentModificationException();
                }
                m.deleteEntry(lastReturned);
                lastReturned = null;
                expectedModCount = m.modCount;
            }

        }

        final class SubMapEntryIterator extends SubMapIterator<Entry<K,V>> {
            SubMapEntryIterator(TreeMap.Entry<K,V> first,
                                TreeMap.Entry<K,V> fence) {
                super(first, fence);
            }
            @Override
            public Entry<K,V> next() {
                return nextEntry();
            }
            @Override
            public void remove() {
                removeAscending();
            }
        }

        final class DescendingSubMapEntryIterator extends SubMapIterator<Entry<K,V>> {
            DescendingSubMapEntryIterator(TreeMap.Entry<K,V> last,
                                          TreeMap.Entry<K,V> fence) {
                super(last, fence);
            }

            @Override
            public Entry<K,V> next() {
                return prevEntry();
            }
            @Override
            public void remove() {
                removeDescending();
            }
        }

        // Implement minimal Spliterator as KeySpliterator backup
        final class SubMapKeyIterator extends SubMapIterator<K>
            implements Spliterator<K> {
            SubMapKeyIterator(TreeMap.Entry<K,V> first,
                              TreeMap.Entry<K,V> fence) {
                super(first, fence);
            }
            @Override
            public K next() {
                return nextEntry().key;
            }
            @Override
            public void remove() {
                removeAscending();
            }
            @Override
            public Spliterator<K> trySplit() {
                return null;
            }
            @Override
            public void forEachRemaining(Consumer<? super K> action) {
                while (hasNext()) {
                    action.accept(next());
                }
            }
            @Override
            public boolean tryAdvance(Consumer<? super K> action) {
                if (hasNext()) {
                    action.accept(next());
                    return true;
                }
                return false;
            }
            @Override
            public long estimateSize() {
                return Long.MAX_VALUE;
            }
            @Override
            public int characteristics() {
                return Spliterator.DISTINCT | Spliterator.ORDERED |
                    Spliterator.SORTED;
            }
            @Override
            public final Comparator<? super K>  getComparator() {
                return NavigableSubMap.this.comparator();
            }
        }

        final class DescendingSubMapKeyIterator extends SubMapIterator<K>
            implements Spliterator<K> {
            DescendingSubMapKeyIterator(TreeMap.Entry<K,V> last,
                                        TreeMap.Entry<K,V> fence) {
                super(last, fence);
            }
            @Override
            public K next() {
                return prevEntry().key;
            }
            @Override
            public void remove() {
                removeDescending();
            }
            @Override
            public Spliterator<K> trySplit() {
                return null;
            }
            @Override
            public void forEachRemaining(Consumer<? super K> action) {
                while (hasNext()) {
                    action.accept(next());
                }
            }
            @Override
            public boolean tryAdvance(Consumer<? super K> action) {
                if (hasNext()) {
                    action.accept(next());
                    return true;
                }
                return false;
            }
            @Override
            public long estimateSize() {
                return Long.MAX_VALUE;
            }
            @Override
            public int characteristics() {
                return Spliterator.DISTINCT | Spliterator.ORDERED;
            }
        }
    }

    /**
     * @serial include
     */
    static final class AscendingSubMap<K,V> extends NavigableSubMap<K,V> {
        private static final long serialVersionUID = 912986545866124060L;

        AscendingSubMap(TreeMap<K,V> m,
                        boolean fromStart, K lo, boolean loInclusive,
                        boolean toEnd,     K hi, boolean hiInclusive) {
            super(m, fromStart, lo, loInclusive, toEnd, hi, hiInclusive);
        }

        @Override
        public Comparator<? super K> comparator() {
            return m.comparator();
        }

        @Override
        public NavigableMap<K,V> subMap(K fromKey, boolean fromInclusive,
                                        K toKey, boolean toInclusive) {
            if (!inRange(fromKey, fromInclusive)) {
                throw new IllegalArgumentException("fromKey out of range");
            }
            if (!inRange(toKey, toInclusive)) {
                throw new IllegalArgumentException("toKey out of range");
            }
            return new AscendingSubMap<>(m,
                                         false, fromKey, fromInclusive,
                                         false, toKey,   toInclusive);
        }

        @Override
        public NavigableMap<K,V> headMap(K toKey, boolean inclusive) {
            if (!inRange(toKey, inclusive)) {
                throw new IllegalArgumentException("toKey out of range");
            }
            return new AscendingSubMap<>(m,
                                         fromStart, lo,    loInclusive,
                                         false,     toKey, inclusive);
        }

        @Override
        public NavigableMap<K,V> tailMap(K fromKey, boolean inclusive) {
            if (!inRange(fromKey, inclusive)) {
                throw new IllegalArgumentException("fromKey out of range");
            }
            return new AscendingSubMap<>(m,
                                         false, fromKey, inclusive,
                                         toEnd, hi,      hiInclusive);
        }

        @Override
        public NavigableMap<K,V> descendingMap() {
            NavigableMap<K,V> mv = descendingMapView;
            return (mv != null) ? mv :
                (descendingMapView =
                 new DescendingSubMap<>(m,
                                        fromStart, lo, loInclusive,
                                        toEnd,     hi, hiInclusive));
        }

        @Override
        Iterator<K> keyIterator() {
            return new SubMapKeyIterator(absLowest(), absHighFence());
        }

        @Override
        Spliterator<K> keySpliterator() {
            return new SubMapKeyIterator(absLowest(), absHighFence());
        }

        @Override
        Iterator<K> descendingKeyIterator() {
            return new DescendingSubMapKeyIterator(absHighest(), absLowFence());
        }

        final class AscendingEntrySetView extends EntrySetView {
            @Override
            public Iterator<Entry<K,V>> iterator() {
                return new SubMapEntryIterator(absLowest(), absHighFence());
            }
        }

        @Override
        public Set<Entry<K,V>> entrySet() {
            NavigableSubMap.EntrySetView es = entrySetView;
            return (es != null) ? es : (entrySetView = new AscendingEntrySetView());
        }

        @Override
        TreeMap.Entry<K,V> subLowest()       { return absLowest(); }
        @Override
        TreeMap.Entry<K,V> subHighest()      { return absHighest(); }
        @Override
        TreeMap.Entry<K,V> subCeiling(K key) { return absCeiling(key); }
        @Override
        TreeMap.Entry<K,V> subHigher(K key)  { return absHigher(key); }
        @Override
        TreeMap.Entry<K,V> subFloor(K key)   { return absFloor(key); }
        @Override
        TreeMap.Entry<K,V> subLower(K key)   { return absLower(key); }
    }

    /**
     * @serial include
     */
    static final class DescendingSubMap<K,V>  extends NavigableSubMap<K,V> {
        private static final long serialVersionUID = 912986545866120460L;
        DescendingSubMap(TreeMap<K,V> m,
                        boolean fromStart, K lo, boolean loInclusive,
                        boolean toEnd,     K hi, boolean hiInclusive) {
            super(m, fromStart, lo, loInclusive, toEnd, hi, hiInclusive);
        }

        private final Comparator<? super K> reverseComparator =
            Collections.reverseOrder(m.comparator);

        @Override
        public Comparator<? super K> comparator() {
            return reverseComparator;
        }

        @Override
        public NavigableMap<K,V> subMap(K fromKey, boolean fromInclusive,
                                        K toKey, boolean toInclusive) {
            if (!inRange(fromKey, fromInclusive)) {
                throw new IllegalArgumentException("fromKey out of range");
            }
            if (!inRange(toKey, toInclusive)) {
                throw new IllegalArgumentException("toKey out of range");
            }
            return new DescendingSubMap<>(m,
                                          false, toKey,   toInclusive,
                                          false, fromKey, fromInclusive);
        }

        @Override
        public NavigableMap<K,V> headMap(K toKey, boolean inclusive) {
            if (!inRange(toKey, inclusive)) {
                throw new IllegalArgumentException("toKey out of range");
            }
            return new DescendingSubMap<>(m,
                                          false, toKey, inclusive,
                                          toEnd, hi,    hiInclusive);
        }

        @Override
        public NavigableMap<K,V> tailMap(K fromKey, boolean inclusive) {
            if (!inRange(fromKey, inclusive)) {
                throw new IllegalArgumentException("fromKey out of range");
            }
            return new DescendingSubMap<>(m,
                                          fromStart, lo, loInclusive,
                                          false, fromKey, inclusive);
        }

        @Override
        public NavigableMap<K,V> descendingMap() {
            NavigableMap<K,V> mv = descendingMapView;
            return (mv != null) ? mv :
                (descendingMapView =
                 new AscendingSubMap<>(m,
                                       fromStart, lo, loInclusive,
                                       toEnd,     hi, hiInclusive));
        }

        @Override
        Iterator<K> keyIterator() {
            return new DescendingSubMapKeyIterator(absHighest(), absLowFence());
        }

        @Override
        Spliterator<K> keySpliterator() {
            return new DescendingSubMapKeyIterator(absHighest(), absLowFence());
        }

        @Override
        Iterator<K> descendingKeyIterator() {
            return new SubMapKeyIterator(absLowest(), absHighFence());
        }

        final class DescendingEntrySetView extends NavigableSubMap<K,V>.EntrySetView {
            @Override
            public Iterator<Entry<K, V>> iterator() {
                return new DescendingSubMapEntryIterator(absHighest(), absLowFence());
            }
        }

        @Override
        public Set<Entry<K,V>> entrySet() {
            NavigableSubMap.EntrySetView es = entrySetView;
            return (es != null) ? es : (entrySetView = new DescendingEntrySetView());
        }

        @Override
        TreeMap.Entry<K,V> subLowest()       { return absHighest(); }
        @Override
        TreeMap.Entry<K,V> subHighest()      { return absLowest(); }
        @Override
        TreeMap.Entry<K,V> subCeiling(K key) { return absFloor(key); }
        @Override
        TreeMap.Entry<K,V> subHigher(K key)  { return absLower(key); }
        @Override
        TreeMap.Entry<K,V> subFloor(K key)   { return absCeiling(key); }
        @Override
        TreeMap.Entry<K,V> subLower(K key)   { return absHigher(key); }
    }

    /**
     * This class exists solely for the sake of serialization
     * compatibility with previous releases of TreeMap that did not
     * support NavigableMap.  It translates an old-version SubMap into
     * a new-version AscendingSubMap. This class is never otherwise
     * used.
     *
     * @serial include
     */
    private class SubMap extends AbstractMap<K,V>
        implements SortedMap<K,V>, Serializable {
        private static final long serialVersionUID = -6520786458950516097L;
        private boolean fromStart = false, toEnd = false;
        private K fromKey, toKey;
        private Object readResolve() {
            return new AscendingSubMap<>(TreeMap.this,
                                         fromStart, fromKey, true,
                                         toEnd, toKey, false);
        }
        @Override
        public Set<Entry<K,V>> entrySet() { throw new InternalError(); }
        @Override
        public K lastKey() { throw new InternalError(); }
        @Override
        public K firstKey() { throw new InternalError(); }
        @Override
        public SortedMap<K,V> subMap(K fromKey, K toKey) { throw new InternalError(); }
        @Override
        public SortedMap<K,V> headMap(K toKey) { throw new InternalError(); }
        @Override
        public SortedMap<K,V> tailMap(K fromKey) { throw new InternalError(); }
        @Override
        public Comparator<? super K> comparator() { throw new InternalError(); }
    }


    // Red-black mechanics

    private static final boolean RED   = false;
    private static final boolean BLACK = true;

    /**
     * Node in the Tree.  Doubles as a means to pass key-value pairs back to
     * user (see Map.Entry).
     */

    static final class Entry<K,V> implements Map.Entry<K,V> {
        K key;
        V value;
        Entry<K,V> left;
        Entry<K,V> right;
        Entry<K,V> parent;
        boolean color = BLACK;

        /**
         * Make a new cell with given key, value, and parent, and with
         * {@code null} child links, and BLACK color.
         */
        Entry(K key, V value, Entry<K,V> parent) {
            this.key = key;
            this.value = value;
            this.parent = parent;
        }

        /**
         * Returns the key.
         *
         * @return the key
         */
        @Override
        public K getKey() {
            return key;
        }

        /**
         * Returns the value associated with the key.
         *
         * @return the value associated with the key
         */
        @Override
        public V getValue() {
            return value;
        }

        /**
         * Replaces the value currently associated with the key with the given
         * value.
         *
         * @return the value associated with the key before this method was
         *         called
         */
        @Override
        public V setValue(V value) {
            V oldValue = this.value;
            this.value = value;
            return oldValue;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            Map.Entry<?,?> e = (Map.Entry<?,?>)o;

            return valEquals(key,e.getKey()) && valEquals(value,e.getValue());
        }

        @Override
        public int hashCode() {
            int keyHash = (key==null ? 0 : key.hashCode());
            int valueHash = (value==null ? 0 : value.hashCode());
            return keyHash ^ valueHash;
        }

        @Override
        public String toString() {
            return key + "=" + value;
        }
    }

    /**
     * 返回 TreeMap 中的第一个节点。
     * 如果 TreeMap 为空，则返回 null。
     *
     * 注意：这里的第一个节点，并非是根节点，而是按照比较器，排序后，最小的那个值。
     * 即：TreeMap 最左（小）的那个节点。
     */
    final Entry<K,V> getFirstEntry() {
        // 1、获取根节点
        Entry<K,V> p = root;
        // 2、判断根节点是否为空
        if (p != null) {
            // 2.2、如果根节点不为空，则遍历左子树，找出最左（小）的那个节点
            while (p.left != null) {
                p = p.left;
            }
        }
        return p;
    }

    /**
     * Returns the last Entry in the TreeMap (according to the TreeMap's
     * key-sort function).  Returns null if the TreeMap is empty.
     */
    final Entry<K,V> getLastEntry() {
        Entry<K,V> p = root;
        if (p != null) {
            while (p.right != null) {
                p = p.right;
            }
        }
        return p;
    }

    /**
     * 找出指定节点的下一个节点。
     *
     * @param t 指定节点。
     * @param <K> 指定节点的 key 的类型。
     * @param <V> 指定节点的 value 的类型。
     * @return 指定节点的下一个节点。
     */
    static <K,V> Entry<K,V> successor(Entry<K,V> t) {
        // 1、判断 t 节点是否为 null
        if (t == null) {
            return null;
        } else if (t.right != null) {
            // 2、如果右节点不为空，则将右节点赋值给 p
            Entry<K,V> p = t.right;
            // 2.1、遍历 p 节点的左子树，获取 p 节点最左（即最小）的节点
            while (p.left != null) {
                p = p.left;
            }
            return p;
        } else {
            // 3、如果 t 节点的右子树为空，则将 t 节点的父节点，赋值给 p
            Entry<K,V> p = t.parent;
            // 4、将 t 节点赋值给 ch
            Entry<K,V> ch = t;
            // 5、遍历，找出下一个节点
            // 详细说明：
            //      当 t 节点的右子树为空的情况下，说明 t 节点是当前子树最大的节点，则 t 节点的下个节点，可能存在3种情况：
            //          1、t 节点父节点为空，这种情况：下一个节点为 null。
            //          2、t 节点为父节点的左节点，这种情况：下一个节点为父节点。
            //          3、t 节点为父节点的右节点，这种情况：则需要判断父节点的父节点，也就是爷爷节点，
            //              这种情况需要重新走一遍判断逻辑，直到符合1、2两种情况。
            //              极端情况：t节点为最后一个节点，即这个节点在右子树的最右边(最大的那个节点)，这种情况会遍历右子树，最后返回 null。
            //
            // 所以 p != null && ch == p.right 我们可以这样理解：
            //      1、p != null：如果父节点不为空，则说明要判断2、3两种情况，反之直接返回 null (p 的值为 null)
            //      2、ch == p.right：如果 t 节点为父节点的右节点，则需要判断爷爷节点，反之直接返回 p 节点 (p 的值为父节点)
            while (p != null && ch == p.right) {
                ch = p;
                p = p.parent;
            }
            return p;
        }
    }

    /**
     * Returns the predecessor of the specified Entry, or null if no such.
     */
    static <K,V> Entry<K,V> predecessor(Entry<K,V> t) {
        if (t == null) {
            return null;
        } else if (t.left != null) {
            Entry<K,V> p = t.left;
            while (p.right != null) {
                p = p.right;
            }
            return p;
        } else {
            Entry<K,V> p = t.parent;
            Entry<K,V> ch = t;
            while (p != null && ch == p.left) {
                ch = p;
                p = p.parent;
            }
            return p;
        }
    }

    /**
     * Balancing operations.
     *
     * Implementations of rebalancings during insertion and deletion are
     * slightly different than the CLR version.  Rather than using dummy
     * nilnodes, we use a set of accessors that deal properly with null.  They
     * are used to avoid messiness surrounding nullness checks in the main
     * algorithms.
     */

    private static <K,V> boolean colorOf(Entry<K,V> p) {
        return (p == null ? BLACK : p.color);
    }

    private static <K,V> Entry<K,V> parentOf(Entry<K,V> p) {
        return (p == null ? null: p.parent);
    }

    private static <K,V> void setColor(Entry<K,V> p, boolean c) {
        if (p != null) {
            p.color = c;
        }
    }

    private static <K,V> Entry<K,V> leftOf(Entry<K,V> p) {
        return (p == null) ? null: p.left;
    }

    private static <K,V> Entry<K,V> rightOf(Entry<K,V> p) {
        return (p == null) ? null: p.right;
    }

    /** From CLR */
    private void rotateLeft(Entry<K,V> p) {
        if (p != null) {
            Entry<K,V> r = p.right;
            p.right = r.left;
            if (r.left == null) {
            } else {
                r.left.parent = p;
            }
            r.parent = p.parent;
            if (p.parent == null) {
                root = r;
            } else if (p.parent.left == p) {
                p.parent.left = r;
            } else {
                p.parent.right = r;
            }
            r.left = p;
            p.parent = r;
        }
    }

    /** From CLR */
    private void rotateRight(Entry<K,V> p) {
        if (p != null) {
            Entry<K,V> l = p.left;
            p.left = l.right;
            if (l.right != null) {
                l.right.parent = p;
            }
            l.parent = p.parent;
            if (p.parent == null) {
                root = l;
            } else if (p.parent.right != p) {
                p.parent.left = l;
            } else {
                p.parent.right = l;
            }
            l.right = p;
            p.parent = l;
        }
    }

    /** From CLR */
    private void fixAfterInsertion(Entry<K,V> x) {
        x.color = RED;

        while (x != null && x != root && x.parent.color == RED) {
            if (parentOf(x) == leftOf(parentOf(parentOf(x)))) {
                Entry<K,V> y = rightOf(parentOf(parentOf(x)));
                if (colorOf(y) == RED) {
                    setColor(parentOf(x), BLACK);
                    setColor(y, BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    x = parentOf(parentOf(x));
                } else {
                    if (x == rightOf(parentOf(x))) {
                        x = parentOf(x);
                        rotateLeft(x);
                    }
                    setColor(parentOf(x), BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    rotateRight(parentOf(parentOf(x)));
                }
            } else {
                Entry<K,V> y = leftOf(parentOf(parentOf(x)));
                if (colorOf(y) == RED) {
                    setColor(parentOf(x), BLACK);
                    setColor(y, BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    x = parentOf(parentOf(x));
                } else {
                    if (x == leftOf(parentOf(x))) {
                        x = parentOf(x);
                        rotateRight(x);
                    }
                    setColor(parentOf(x), BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    rotateLeft(parentOf(parentOf(x)));
                }
            }
        }
        root.color = BLACK;
    }

    /**
     * Delete node p, and then rebalance the tree.
     */
    private void deleteEntry(Entry<K,V> p) {
        modCount++;
        size--;

        // If strictly internal, copy successor's element to p and then make p
        // point to successor.
        if (p.left != null && p.right != null) {
            Entry<K,V> s = successor(p);
            p.key = s.key;
            p.value = s.value;
            p = s;
        } // p has 2 children

        // Start fixup at replacement node, if it exists.
        Entry<K,V> replacement = (p.left != null ? p.left : p.right);

        if (replacement != null) {
            // Link replacement to parent
            replacement.parent = p.parent;
            if (p.parent == null) {
                root = replacement;
            } else if (p == p.parent.left) {
                p.parent.left  = replacement;
            } else {
                p.parent.right = replacement;
            }

            // Null out links so they are OK to use by fixAfterDeletion.
            p.left = p.right = p.parent = null;

            // Fix replacement
            if (p.color == BLACK) {
                fixAfterDeletion(replacement);
            }
        } else if (p.parent == null) { // return if we are the only node.
            root = null;
        } else { //  No children. Use self as phantom replacement and unlink.
            if (p.color == BLACK) {
                fixAfterDeletion(p);
            }

            if (p.parent != null) {
                if (p == p.parent.left) {
                    p.parent.left = null;
                } else if (p == p.parent.right) {
                    p.parent.right = null;
                }
                p.parent = null;
            }
        }
    }

    /** From CLR */
    private void fixAfterDeletion(Entry<K,V> x) {
        while (x != root && colorOf(x) == BLACK) {
            if (x == leftOf(parentOf(x))) {
                Entry<K,V> sib = rightOf(parentOf(x));

                if (colorOf(sib) == RED) {
                    setColor(sib, BLACK);
                    setColor(parentOf(x), RED);
                    rotateLeft(parentOf(x));
                    sib = rightOf(parentOf(x));
                }

                if (colorOf(leftOf(sib))  == BLACK &&
                    colorOf(rightOf(sib)) == BLACK) {
                    setColor(sib, RED);
                    x = parentOf(x);
                } else {
                    if (colorOf(rightOf(sib)) == BLACK) {
                        setColor(leftOf(sib), BLACK);
                        setColor(sib, RED);
                        rotateRight(sib);
                        sib = rightOf(parentOf(x));
                    }
                    setColor(sib, colorOf(parentOf(x)));
                    setColor(parentOf(x), BLACK);
                    setColor(rightOf(sib), BLACK);
                    rotateLeft(parentOf(x));
                    x = root;
                }
            } else { // symmetric
                Entry<K,V> sib = leftOf(parentOf(x));

                if (colorOf(sib) == RED) {
                    setColor(sib, BLACK);
                    setColor(parentOf(x), RED);
                    rotateRight(parentOf(x));
                    sib = leftOf(parentOf(x));
                }

                if (colorOf(rightOf(sib)) == BLACK &&
                    colorOf(leftOf(sib)) == BLACK) {
                    setColor(sib, RED);
                    x = parentOf(x);
                } else {
                    if (colorOf(leftOf(sib)) == BLACK) {
                        setColor(rightOf(sib), BLACK);
                        setColor(sib, RED);
                        rotateLeft(sib);
                        sib = leftOf(parentOf(x));
                    }
                    setColor(sib, colorOf(parentOf(x)));
                    setColor(parentOf(x), BLACK);
                    setColor(leftOf(sib), BLACK);
                    rotateRight(parentOf(x));
                    x = root;
                }
            }
        }

        setColor(x, BLACK);
    }

    private static final long serialVersionUID = 919286545866124006L;

    /**
     * Save the state of the {@code TreeMap} instance to a stream (i.e.,
     * serialize it).
     *
     * @serialData The <em>size</em> of the TreeMap (the number of key-value
     *             mappings) is emitted (int), followed by the key (Object)
     *             and value (Object) for each key-value mapping represented
     *             by the TreeMap. The key-value mappings are emitted in
     *             key-order (as determined by the TreeMap's Comparator,
     *             or by the keys' natural ordering if the TreeMap has no
     *             Comparator).
     */
    private void writeObject(java.io.ObjectOutputStream s)
        throws java.io.IOException {
        // Write out the Comparator and any hidden stuff
        s.defaultWriteObject();

        // Write out size (number of Mappings)
        s.writeInt(size);

        // Write out keys and values (alternating)
        for (Iterator<Map.Entry<K,V>> i = entrySet().iterator(); i.hasNext(); ) {
            Map.Entry<K,V> e = i.next();
            s.writeObject(e.getKey());
            s.writeObject(e.getValue());
        }
    }

    /**
     * Reconstitute the {@code TreeMap} instance from a stream (i.e.,
     * deserialize it).
     */
    private void readObject(final java.io.ObjectInputStream s)
        throws java.io.IOException, ClassNotFoundException {
        // Read in the Comparator and any hidden stuff
        s.defaultReadObject();

        // Read in size
        int size = s.readInt();

        buildFromSorted(size, null, s, null);
    }

    /** Intended to be called only from TreeSet.readObject */
    void readTreeSet(int size, java.io.ObjectInputStream s, V defaultVal)
        throws java.io.IOException, ClassNotFoundException {
        buildFromSorted(size, null, s, defaultVal);
    }

    /** Intended to be called only from TreeSet.addAll */
    void addAllForTreeSet(SortedSet<? extends K> set, V defaultVal) {
        try {
            buildFromSorted(set.size(), set.iterator(), null, defaultVal);
        } catch (java.io.IOException cannotHappen) {
        } catch (ClassNotFoundException cannotHappen) {
        }
    }


    /**
     * Linear time tree building algorithm from sorted data.  Can accept keys
     * and/or values from iterator or stream. This leads to too many
     * parameters, but seems better than alternatives.  The four formats
     * that this method accepts are:
     *
     *    1) An iterator of Map.Entries.  (it != null, defaultVal == null).
     *    2) An iterator of keys.         (it != null, defaultVal != null).
     *    3) A stream of alternating serialized keys and values.
     *                                   (it == null, defaultVal == null).
     *    4) A stream of serialized keys. (it == null, defaultVal != null).
     *
     * It is assumed that the comparator of the TreeMap is already set prior
     * to calling this method.
     *
     * @param size the number of keys (or key-value pairs) to be read from
     *        the iterator or stream
     * @param it If non-null, new entries are created from entries
     *        or keys read from this iterator.
     * @param str If non-null, new entries are created from keys and
     *        possibly values read from this stream in serialized form.
     *        Exactly one of it and str should be non-null.
     * @param defaultVal if non-null, this default value is used for
     *        each value in the map.  If null, each value is read from
     *        iterator or stream, as described above.
     * @throws java.io.IOException propagated from stream reads. This cannot
     *         occur if str is null.
     * @throws ClassNotFoundException propagated from readObject.
     *         This cannot occur if str is null.
     */
    private void buildFromSorted(int size, Iterator<?> it,
                                 ObjectInputStream str,
                                 V defaultVal)
        throws IOException, ClassNotFoundException {
        this.size = size;
        root = buildFromSorted(0, 0, size-1, computeRedLevel(size),
                               it, str, defaultVal);
    }

    /**
     * Recursive "helper method" that does the real work of the
     * previous method.  Identically named parameters have
     * identical definitions.  Additional parameters are documented below.
     * It is assumed that the comparator and size fields of the TreeMap are
     * already set prior to calling this method.  (It ignores both fields.)
     *
     * @param level the current level of tree. Initial call should be 0.
     * @param lo the first element index of this subtree. Initial should be 0.
     * @param hi the last element index of this subtree.  Initial should be
     *        size-1.
     * @param redLevel the level at which nodes should be red.
     *        Must be equal to computeRedLevel for tree of this size.
     */
    @SuppressWarnings("unchecked")
    private final Entry<K,V> buildFromSorted(int level, int lo, int hi,
                                             int redLevel,
                                             Iterator<?> it,
                                             ObjectInputStream str,
                                             V defaultVal)
        throws  IOException, ClassNotFoundException {
        /*
         * Strategy: The root is the middlemost element. To get to it, we
         * have to first recursively construct the entire left subtree,
         * so as to grab all of its elements. We can then proceed with right
         * subtree.
         *
         * The lo and hi arguments are the minimum and maximum
         * indices to pull out of the iterator or stream for current subtree.
         * They are not actually indexed, we just proceed sequentially,
         * ensuring that items are extracted in corresponding order.
         */

        if (hi < lo) {
            return null;
        }

        int mid = (lo + hi) >>> 1;

        Entry<K,V> left  = null;
        if (lo < mid) {
            left = buildFromSorted(level+1, lo, mid - 1, redLevel,
                                   it, str, defaultVal);
        }

        // extract key and/or value from iterator or stream
        K key;
        V value;
        if (it != null) {
            if (defaultVal==null) {
                Map.Entry<?,?> entry = (Map.Entry<?,?>)it.next();
                key = (K)entry.getKey();
                value = (V)entry.getValue();
            } else {
                key = (K)it.next();
                value = defaultVal;
            }
        } else { // use stream
            key = (K) str.readObject();
            value = (defaultVal != null ? defaultVal : (V) str.readObject());
        }

        Entry<K,V> middle =  new Entry<>(key, value, null);

        // color nodes in non-full bottommost level red
        if (level == redLevel) {
            middle.color = RED;
        }

        if (left != null) {
            middle.left = left;
            left.parent = middle;
        }

        if (mid < hi) {
            Entry<K,V> right = buildFromSorted(level+1, mid+1, hi, redLevel,
                                               it, str, defaultVal);
            middle.right = right;
            right.parent = middle;
        }

        return middle;
    }

    /**
     * Find the level down to which to assign all nodes BLACK.  This is the
     * last `full' level of the complete binary tree produced by
     * buildTree. The remaining nodes are colored RED. (This makes a `nice'
     * set of color assignments wrt future insertions.) This level number is
     * computed by finding the number of splits needed to reach the zeroeth
     * node.  (The answer is ~lg(N), but in any case must be computed by same
     * quick O(lg(N)) loop.)
     */
    private static int computeRedLevel(int sz) {
        int level = 0;
        for (int m = sz - 1; m >= 0; m = m / 2 - 1) {
            level++;
        }
        return level;
    }

    /**
     * Currently, we support Spliterator-based versions only for the
     * full map, in either plain of descending form, otherwise relying
     * on defaults because size estimation for submaps would dominate
     * costs. The type tests needed to check these for key views are
     * not very nice but avoid disrupting existing class
     * structures. Callers must use plain default spliterators if this
     * returns null.
     */
    static <K> Spliterator<K> keySpliteratorFor(NavigableMap<K,?> m) {
        if (m instanceof TreeMap) {
            @SuppressWarnings("unchecked") TreeMap<K,Object> t =
                (TreeMap<K,Object>) m;
            return t.keySpliterator();
        }
        if (m instanceof DescendingSubMap) {
            @SuppressWarnings("unchecked") DescendingSubMap<K,?> dm =
                (DescendingSubMap<K,?>) m;
            TreeMap<K,?> tm = dm.m;
            if (dm == tm.descendingMap) {
                @SuppressWarnings("unchecked") TreeMap<K,Object> t =
                    (TreeMap<K,Object>) tm;
                return t.descendingKeySpliterator();
            }
        }
        @SuppressWarnings("unchecked") NavigableSubMap<K,?> sm =
            (NavigableSubMap<K,?>) m;
        return sm.keySpliterator();
    }

    final Spliterator<K> keySpliterator() {
        return new KeySpliterator<K,V>(this, null, null, 0, -1, 0);
    }

    final Spliterator<K> descendingKeySpliterator() {
        return new DescendingKeySpliterator<K,V>(this, null, null, 0, -2, 0);
    }

    /**
     * Base class for spliterators.  Iteration starts at a given
     * origin and continues up to but not including a given fence (or
     * null for end).  At top-level, for ascending cases, the first
     * split uses the root as left-fence/right-origin. From there,
     * right-hand splits replace the current fence with its left
     * child, also serving as origin for the split-off spliterator.
     * Left-hands are symmetric. Descending versions place the origin
     * at the end and invert ascending split rules.  This base class
     * is non-commital about directionality, or whether the top-level
     * spliterator covers the whole tree. This means that the actual
     * split mechanics are located in subclasses. Some of the subclass
     * trySplit methods are identical (except for return types), but
     * not nicely factorable.
     *
     * Currently, subclass versions exist only for the full map
     * (including descending keys via its descendingMap).  Others are
     * possible but currently not worthwhile because submaps require
     * O(n) computations to determine size, which substantially limits
     * potential speed-ups of using custom Spliterators versus default
     * mechanics.
     *
     * To boostrap initialization, external constructors use
     * negative size estimates: -1 for ascend, -2 for descend.
     */
    static class TreeMapSpliterator<K,V> {
        final TreeMap<K,V> tree;
        Entry<K,V> current; // traverser; initially first node in range
        Entry<K,V> fence;   // one past last, or null
        int side;                   // 0: top, -1: is a left split, +1: right
        int est;                    // size estimate (exact only for top-level)
        int expectedModCount;       // for CME checks

        TreeMapSpliterator(TreeMap<K,V> tree,
                           Entry<K,V> origin, Entry<K,V> fence,
                           int side, int est, int expectedModCount) {
            this.tree = tree;
            this.current = origin;
            this.fence = fence;
            this.side = side;
            this.est = est;
            this.expectedModCount = expectedModCount;
        }

        final int getEstimate() { // force initialization
            int s; TreeMap<K,V> t;
            if ((s = est) < 0) {
                if ((t = tree) != null) {
                    current = (s == -1) ? t.getFirstEntry() : t.getLastEntry();
                    s = est = t.size;
                    expectedModCount = t.modCount;
                }
                else {
                    s = est = 0;
                }
            }
            return s;
        }

        public final long estimateSize() {
            return (long)getEstimate();
        }
    }

    static final class KeySpliterator<K,V>
        extends TreeMapSpliterator<K,V>
        implements Spliterator<K> {
        KeySpliterator(TreeMap<K,V> tree,
                       Entry<K,V> origin, Entry<K,V> fence,
                       int side, int est, int expectedModCount) {
            super(tree, origin, fence, side, est, expectedModCount);
        }

        @Override
        public KeySpliterator<K,V> trySplit() {
            if (est < 0) {
                getEstimate(); // force initialization
            }
            int d = side;
            Entry<K,V> e = current, f = fence,
                s = ((e == null || e == f) ? null :      // empty
                     (d == 0)              ? tree.root : // was top
                     (d >  0)              ? e.right :   // was right
                     (d <  0 && f != null) ? f.left :    // was left
                     null);
            if (s != null && s != e && s != f &&
                tree.compare(e.key, s.key) < 0) {        // e not already past s
                side = 1;
                return new KeySpliterator<>
                    (tree, e, current = s, -1, est >>>= 1, expectedModCount);
            }
            return null;
        }

        @Override
        public void forEachRemaining(Consumer<? super K> action) {
            if (action == null) {
                throw new NullPointerException();
            }
            if (est < 0) {
                getEstimate(); // force initialization
            }
            Entry<K,V> f = fence, e, p, pl;
            if ((e = current) != null && e != f) {
                current = f; // exhaust
                do {
                    action.accept(e.key);
                    if ((p = e.right) != null) {
                        while ((pl = p.left) != null) {
                            p = pl;
                        }
                    }
                    else {
                        while ((p = e.parent) != null && e == p.right) {
                            e = p;
                        }
                    }
                } while ((e = p) != null && e != f);
                if (tree.modCount != expectedModCount) {
                    throw new ConcurrentModificationException();
                }
            }
        }

        @Override
        public boolean tryAdvance(Consumer<? super K> action) {
            Entry<K,V> e;
            if (action == null) {
                throw new NullPointerException();
            }
            if (est < 0) {
                getEstimate(); // force initialization
            }
            if ((e = current) == null || e == fence) {
                return false;
            }
            current = successor(e);
            action.accept(e.key);
            if (tree.modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            return true;
        }

        @Override
        public int characteristics() {
            return (side == 0 ? Spliterator.SIZED : 0) |
                Spliterator.DISTINCT | Spliterator.SORTED | Spliterator.ORDERED;
        }

        @Override
        public final Comparator<? super K>  getComparator() {
            return tree.comparator;
        }

    }

    static final class DescendingKeySpliterator<K,V>
        extends TreeMapSpliterator<K,V>
        implements Spliterator<K> {
        DescendingKeySpliterator(TreeMap<K,V> tree,
                                 Entry<K,V> origin, Entry<K,V> fence,
                                 int side, int est, int expectedModCount) {
            super(tree, origin, fence, side, est, expectedModCount);
        }

        @Override
        public DescendingKeySpliterator<K,V> trySplit() {
            if (est < 0) {
                getEstimate(); // force initialization
            }
            int d = side;
            Entry<K,V> e = current, f = fence,
                    s = ((e == null || e == f) ? null :      // empty
                         (d == 0)              ? tree.root : // was top
                         (d <  0)              ? e.left :    // was left
                         (d >  0 && f != null) ? f.right :   // was right
                         null);
            if (s != null && s != e && s != f &&
                tree.compare(e.key, s.key) > 0) {       // e not already past s
                side = 1;
                return new DescendingKeySpliterator<>
                        (tree, e, current = s, -1, est >>>= 1, expectedModCount);
            }
            return null;
        }

        @Override
        public void forEachRemaining(Consumer<? super K> action) {
            if (action == null) {
                throw new NullPointerException();
            }
            if (est < 0) {
                getEstimate(); // force initialization
            }
            Entry<K,V> f = fence, e, p, pr;
            if ((e = current) != null && e != f) {
                current = f; // exhaust
                do {
                    action.accept(e.key);
                    if ((p = e.left) != null) {
                        while ((pr = p.right) != null) {
                            p = pr;
                        }
                    }
                    else {
                        while ((p = e.parent) != null && e == p.left) {
                            e = p;
                        }
                    }
                } while ((e = p) != null && e != f);
                if (tree.modCount != expectedModCount) {
                    throw new ConcurrentModificationException();
                }
            }
        }

        @Override
        public boolean tryAdvance(Consumer<? super K> action) {
            Entry<K,V> e;
            if (action == null) {
                throw new NullPointerException();
            }
            if (est < 0) {
                getEstimate(); // force initialization
            }
            if ((e = current) == null || e == fence) {
                return false;
            }
            current = predecessor(e);
            action.accept(e.key);
            if (tree.modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            return true;
        }

        @Override
        public int characteristics() {
            return (side == 0 ? Spliterator.SIZED : 0) |
                Spliterator.DISTINCT | Spliterator.ORDERED;
        }
    }

    static final class ValueSpliterator<K,V>
            extends TreeMapSpliterator<K,V>
            implements Spliterator<V> {
        ValueSpliterator(TreeMap<K,V> tree,
                         Entry<K,V> origin, Entry<K,V> fence,
                         int side, int est, int expectedModCount) {
            super(tree, origin, fence, side, est, expectedModCount);
        }

        @Override
        public ValueSpliterator<K,V> trySplit() {
            if (est < 0) {
                getEstimate(); // force initialization
            }
            int d = side;
            Entry<K,V> e = current, f = fence,
                    s = ((e == null || e == f) ? null :      // empty
                         (d == 0)              ? tree.root : // was top
                         (d >  0)              ? e.right :   // was right
                         (d <  0 && f != null) ? f.left :    // was left
                         null);
            if (s != null && s != e && s != f &&
                tree.compare(e.key, s.key) < 0) {        // e not already past s
                side = 1;
                return new ValueSpliterator<>
                        (tree, e, current = s, -1, est >>>= 1, expectedModCount);
            }
            return null;
        }

        @Override
        public void forEachRemaining(Consumer<? super V> action) {
            if (action == null) {
                throw new NullPointerException();
            }
            if (est < 0) {
                getEstimate(); // force initialization
            }
            Entry<K,V> f = fence, e, p, pl;
            if ((e = current) != null && e != f) {
                current = f; // exhaust
                do {
                    action.accept(e.value);
                    if ((p = e.right) != null) {
                        while ((pl = p.left) != null) {
                            p = pl;
                        }
                    }
                    else {
                        while ((p = e.parent) != null && e == p.right) {
                            e = p;
                        }
                    }
                } while ((e = p) != null && e != f);
                if (tree.modCount != expectedModCount) {
                    throw new ConcurrentModificationException();
                }
            }
        }

        @Override
        public boolean tryAdvance(Consumer<? super V> action) {
            Entry<K,V> e;
            if (action == null) {
                throw new NullPointerException();
            }
            if (est < 0) {
                getEstimate(); // force initialization
            }
            if ((e = current) == null || e == fence) {
                return false;
            }
            current = successor(e);
            action.accept(e.value);
            if (tree.modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            return true;
        }

        @Override
        public int characteristics() {
            return (side == 0 ? Spliterator.SIZED : 0) | Spliterator.ORDERED;
        }
    }

    static final class EntrySpliterator<K,V>
        extends TreeMapSpliterator<K,V>
        implements Spliterator<Map.Entry<K,V>> {
        EntrySpliterator(TreeMap<K,V> tree,
                         Entry<K,V> origin, Entry<K,V> fence,
                         int side, int est, int expectedModCount) {
            super(tree, origin, fence, side, est, expectedModCount);
        }

        @Override
        public EntrySpliterator<K,V> trySplit() {
            if (est < 0) {
                getEstimate(); // force initialization
            }
            int d = side;
            Entry<K,V> e = current, f = fence,
                    s = ((e == null || e == f) ? null :      // empty
                         (d == 0)              ? tree.root : // was top
                         (d >  0)              ? e.right :   // was right
                         (d <  0 && f != null) ? f.left :    // was left
                         null);
            if (s != null && s != e && s != f &&
                tree.compare(e.key, s.key) < 0) {        // e not already past s
                side = 1;
                return new EntrySpliterator<>
                        (tree, e, current = s, -1, est >>>= 1, expectedModCount);
            }
            return null;
        }

        @Override
        public void forEachRemaining(Consumer<? super Map.Entry<K, V>> action) {
            if (action == null) {
                throw new NullPointerException();
            }
            if (est < 0) {
                getEstimate(); // force initialization
            }
            Entry<K,V> f = fence, e, p, pl;
            if ((e = current) != null && e != f) {
                current = f; // exhaust
                do {
                    action.accept(e);
                    if ((p = e.right) != null) {
                        while ((pl = p.left) != null) {
                            p = pl;
                        }
                    }
                    else {
                        while ((p = e.parent) != null && e == p.right) {
                            e = p;
                        }
                    }
                } while ((e = p) != null && e != f);
                if (tree.modCount != expectedModCount) {
                    throw new ConcurrentModificationException();
                }
            }
        }

        @Override
        public boolean tryAdvance(Consumer<? super Map.Entry<K,V>> action) {
            Entry<K,V> e;
            if (action == null) {
                throw new NullPointerException();
            }
            if (est < 0) {
                getEstimate(); // force initialization
            }
            if ((e = current) == null || e == fence) {
                return false;
            }
            current = successor(e);
            action.accept(e);
            if (tree.modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            return true;
        }

        @Override
        public int characteristics() {
            return (side == 0 ? Spliterator.SIZED : 0) |
                    Spliterator.DISTINCT | Spliterator.SORTED | Spliterator.ORDERED;
        }

        @Override
        public Comparator<Map.Entry<K, V>> getComparator() {
            // Adapt or create a key-based comparator
            if (tree.comparator != null) {
                return Map.Entry.comparingByKey(tree.comparator);
            }
            else {
                return (Comparator<Map.Entry<K, V>> & Serializable) (e1, e2) -> {
                    @SuppressWarnings("unchecked")
                    Comparable<? super K> k1 = (Comparable<? super K>) e1.getKey();
                    return k1.compareTo(e2.getKey());
                };
            }
        }
    }
}
