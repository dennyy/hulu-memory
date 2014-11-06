package com.demo.hulukv.cache;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.LinkedBlockingQueue;

import junit.framework.Assert;

import org.junit.Test;

import com.demo.hulukv.cache.AllocateFailedException;
import com.demo.hulukv.cache.DataUnit;
import com.demo.hulukv.cache.DirectMemoryCache;
import com.demo.hulukv.cache.MemoryAllocateConf;
import com.demo.hulukv.cache.Pointer;

/**
 * Class Description
 *
 * @author Denny Ye
 * @since 2012-11-10
 * @version 1.0
 */
public class TestPerf {

  /**
   * Round#1: 200MB, 720000 allocates /sec
   * Round#2: 200MB, 45w allocates /sec updated architecture
   */
  public void singleThreadAllocationAllSpaces() {
    MemoryAllocateConf conf = new MemoryAllocateConf();
    
    conf.setTotalMemory(200, DataUnit.MB);
    
    DirectMemoryCache cache = null;
    try {
      cache = new DirectMemoryCache(conf);
    } catch (IOException e) {
      Assert.fail();
    }
    
    byte[] buf = new byte[230];
    long start = System.currentTimeMillis();
    
    try {
      while (true) {
        cache.store(buf);
      }
    } catch (AllocateFailedException e) {
      cache.status();
    }
    
    long cost = System.currentTimeMillis() - start;
    System.out.println("Cost:" + cost);
    
    try {
      cache.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  @Test
  public void singleThreadAllocationAllSpacesAtMultipleBuffers() {
    
  }
  
  /**
   * Round#1: 80w/sec in order
   * Round#2: 3w/sec in random order
   */
  @Test
  public void singleThreadAllocationAndReleaseAllSpace() {
    MemoryAllocateConf conf = new MemoryAllocateConf();
    
    conf.setTotalMemory(200, DataUnit.MB);
    conf.setMemorySlices(100 << 20);
    conf.setArrangementThreadInterval(3000);
//    conf.setCapacity(10000);
    
    DirectMemoryCache cache = null;
    try {
      cache = new DirectMemoryCache(conf);
    } catch (IOException e) {
      Assert.fail();
    }
    
    byte[] buf = new byte[230];
    long start = System.currentTimeMillis();
    
    ArrayList<Pointer> pointers = new ArrayList<Pointer>(conf.getCapacity()); 
    try {
      while (true) {
        pointers.add(cache.store(buf));
      }
    } catch (AllocateFailedException e) {
      System.out.println("Failed to allocate : \n" + cache.status());
    }
    
    long t2 = System.currentTimeMillis();
    long cost = t2 - start;
    System.out.println("Allocation cost:" + cost);
    
    new Checker(cache).start();
    
//    for (int i = pointers.size() - 1; i >= 0 ; i--) {
//      cache.release(pointers.get(i));
//    }
    
//  for (int i = 0; i < pointers.size() ; i++) {
//    cache.release(pointers.get(i));
//  }
    
    Collections.shuffle(pointers);
    for (Pointer p : pointers) {
      cache.release(p);
    }
    
    long cost2 = System.currentTimeMillis() - t2;
    System.out.println("Release all. " + cost2);
    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {}
  }
  
 
  public void multipleThreadsAllocationAllSpaces() {
    MemoryAllocateConf conf = new MemoryAllocateConf();
    
    conf.setTotalMemory(200, DataUnit.MB);
    conf.setMemorySlices(100 << 20);
    conf.setArrangementThreadInterval(3000);
    
    DirectMemoryCache cache = null;
    try {
      cache = new DirectMemoryCache(conf);
    } catch (IOException e) {
      Assert.fail();
    }
    
    LinkedBlockingQueue<Pointer> queue = new LinkedBlockingQueue<Pointer>(100000);
    new Requier(cache, queue).start();
    new Retriever(cache, queue).start();
    new Checker(cache).start();
    
    try {
      Thread.sleep(100000);
    } catch (InterruptedException e) {}
  }
  
  @Test
  public void multipleThreadsAllocationAllSpacesAtMultipleBuffers() {
    
  }
  
  private class Requier extends Thread {
    
    DirectMemoryCache cache;
    LinkedBlockingQueue<Pointer> queue;
    Requier(DirectMemoryCache cache, LinkedBlockingQueue<Pointer> queue) {
      this.cache = cache;
      this.queue = queue;
      
      this.setDaemon(true);
    }
    
    public void run() {
      byte[] mid = new byte[1024];
      while (true) {
        try {
          queue.put(cache.store(mid));
        } catch (AllocateFailedException e) {
          System.out.println("Failed to allocate " + e.getMessage());
        } catch (Exception e) {
          e.printStackTrace();
          break;
        }
      }
      
      System.out.println("Producer exit");
    }
  }
  
  private class Retriever extends Thread {
    
    DirectMemoryCache cache;
    LinkedBlockingQueue<Pointer> queue;
    Retriever(DirectMemoryCache cache, LinkedBlockingQueue<Pointer> queue) {
      this.cache = cache;
      this.queue = queue;
      
      this.setDaemon(true);
    }
    
    public void run() {
      while (true) {
        try {
          Pointer p = queue.take();
          byte[] datas = cache.getData(p);
          if (datas.length != 1024) {
            System.out.println("Read failed");
          }
          
          cache.release(p);
        } catch (Exception e) {
          e.printStackTrace();
          break;
        }
        
//        try {
//          Thread.sleep(10);
//        } catch (InterruptedException e) {
//        }
      }
      
      System.out.println("Consumer exit");
    }
  }
  
  
  private class Checker extends Thread {
    
    DirectMemoryCache cache;
    Checker(DirectMemoryCache cache) {
      this.cache = cache;
      this.setDaemon(true);
    }
    
    public void run() {
      while (true) {
        int i = 0;
        while (i++ < 100) {
          System.out.println("Round#" + i);
          System.out.println(cache.status());
          
          try {
            Thread.sleep(1000);
          } catch (InterruptedException e) {
          }
        }
      }
    }
  }
}

