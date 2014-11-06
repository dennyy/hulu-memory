package com.cisco.ramp.hulukv.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import com.cisco.ramp.hulukv.cache.IndexedLinkedList;

/**
 * Class Description
 *
 * @author Denny Ye
 * @since 2012-11-14
 * @version 1.0
 */
public class TestIndexedLinkedList {

  Random ran = new Random();
  
  @Test
  public void sortInRandom() {
    
    IndexedLinkedList<Num> linked = new IndexedLinkedList<Num>(300);
    List<Num> list = new ArrayList<Num>();
    
    for (int i = 0; i < 200; i++) {
      list.add(new Num(i));
    }
    
    Collections.shuffle(list);
    
    for (Num num : list) {
      linked.put(num);
    }
    
    if (!isOrdered(linked.getAllValues(), 200)) {
      System.out.println("Failed:" + list);
      System.out.println("Un-ordered:" + linked.getAllValues());
      System.out.println(linked.getIndexStatus());
    } else {
      System.out.println("OK:");
      System.out.println(linked.getIndexStatus());
    }
  }
  
  @Test
  public void comparePutSortedWithLinkedList() {
    List<Num> list = new ArrayList<Num>();
    int total = 10000;
    
    for (int i = 0; i < total; i++) {
      list.add(new Num(i));
    }
    
    IndexedLinkedList<Num> linked = new IndexedLinkedList<Num>(1000);
    
    Collections.shuffle(list);
    
    long start = System.currentTimeMillis();
    for (Num num : list) {
      linked.put(num);
    }
    
    
    long end1 = System.currentTimeMillis();
    System.out.println("IndexedLinkedList:" + (end1 - start));
    
    LinkedList<Num> linkedList = new LinkedList<Num>();
    
    for (Num num : list) {
      sortAt(linkedList, num);
    }
    long end2 = System.currentTimeMillis();
    
    if (!isOrdered(linked.getAllValues(), total)) {
      System.err.println("IndexedLinkedList isnot ordered");
    } else {
      System.out.println("IndexedLinkedList is ordered");
    }
    
    if (isOrderd(linkedList)) {
      System.out.println("LinkedList is ordered");
    } else {
      System.err.println("LinkedList isnot ordered");
    }
    
    
    System.out.println("LinkedList:" + (end2 - end1));
  }
  
  @Test
  public void getSpecifiedLocation() {
    int total = 30000;
    
    LinkedList<Num> list = new LinkedList<Num>();
    IndexedLinkedList<Num> linked = new IndexedLinkedList<Num>(1000);
    
    for (int i = 0; i < total; i++) {
      list.add(new Num(i));
    }
    
    for (Num num : list) {
      linked.put(num);
    }
    
    long start = System.currentTimeMillis();
    for (int i = 0; i < 10000; i++) {
      Assert.assertEquals(28000, linked.get(28000).get().getValue());
    }
    long end1 = System.currentTimeMillis();
    System.out.println("IndexedLinkedList:" + (end1 - start));
    
    for (int i = 0; i < 10000; i++) {
      Assert.assertEquals(28000, list.get(28000).value);
    }
    long end2 = System.currentTimeMillis();
    System.out.println("LinkedList:" + (end2 - end1));
  }
  
  @Test
  public void compareGetIndexWithLinkedList() {
    List<Num> list = new ArrayList<Num>();
    int total = 30000;
    
    for (int i = 0; i < total; i++) {
      list.add(new Num(i));
    }
    
    IndexedLinkedList<Num> linked = new IndexedLinkedList<Num>(1000);
    
    for (Num num : list) {
      linked.put(num);
    }
  }
  
