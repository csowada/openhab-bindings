package org.openhab.binding.ebus.serial;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.util.TooManyListenersException;

import org.openhab.binding.ebus.EbusTelegram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EBusSerialConnector {

	private static final Logger logger = LoggerFactory
			.getLogger(EBusSerialConnector.class);
	
	private SerialPort serialPort;

	private EBusSerialPortEvent event;

	public void send(byte[] data) {
		event.send(data);
	}
	
	public boolean open(String port) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException {

		final CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(port);
		
		serialPort = (SerialPort) portIdentifier.open("openhab-ebus", 3000);
		serialPort.setSerialPortParams(2400, SerialPort.DATABITS_8,
				SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

		serialPort.disableReceiveTimeout();
		serialPort.enableReceiveThreshold(1);
		
		event = new EBusSerialPortEvent(serialPort) {
			@Override
			public void onEBusTelegramAvailable(EbusTelegram telegram) {
				// TODO Auto-generated method stub
				logger.info("---> Paket an openHAB übergeben !");
//				System.out
//						.println("EBusSerialConnector.open(...).new EBusSerialPortEvent() {...}.onEBusTelegramAvailable()");
			}
		};
		
		// setz events
		try {
			serialPort.addEventListener(event);
			serialPort.notifyOnDataAvailable(true);
			
		} catch (TooManyListenersException e) {
			logger.error(e.toString(), e);
			return false;
		}
		
		return true;
	}
	
	/**
	 * Closes the connector
	 */
	public void close() {
		logger.debug("Close EBus Connector ...");
		if(serialPort != null) {
			serialPort.close();
		}
	}

	/**
	 * Check if the connector is open
	 * @return true if the connector is open
	 */
	public boolean isOpen() {
		return serialPort != null;
	}
	
	public void sendSYN() {
		event.sendSYN();
	}
}