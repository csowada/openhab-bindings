package org.openhab.binding.lacrosse;

import java.util.Dictionary;

import org.apache.commons.lang.StringUtils;
import org.openhab.binding.lacrosse.connector.LaCrosseConnector;
import org.openhab.binding.lacrosse.connector.LaCrosseConnector2;
import org.openhab.core.binding.AbstractActiveBinding;
import org.openhab.core.types.State;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LaCrosseBinding extends AbstractActiveBinding<LaCrosseBindingProvider> implements ManagedService {

	private static final Logger logger = LoggerFactory
			.getLogger(LaCrosseBinding.class);
	
	LaCrosseConnector connector;
	
	@Override
	public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
		logger.debug("Update LaCrosse Binding ...");
		
		if(connector != null && connector.isOpen()) {
			connector.close();
		}

		if (properties != null) {
			String newPort = (String) properties.get("port"); //$NON-NLS-1$

			connector = new LaCrosseConnector2(this);
			connector.open(newPort);
		}
	}
	
	public void postUpdate(String id, String type, State newState) {
		for (LaCrosseBindingProvider provider : providers) {
			String itemName = provider.getItemName(id+"."+type);
			if(StringUtils.isNotEmpty(itemName)) {
//				String type0 = provider.getType(itemName);
//				if(StringUtils.equals(type0, type)) {
					eventPublisher.postUpdate(itemName, newState);
//				}
			}
		}
	}
	
	/**
	 * @{inheritDoc
	 */
	public void activate() {

	}

	/**
	 * @{inheritDoc
	 */
	public void deactivate() {
		if(connector != null) {
			connector.close();
			connector = null;
		}
	}

	@Override
	protected void execute() {
	}

	@Override
	protected long getRefreshInterval() {
		return 10;
	}

	@Override
	protected String getName() {
		return "LaCrosse";
	}
}