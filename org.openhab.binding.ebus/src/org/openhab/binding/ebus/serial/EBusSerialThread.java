package org.openhab.binding.ebus.serial;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;

public class EBusSerialThread extends EBusThread {

	private SerialPort serialPort;
	private String port;

	public EBusSerialThread(String port) {
		this.port = port;
	}

	@Override
	protected void connect() {
		try {
			final CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(port);
			
			serialPort = (SerialPort) portIdentifier.open("openhab-ebus", 3000);
			serialPort.setSerialPortParams(2400, SerialPort.DATABITS_8,
					SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

			serialPort.disableReceiveTimeout();
			serialPort.enableReceiveThreshold(1);
			
			outputStream = serialPort.getOutputStream();
			inputStream = serialPort.getInputStream();
			
		} catch (NoSuchPortException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (PortInUseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedCommOperationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	protected void disconnect() {
		serialPort.close();
	}
}
