package com.cisco.ramp.hulukv.cache;

/**
 * Class Description
 *
 * @author Denny Ye
 * @since 2012-11-8
 * @version 1.0
 */
public class MemoryAllocateConf {

  /** Expect memory size at off-heap */
  private long totalMemory =  1 << 30;
  
  /** Split total memory into multiple slices */
  private int memorySlices = 1 << 30;
  
  /** Maximum elements that cache can holds */
  private int capacity = 1000000;

  /** Thread interval to run each time. unit:ms */
  private int arrangementThreadInterval = 1000;
  
  /** All fragments should be less than this limit */
  private int fragmentLimit = 1 << 20;
  
  /** Physical memory arrangement means migrate all allocated memory
   * from one direct buffer to another. 
   * It might be heavy for application
   */
  private boolean enablePhysicalArrangment = false;
  
  
  public long getTotalMemory() {
    return totalMemory;
  }

  public void setTotalMemory(long totalMemory) {
    if (totalMemory <= 0) {
      throw new IllegalArgumentException("negative memory");
    }
    
    this.totalMemory = totalMemory;
  }
  
  public void setTotalMemory(int total, DataUnit unit) {
    if (total <= 0) {
      throw new IllegalArgumentException("negative memory");
    }
    
    this.totalMemory = ((long) total) * unit.getBase();
  }
  
  public int getMemorySlices() {
    return memorySlices;
  }

  public void setMemorySlices(int memorySlices) {
    if (memorySlices <= 0) {
      throw new IllegalArgumentException("memory slice");
    }
    
    this.memorySlices = memorySlices;
  }

  public int getCapacity() {
    return capacity;
  }

  public void setCapacity(int capacity) {
    if (capacity <= 0) {
      throw new IllegalArgumentException("negative capacity");
    }
    
    this.capacity = capacity;
  }

  public int getArrangementThreadInterval() {
    return arrangementThreadInterval;
  }

  public void setArrangementThreadInterval(int arrangementThreadInterval) {
    if (arrangementThreadInterval <= 0) {
      throw new IllegalArgumentException("thread interval. unit:ms");
    }
    
    this.arrangementThreadInterval = arrangementThreadInterval;
  }

  public boolean isEnablePhysicalArrangment() {
    return enablePhysicalArrangment;
  }

  public void setEnablePhysicalArrangment(boolean enablePhysicalArrangment) {
    this.enablePhysicalArrangment = enablePhysicalArrangment;
  }

  public int getFragmentLimit() {
    return fragmentLimit;
  }

  public void setFragmentLimit(int fragmentLimit) {
    this.fragmentLimit = fragmentLimit;
  }

  
}

