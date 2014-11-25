/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.ebus.parser;

import java.math.BigDecimal;

/**
 * @author Christian Sowada
 * @since 1.7.0
 */
public class NumberUtils {

	/**
	 * Convert number object to BigDecimal
	 * @param obj
	 * @return
	 */
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
