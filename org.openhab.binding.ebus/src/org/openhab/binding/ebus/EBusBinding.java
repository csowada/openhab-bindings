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
import org.openhab.binding.ebus.connection.AbstractEBusConnector;
import org.openhab.binding.ebus.connection.EBusCommandProcessor;
import org.openhab.binding.ebus.connection.EBusConnectorEventListener;
import org.openhab.binding.ebus.connection.EBusSerialConnector;
import org.openhab.binding.ebus.connection.EBusTCPConnector;
import org.openhab.binding.ebus.parser.EBusConfigurationProvider;
import org.openhab.binding.ebus.parser.EBusTelegramParser;
import org.openhab.binding.ebus.parser.EBusUtils;
import org.openhab.core.binding.AbstractBinding;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christian Sowada
 * @since 1.7.0
 */
public class EBusBinding extends AbstractBinding<EBusBindingProvider> implements ManagedService, EBusConnectorEventListener {

	private static final Logger logger = LoggerFactory
			.getLogger(EBusBinding.class);

	private EBusCommandProcessor commandProcessor;
	private AbstractEBusConnector connector;
	private EBusTelegramParser parser;

	private ConfigurationAdmin configurationAdminService;
	private EBusConfigurationProvider configurationProvider;

	/* (non-Javadoc)
	 * @see org.openhab.core.binding.AbstractBinding#internalReceiveCommand(java.lang.String, org.openhab.core.types.Command)
	 */
	@Override
	protected void internalReceiveCommand(String itemName, Command command) {

		final String type = command.toString().toLowerCase();

		for (EBusBindingProvider provider : providers) {
			byte[] data = commandProcessor.composeSendData(
					provider, itemName, type);

			connector.send(data);
		}
	}

	/**
	 * Set the OSGI Admin Service
	 * @param configurationAdminService
	 */
	public void setConfigurationAdmin(ConfigurationAdmin configurationAdminService) {
		this.configurationAdminService = configurationAdminService;
	}

	/**
	 * Unset the OSGI Admin Service
	 * @param configurationAdminService
	 */
	public void unsetConfigurationAdmin(ConfigurationAdmin configurationAdminService) {
		this.configurationAdminService = null;
	}

