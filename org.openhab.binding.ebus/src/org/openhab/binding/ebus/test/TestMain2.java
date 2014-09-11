package org.openhab.binding.ebus.test;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.util.TooManyListenersException;

import org.openhab.binding.ebus.serial.EbusSerialPortEvent;

public class TestMain2 {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		try {
			CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier("COM6");
			
			SerialPort serialPort = (SerialPort) portIdentifier.open("openhab", 3000);
			serialPort.setSerialPortParams(2400, SerialPort.DATABITS_8,
					SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

			serialPort.disableReceiveTimeout();
			serialPort.enableReceiveThreshold(1);
			
			serialPort.addEventListener(new EbusSerialPortEvent());
			serialPort.notifyOnDataAvailable(true);
			serialPort.notifyOnOutputEmpty(true);
			
			
//			DataInputStream inputStream = new DataInputStream(
//					new BufferedInputStream(serialPort.getInputStream()));
//			OutputStream outputStream = serialPort.getOutputStream();
//			byte[] buffer = new byte[50];
//					
//			while (inputStream.available() > 0) {
//				
//				int l = inputStream.read(buffer);
//				
//				
//				
//				Thread.sleep(40);
//			}
			
			
			
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
		}
		
	}

}
