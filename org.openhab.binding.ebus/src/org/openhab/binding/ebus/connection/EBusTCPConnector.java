/**
* Copyright (c) 2010-2014, openHAB.org and others.
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*/
package org.openhab.binding.ebus.connection;

import java.io.IOException;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author Christian Sowada
* @since 1.7.0
*/
public class EBusTCPConnector extends AbstractEBusConnector {

	private static final Logger logger = LoggerFactory
			.getLogger(EBusTCPConnector.class);
	
	/** The tcp socket */
	private Socket socket;

	/** The tcp hostname */
	private String hostname;

	/** The tcp port */
	private int port;

	/**
	 * Constructor
	 * @param hostname
	 * @param port
	 */
	public EBusTCPConnector(String hostname, int port) {
		this.hostname = hostname;
		this.port = port;
	}
	
	/* (non-Javadoc)
	 * @see org.openhab.binding.ebus.connection.AbstractEBusConnector#connect()
	 */
	@Override
	public boolean connect() throws IOException  {
		try {
			socket = new Socket(hostname, port);
			socket.setSoTimeout(10000);
			socket.setKeepAlive(true);
			
			inputStream = socket.getInputStream();
			outputStream = socket.getOutputStream();
			
			logger.debug("TCP connection established ...");
			
			return true;
		} catch (Exception e) {
			logger.error(e.toString(), e);
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.openhab.binding.ebus.connection.AbstractEBusConnector#disconnect()
	 */
	@Override
	public boolean disconnect() throws IOException  {
		
		logger.debug("TCP connection disconnected ...");
		
		if(socket != null) {
			socket.close();
			socket = null;
		}
		return true;
	}

}