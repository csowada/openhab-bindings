package org.openhab.binding.ebus;

import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;

import java.io.IOException;
import java.util.Dictionary;

import org.apache.commons.lang.StringUtils;
import org.json.simple.parser.ParseException;
import org.openhab.core.binding.AbstractActiveBinding;
import org.openhab.core.types.State;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EBusBinding extends AbstractActiveBinding<EBusBindingProvider> implements ManagedService {

	private static final Logger logger = LoggerFactory
			.getLogger(EBusBinding.class);

	/** the connector, it handles the serial communication */
	private EBusConnector connector;

	/* (non-Javadoc)
	 * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
	 */
	@Override
	public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
		logger.debug("Update EBus Binding ...");

		if(connector != null && connector.isOpen()) {
			connector.close();
		}

		connector = new EBusConnector();
		try {
			connector.open(this, properties);
			
		} catch (NoSuchPortException e) {
			throw new ConfigurationException("serialPort", "No port with this name available!", e);
		} catch (PortInUseException e) {
			throw new ConfigurationException("serialPort", "Port already in use!", e);
		} catch (IOException e) {
			throw new ConfigurationException("parserUrl", "Unable to open file!", e);
		} catch (ParseException e) {
			throw new ConfigurationException("parserUrl", "Customized parser configuration invalid!", e);
		}
	}

	/**
	 * @param id
	 * @param newState
	 */
	public void postUpdate(String id, State newState) {
		for (EBusBindingProvider provider : providers) {
			String itemName = provider.getItemName(id);
			if(StringUtils.isNotEmpty(itemName)) {
				eventPublisher.postUpdate(itemName, newState);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.openhab.core.binding.AbstractBinding#activate()
	 */
	public void activate() {
		logger.debug("EBus binding has been started.");
	}

	/* (non-Javadoc)
	 * @see org.openhab.core.binding.AbstractBinding#deactivate()
	 */
	public void deactivate() {
		logger.debug("EBus binding has been stopped.");
		if(connector != null) {
			connector.close();
			connector = null;
		}
	}

	/* (non-Javadoc)
	 * @see org.openhab.core.binding.AbstractActiveBinding#execute()
	 */
	@Override
	protected void execute() {

	}

	/* (non-Javadoc)
	 * @see org.openhab.core.binding.AbstractActiveBinding#getRefreshInterval()
	 */
	@Override
	protected long getRefreshInterval() {
		return 10;
	}

	/* (non-Javadoc)
	 * @see org.openhab.core.binding.AbstractActiveBinding#getName()
	 */
	@Override
	protected String getName() {
		return "EBus";
	}
}