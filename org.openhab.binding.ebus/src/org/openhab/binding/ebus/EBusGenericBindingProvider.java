package org.openhab.binding.ebus;

import java.util.Map.Entry;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.lang.StringUtils;
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
			if(StringUtils.equals(cfg.id, id)) {
				return entry.getKey();
			}
		}
		
		return null;
	}
	
	@Override
	public void processBindingConfiguration(String context, Item item,
			String bindingConfig) throws BindingConfigParseException {
		super.processBindingConfiguration(context, item, bindingConfig);

		for (String set : bindingConfig.trim().split(",")) {
			String[] configParts = set.split(":");
			if (configParts.length > 2) {
				throw new BindingConfigParseException("eBus binding configuration must not contain more than two parts");
			}
			
			EBusBindingConfig config = new EBusBindingConfig();
			if(configParts[0].equals("id")) {
				config.id = configParts[1];
			} else if(configParts[0].equals("data")) {
				config.data = DatatypeConverter.parseHexBinary(configParts[1].trim().replaceAll(" ", ""));
			} else {
				throw new BindingConfigParseException("eBus binding configuration must contain id");
			}
			
			addBindingConfig(item, config);
		}
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
		public byte[] data;
	}

	@Override
	public byte[] getCommandData(String itemName) {
		EBusBindingConfig bindingConfig = (EBusBindingConfig) bindingConfigs.get(itemName);
		if(bindingConfig != null) {
			return bindingConfig.data;
		}
		return null;
	}
	
}
