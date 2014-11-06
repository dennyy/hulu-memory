package com.demo.hulukv.cache;

import java.io.IOException;

import junit.framework.Assert;

import org.junit.Test;

import com.demo.hulukv.cache.AllocateFailedException;
import com.demo.hulukv.cache.DirectMemoryCache;
import com.demo.hulukv.cache.MemoryAllocateConf;
import com.demo.hulukv.cache.Pointer;

/**
 * Class Description
 *
 * @author Denny Ye
 * @since 2012-11-8
 * @version 1.0
 */
public class TestDirectMemoryCache {

  @Test
  public void allocateFixedMemory() {
    MemoryAllocateConf conf = new MemoryAllocateConf();
    
    conf.setTotalMemory(100000);
    conf.setMemorySlices(10 << 20);
    
    try {
      DirectMemoryCache cache = new DirectMemoryCache(conf);
      
      cache.close();
    } catch (IOException e) {
      e.printStackTrace();
      Assert.fail();
    }
  }
  
  @Test
  public void failedToAllocatedLargeMemory() {
    MemoryAllocateConf conf = new MemoryAllocateConf();
    
    conf.setTotalMemory(Integer.MAX_VALUE);
    
    try {
      @SuppressWarnings("unused")
      DirectMemoryCache cache = new DirectMemoryCache(conf);
      Assert.fail();
    } catch (IOException e) {
    }
    
  }
  
  @Test
  public void clearAllDatas() {
    MemoryAllocateConf conf = new MemoryAllocateConf();
    
    conf.setTotalMemory(100000);
    conf.setMemorySlices(10 << 20);
    
    DirectMemoryCache cache = null;
    try {
       cache = new DirectMemoryCache(conf);
    } catch (IOException e) {
      Assert.fail();
    }
    
    Assert.assertTrue(cache.isRunning());
    
    try {
      cache.close();
      Assert.assertEquals(0, cache.size());
      Assert.assertFalse(cache.isRunning());
    } catch (IOException e) {
      Assert.fail();
    }
    
  }
  
  @Test
  public void getSize() {
    MemoryAllocateConf conf = new MemoryAllocateConf();
    
    conf.setTotalMemory(100000);
    conf.setMemorySlices(10 << 20);
    
    DirectMemoryCache cache = null;
    try {
       cache = new DirectMemoryCache(conf);
    } catch (IOException e) {
      Assert.fail();
    }
    
    
    Pointer p = null;
    try {
      p = cache.store("haha".getBytes());
    } catch (AllocateFailedException e) {
      Assert.fail();
    }
    
    Assert.assertEquals(1, cache.size());
    
    byte[] result = cache.getData(p);
    Assert.assertEquals(4, result.length);
    Assert.assertEquals("haha", new String(result));
    
    cache.release(p);
    Assert.assertEquals(0, cache.size());
  }
  
  @Test
  public void putSingleElement() {
    MemoryAllocateConf conf = new MemoryAllocateConf();
    
    conf.setTotalMemory(100000);
    conf.setMemorySlices(10 << 20);
    DirectMemoryCache cache = null;
    try {
       cache = new DirectMemoryCache(conf);
    } catch (IOException e) {
      Assert.fail();
    }
    
    byte[] content = "haha".getBytes();
    Pointer pointer = null;
    try {
      pointer = cache.store(content);
    } catch (AllocateFailedException e) {
      
    }
    
    Assert.assertTrue(pointer != null);
    Assert.assertTrue(pointer.getIndex() >= 0 && pointer.getIndex() < conf.getCapacity());
    
    System.out.println(cache.status());
    
    byte[] result = cache.getData(pointer);
    Assert.assertEquals("haha", new String(result));
    
    cache.release(pointer);
    
    try {
      result = cache.getData(pointer);
      Assert.fail();
    } catch (Exception e) {
      
    }
  }
  
  @Test
  public void getStatus() {
    
  }
  
  @Test
  public void putIllegalElement() {
    
  }
  
  @Test
  public void putLargeSizeElement() {
    
  }
  
  @Test
  public void putTooManyElements() {
    
  }
  
  @Test
  public void putAndCheckManyElementsWithContent() {
    
  }
  
  @Test
  public void putTooManySmallElements() {
    
  }
  
}

