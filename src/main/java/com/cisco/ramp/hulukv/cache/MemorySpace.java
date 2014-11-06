package com.cisco.ramp.hulukv.cache;

/**
 * This class represents memory space no matter large or less
 *
 * @author Denny Ye
 * @since 2012-11-9
 * @version 1.0
 */
public class MemorySpace implements Comparable<MemorySpace>{
  
  private int directByteId;
  
  private int offset;
  private int limit;
  
  private int spaceLen;
  
  public MemorySpace(int id, int offset, int limit) {
    if (offset < 0 || limit <= 0 || offset >= limit) {
      throw new IllegalArgumentException("offset:" + offset + ", limit:" + limit);
    }
    
    this.directByteId = id;
    
    this.offset = offset;
    this.limit = limit;
    this.spaceLen = limit - offset;
  }
  
  public int getDirectByteId() {
    return directByteId;
  }
  
  public int getOffset() {
    return offset;
  }
  
  public void setOffset(int offset) {
    this.offset = offset;
  }
  
  public int getLimit() {
    return limit;
  }
  
  public void setLimit(int limit) {
    this.limit = limit;
  }
  
  public int getSpaceLen() {
    return spaceLen;
  }
  
  public boolean equals(Object target) {
    if (target == null) {
      return false;
    }
    
    if (!(target instanceof MemorySpace)) {
      return false;
    }
    
    MemorySpace abo = (MemorySpace) target;
    
    return abo.getDirectByteId() == this.directByteId 
                  && abo.getOffset() == this.limit 
                        && abo.getSpaceLen() == this.spaceLen;
  }
  
  public int hashCode() {
    int hash = directByteId * 31;
    hash *= (limit * 31);
    hash *= (spaceLen * 31);
    
    return hash;
  }
  
  public String toString() {
    return "[space#" + this.directByteId + ":" + this.offset 
                  + "-" + this.limit + ":" + this.spaceLen + "]";
  }

  @Override
  public int compareTo(MemorySpace o) {
    int diff = this.offset - o.offset;
    if (diff == 0) {
      diff = this.spaceLen - o.spaceLen;
    }
    
    return diff;
  }
  
  public Pointer toPointer(int index, long stamp) {
    return new Pointer(index, directByteId, stamp, offset, spaceLen);
  }

}

