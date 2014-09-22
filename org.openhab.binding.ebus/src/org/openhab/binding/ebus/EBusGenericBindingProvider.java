/**
* Copyright (c) 2010-2014, openHAB.org and others.
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*/
package org.openhab.binding.ebus;

import java.util.Map.Entry;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.lang.StringUtils;
import org.openhab.core.binding.BindingConfig;
import org.openhab.core.items.Item;
import org.openhab.model.item.binding.AbstractGenericBindingProvider;
import org.openhab.model.item.binding.BindingConfigParseException;

/**
* @author Christian Sowada
* @since 1.6.0
*/
public class EBusGenericBindingProvider extends
		AbstractGenericBindingProvider implements EBusBindingProvider {

	/* (non-Javadoc)
	 * @see org.openhab.model.item.binding.BindingConfigReader#getBindingType()
	 */
	@Override
	public String getBindingType() {
		return "ebus";
	}

	/* (non-Javadoc)
	 * @see org.openhab.binding.ebus.EBusBindingProvider#getItemName(java.lang.String)
	 */
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
	
	/* (non-Javadoc)
	 * @see org.openhab.model.item.binding.AbstractGenericBindingProvider#processBindingConfiguration(java.lang.String, org.openhab.core.items.Item, java.lang.String)
	 */
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

	/* (non-Javadoc)
	 * @see org.openhab.model.item.binding.BindingConfigReader#validateItemType(org.openhab.core.items.Item, java.lang.String)
	 */
	@Override
	public void validateItemType(Item item, String bindingConfig)
			throws BindingConfigParseException {
	}

	/**
	 * This is an internal data structure to store information from the binding
	 * config strings.
	 */
	class EBusBindingConfig implements BindingConfig {
		public String id;
		public byte[] data;
	}

	/* (non-Javadoc)
	 * @see org.openhab.binding.ebus.EBusBindingProvider#getCommandData(java.lang.String)
	 */
	@Override
	public byte[] getCommandData(String itemName) {
		EBusBindingConfig bindingConfig = (EBusBindingConfig) bindingConfigs.get(itemName);
		if(bindingConfig != null) {
			return bindingConfig.data;
		}
		return null;
	}
	
}
