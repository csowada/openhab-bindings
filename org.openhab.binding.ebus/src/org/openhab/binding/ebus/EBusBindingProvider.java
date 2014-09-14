package org.openhab.binding.ebus;

import org.openhab.core.binding.BindingProvider;

public interface EBusBindingProvider extends BindingProvider {
	
	/**
	 * Return the item name for the ebus id
	 * @param id The ebus id (see ebus-configuration.json)
	 * @return The openhab item name
	 */
	public String getItemName(String id);
}
