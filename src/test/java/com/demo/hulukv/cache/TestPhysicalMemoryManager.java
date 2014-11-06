package com.demo.hulukv.cache;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.demo.hulukv.cache.PhysicalMemoryManager;
import com.demo.hulukv.cache.Pointer;

/**
 * Class Description
 *
 * @author Denny Ye
 * @since 2012-11-18
 * @version 1.0
 */
public class TestPhysicalMemoryManager {

  @Test
  public void init() {
    Map<Integer, ByteBuffer> buffers = new HashMap<Integer, ByteBuffer>();
    
    buffers.put(0, ByteBuffer.allocate(50 << 20));
    buffers.put(-1, ByteBuffer.allocate(50 << 20));
    
    PhysicalMemoryManager manager = new PhysicalMemoryManager(buffers);
    manager.getStat(0);
  }
  
  @Test
  public void copy() {
    Map<Integer, ByteBuffer> buffers = new HashMap<Integer, ByteBuffer>();
    
    ByteBuffer first = ByteBuffer.allocate(50 << 20);
    ByteBuffer second = ByteBuffer.allocate(50 << 20);
    buffers.put(0, first);
    buffers.put(-1, second);
    
    PhysicalMemoryManager manager = new PhysicalMemoryManager(buffers);
    
    List<Pointer> allPointers = new ArrayList<Pointer>();
    
    for (int offset = 0, index = 0; offset < 50 << 20; index++, offset = offset + 100) {
      allPointers.add(new Pointer(index, 0, 0, offset, 100));
    }
    
    Pointer[] pointers = new Pointer[allPointers.size()];
    manager.memoryCopyFor(0, allPointers.toArray(pointers));
  }
}

