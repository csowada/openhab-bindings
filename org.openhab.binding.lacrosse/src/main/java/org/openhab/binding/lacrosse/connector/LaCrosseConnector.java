package org.openhab.binding.lacrosse.connector;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.TooManyListenersException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LaCrosseConnector {

	private static final Logger logger = LoggerFactory
			.getLogger(LaCrosseConnector.class);

	private BufferedReader input;
	private SerialPort serialPort;
	private String port;

	public boolean isOpen() {
		return (serialPort != null);
	}

	public void close() {
		if(serialPort != null) {
			serialPort.close();
		}
	}

	/**
	 * Open and initialize a serial port.
	 * 
	 * @param portName
	 *            e.g. /dev/ttyS0
	 * @param listener
	 *            the listener which is informed after a successful response
	 *            read
	 * @throws InitializationException
	 */
	public void open(String portName)  {

		logger.info("Open LaCrosse connection");

		port = portName;
		CommPortIdentifier portIdentifier;

		@SuppressWarnings("rawtypes")
		Enumeration portList0 = CommPortIdentifier.getPortIdentifiers();
		StringBuilder sb0 = new StringBuilder();
		while (portList0.hasMoreElements()) {
			CommPortIdentifier id = (CommPortIdentifier) portList0
					.nextElement();
			if (id.getPortType() == CommPortIdentifier.PORT_SERIAL) {
				sb0.append(id.getName() + "\n");
			}
		}

		try {
			portIdentifier = CommPortIdentifier.getPortIdentifier(port);
			try {
				serialPort = (SerialPort) portIdentifier.open("openhab", 3000);
				serialPort.setSerialPortParams(57600, SerialPort.DATABITS_8,
						SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

				input = new BufferedReader(new InputStreamReader(serialPort.getInputStream()));

				serialPort.addEventListener(new SerialPortEventListener() {
					@Override
					public void serialEvent(SerialPortEvent event) {

						if (event.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
							try {
								String inputLine=input.readLine();

								if(inputLine.startsWith("[") && inputLine.endsWith("]")) {
									logger.debug("Use HW Version: " + inputLine);
								}

								// ignore comment lines
								if(inputLine.startsWith("OK")) {
									logger.trace("Received data: {}", inputLine);

									String[] parts = inputLine.substring(5).split(" ");
									if(parts.length == 5) {
										int addr = Integer.parseInt(parts[0]);
										int c = Integer.parseInt(parts[1]);

										int battery_new = (c & 0x80) >> 7;
										//										int type = (c & 0x70) >> 7;
										//										int channel = c & 0x0F;

										float temperature = (float)(Integer.parseInt(parts[2]) * 256 + Integer.parseInt(parts[3]) - 1000) / 10;
										int humidity = Integer.parseInt(parts[4]) & 0x7f;
										int battery_low = (Integer.parseInt(parts[4]) & 0x80) >> 7;

										boolean batteryLow = battery_low == 1;
										boolean batteryNew = battery_new == 1;

										onDataReceived(addr, temperature, humidity, batteryNew, batteryLow);
									}
								}
							} catch (Exception e) {
								logger.error("error", e);
							}
						}
					}
				});

				serialPort.notifyOnDataAvailable(true);


			} catch (PortInUseException e) {
				logger.error("port already in use", e);
			} catch (UnsupportedCommOperationException e) {
				logger.error("unsupported comm operation", e);
			} catch (IOException e) {
				logger.error("", e);
			} catch (TooManyListenersException e) {
				logger.error("too many listeners", e);
			}

		} catch (NoSuchPortException e) {
			StringBuilder sb = new StringBuilder();

			@SuppressWarnings("rawtypes")
			Enumeration portList = CommPortIdentifier.getPortIdentifiers();
			while (portList.hasMoreElements()) {
				CommPortIdentifier id = (CommPortIdentifier) portList
						.nextElement();
				if (id.getPortType() == CommPortIdentifier.PORT_SERIAL) {
					sb.append(id.getName() + "\n");
				}
			}

			logger.warn(("Serial port '" + port
					+ "' could not be found. Available ports are:\n"
					+ sb.toString()));

			throw new RuntimeException("Serial port '" + port
					+ "' could not be found. Available ports are:\n"
					+ sb.toString());
		}
	}

	public void onDataReceived(int address, float temperature, int humidity, boolean batteryNew, boolean batteryWeak) {
		// noop
	}

}