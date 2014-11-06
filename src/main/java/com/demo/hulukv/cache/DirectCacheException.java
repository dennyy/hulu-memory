package com.demo.hulukv.cache;

/**
 * Class Description
 *
 * @author Denny Ye
 * @since 2012-11-15
 * @version 1.0
 */
public class DirectCacheException extends RuntimeException {

  /** */
  private static final long serialVersionUID = -2062962871481646077L;

  public DirectCacheException(String msg) {
    super(msg);
  }
  
  public DirectCacheException(String msg, Throwable e) {
    super(msg, e);
  }
  
}

