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
import org.openhab.binding.ebus.connection.EBusCommandProcessor;
import org.openhab.binding.ebus.connection.EBusConnectorEventListener;
import org.openhab.binding.ebus.connection.EBusSerialConnector;
import org.openhab.binding.ebus.connection.EBusTCPConnector;
import org.openhab.binding.ebus.parser.EBusConfigurationProvider;
import org.openhab.binding.ebus.parser.EBusTelegramParser;
import org.openhab.binding.ebus.parser.EBusUtils;
import org.openhab.core.binding.AbstractBinding;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.StringType;
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
 * @since 1.6.0
 */
public class EBusBinding extends AbstractBinding<EBusBindingProvider> implements ManagedService, EBusConnectorEventListener {

	private static final Logger logger = LoggerFactory
			.getLogger(EBusBinding.class);

	private EBusCommandProcessor commandProcessor;
	private AbstractEBusConnector connector;
	private EBusTelegramParser parser;

	private ConfigurationAdmin configurationAdminService;
	private EBusConfigurationProvider configurationProvider;

	/** default sender id */
	private byte senderId = (byte)0xFF;

	public byte[] getSendData(EBusBindingProvider provider, String itemName, String type) {

		byte[] data = null;
		
		String cmd = provider.getCommand(itemName);
		String cmdClass = provider.getCommandClass(itemName);

		Map<String, Object> command2 = configurationProvider.getCommandById(cmd, cmdClass);

		if(command2 != null) {

			byte[] b = EBusUtils.toByteArray((String) command2.get("data"));
			byte[] b2 = EBusUtils.toByteArray((String) command2.get("command"));
			
			Byte dst = provider.getTelegramDestination(itemName);
			Byte src = provider.getTelegramSource(itemName);

			if(dst == null) {
				throw new RuntimeException("no destination!");
			}

			if(src == null) {
				src = senderId;
			}

			byte[] buffer = new byte[b.length+6];
			buffer[0] = src;
			buffer[1] = dst;
			buffer[4] = (byte) b.length;
			System.arraycopy(b2, 0, buffer, 2, b2.length);
			System.arraycopy(b, 0, buffer, 5, b.length);

			data = buffer;
		}
		
		// first try, data-ON, data-OFF, etc.
		if(data == null && StringUtils.isNotEmpty(type)) {
			data = provider.getTelegramData(itemName, type);
		}

		if(data == null) {
			// ok, try data param
			data = provider.getTelegramData(itemName);
		}
		
		return data;
	}

	/* (non-Javadoc)
	 * @see org.openhab.core.binding.AbstractBinding#internalReceiveCommand(java.lang.String, org.openhab.core.types.Command)
	 */
	@Override
	protected void internalReceiveCommand(String itemName, Command command) {
		super.internalReceiveCommand(itemName, command);

		String type = command.toString().toLowerCase();

		for (EBusBindingProvider provider : providers) {

			byte[] data = getSendData(provider, itemName, type);

			if(data != null) {
				connector.send(data);
			} else {
				logger.warn("Nothing to send, .......");
			}
		}
	}

	public void setConfigurationAdmin(ConfigurationAdmin configurationAdminService) {
		this.configurationAdminService = configurationAdminService;
	}

	public void unsetConfigurationAdmin(ConfigurationAdmin cfg) {
		this.configurationAdminService = null;
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
	 */
	@Override
	public void updated(Dictionary<String, ?> properties) throws ConfigurationException {

		logger.info("Update eBus Binding configuration ...");

		try {
			// stop last thread if avctive
			if(connector != null && connector.isAlive()) {
				connector.interrupt();
			}

			configurationProvider = new EBusConfigurationProvider();

			// load parser from default url
			parser = new EBusTelegramParser(configurationProvider);

			URL configurationUrl = null;
			String parsers = (String) properties.get("parsers");
			if(parsers == null) {
				// set all known configurations as default
				parsers = "common,wolf,vaillant,testing";
			}

			for (String elem : parsers.split(",")) {
				configurationUrl = null;
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
				connector = new EBusSerialConnector(
						(String) properties.get("serialPort"));
			} else if(StringUtils.isNotEmpty((String) properties.get("hostname"))) {
				connector = new EBusTCPConnector(
						(String) properties.get("hostname"),
						Integer.parseInt((String) properties.get("port")));
			}

			if(StringUtils.isNotEmpty((String)properties.get("senderId"))) {
				senderId = EBusUtils.toByte((String) properties.get("senderId"));
			}

			// add event listener
			connector.addEBusEventListener(this);

			// start thread
			connector.start();

			// set the new connector
			commandProcessor.setConnector(connector);
			commandProcessor.setBinding(this);

		} catch (MalformedURLException e) {
			logger.error(e.toString(), e);
		} catch (ParseException e) {
			logger.error(e.toString(), e);
		} catch (IOException e) {
			throw new ConfigurationException("general", e.toString(), e);
		}
	}

	/**
	 * @param id
	 * @param newState
	 */
	public void postUpdate(String id, State newState) {
		for (EBusBindingProvider provider : providers) {
			for (String itemName : provider.getItemNames(id)) {
				eventPublisher.postUpdate(itemName, newState);
			}
		}
	}

	@Override
	public void addBindingProvider(EBusBindingProvider provider) {
		super.addBindingProvider(provider);

		if(commandProcessor == null) {
			commandProcessor = new EBusCommandProcessor();
		}

		if(provider.providesBinding()) {
			// items already processed, so to late for this listener. do it manually
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