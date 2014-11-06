package com.demo.hulukv.cache;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * Class Description
 *
 * @author Denny Ye
 * @since 2012-11-8
 * @version 1.0
 */
public class DirectUtils {
  
  public static final String MAX_DIRECT_MEMORY_PARAM = "-XX:MaxDirectMemorySize=";
  
  public static long getDirectMemorySize() {
    RuntimeMXBean RuntimemxBean = ManagementFactory.getRuntimeMXBean();
    List<String> arguments = RuntimemxBean.getInputArguments();
    long multiplier = 1; //for the byte case.
    for (String s : arguments) {
      if (s.contains(MAX_DIRECT_MEMORY_PARAM)) {
        String memSize = s.toLowerCase()
            .replace(MAX_DIRECT_MEMORY_PARAM.toLowerCase(), "").trim();

        if (memSize.contains("k")) {
          multiplier = 1024;
        } else if (memSize.contains("m")) {
          multiplier = 1048576;
        } else if (memSize.contains("g")) {
          multiplier = 1073741824;
        }
        
        memSize = memSize.replaceAll("[^\\d]", "");
        long retValue = Long.parseLong(memSize);
        return retValue * multiplier;
      }
    }
    
    return getDefaultDirectMemorySize();
  }
  
  private static long getDefaultDirectMemorySize() {
    try {
      Class<?> VM = Class.forName("sun.misc.VM");
      Method maxDirectMemory = VM.getDeclaredMethod("maxDirectMemory", (Class<?>)null);
      Object result = maxDirectMemory.invoke(null, (Object[])null);
      if(result != null && result instanceof Long) {
        return (Long)result;
      }
    } catch (Exception e) {}
    
    return Runtime.getRuntime().maxMemory();
  }
  
  public static void clean(ByteBuffer buffer) throws Exception {
    Method cleanerMethod = buffer.getClass().getMethod("cleaner");
    cleanerMethod.setAccessible(true);
    Object cleaner = cleanerMethod.invoke(buffer);
    Method cleanMethod = cleaner.getClass().getMethod("clean");
    cleanMethod.setAccessible(true);
    cleanMethod.invoke(cleaner);
  }
  
  
}

