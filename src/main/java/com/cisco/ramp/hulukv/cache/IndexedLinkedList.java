package com.cisco.ramp.hulukv.cache;

/**
 * As we know, the time complexity of ordered {@code LinkedList} is O(n). 
 * {@code ArrayList} also cannot support random and flexible insertion. Meanwhile, 
 * performance of {@code LinkedList#get(int)} and {@code LinkedList#set(int, Object)} 
 * methods have low performance with long-term traversal.
 * 
 * <br>
 * In order to improve performance at ordered linked structure, we would like
 * to build additional index for skiping by comparison.
 * 
 * <p><b>Structure Example</b><br>
 * 
 * |header|      -->    |Entry#1|--> |Entry#2|--> |Entry#3|--> |Entry#4|--> |Entry#5|<br>
 *                       ^                                        ^                  <br>
 *                       |                                        |                  <br>
 * |indexHeader| -->    |IndexEntry#1|             -->        |IndexEntry#2|         <br>
 * 
 * Link that starts from header element is original link same as {@code LinkedList}, but it's
 * ordered by comparison in specified interface.
 * Primary improvments are index link. It records Entry with minimum value, then splitting
 * more indexs during expansion of original link.
 *
 * @author Denny Ye
 * @since 2012-11-14
 * @version 1.0
 */
public class IndexedLinkedList<E extends Comparable<E>>  {

  /** Links with ordered elements */
  private Entry<E> header = new Entry<E>(null, null, null);
  
  /** Links with index structure */
  private IndexEntry indexHeader = new IndexEntry(null, null, null);
  
  /** standard range between current index and next index entry */
  private int indexRange;
  
  private int size;
  
  public IndexedLinkedList(int indexRange)  {
    if (indexRange < 100 || indexRange > 10000) {
      throw new IllegalArgumentException("indexRange [100, 10000]");
    }
    
    this.indexRange = indexRange;
    
    header.next = header.previous = header;
    indexHeader.next = indexHeader.previous = indexHeader;
    indexHeader.target = null;
  }
  
  /**
   * Puts element into link in order.
   * 
   * @param e comparable element 
   * @exception IllegalArgumentException (if element is null)
   */
  public synchronized Entry<E> put(E e) {
    if (e == null) {
      throw new IllegalArgumentException("element is null");
    }
    
    Entry<E> target = null;
    
    IndexEntry index = findSuitableIndex(e);
    if (index == indexHeader) {
      //Create first index entry
      Entry<E> insertPoint = findSuitableEntry(e, header.next, header);
      Entry<E> result = addBefore(e, insertPoint);
      
      index = new IndexEntry(result, indexHeader, indexHeader);
      index.previous.next = index;
      index.previous.previous = index;
      ++index.range;
      
      target = result;
    } else {
      Entry<E> insertPoint = findSuitableEntry(e, index.target, index.next.target);
      Entry<E> newEntry = addBefore(e, insertPoint);
      target = newEntry;
      
      if (insertPoint == index.target) {
        index.target = newEntry;
      } 
      
      if ( ++index.range >= (indexRange << 1)){
        //build new index
        Entry<E> limit = index.next != indexHeader ? index.next.target : header;
        buildNewIndex(index, limit);
      }
    }
    
    return target;
  }
  
  /**
   * Obtains Entry object by element index. 
   * element = Entry.get()
   * 
   * @param i 0 <= i < size
   * @return Entry<E> Entry but not physical element, in order to get contiguous element rapidly
   * @exception IllegalArgumentException -
   *             (specified target isnot in element range 0 <= i < size)
   */
  public synchronized Entry<E> get(int i) {
    if (i < 0 || i >= size) {
      throw new IllegalArgumentException("size:" + size + ", expect:" + i);
    }
    
    IndexEntry entry = null;
    
    int base = 0;
    for (entry = indexHeader.next; entry != indexHeader; entry = entry.next) {
      if (entry.range > (i - base)) {
        break;
      } else {
        base += entry.range;
      }
    }
    
    Entry<E> result = findSuitableEntry(i - base, entry.target);
    
    return result;
  }
  
