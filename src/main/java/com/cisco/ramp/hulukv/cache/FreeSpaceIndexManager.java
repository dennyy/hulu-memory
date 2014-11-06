package com.cisco.ramp.hulukv.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cisco.ramp.hulukv.cache.IndexedLinkedList.Entry;


/**
 * Primary function of this class is storing all indexs of free spaces
 * at memory in head.
 * 
 * Free space represents empty content with range of both offset and limit 
 * to pointer at physical direct buffer.
 * 
 * 
 * @author Denny Ye
 * @since 2012-11-13
 * @version 1.0
 */
public class FreeSpaceIndexManager {
  private static final Logger LOG = LoggerFactory.getLogger(FreeSpaceIndexManager.class);
  
  
  private byte[] mergeLock = new byte[0];
  
  private MemoryAllocateConf conf;
  
  /** Judged by fragment limitation */
  private final FreeSpaceRange large;
  private final FreeSpaceRange fragment;
  
  private volatile boolean isRunning;
  
  private int bufNum;
  
  public FreeSpaceIndexManager(MemoryAllocateConf conf, int bufNum) {
    this.conf = conf;
    this.bufNum = bufNum;
    
    large = new FreeSpaceRange(bufNum, true);
    fragment = new FreeSpaceRange(bufNum, false);
    
//    new FreeSpaceIndexArrangmentThread(this).start();
    
    isRunning = true;
  }
  
  /**
   * Allocate index to mapping at direct buffer
   * 
   * @param expectLen
   * @return Actual index or exception
   * @throws AllocateFailedException
   */
  public MemorySpace allocate(int expectLen) throws AllocateFailedException {
    if (!isRunning) {
      throw new AllocateFailedException("Free space manage has been closed");
    }

    MemorySpace free = null;
    synchronized (mergeLock) {
      free = large.allocateFromRange(expectLen);
      
      if (free == null) {
        free = fragment.allocateFromRange(expectLen);
      }
      
      if (free == null) {
        throw new AllocateFailedException("No space left for expect length " + expectLen);
      }
    }
    
    return free;
  }
  
  /**
   * Inserts into concrete location by space length
   * 
   * @param free
   */
  public void addFreeSpace(MemorySpace free) {
    synchronized (mergeLock) {
      if (free.getSpaceLen() < conf.getFragmentLimit()) {
        fragment.addFreeSpaceToRange(free);
      } else {
        large.addFreeSpaceToRange(free);
      }
    }
  }
  
  /**
   * Arrange memory for each physical memory
   */
  public void arrangeMemory() {
    for (int i = 0; i < bufNum; i++) {
      if (shouldArrange(i)) {
        
        LOG.info("Start to index arrangement ");
        synchronized (mergeLock) {
          
          int mergeCount = 0;
          long start = System.currentTimeMillis();
          
          IndexedLinkedList<MemorySpace> largeChain = large.getSpecifiedChain(i);
          IndexedLinkedList<MemorySpace> fragmentChain = fragment.getSpecifiedChain(i);
          
          int initLarge = largeChain.size();
          int initFragment = fragmentChain.size();
          
          Set<MemorySpace> needToRemove = new HashSet<MemorySpace>();
          for (int j = 0; j < fragmentChain.size(); j++) {
            MemorySpace fragment = fragmentChain.get(j).get();
            
            Entry<MemorySpace> insertPoint = largeChain.getNext(fragment);
            
            boolean hasMerged = false;
            if (insertPoint != null) {
              if (fragment.getLimit() == insertPoint.get().getOffset()) {
                fragment = new MemorySpace(i, fragment.getOffset(), insertPoint.get().getLimit());
                
                largeChain.remove(insertPoint.get());
                
                needToRemove.add(fragment);
                hasMerged = true;
                mergeCount++;
              }
              
              if (insertPoint.getPrevious() != null  
                    &&  insertPoint.getPrevious().get() != null 
                          && insertPoint.getPrevious().get().getLimit() == fragment.getOffset()) {
                fragment = new MemorySpace(i, insertPoint.getPrevious().get().getOffset(), fragment.getLimit());
                
                largeChain.remove(insertPoint.getPrevious().get());
                
                needToRemove.add(fragment);
                hasMerged = true;
                mergeCount++;
              }
            }
            
            if (hasMerged) {
              largeChain.put(fragment);
            }
            
          }
          
          if (needToRemove.size() > 0) {
            Iterator<MemorySpace> it = needToRemove.iterator();
            while (it.hasNext()) {
              MemorySpace mp = it.next();
              fragmentChain.remove(mp);
            }
          }
          
          long cost = System.currentTimeMillis() - start;
          
          LOG.info("Index merge finished. Merged:" + mergeCount + " spaces used " + cost + " ms." +
              " Before merge : large " + initLarge + " fragment " + initFragment + ". After merge : " +
              "large " + largeChain.size() + " fragment " + fragmentChain.size());
        }
      }
    }
  }
  
