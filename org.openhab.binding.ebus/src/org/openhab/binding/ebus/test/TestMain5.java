package org.openhab.binding.ebus.test;

import java.io.IOException;

import org.openhab.binding.ebus.EbusTelegram;
import org.openhab.binding.ebus.parser.EBusUtils;
import org.openhab.binding.ebus.serial.EBusThread;
import org.openhab.binding.ebus.serial.IEBusEventListener;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

public class TestMain5 {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		System.out.println("TestMain5.main()");
		
		try {
			final CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier("COM6");
			
			SerialPort serialPort = (SerialPort) portIdentifier.open("openhab-ebus", 3000);
			serialPort.setSerialPortParams(2400, SerialPort.DATABITS_8,
					SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

			serialPort.disableReceiveTimeout();
			serialPort.enableReceiveThreshold(1);
			
			EBusThread t = new EBusThread(
					serialPort.getInputStream(), serialPort.getOutputStream());
			
			t.addEBusEventListener(new IEBusEventListener() {
				@Override
				public void onTelegramReceived(EbusTelegram telegram) {
					System.err.println(EBusUtils.toHexDumpString(telegram.getBuffer()));
				}
			});
			
			t.start();
			
			byte[] data = TestMain3.convertString("30 76 50 22 03 CC 2B 0A BF 00 02 14 01 DE 00 AA");
			t.send(data);
			
			Thread.sleep(60000);
			
			
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
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
