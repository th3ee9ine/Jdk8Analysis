package com.th3ee9ine;

import com.th3ee9ine.java.util.TreeMap;

/**
 * @author th3ee9ine.
 * @date 2022/2/8 14:43.
 */
public class TestComparable {
    public static void main(String[] args) {
        Student student = new Student();
        student.setName("张三");
        student.setAge(3);
//        Comparable<Student> k = (Comparable<Student>) student;

        TreeMap<Student, Integer> treeMap = new TreeMap<>();
        treeMap.put(student, 1);
//
//        System.out.println(k);
    }
}