  private boolean shouldArrange(int directId) {
    // TODO
    SpaceChain chain = fragment.spacesChainMap.get(directId);
    return chain.spacesNumAtChain.get() >= 100 || chain.spacesLengthAtChain.get() >= 10 << 20;
  }
  
  public String getStat(int directId) {
    SpaceChain chain = fragment.spacesChainMap.get(directId);
    return chain.spacesNumAtChain.get() + "," + chain.spacesLengthAtChain.get();
  }
  
  public MemoryAllocateConf getConf() {
    return this.conf;
  }
  
  /**
   * Component status of index manager
   * 
   * @return
   */
  public String getStatus() {
    StringBuffer appender = new StringBuffer();
    
    appender.append(Transformer.strFormat("Large(>=" 
        + Transformer.getSpecificSize(conf.getFragmentLimit())  + ") " 
        + large.getSpacesNumAtRange() + " spaces " 
        + large.getSpacesLengthAtRange() + " bytes", 45));
    
    for (int i = 0; i < bufNum; i++) {
      appender.append(Transformer.strFormat(large.spacesChainMap.get(i).spacesNumAtChain + " spaces " 
          + large.spacesChainMap.get(i).spacesLengthAtChain + " bytes", 100));
    }
    appender.append("\n");
    
    appender.append(Transformer.strFormat("Fragment " 
        + fragment.getSpacesNumAtRange() + " spaces " 
        + fragment.getSpacesLengthAtRange() + " bytes", 45));
    
    for (int i = 0; i < bufNum; i++) {
      appender.append(Transformer.strFormat(fragment.spacesChainMap.get(i).spacesNumAtChain + " spaces " 
          + fragment.spacesChainMap.get(i).spacesLengthAtChain + " bytes", 100));
    }
    return appender.toString();
  }
  
  public void close() {
    isRunning = false;
    
    large.clear();
    fragment.clear();
  }
  
  /**
   * this class manages all free spaces with range under limit
   * (range low limit < free space lenght(limit - offset) <= range low limit)
   * 
   * Range is user defined level to store released/free spaces with space range.
   * For example, range 30 means space limit minus space offset is 30.
   * 
   * @author Denny Ye
   * @since 2012-11-13
   * @version 1.0
   */
  private class FreeSpaceRange {
    
    private final Map<Integer, SpaceChain> spacesChainMap;
    private final List<SpaceChain> spacesChainList; //Order to shuffle each time
    
    private FreeSpaceRange(int bufNum, boolean isAllocateDirect) {
      this.spacesChainMap = new HashMap<Integer, SpaceChain>();
      this.spacesChainList = new ArrayList<SpaceChain>();
      for (int i = 0; i < bufNum; i++) {
        SpaceChain chain = new SpaceChain(i, isAllocateDirect);
        this.spacesChainMap.put(i, chain);
        this.spacesChainList.add(chain);
      }
    }
    
    /**
     * @param expectLen
     * @return null if missing allocation
     */
    private MemorySpace allocateFromRange(int expectLen) {
      Collections.shuffle(spacesChainList);
      
      MemorySpace free = null;
      for (SpaceChain chain : spacesChainList) {
        free = chain.findSuitableFreeSpace(expectLen);
        
        if (free != null) {
          LOG.debug("Allocate free space " + free);
          break;
        }
      }
      
      return free;
    }
    
    private void addFreeSpaceToRange(MemorySpace free) {
      SpaceChain chain = spacesChainMap.get(free.getDirectByteId());
      chain.addSpace(free);
    }
    
    private IndexedLinkedList<MemorySpace> getSpecifiedChain(int directId) {
      SpaceChain chain = spacesChainMap.get(directId);
      return chain.getChain();
    }
    
    private long getSpacesLengthAtRange() {
      long total = 0L;
      
      for (SpaceChain chain : spacesChainList) {
        total += chain.spacesLengthAtChain.get();
      }
      
      return total;
    }
    
    private int getSpacesNumAtRange() {
      int total = 0;
      
      for (SpaceChain chain : spacesChainList) {
        total += chain.spacesNumAtChain.get();
      }
      
      return total;
    }
    
    private void clear() {
      for (SpaceChain chain : spacesChainList) {
        chain.clearChain();
      }
    }
    
  }
  
  /**
   * This class contains two parts : 
   * 1. List that managed all free spaces
   * 2. additional status/metadata of this list
   *
   * @author Denny Ye
   * @since 2012-11-13
   * @version direct
   */
  private class SpaceChain {
    
