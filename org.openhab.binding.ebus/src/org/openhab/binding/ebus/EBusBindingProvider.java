package org.openhab.binding.ebus;

import org.openhab.core.binding.BindingProvider;

public interface EBusBindingProvider extends BindingProvider {
	
	
	/**
	 * 
	 * @param id
	 * @return
	 */
	public String getItemName(String id);
}
