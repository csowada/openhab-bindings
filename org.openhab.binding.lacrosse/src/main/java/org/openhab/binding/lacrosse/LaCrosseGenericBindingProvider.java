package org.openhab.binding.lacrosse;

import java.util.Map.Entry;

import org.openhab.core.binding.BindingConfig;
import org.openhab.core.items.Item;
import org.openhab.model.item.binding.AbstractGenericBindingProvider;
import org.openhab.model.item.binding.BindingConfigParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LaCrosseGenericBindingProvider extends
		AbstractGenericBindingProvider implements LaCrosseBindingProvider {

	private static final Logger logger = LoggerFactory
			.getLogger(LaCrosseBinding.class);
	
	@Override
	public String getBindingType() {
		return "lacrosse";
	}

	@Override
	public String getItemName(String id) {
		for (Entry<String, BindingConfig> entry : bindingConfigs.entrySet()) {
			LaCrosseBindingConfig cfg = (LaCrosseBindingConfig) entry.getValue();
			if(id.equals(cfg.id + "." + cfg.type)) {
				return entry.getKey();
			}
		}
		
		return null;
	}
	
	public String getType(String itemName) {
		LaCrosseBindingConfig bindingConfig = (LaCrosseBindingConfig) bindingConfigs.get(itemName);
		if(bindingConfig != null) {
			return bindingConfig.type;
		}
		return null;
	}
	
	@Override
	public void processBindingConfiguration(String context, Item item,
			String bindingConfig) throws BindingConfigParseException {
		
		super.processBindingConfiguration(context, item, bindingConfig);

		logger.debug("Process binding cfg for {} with settings {} [Context:{}]",
				item.getName(), bindingConfig, context);
		
		LaCrosseBindingConfig config = new LaCrosseBindingConfig();
		for (String set : bindingConfig.trim().split(",")) {
			String[] configParts = set.split(":");
			if (configParts.length > 2) {
				throw new BindingConfigParseException("eBus binding configuration must not contain more than two parts");
			}
			
			if(configParts[0].trim().equals("id")) {
				config.id = configParts[1];
			}
			if(configParts[0].trim().equals("type")) {
				config.type = configParts[1];
			}
		}

		addBindingConfig(item, config);
	}

	@Override
	public void validateItemType(Item item, String bindingConfig)
			throws BindingConfigParseException {
	}

	/**
	 * This is an internal data structure to store information from the binding
	 * config strings.
	 */
	class LaCrosseBindingConfig implements BindingConfig {
		public String id;
		public String type;
		public LaCrosseBindingConfig() {

		}
	}
	
}