  /**
   * Obtains contiguous element in list. 
   * More than specified element
   * 
   * @param e
   * @return null if no element choosen
   * @throws IllegalArgumentException
   */
  public synchronized Entry<E> getNext(E e) {
    if (e == null) {
      throw new IllegalArgumentException("element is null");
    }
    
    if (size > 0) {
      IndexEntry index = findSuitableIndex(e);
      Entry<E> found = findSuitableEntry(e, index.target, index.next.target);
      return found;
    }
    
    return null;
  }
  
  /**
   * Replaces element at specified location.
   * 
   * @param i
   * @param e
   * @return E element that located at that location
   * IllegalArgumentException
   */
  public synchronized E set(int i, E e) {
    if (i < 0 || i >= size) {
      throw new IllegalArgumentException("size:" + size + ", expect:" + i);
    }
    
    if (e == null) {
      throw new IllegalArgumentException("element is null");
    }
    
    IndexEntry index = findSuitableIndex(e);
    Entry<E> found = findSuitableEntry(e, index.target, index.next.target);
    Entry<E> replacePoint = found.previous;
    
    E located = replacePoint.element;
    replacePoint.element = e;
    
    return located;
  }
  
  /**
   * Returns <tt>true</tt> if this list contains the specified element.
   * 
   * @param e
   * @return
   * @exception IllegalArgumentException
   */
  public synchronized boolean contains(E e) {
    if (e == null) {
      throw new IllegalArgumentException("element is null");
    }
    
    IndexEntry index = findSuitableIndex(e);
    Entry<E> found = findSuitableEntry(e, index.target, index.next.target);
    Entry<E> searchPoint = found.previous;
    
    return searchPoint.element.compareTo(e) == 0;
  }
  
  /**
   * Deletes specified element if it's existing.
   * <br>
   * Returns <tt>True</tt> if that element is located at there and delete it
   * successfully.
   * 
   * @param e
   * @exception IllegalArgumentException (element is null)
   */
  public synchronized boolean remove(E e) {
    if (e == null) {
      throw new IllegalArgumentException("element is null");
    }
    
    IndexEntry index = findSuitableIndex(e);
    Entry<E> found = findSuitableEntry(e, index.target, index.next.target);
    Entry<E> deletePoint = found.previous;
    
    if (deletePoint.element.compareTo(e) == 0) {
      removeProcess(deletePoint, index);
      return true;
    }
    
    return false;
  }
  
  /**
   * Removes element at specified location
   * 
   * @param i
   * @return
   */
  public synchronized E remove(int i) {
    if (i < 0 || i >= size) {
      throw new IllegalArgumentException("size:" + size + ", expect:" + i);
    }
    
    IndexEntry entry = null;
    int base = 0;
    for (entry = indexHeader.next; entry != indexHeader; entry = entry.next) {
      if (entry.range > (i - base)) {
        break;
      } else {
        base += entry.range;
      }
    }
    
    Entry<E> e = findSuitableEntry(i - base, entry.target);
    
    E result = e.element;
    removeProcess(e, entry);
    return result;
  }
  
  /**
   * Number of total elements
   * 
   * @return
   */
  public synchronized int size() {
    return this.size;
  }
  
  /**
   * All elements and indexs should be clear
   */
  public synchronized void clear() {
    header.previous = header.next = header;
    indexHeader.previous = indexHeader.next = indexHeader;
    
    size = 0;
  }
  
  private void removeProcess(Entry<E> e, IndexEntry entry) {
    if (e == entry.target) {
      entry.target = e.next;
    }
    
    e.previous.next = e.next;
    e.next.previous = e.previous;
    e.next = e.previous = null;
    e.element = null;
    
    if(--entry.range <= (indexRange >> 2)) {
      if (entry == indexHeader.next) {
        if (entry.next != indexHeader) {
          //remove next IndexEntry
          IndexEntry needToDelete = entry.next;
          
          entry.range += needToDelete.range;
          needToDelete.previous.next = needToDelete.next;
          needToDelete.next.previous = needToDelete.previous;
          needToDelete.next = needToDelete.previous = null;
        } else if (entry.range == 0) {
          //No element left
          indexHeader.next = indexHeader.previous = indexHeader;
        }
      } else {
        //remove current IndexEntry
        entry.previous.range += entry.range;
        
        entry.previous.next = entry.next;
        entry.next.previous = entry.previous;
        entry.next = entry.previous = null;
      }
    }
    
    size--;
  }
  
