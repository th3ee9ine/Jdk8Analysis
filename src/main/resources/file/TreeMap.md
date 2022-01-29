## 前提

建议掌握以下知识点后，再进行阅读以下内容

- Map 基本概念
- transient 关键字
- 泛型
- 红黑树的基本概念
- SuppressWarnings 注解

## 概述

- 基于红黑树实现的
- get、put、remove 操作的时间复杂度为 log(n)
- TreeMap 根据 key 的自然顺序排序或者由 TreeMap 创建时提供的 Comparator 排序
- TreeMap是线程不安全的
- 如果在创建迭代器后的对TreeMap进行修改(添加、删除)操作，除了通过迭代器自己的remove之外的任何方式，迭代器将抛出 ConcurrentModificationException 异常