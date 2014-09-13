package org.openhab.binding.ebus;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TooManyListenersException;

import org.json.simple.parser.ParseException;
import org.openhab.binding.ebus.parser.EBusTelegramParser;
import org.openhab.binding.ebus.serial.EbusSerialPortEvent;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.types.PrimitiveType;
import org.openhab.core.types.State;
//import org.openhab.core.library.types.DecimalType;
//import org.openhab.core.types.PrimitiveType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EBusConnector {

	private static final Logger logger = LoggerFactory
			.getLogger(EBusConnector.class);
	
	private SerialPort serialPort;
	
	public void open(final EBusBinding eBusBinding, Dictionary<String, ?> properties) throws NoSuchPortException, PortInUseException, IOException, ParseException {

		try {
			CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier("COM6");
			
			serialPort = (SerialPort) portIdentifier.open("openhab-ebus", 3000);
			serialPort.setSerialPortParams(2400, SerialPort.DATABITS_8,
					SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

			serialPort.disableReceiveTimeout();
			serialPort.enableReceiveThreshold(1);
			
			final EBusTelegramParser parser = new EBusTelegramParser();
			URL configurationUrl = this.getClass().getResource("/META-INF/ebus-configuration.json");

			// check customized parser url
			String parserUrl = (String) properties.get("parserUrl");
			if(parserUrl != null) {
				logger.info("Use custom parser with url {}", parserUrl);
				configurationUrl = new URL(parserUrl);
			}
			
			parser.loadConfigurationFile(configurationUrl);
			
			String parserDebug = (String) properties.get("parser.debug");
			
			parser.setDebugLevel(EBusTelegramParser.DEBUG_UNKNOWN);
			
			EbusSerialPortEvent event = new EbusSerialPortEvent() {
				@Override
				public void onEBusTelegramAvailable(EbusTelegram telegram) {
					Map<String, Object> results = parser.parse(telegram);
					if(results != null) {
						for (Entry<String, Object> entry : results.entrySet()) {
							
//							DecimalType decimalType = new DecimalType();
							State state = null;
							if(entry.getValue() instanceof Float) {
								state = new DecimalType((Float)entry.getValue());
							} else if(entry.getValue() instanceof Integer) {
									state = new DecimalType((Integer)entry.getValue());
							} else if(entry.getValue() instanceof Byte) {
								state = new DecimalType((Byte)entry.getValue());
							} else if(entry.getValue() instanceof Boolean) {
								state = (boolean)entry.getValue() ? OnOffType.ON : OnOffType.OFF;
							}
							
							if(state != null) {
								eBusBinding.postUpdate(entry.getKey(), state);
							}
						}
					};
					
				}
			};
			
			// setz events
			serialPort.addEventListener(event);
			serialPort.notifyOnDataAvailable(true);

		} catch (TooManyListenersException e) {
			logger.error(e.toString(), e);
		} catch (UnsupportedCommOperationException e) {
			logger.error(e.toString(), e);
		}
	}

	public void close() {
		if(serialPort != null) {
			serialPort.close();
		}
	}

	public boolean isOpen() {
		return serialPort != null;
	}
}