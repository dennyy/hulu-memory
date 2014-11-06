package com.cisco.ramp.hulukv.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Arrangement for physical memory (direct buffer)
 *
 * @author Denny Ye
 * @since 2012-11-13
 * @version 1.0
 */
public class PhysicalMemoryArrangementThread extends Thread {
  private static final Logger LOG = LoggerFactory.getLogger(PhysicalMemoryArrangementThread.class);
  
  private DirectMemoryCache cache;
  private int bufferNum;
  private long lastDone = 0L;
  
  public PhysicalMemoryArrangementThread(DirectMemoryCache cache, int bufferNum) {
    super("PhysicalMemoryArrangement");
    
    this.cache = cache;
    this.bufferNum = bufferNum;
    
    this.setDaemon(true);
  }
  
  public void run() {
    while (cache.isRunning()) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {}
      
      int directId = pickUpDirectToArrange();
      if (directId >= 0) {
        try {
          cache.arrangeMemory(directId);
        } catch (Throwable e) {
          LOG.warn("Failed to arrange physical memeory", e);
        }
        
        lastDone = System.currentTimeMillis();
      }
    }
      
  }
  
  private int pickUpDirectToArrange() {
    
    if (lastDone != 0 && System.currentTimeMillis() - lastDone < 10000) {//10s
      return -1;
    }
    
    for (int i = 0; i < bufferNum; i++) {
      String stat = cache.getFreeSpaceIndexManager().getStat(i);
      
      String[] parts = stat.split(",");
      long freeSpaceBytes = Long.parseLong(parts[1]);
      
      stat = cache.getPhysicalMemoryManager().getStat(i);
      parts = stat.split(",");
      
      long capacity = Integer.parseInt(parts[1]);
      
      if (((float)freeSpaceBytes / capacity) > 0.8) {
        LOG.info("fragment rate of memory#" + i + " is " 
                + (((float)freeSpaceBytes / capacity)) + ", physical begining");
        return i;
      }
    }
    
    return -1;
  }
  
}

