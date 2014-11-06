package com.cisco.ramp.hulukv.cache;

/**
 * Class Description
 *
 * @author Denny Ye
 * @since 2012-11-9
 * @version 1.0
 */
public enum DataUnit {

  KB(1024), MB(1048576), GB(1073741824);
  
  private int base;
  
  private DataUnit(int base) {
    this.base = base;
  }
  
  public int getBase() {
    return this.base;
  }
  
}