	private void checkConfigurationProvider() {
		if(configurationProvider == null)
			configurationProvider = new EBusConfigurationProvider();
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
	 */
	@Override
	public void updated(Dictionary<String, ?> properties) throws ConfigurationException {

		logger.info("Update eBus Binding configuration ...");

		try {
			// stop last thread if active
			if(connector != null && connector.isAlive()) {
				connector.interrupt();
			}

			// check to ensure that it is available
			checkConfigurationProvider();

			// clear current configuration
			configurationProvider.clear();

			// load parser from default url
			parser = new EBusTelegramParser(configurationProvider);

			URL configurationUrl = null;
			String parsers = (String) properties.get("parsers");

			if(StringUtils.isEmpty(parsers)) {
				// set to current stable configurations as default
				parsers = "common,wolf";
			}

			for (String elem : parsers.split(",")) {
				configurationUrl = null;

				// check for keyword custom to load custom configuration
				if(elem.trim().equals("custom")) {
					String parserUrl = (String) properties.get("parserUrl");
					if(parserUrl != null) {
						logger.debug("Load custom eBus Parser with url {}", parserUrl);
						configurationUrl = new URL(parserUrl);
					}

				} else {
					logger.debug("Load eBus Parser Configuration \"{}\" ...", elem.trim());
					configurationUrl = this.getClass().getResource(
							"/META-INF/" + elem.trim() + "-configuration.json");
				}

				if(configurationUrl != null) {
					configurationProvider.loadConfigurationFile(configurationUrl);
				}
			}


			// check minimal config
			if(properties.get("serialPort") != null && properties.get("hostname") != null) {
				throw new ConfigurationException("hostname", "Set property serialPort or hostname, not both!");
			}

			if(StringUtils.isNotEmpty((String) properties.get("serialPort"))) {

				// use the serial connector
				connector = new EBusSerialConnector(
						(String) properties.get("serialPort"));

			} else if(StringUtils.isNotEmpty((String) properties.get("hostname"))) {

				// use the tcp-ip connector
				connector = new EBusTCPConnector(
						(String) properties.get("hostname"),
						Integer.parseInt((String) properties.get("port")));
			}

			// Set eBus sender id or default 0xFF
			if(StringUtils.isNotEmpty((String)properties.get("senderId"))) {
				connector.setSenderId(
						EBusUtils.toByte((String) properties.get("senderId")));
			}

			// add event listener
			connector.addEBusEventListener(this);

			// start thread
			connector.start();

			// set the new connector
			commandProcessor.setConnector(connector);
			commandProcessor.setConfigurationProvider(configurationProvider);

		} catch (MalformedURLException e) {
			logger.error(e.toString(), e);
		} catch (IOException e) {
			throw new ConfigurationException("general", e.toString(), e);
		}
	}

	@Override
	public void addBindingProvider(EBusBindingProvider provider) {
		super.addBindingProvider(provider);

		if(commandProcessor == null) {
			commandProcessor = new EBusCommandProcessor();
			commandProcessor.setConfigurationProvider(configurationProvider);
		}

		if(provider.providesBinding()) {
			// items already processed, so too late for this listener. do it manually
			commandProcessor.allBindingsChanged(provider);
		}

		provider.addBindingChangeListener(commandProcessor);
	}

	@Override
	public void removeBindingProvider(EBusBindingProvider provider) {
		super.removeBindingProvider(provider);
		provider.removeBindingChangeListener(commandProcessor);
	}

	/* (non-Javadoc)
	 * @see org.openhab.core.binding.AbstractBinding#activate()
	 */
	public void activate() {
		super.activate();
		logger.debug("eBus binding has been started.");

		// check to ensure that it is available
		checkConfigurationProvider();

		// observe connection, if not started 15 sec. later than start it manually
		// replacing a bundle doesn't recall update function, more 
		// a bug/enhancement in openhab
		new Thread() {
			@Override
			public void run() {

				try {
					sleep(15000);

					if(connector == null) {
						logger.warn("eBus connector still not started, started it yet!");

						Configuration configuration = configurationAdminService.getConfiguration("org.openhab.ebus", null);
						if(configuration != null) {
							updated(configuration.getProperties());

							for (EBusBindingProvider provider : EBusBinding.this.providers) {
								commandProcessor.allBindingsChanged(provider);
							}
						}
					}

				} catch (InterruptedException | ConfigurationException | 
						IOException e) {
					logger.error(e.toString(), e);
				}

			}
		}.start();;

	}

	/* (non-Javadoc)
	 * @see org.openhab.core.binding.AbstractBinding#deactivate()
	 */
	public void deactivate() {
		super.deactivate();

		logger.debug("eBus binding has been stopped.");

		if(connector != null && connector.isAlive()) {
			connector.interrupt();
			connector = null;
		}

		if(commandProcessor != null) {
			commandProcessor.deactivate();
			commandProcessor = null;
		}
	}

	/* (non-Javadoc)
	 * @see org.openhab.binding.ebus.connection.EBusConnectorEventListener#onTelegramReceived(org.openhab.binding.ebus.EbusTelegram)
	 */
	@Override
	public void onTelegramReceived(EBusTelegram telegram) {

		// parse the raw telegram to a key/value map
		final Map<String, Object> results = parser.parse(telegram);

		if(results == null) {
			logger.debug("No valid parser result for raw telegram!");
			return;
		}
		
		for (Entry<String, Object> entry : results.entrySet()) {

			State state = StateUtils.convertToState(entry.getValue());

			// process if the state is set
			if(state != null) {

				// loop over all items to update the state
				for (EBusBindingProvider provider : providers) {
					for (String itemName : provider.getItemNames(entry.getKey())) {

						Byte telegramSource = provider.getTelegramSource(itemName);
						Byte telegramDestination = provider.getTelegramDestination(itemName);

						// check if this item has a src or dst defined
						if(telegramSource == null || telegram.getSource() == telegramSource) {
							if(telegramDestination == null || telegram.getDestination() == telegramDestination) {
								eventPublisher.postUpdate(itemName, state);
							}
						}
					}
				} // for
			} // if
		}
	}

}