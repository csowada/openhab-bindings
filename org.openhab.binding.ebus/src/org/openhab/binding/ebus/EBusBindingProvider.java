/**
* Copyright (c) 2010-2014, openHAB.org and others.
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*/
package org.openhab.binding.ebus;

import org.openhab.core.binding.BindingProvider;

/**
* @author Christian Sowada
* @since 1.6.0
*/
public interface EBusBindingProvider extends BindingProvider {
	
	/**
	 * Return the item name for the ebus id
	 * @param id The ebus id (see ebus-configuration.json)
	 * @return The openhab item name
	 */
	public String getItemName(String id);

	/**
	 * Return the byte data to send
	 * @param itemName The itemName
	 * @return data or null
	 */
	public byte[] getCommandData(String itemName);
	
	/**
	 * Return the byte data to send for a type
	 * @param itemName
	 * @param type
	 * @return
	 */
	public byte[] getCommandData(String itemName, String type);
	
//	/**
//	 * Unused!
//	 * @param itemName
//	 * @return
//	 */
//	public String getCommand(String itemName);
	
	/**
	 * Return the refresh rate for a polling commands
	 * @param itemName
	 * @return
	 */
	public int getRefreshRate(String itemName);
}
