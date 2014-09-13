package org.openhab.binding.ebus;

import java.util.Map.Entry;

import org.openhab.core.binding.BindingConfig;
import org.openhab.core.items.Item;
import org.openhab.model.item.binding.AbstractGenericBindingProvider;
import org.openhab.model.item.binding.BindingConfigParseException;

public class EBusGenericBindingProvider extends
		AbstractGenericBindingProvider implements EBusBindingProvider {

	@Override
	public String getBindingType() {
		return "ebus";
	}

	@Override
	public String getItemName(String id) {
		for (Entry<String, BindingConfig> entry : bindingConfigs.entrySet()) {
			EBusBindingConfig cfg = (EBusBindingConfig) entry.getValue();
			if(cfg.id.equals(id)) {
				return entry.getKey();
			}
		}
		
		return null;
	}
	
	@Override
	public void processBindingConfiguration(String context, Item item,
			String bindingConfig) throws BindingConfigParseException {
		super.processBindingConfiguration(context, item, bindingConfig);
		
		
		String[] configParts = bindingConfig.trim().split(":");
		if (configParts.length > 2) {
			throw new BindingConfigParseException("WX binding configuration must not contain more than two parts");
		}
		
		EBusBindingConfig config = new EBusBindingConfig();
		if(configParts[0].equals("id")) {
			config.id = configParts[1];
		} else {
			throw new BindingConfigParseException("EBus binding configuration must contain id");
		}
		
		
		
		
//		System.out
//				.println("EBusGenericBindingProvider.processBindingConfiguration()");
//		EBusBindingConfig config = new EBusBindingConfig(bindingConfig);
		addBindingConfig(item, config);
	}

	@Override
	public void validateItemType(Item item, String bindingConfig)
			throws BindingConfigParseException {
		System.out.println("EBusGenericBindingProvider.validateItemType()");
	}

	/**
	 * This is an internal data structure to store information from the binding
	 * config strings.
	 */
	class EBusBindingConfig implements BindingConfig {
		public String id;
	}
	
}
