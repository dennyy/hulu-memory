package com.demo.hulukv.cache;

/**
 * Pointer to direct memory
 *
 * @author Denny Ye
 * @since 2012-11-8
 * @version 1.0
 */
public class Pointer {

  /** index less than cache capacity */
  private int index;
  
  private int directMemoryId;
  
  private long timestamp;
  
  private int offset;
  private int len;
  
  public Pointer(int index, int directId, long stamp, int offset, int len) {
    this.index = index;
    this.directMemoryId = directId;
    
    this.timestamp = stamp;
    
    this.offset = offset;
    this.len = len;
  }

  public int getIndex() {
    return index;
  }

  public long getTimestamp() {
    return timestamp;
  }
  
  public int getOffset() {
    return offset;
  }
  
  void setOffset(int offset) {
    this.offset = offset;
  }

  public int getLen() {
    return len;
  }
  
  public int getDirectMemoryId() {
    return directMemoryId;
  }
  
  public String toString() {
    return "Pointer[index:" + index + ", stamp:" + timestamp 
                      + ", offset:" + offset + ", len:" + len + "]";
  }
  
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    
    if (!(obj instanceof Pointer)) {
      return false;
    }
    
    Pointer b = (Pointer) obj;
    return this.index == b.index && this.timestamp == b.timestamp;
  }

  public MemorySpace toMemorySpace() {
    return new MemorySpace(directMemoryId, offset, (offset + len));
  }
  
}

