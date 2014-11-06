package com.cisco.ramp.hulukv.cache;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class Description
 *
 * @author Denny Ye
 * @since 2012-11-8
 * @version 1.0
 */
public class DirectMemoryCache {

  private static final Logger LOG = LoggerFactory.getLogger(DirectMemoryCache.class);
  
  /** Mapping from pointer to physical data located at direct memory */
  private Pointer[] allocateMapping;
  private LinkedBlockingQueue<Integer> unAllocatedIndexQueue;
  
  private AtomicLong indexIdentity = new AtomicLong();
  /** elements that allocated from direct memory */
  private AtomicInteger dataNum = new AtomicInteger();
  
  private final ReentrantReadWriteLock memoryArrangementLock = new ReentrantReadWriteLock(true);
  
  private final ReadLock readLock = memoryArrangementLock.readLock();
  private final WriteLock writeLock = memoryArrangementLock.writeLock();
  
  private MemoryAllocateConf conf;
  
  private final PhysicalMemoryManager physicalManager;
  private final FreeSpaceIndexManager freeIndexManager;
  
  private String nameDescriptor;
  private volatile boolean isRunning;
  
  public DirectMemoryCache(MemoryAllocateConf conf) throws IOException {
    this.conf = conf;
    
    Map<Integer, ByteBuffer> buffers = new HashMap<Integer, ByteBuffer>();
    Set<MemorySpace> initSpaces = new LinkedHashSet<MemorySpace>();
    
    try {
      //Allocate physical memory that user required
      int index = 0;
      for (long remaining = conf.getTotalMemory(); remaining > 0; index++) {
          if (remaining >= conf.getMemorySlices()) {
            initSpaces.add(new MemorySpace(index, 0, conf.getMemorySlices()));
            buffers.put(index, ByteBuffer.allocateDirect(conf.getMemorySlices()));
            remaining -= conf.getMemorySlices();
          } else {
            initSpaces.add(new MemorySpace(index, 0, (int) remaining));
            buffers.put(index, ByteBuffer.allocateDirect((int) remaining));
            remaining = 0;
          }
      }
      
      //Allocate memory that for spare
      if (conf.isEnablePhysicalArrangment()) {
        buffers.put(-1, ByteBuffer.allocateDirect(conf.getMemorySlices()));
      }
    } catch (Throwable e) {
      LOG.warn("Maximum setting of direct memory is " 
          + Transformer.getSpecificSize(DirectUtils.getDirectMemorySize()) 
          + ", compares with user expect size " 
          + Transformer.getSpecificSize(conf.getTotalMemory())
          + ". Using JVM option : " + DirectUtils.MAX_DIRECT_MEMORY_PARAM);
      
      throw new IOException("Failed to allocate ", e);
    }
    
    this.physicalManager = new PhysicalMemoryManager(buffers);
    this.freeIndexManager = new FreeSpaceIndexManager(conf, initSpaces.size());
    
    for (MemorySpace free : initSpaces) {
      this.freeIndexManager.addFreeSpace(free);
    }
    
    allocateMapping = new Pointer[conf.getCapacity()];
    unAllocatedIndexQueue = new LinkedBlockingQueue<Integer>();
    for (int i = 0; i < conf.getCapacity(); i++) {
      unAllocatedIndexQueue.add(i);
    }
    
    nameDescriptor = "DM-" + UUID.randomUUID();
    
    
    isRunning = true;
    
    if (conf.isEnablePhysicalArrangment()) {
      new PhysicalMemoryArrangementThread(this, initSpaces.size()).start();
    }
    
    LOG.info("Direct memory allocated successfully. Name:" + nameDescriptor + ",Total:" 
            + Transformer.getSpecificSize(conf.getTotalMemory()) 
            + ", capacity:" + conf.getCapacity() + ", memory:" + initSpaces);
  }
  
  
  /**
   * Stores byte content into direct memory 
   * 
   * @param buffer
   * @return
   * @exception AllocateFailedException
   */
  public Pointer store(byte[] data) throws AllocateFailedException {
    if (!isRunning) {
      throw new AllocateFailedException("inactive cache");
    }
    
    if (!tryLockShared()) {
      throw new DirectCacheException("Cannot obtains lock to store data ");
    }
    
    Pointer pointer = null;
    try {
      Integer index = -1;
      try {
        index = unAllocatedIndexQueue.poll(1, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        throw new AllocateFailedException("Wait thread has been interrupted");
      }
      
      if (index == null) {
        throw new AllocateFailedException("No capacity left. Current capacity is :" + allocateMapping.length);
      }
      
      MemorySpace space = null;
      try {
        space = freeIndexManager.allocate(data.length);
      } catch (AllocateFailedException e) {
        //try to allocate from fragment
        LOG.debug("Failed to allocation at large spaces for length " + data.length, e);
        
        unAllocatedIndexQueue.add(index);//return to pool
        throw e;
      }
      
      if (space == null) {
        throw new AllocateFailedException("Un-expect space for expect length " + data.length);
      }
      
      
      pointer = new Pointer(index, space.getDirectByteId(), 
          indexIdentity.incrementAndGet(), space.getOffset(), space.getSpaceLen());
      physicalManager.writeData(pointer, data);
      allocateMapping[index] = pointer;
      dataNum.incrementAndGet();
    } finally {
      unlockShared();
    }
    
    return pointer;
  }
  
  /**
   * Pay attention to no match/missing case
   * 
   * @param pointer
   * @return
   */
  public byte[] getData(Pointer pointer) {
    if (!isRunning) {
      throw new IllegalStateException("inactive cache");
    }
    
    if (pointer == null) {
      throw new IllegalArgumentException("pointer");
    }
    
    if (!tryLockShared()) {
      throw new DirectCacheException("Cannot obtains lock to get data ");
    }
    
    try {
      Pointer mapping = allocateMapping[pointer.getIndex()];
      if (mapping == null) {
        throw new DirectCacheException("No stored pointer. user:" + pointer);
      }
      
      if (pointer.equals(mapping)) {
        return physicalManager.readData(pointer);
      } else {
        throw new DirectCacheException("No match pointer. user:" 
            + pointer + ", stored:" + mapping);
      }
    } finally {
      unlockShared();
    }
  }
  
  /**
   * Release space and return space to space manager
   * 
   * @param pointer
   */
  public void release(Pointer pointer) {
    if (!isRunning) {
      throw new IllegalStateException("inactive cache");
    }
    
    if (pointer == null) {
      throw new IllegalArgumentException("pointer");
    }
    
    if (!tryLockShared()) {
      throw new DirectCacheException("Cannot obtains lock to release data ");
    }
    
    try {
      Pointer mapping = allocateMapping[pointer.getIndex()];
      if (mapping == null) {
        throw new DirectCacheException("No stored pointer. user:" + pointer);
      }
      
      if (pointer.equals(mapping)) {
// XXX
//        MemorySpace freeSpace = new MemorySpace(pointer.getDirectMemoryId(), pointer.getOffset(), 
//            pointer.getOffset() + pointer.getLen());
        
        MemorySpace freeSpace = pointer.toMemorySpace();
        physicalManager.release(pointer);
        freeIndexManager.addFreeSpace(freeSpace);
        
        allocateMapping[pointer.getIndex()] = null;
        unAllocatedIndexQueue.add(pointer.getIndex());
        
        dataNum.decrementAndGet();
      } else {
        throw new DirectCacheException("No match pointer. user:" 
            + pointer + ", stored:" + mapping);
      }
    } finally {
      unlockShared();
    }
  }
  
  
  /**
   * Cache status : 
   * [$totalMemoty-$used-$fragment:$usedRatio, total datas]
   * 
   * @return
   */
  public String status() {
    StringBuffer appender = new StringBuffer(100);
    
    appender.append(physicalManager.getStatus())
            .append(freeIndexManager.getStatus());
    
    return appender.toString();
  }
  
  /**
   * @return
   */
  public boolean isRunning() {
    return this.isRunning;
  }
  
  public int size() {
    return dataNum.get();
  }
  
  public MemoryAllocateConf getConf() {
    return this.conf;
  }
  
  void arrangeMemory(int directId) {
    lockExclusive();
    
    try {
      physicalManager.memoryCopyFor(directId, allocateMapping);
    } finally {
      unlockExclusive();
    }
  }
  
  private void lockExclusive(){
    writeLock.lock();
  }
  
  private void unlockExclusive()  {
    writeLock.unlock();
  }

  private boolean tryLockShared() {
    try {
      return readLock.tryLock(10, TimeUnit.SECONDS);
    } catch (InterruptedException ex) {
      LOG.warn("Interrupted while waiting for log shared lock", ex);
    }
    
    return false;
  }

  private void unlockShared()  {
    readLock.unlock();
  }
  
  PhysicalMemoryManager getPhysicalMemoryManager() {
    return this.physicalManager;
  }
  
  FreeSpaceIndexManager getFreeSpaceIndexManager() {
    return this.freeIndexManager;
  }

  public void close() throws IOException {
    if (!isRunning) {
      LOG.info("Direct cache " + nameDescriptor + " is not running");
      return;
    }
    
    LOG.info("Start to close direct cache " + nameDescriptor);
    
    physicalManager.close();
    freeIndexManager.close();
    
    allocateMapping = null;
    unAllocatedIndexQueue.clear();
    
    this.dataNum.set(0);
    
    isRunning = false;
    LOG.info("Direct cache closed " + nameDescriptor);
  }
  
}

