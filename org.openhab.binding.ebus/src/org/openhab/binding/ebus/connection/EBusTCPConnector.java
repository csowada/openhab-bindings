package org.openhab.binding.ebus.connection;

import java.io.IOException;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EBusTCPConnector extends AbstractEBusConnector {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory
			.getLogger(EBusTCPConnector.class);
	
	private Socket socket;

	private String hostname;

	private int port;

	public EBusTCPConnector(String hostname, int port) {
		this.hostname = hostname;
		this.port = port;
	}
	
	@Override
	public boolean connect() throws IOException  {
		socket = new Socket(hostname, port);
		inputStream = socket.getInputStream();
		outputStream = socket.getOutputStream();
		return true;
	}

	@Override
	public boolean disconnect() throws IOException  {
		socket.close();
		socket = null;
		return true;
	}

}