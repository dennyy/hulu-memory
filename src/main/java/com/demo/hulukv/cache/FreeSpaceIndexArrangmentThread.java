package com.demo.hulukv.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Primary purposes:
 * 1. Merge continuous free space to large space
 * 2. Move inapposite space into other range
 *
 * @author Denny Ye
 * @since 2012-11-9
 * @version 1.0
 */
public class FreeSpaceIndexArrangmentThread extends Thread {
  
  private static final Logger LOG = LoggerFactory.getLogger(FreeSpaceIndexArrangmentThread.class);
  
  private FreeSpaceIndexManager spaceManager;
  public FreeSpaceIndexArrangmentThread(FreeSpaceIndexManager mgr) {
    this.spaceManager = mgr;
    
    this.setDaemon(true);
  }
  
  public void run() {
    int interval = spaceManager.getConf().getArrangementThreadInterval();
    
    while (true) {
      try {
        spaceManager.arrangeMemory();
      } catch (Exception e) {
        LOG.warn("Failed to arrange memory", e);
      }
      
      try {
        Thread.sleep(interval);
      } catch (InterruptedException e) {}
    }
  }
}