  /*
   * Creates new index and changes range for two indexs
   */
  private void buildNewIndex(IndexEntry current, Entry<E> limit) {
    Entry<E> p = null;
    for (int i = 0; i < indexRange; i++, limit = limit.previous) {
      p = limit.previous;
    }
    
    IndexEntry newIndex = new IndexEntry(p, current.next, current);
    newIndex.previous.next = newIndex;
    newIndex.next.previous = newIndex;
    newIndex.range = indexRange;
    
    current.range -= indexRange;
  }
  
  /*
   * Finds out locations that 
   */
  private Entry<E> findSuitableEntry(E e, Entry<E> from, Entry<E> limit) {
    Entry<E> entry;
    
    if (limit == null) {
      limit = header;
    }
    
    for (entry = from; entry != limit; entry = entry.next) {
      try {
        if (e.compareTo(entry.element) < 0) {
          return entry;
        }
      } catch (Exception s) {
        s.printStackTrace();
      }
    }
    
    return entry;
  }
  
  private Entry<E> findSuitableEntry(int i, Entry<E> from) {
    Entry<E> entry = from;
    
    for (int base = 0; base < i; base++, entry = entry.next) {}
    
    return entry;
  }
  
  private Entry<E> addBefore(E e, Entry<E> entry) {
    Entry<E> newEntry = new Entry<E>(e, entry, entry.previous);
    newEntry.previous.next = newEntry;
    newEntry.next.previous = newEntry;
    size++;
    
    return newEntry;
  }
  
  private IndexEntry findSuitableIndex(E e) {
    IndexEntry entry = null;
    
    for (entry = indexHeader.next; entry != indexHeader; entry = entry.next) {
      if (entry.next != indexHeader) {
        if (entry.next.target.element != null 
             && e.compareTo(entry.next.target.element) < 0) {
          
           return entry;
        }
      } else {
        return entry;
      }
    }
    
    if (entry == indexHeader && entry.next != indexHeader) {
      entry = entry.next;
    }
    
    return entry;
  }
  
  
  /**
   * Nodes to store user data
   */
  public static class Entry<E> {
    private E element;
    
    private Entry<E> next;
    private Entry<E> previous;
    
    private Entry(E element, Entry<E> next, Entry<E> previous) {
      this.element = element;
      this.next = next;
      this.previous = previous;
    }
    
    public Entry<E> getPrevious() {
      return this.previous;
    }
    
    public Entry<E> getNext() {
      return this.next;
    }
    
    public E get() {
      return this.element;
    }
    
    public String toString() {
      return this.element.toString();
    }
  }
  
  /**
   * Additional nodes to build index structure for original list in order
   */
  private class IndexEntry {
    IndexEntry next;
    IndexEntry previous;
    
    Entry<E> target;
    
    /** Entry number to next IndexEntry*/
    int range;
    
    
    IndexEntry(Entry<E> target, IndexEntry next, IndexEntry previous) {
      this.target = target;
      this.next = next;
      this.previous = previous;
    }
    
    public String toString() {
      if (target != null && target.element != null) {
        return "[" + target.element.toString() + ", range:" + range + "]";
      }
      
      return null; 
    }
    
  }
  
  public String getAllValues() {
    StringBuffer appender = new StringBuffer();
    
    boolean hasValue = false;
    
    appender.append("[");
    for (Entry<E> e = header.next; e != header; e = e.next) {
      appender.append(e.element).append(",");
      hasValue = true;
    }
    
    if (hasValue) {
      appender.deleteCharAt(appender.length() - 1);
    }
    
    appender.append("]");
    
    return appender.toString();
  }
  
  public String getIndexStatus() {
    StringBuffer appender = new StringBuffer();
    
    boolean hasValue = false;
    
    appender.append("Index[");
    for (IndexEntry e = indexHeader.next; e != indexHeader; e = e.next) {
      if (e.target != null) {
        appender.append("from " + e.target.element)
                .append(", range " + e.range).append(",");
        hasValue = true;
      }
    }
    
    if (hasValue) {
      appender.deleteCharAt(appender.length() - 1);
    }
    
    appender.append("]");
    
    return appender.toString();
  }

}