    private int directMemoryId;
    
    /** total size of all spaces at this chain */
    private AtomicLong spacesLengthAtChain = new AtomicLong();
    private AtomicInteger spacesNumAtChain = new AtomicInteger(); 
    
    private int initCapacity = -1;
    
    private final IndexedLinkedList<MemorySpace> chain;
    
    private boolean isLarge;
    
    private SpaceChain(int directId, boolean isAllocateDirect) {
      this.directMemoryId = directId;
      
      this.isLarge = isAllocateDirect;
      this.chain = new IndexedLinkedList<MemorySpace>(100);
    }
    
    /**
     * Adds free space into chain
     * Merge with contiguous free space if necessary
     * 
     * @param free
     */
    private void addSpace(MemorySpace free) {
//      int oriSize = chain.size();
      addSpaceToList(chain, free);
      
      spacesNumAtChain.addAndGet(1);
      spacesLengthAtChain.addAndGet(free.getSpaceLen());
      
      if (initCapacity == -1) {
        initCapacity = free.getSpaceLen();
      }
    }
    
    /**
     * Need to merge with other ranges in same direct buffer
     * 
     * @return
     */
    private IndexedLinkedList<MemorySpace> getChain() {
      return this.chain;
    }
    
    
    private void clearChain() {
      this.chain.clear();
      
      this.spacesLengthAtChain.set(0L);
      this.spacesNumAtChain.set(0);
    }
    
    /**
     * @param expectLen
     * @return null if it cannot find suitable space
     */
    private MemorySpace findSuitableFreeSpace(int expectLen) {
      MemorySpace fakeSpace = new MemorySpace(directMemoryId, 0, expectLen);
      
      Entry<MemorySpace> space = this.chain.getNext(fakeSpace);
      if (space == null) {
        return null;
      }
      
      MemorySpace needHandle = space.get();
      if (needHandle != null) {
        if (needHandle.getSpaceLen() >= expectLen) {
          boolean needSplit = (needHandle.getSpaceLen() - expectLen) > 0 ;
          
          this.chain.remove(needHandle);
          
          this.spacesLengthAtChain.addAndGet(-needHandle.getSpaceLen());
          if (needSplit) {
            MemorySpace result = new MemorySpace(directMemoryId, needHandle.getOffset(), 
                                                needHandle.getOffset() + expectLen);
            
            MemorySpace left = new MemorySpace(directMemoryId, needHandle.getOffset() + expectLen, 
                                          needHandle.getLimit());
            
            if (left.getSpaceLen() < conf.getFragmentLimit()) {
              fragment.addFreeSpaceToRange(left);
              this.spacesNumAtChain.decrementAndGet();
            } else {
              this.chain.put(left);
              this.spacesLengthAtChain.addAndGet(left.getSpaceLen());
            }
            
            return result;
          } else {
            this.spacesNumAtChain.decrementAndGet();
            return needHandle;
          }
        }
      }
      
      return null;
    }
    
    /**
     * Adds into list and merge continuous spaces.
     * <br>
     * before.getLimit == current.getOffset (if before is existing)
     * current.getLimit == after.getOffset (if after is existing)
     * 
     * @param list
     * @param space
     */
    private void addSpaceToList(IndexedLinkedList<MemorySpace> list, MemorySpace space) {
      synchronized (list) {
        
        List<MemorySpace> needToRemove = new ArrayList<MemorySpace>();
        Entry<MemorySpace> insertPoint = list.getNext(space);
        if (insertPoint != null) {
          if (insertPoint.get() != null) {
            if (space.getLimit() == insertPoint.get().getOffset()) {
              space = new MemorySpace(directMemoryId, space.getOffset(), insertPoint.get().getLimit());
              
              needToRemove.add(insertPoint.get());
            }
          }
          
          if (insertPoint.getPrevious() != null && insertPoint.getPrevious().get() != null) {
            MemorySpace before = insertPoint.getPrevious().get();
            if (before.getLimit() == space.getOffset()) {
              space = new MemorySpace(directMemoryId, before.getOffset(), space.getLimit());
              
              needToRemove.add(before);
            }
          }
        }
        
        for (MemorySpace removePoint : needToRemove) {
          list.remove(removePoint);
          this.spacesNumAtChain.decrementAndGet();
        }
        
        if (!isLarge && space.getSpaceLen() > conf.getFragmentLimit()) {
          large.addFreeSpaceToRange(space);
          
          this.spacesNumAtChain.decrementAndGet();
          this.spacesLengthAtChain.addAndGet(-space.getSpaceLen());
        } else {
          list.put(space);
        }
      }
    }
    
  }
  
}

