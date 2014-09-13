package org.openhab.binding.ebus;

import org.openhab.core.binding.BindingProvider;

public interface EBusBindingProvider extends BindingProvider {
	public String getItemName(String id);
}
