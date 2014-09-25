/**
* Copyright (c) 2010-2014, openHAB.org and others.
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*/
package org.openhab.binding.ebus;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.json.simple.parser.ParseException;
import org.openhab.binding.ebus.connection.AbstractEBusConnector;
import org.openhab.binding.ebus.connection.EBusConnectorEventListener;
import org.openhab.binding.ebus.connection.EBusSerialConnector;
import org.openhab.binding.ebus.connection.EBusTCPConnector;
import org.openhab.binding.ebus.parser.EBusTelegramParser;
import org.openhab.core.binding.AbstractActiveBinding;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author Christian Sowada
* @since 1.6.0
*/
public class EBusBinding extends AbstractActiveBinding<EBusBindingProvider> implements ManagedService, EBusConnectorEventListener {

	private static final Logger logger = LoggerFactory
			.getLogger(EBusBinding.class);
	
	private AbstractEBusConnector connector;
	private EBusTelegramParser parser;

	/* (non-Javadoc)
	 * @see org.openhab.core.binding.AbstractBinding#internalReceiveCommand(java.lang.String, org.openhab.core.types.Command)
	 */
	@Override
	protected void internalReceiveCommand(String itemName, Command command) {
		for (EBusBindingProvider provider : providers) {
			byte[] data = provider.getCommandData(itemName);
			if(data != null) {
				connector.send(data);
			}
		}

		super.internalReceiveCommand(itemName, command);
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
	 */
	@Override
	public void updated(Dictionary<String, ?> properties) throws ConfigurationException {

		logger.info("Update eBus Binding ...");

		try {
			// stop last thread if avctive
			if(connector != null && connector.isAlive()) {
				connector.interrupt();
			}
			
			// load parser from default url
			parser = new EBusTelegramParser();
			URL configurationUrl = this.getClass().getResource("/META-INF/ebus-configuration.json");

			// check customized parser url
			String parserUrl = (String) properties.get("parserUrl");
			if(parserUrl != null) {
				logger.info("Use custom parser with url {}", parserUrl);
				configurationUrl = new URL(parserUrl);
			}
			
			// read configuration file
			parser.loadConfigurationFile(configurationUrl);
			
			
			if(properties.get("serialPort") != null && properties.get("hostname") != null) {
				throw new ConfigurationException("hostname", "Set property serialPort or hostname, not both!");
			}
			
			if(StringUtils.isNotEmpty((String) properties.get("serialPort"))) {
				connector = new EBusSerialConnector(
						(String) properties.get("serialPort"));
			} else if(StringUtils.isNotEmpty((String) properties.get("hostname"))) {
				connector = new EBusTCPConnector(
						(String) properties.get("hostname"),
						Integer.parseInt((String) properties.get("port")));
			}

			// add event listener
			connector.addEBusEventListener(this);
			
			// connect the connector
			if(connector.connect()) {
				
				// start thread
				connector.start();
			} else {
				throw new ConfigurationException("general", "Unable to connect with eBus connector ...");
			}

		} catch (MalformedURLException e) {
			logger.error(e.toString(), e);
		} catch (ParseException e) {
			logger.error(e.toString(), e);
		} catch (IOException e) {
			throw new ConfigurationException(
					"general", e.toString(), e);
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
		logger.debug("eBus binding has been started.");
	}

	/* (non-Javadoc)
	 * @see org.openhab.core.binding.AbstractBinding#deactivate()
	 */
	public void deactivate() {
		logger.debug("eBus binding has been stopped.");
		if(connector != null && connector.isAlive()) {
			connector.interrupt();
			connector = null;
		}
	}

	/* (non-Javadoc)
	 * @see org.openhab.core.binding.AbstractActiveBinding#execute()
	 */
	@Override
	protected void execute() {
		logger.debug("Execute ...");
	}

	/* (non-Javadoc)
	 * @see org.openhab.core.binding.AbstractActiveBinding#getRefreshInterval()
	 */
	@Override
	protected long getRefreshInterval() {
		return 100;
	}

	/* (non-Javadoc)
	 * @see org.openhab.core.binding.AbstractActiveBinding#getName()
	 */
	@Override
	protected String getName() {
		return "eBus";
	}

	/* (non-Javadoc)
	 * @see org.openhab.binding.ebus.connection.EBusConnectorEventListener#onTelegramReceived(org.openhab.binding.ebus.EbusTelegram)
	 */
	@Override
	public void onTelegramReceived(EbusTelegram telegram) {
		Map<String, Object> results = parser.parse(telegram);
		if(results != null) {
			for (Entry<String, Object> entry : results.entrySet()) {
				
				State state = null;
				if(entry.getValue() instanceof Float) {
					state = new DecimalType((Float)entry.getValue());
				} else if(entry.getValue() instanceof Double) {
					state = new DecimalType((Double)entry.getValue());
				} else if(entry.getValue() instanceof Integer) {
						state = new DecimalType((Integer)entry.getValue());
				} else if(entry.getValue() instanceof Byte) {
					state = new DecimalType((Byte)entry.getValue());
				} else if(entry.getValue() instanceof Boolean) {
					state = (boolean)entry.getValue() ? OnOffType.ON : OnOffType.OFF;
				} else if(entry.getValue() instanceof String) {
					state = new StringType((String)entry.getValue());
				} else if(entry.getValue() == null) {
					// noop
				} else {
					logger.error("Unknown data type!");
				}
				
				if(state != null) {
					EBusBinding.this.postUpdate(entry.getKey(), state);
				}
			}
		};
	}
}