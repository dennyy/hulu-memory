package com.cisco.ramp.hulukv.cache;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Primary goal of this class is managing multiple buffers with allocation,
 * arrangement and other else.
 * 
 *
 * @author Denny Ye
 * @since 2012-11-13
 * @version 1.0
 */
public class PhysicalMemoryManager {
  private static final Logger LOG = LoggerFactory.getLogger(PhysicalMemoryManager.class);
  
  private Map<Integer, Record> bytesRecords;
  private Map<Integer, ByteBuffer> physicalBuffers;
  PhysicalMemoryManager(Map<Integer, ByteBuffer> buffers) {
    this.physicalBuffers = buffers;
    
    this.bytesRecords = new LinkedHashMap<Integer, Record>();
    
    for (Map.Entry<Integer, ByteBuffer> entry : buffers.entrySet()) {
      if (entry.getKey() >= 0) {
        bytesRecords.put(entry.getKey(), new Record());
      }
    }
  }
  
  
  /**
   * Writes data into buffer
   * 
   * @param space
   * @param data
   * @return no exception
   */
  public void writeData(Pointer pointer, byte[] data) {
    ByteBuffer buffer = physicalBuffers.get(pointer.getDirectMemoryId());
    
    synchronized (buffer) {
      buffer.limit(pointer.getOffset() + pointer.getLen());
      buffer.position(pointer.getOffset());
      
      buffer.put(data);
      
      Record record = bytesRecords.get(pointer.getDirectMemoryId());
      record.allocatedBytes.addAndGet(data.length);
      record.allocatedNum.incrementAndGet();
    }
  }
  
  public byte[] readData(Pointer pointer) {
    ByteBuffer buffer = physicalBuffers.get(pointer.getDirectMemoryId());
    
    synchronized (buffer) {
      buffer.limit(pointer.getOffset() + pointer.getLen());
      buffer.position(pointer.getOffset());
      
      byte[] result = new byte[pointer.getLen()];
      buffer.get(result);
      
      return result;
    }
  }
  
  public void release(Pointer pointer) {
    Record record = bytesRecords.get(pointer.getDirectMemoryId());
    
    record.allocatedBytes.addAndGet(-pointer.getLen());
    record.allocatedNum.decrementAndGet();
  }
  
  public String getStatus() {
    StringBuffer appender = new StringBuffer();
    
    
    appender.append(Transformer.strFormat("MEMORY", 45));
    
    for (Map.Entry<Integer, Record> entry : bytesRecords.entrySet()) {
      appender.append(Transformer.strFormat("memory#" + entry.getKey() + " - " + entry.getValue().allocatedNum.get() 
          + " datas used " + entry.getValue().allocatedBytes.get() + " bytes(total " 
          + physicalBuffers.get(entry.getKey()).capacity() + "), " 
          + ((float) entry.getValue().allocatedBytes.get() * 100 / physicalBuffers.get(entry.getKey()).capacity()) 
          + "%", 100));
    }
    
    appender.append("\n");
    
    return appender.toString();
  }
  
  public String getStat(int directId) {
    Record record = bytesRecords.get(directId);
    ByteBuffer buffer = physicalBuffers.get(directId);
    
    return record.allocatedBytes.get() + "," + buffer.capacity();
  }
  
  void memoryCopyFor(int directId, Pointer[] allPointers) {
    ByteBuffer original = physicalBuffers.get(directId);
    
    ByteBuffer target = physicalBuffers.get(-1);
    long start = System.currentTimeMillis();
    
    int index = 0;
    
    int copyCount = 0;
    for (int i = 0; i < allPointers.length; i++) {
      Pointer p = allPointers[i];
      
      if (p.getDirectMemoryId() == directId) {
        byte[] buf = new byte[p.getLen()];
        
        original.limit(p.getOffset() + p.getLen());
        original.position(p.getOffset());
        
        original.get(buf);
        
        target.limit(index + buf.length);
        target.position(index);
        target.put(buf);
        
        p.setOffset(index);
        index += buf.length;
        
        copyCount++;
      }
    }
    
    physicalBuffers.put(-1, original);
    physicalBuffers.put(directId, target);
    
    long cost = System.currentTimeMillis() - start;
    
    LOG.info("Finished arrangement for memory#" + directId + ", copied datas: " + copyCount 
        + " bytes: " + target.limit() + ", cost:" + cost + " ms");
  }
  
  public void close() {
    physicalBuffers.clear();
    bytesRecords.clear();
  }
  
  private class Record {
    
    private AtomicLong allocatedBytes = new AtomicLong();
    private AtomicInteger allocatedNum = new AtomicInteger();
    
    public String toString() {
      return allocatedBytes.get() + ", " + allocatedNum.get();
    }
    
  }
  
}

