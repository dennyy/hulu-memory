package com.cisco.ramp.hulukv.cache;

/**
 * Format transformer
 *
 * @author Denny Ye
 * @since 2011-5-24
 * @version wbxhive
 */
public class Transformer {

	/**
	 * Transferring long type time to visible format
	 * that people can knows clearly
	 *
	 * <br>
	 * 1342  ---> 1sec   <br>
	 * 4231234 ---> 1hour, 10mins, 31sec
	 *
	 * @param time long type timestamp
	 * @return
	 */
	public static String getVisibleTime(long time, String columnSeparator) {
		if (time <= 0) {
			return time + "ms";
		}

		StringBuffer appender = new StringBuffer();

		long temp = time;
		for (int i = 4; i > 0; i--) {
			Object[] result = timeUnitIndex(i);
			long k = temp / (Long) result[0];
			temp = temp % (Long) result[0];

			if (k > 0) {
				if (i == 0 || temp == 0) {
					appender.append(k).append(result[1]);
				} else {
					appender.append(k).append(result[1]).append(columnSeparator);
				}
			}
		}
		return appender.toString();
	}

	/**
	 * Four levels from hours to microsecond
	 *
	 * @param index
	 * @return
	 */
	private static Object[] timeUnitIndex(int index) {
		Object[] result = null;
		switch (index) {
			case 4 : result = new Object[]{3600000L, "hours"};
					break;
			case 3 : result = new Object[]{60000L, "mins"};
					break;
			case 2 : result = new Object[]{1000L, "sec"};
					break;
			case 1 : result = new Object[]{1L, "ms"};
		}

		return result;
	}

	/**
	 * Transferring bytes to regular storage unit
	 * <br>
	 * 13212 ---> 12KB,924bytes  <br>
	 * 32134345  ---> 30MB,661KB,201bytes <br>
	 * 2342342306246  ---> 2181GB,487MB,714KB,454bytes
	 *
	 * @param byteSize long bytes size
	 * @return
	 */
	public static String getSpecificSize(long byteSize) {
		if (byteSize <= 0) {
			return byteSize + "bytes";
		}

		StringBuffer appender = new StringBuffer();

		long temp = byteSize;
		for (int i = 30; i >= 0 ; i -= 10) {
			long k = temp / (1 << i);
			temp = temp % (1 << i);
			if (k > 0) {
				if (i == 0 || temp == 0) {
					appender.append(k).append(unitIndex(i));
				} else {
					appender.append(k).append(unitIndex(i)).append(",");
				}
			}
		}

		return appender.toString();
	}

	private static String unitIndex(int index) {
		String unit = null;
		switch (index) {
			case 30 : unit = "GB"; break;
			case 20 : unit = "MB"; break;
			case 10 : unit = "KB"; break;
			case 0  : unit = "bytes"; break;
		}

		return unit;
	}
	
	public static String strFormat(String tail, int format) {
    int diff = tail.length() - format;
    
    if (diff >= 0) {
      return tail;
    } else {
      StringBuffer sb = new StringBuffer();
      for (int i = diff; i < 0; i++) {
        sb.append(" ");
      }
      
      return sb.toString() + tail;
    }
  }
  

}


