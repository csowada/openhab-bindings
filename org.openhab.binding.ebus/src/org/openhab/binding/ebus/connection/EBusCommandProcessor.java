/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.ebus.connection;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.openhab.binding.ebus.EBusBindingProvider;
import org.openhab.core.binding.BindingChangeListener;
import org.openhab.core.binding.BindingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christian Sowada
 * @since 1.6.0
 */
public class EBusCommandProcessor implements BindingChangeListener {
	
	private static final Logger logger = LoggerFactory
			.getLogger(EBusCommandProcessor.class);
	
	private Map<String, ScheduledFuture<?>> futureMap = new HashMap<String, ScheduledFuture<?>>();
	private ScheduledExecutorService scheduler;
	private AbstractEBusConnector connector;

	public void setConnector(AbstractEBusConnector connector) {
		this.connector = connector;
	}
	
	public void deactivate() {
		if(scheduler != null) {
			scheduler.shutdown();
		}
	}
	
	@Override
	public void allBindingsChanged(BindingProvider provider) {
		logger.debug("Remove all polling items for this provider from scheduler ...");
		for (String itemName : provider.getItemNames()) {
			if(futureMap.containsKey(itemName)) {
				futureMap.get(itemName).cancel(true);
			}
		}
	}

	@Override
	public void bindingChanged(BindingProvider provider, String itemName) {
		logger.trace("Binding changed for item {}", itemName);
		
		EBusBindingProvider eBusProvider = (EBusBindingProvider)provider;
		int refreshRate = eBusProvider.getRefreshRate(itemName);

		if(refreshRate > 0) {
			final byte[] commandData = eBusProvider.getCommandData(itemName);

			Runnable r = new Runnable() {
				@Override
				public void run() {
					connector.send(commandData);
				}
			};

			if(futureMap.containsKey(itemName)) {
				logger.trace("Stopped old polling item ...");
				futureMap.remove(itemName).cancel(true);
			}

			if(scheduler == null) {
				scheduler = Executors.newScheduledThreadPool(2);
			}
			
			logger.debug("Add polling item {} with refresh rate {} to scheduler ...", itemName, refreshRate);
			futureMap.put(itemName, scheduler.scheduleWithFixedDelay(r, 3, refreshRate, TimeUnit.SECONDS));

		} else if(futureMap.containsKey(itemName)) {
			logger.debug("Remove scheduled refresh for item {}", itemName);
			futureMap.get(itemName).cancel(true);
			futureMap.remove(itemName);
		}
	}
}
