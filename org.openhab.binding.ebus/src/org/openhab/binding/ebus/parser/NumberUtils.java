package org.openhab.binding.ebus.parser;

import java.math.BigDecimal;

public class NumberUtils {

	public static BigDecimal toBigDecimal(Object obj) {
		
		//Byte, Double, Float, Integer, Long, Short
		if(obj instanceof Integer || obj instanceof Long || obj instanceof Short) {
			return BigDecimal.valueOf((long)obj);
		} else if(obj instanceof Double || obj instanceof Float) {
			return BigDecimal.valueOf((double)obj);
		}
		
		return null;
	}
	
}
