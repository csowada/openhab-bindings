package org.openhab.binding.ebus.connection;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EBusSerialConnector extends AbstractEBusConnector {

	private static final Logger logger = LoggerFactory
			.getLogger(EBusSerialConnector.class);
	
	private SerialPort serialPort;
	private String port;

	public EBusSerialConnector(String port) {
		this.port = port;
	}

	@Override
	public boolean connect() throws IOException {
		try {
			final CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(port);
			
			serialPort = (SerialPort) portIdentifier.open("openhab-ebus", 3000);
			serialPort.setSerialPortParams(2400, SerialPort.DATABITS_8,
					SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

			serialPort.disableReceiveTimeout();
			serialPort.enableReceiveThreshold(1);
			
			outputStream = serialPort.getOutputStream();
			inputStream = serialPort.getInputStream();
			
			return true;
			
		} catch (NoSuchPortException e) {
			logger.error(e.toString(), e);
		} catch (PortInUseException e) {
			logger.error(e.toString(), e);
		} catch (UnsupportedCommOperationException e) {
			logger.error(e.toString(), e);
		}
		
		return false;
	}

	@Override
	public boolean disconnect() throws IOException  {
		serialPort.close();
		serialPort = null;
		return true;
	}
}