  @Test
  public void removeElement() {
    List<Num> list = new ArrayList<Num>();
    int total = 3000;
    
    for (int i = 0; i < total; i++) {
      list.add(new Num(i));
    }
    
    IndexedLinkedList<Num> linked = new IndexedLinkedList<Num>(1000);
    
    for (Num num : list) {
      linked.put(num);
    }
    
    Assert.assertEquals(3000, linked.size());
    Assert.assertEquals(2, linked.get(2).get().getValue());
    linked.remove(linked.get(2).get());
    Assert.assertEquals(2999, linked.size());
    Assert.assertEquals(3, linked.get(2).get().getValue());
    
    linked.clear();
    for (int i = 0; i < total; i++) {
      linked.put(new Num(i));
    }
    
    Assert.assertEquals(3000, linked.size());
    for (int i = 0; i < total; i++) {
      Assert.assertTrue(linked.remove(new Num(i)));
    }
    
    Assert.assertEquals(0, linked.size());
    Assert.assertEquals("Index[]", linked.getIndexStatus());
    
    for (int i = 0; i < total; i++) {
      linked.put(new Num(i));
    }
    
    Assert.assertEquals(3000, linked.size());
    
    Collections.shuffle(list);
    for (int i = 0; i < list.size(); i++) {
      Num want = list.get(i);
      Assert.assertTrue(linked.remove(new Num(want.getValue())));
    }
    
  }
  
  @Test
  public void removeSpecifiedLocation() {
    //delete all
    //delete specified location
    //delete specified location and adds that back
    
    IndexedLinkedList<Num> linked = new IndexedLinkedList<Num>(100);
    
    int total = 3000;
    for (int i = 0; i < total; i++) {
      linked.put(new Num(i));
    }
    
    for (int i = total - 1; i >= 0; i--) {
      Assert.assertEquals(i, linked.remove(i).getValue());
      
      if (i > 0) {
        Assert.assertTrue(isOrdered(linked.getAllValues(), i));
      }
    }
    
    Assert.assertEquals(0, linked.size());
    
    System.out.println("Removed finished");
    for (int i = 0; i < total; i++) {
      linked.put(new Num(i));
    }
    
    Assert.assertEquals(3000, linked.size());
    
    for (int i = 0; i < total; i++) {
      Assert.assertEquals(i, linked.remove(0).getValue());
    }
    
    Assert.assertEquals(0, linked.size());
    
    for (int i = 0; i < total; i++) {
      linked.put(new Num(i));
    }
    
    Assert.assertEquals(total, linked.size());
    
    System.out.println(linked.getIndexStatus());
    LinkedList<Num> collect = new LinkedList<Num>();
    for (int i = 0; i < total; i++) {
      int removePoint = ran.nextInt(linked.size());
      
      Num value = linked.remove(removePoint);
      sortAt(collect, value);
    }
    
    Assert.assertTrue(isOrderd(collect));
    Assert.assertEquals(0, linked.size());
    System.out.println(linked.getIndexStatus());
  }
  
  private void sortAt(LinkedList<Num> list, Num value) {
    if (list.size() == 0) {
      list.add(value);
      return;
    }
    
    Iterator<Num> it = list.iterator();
    int index = 0;
    while (it.hasNext()) {
      Num current = it.next();
      
      if (value.getValue() < current.getValue()) {
        break;
      }
      
      index++;
    }
    
    list.add(index, value);
  }
  
  private boolean isOrdered(String numStr, int n) {
    List<Integer> numbers = new ArrayList<Integer>();
    numStr = numStr.substring(1, numStr.length() - 1);
    
    for (String str : numStr.split(",")) {
      numbers.add(Integer.parseInt(str));
    }
    
    if (n != numbers.size()) {
      System.err.println("Un-matched size: n:" + n + ", actual:" + numbers.size());
    }
    
    for (int i = 0; i < numbers.size() - 1; i++) {
      if (numbers.get(i) > numbers.get(i + 1)) {
        return false;
      }
    }
    
    return true;
  }
  
  private boolean isOrderd(LinkedList<Num> list) {
    boolean isOrdered = true;
    
    Num before = null;
    
    Iterator<Num> it = list.iterator();
    while (it.hasNext()) {
      Num current = it.next();
      
      if (before != null && before.getValue() >= current.getValue()) {
        System.out.println("Failed:" + before.getValue() + ", " + current.getValue());
        return false;
      } else {
        before = current;
      }
    }
    
    
    return isOrdered;
  }
  
  private class Num implements Comparable<Num> {
    
    private int value;
    
    Num(int i) {
      this.value = i;
    }
    
    public int getValue() {
      return this.value;
    }
    
    public String toString() {
      return String.valueOf(this.value);
    }

    @Override
    public int compareTo(Num o) {
      return this.value - o.value;
    }
  }
}

