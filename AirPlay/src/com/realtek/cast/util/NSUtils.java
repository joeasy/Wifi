package com.realtek.cast.util;

import com.dd.plist.NSDictionary;
import com.dd.plist.NSNumber;
import com.dd.plist.NSObject;
import com.dd.plist.NSString;

public class NSUtils {

	public static int getInteger(NSDictionary dict, String key, int defaultValue) {
		int value = defaultValue;
		if (dict.containsKey(key)) {
			NSObject nsObj = dict.get(key);
			if (nsObj instanceof NSNumber) {
				value = ((NSNumber) nsObj).intValue();
			} else if (nsObj instanceof NSString) {
				String str = ((NSString) nsObj).getContent();
				try {
					value = Integer.parseInt(str);
				} catch (NumberFormatException e) {
				}
			}
		}
		
		return value;
	}
}
