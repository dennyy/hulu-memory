package com.cisco.ramp.hulukv.cache;

/**
 * Class Description
 *
 * @author Denny Ye
 * @since 2012-11-9
 * @version 1.0
 */
public class AllocateFailedException extends Exception {

  /** */
  private static final long serialVersionUID = -5758104169950715177L;
  
  public AllocateFailedException(String message) {
    super(message);
  }
  
  public AllocateFailedException(String message, Throwable e) {
    super(message, e);
  }

}

