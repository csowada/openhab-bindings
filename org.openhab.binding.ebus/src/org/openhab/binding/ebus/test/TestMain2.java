package org.openhab.binding.ebus.test;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.TooManyListenersException;

import org.json.simple.parser.ParseException;
import org.openhab.binding.ebus.EbusTelegram;
import org.openhab.binding.ebus.parser.EBusTelegramParser;
import org.openhab.binding.ebus.serial.EBusSerialPortEvent;

public class TestMain2 {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		try {
			CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier("COM10");
			
			SerialPort serialPort = (SerialPort) portIdentifier.open("openhab", 3000);
			serialPort.setSerialPortParams(2400, SerialPort.DATABITS_8,
					SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

			serialPort.disableReceiveTimeout();
			serialPort.enableReceiveThreshold(1);
			
			final URL configurationUrl = ClassLoader.getSystemResource("META-INF/ebus-configuration.json");
			final EBusTelegramParser parser = new EBusTelegramParser();
//			url = new URL("platform:/plugin/de.vogella.rcp.plugin.filereader/files/test.txt")
			try {
				parser.loadConfigurationFile(configurationUrl);
			} catch (IOException | ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			EBusSerialPortEvent event = new EBusSerialPortEvent(serialPort) {
				@Override
				public void onEBusTelegramAvailable(EbusTelegram telegram) {
					System.out.println(telegram);
					parser.parse(telegram);
				}
			};
			
			serialPort.addEventListener(event);
			serialPort.notifyOnDataAvailable(true);
			serialPort.notifyOnOutputEmpty(true);
			
			
			
			OutputStream outputStream = serialPort.getOutputStream();
			byte[] convertString = TestMain3.convertString("30 76 50 22 03 CC 2B 0A BF 00 02 14 01 DE 00 AA");
			outputStream.write(convertString);
//			outputStream.flush();
			
			Thread.sleep(1000);
			
			serialPort.close();
			System.out.println("Ende!");
			
		} catch (NoSuchPortException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (PortInUseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedCommOperationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TooManyListenersException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
