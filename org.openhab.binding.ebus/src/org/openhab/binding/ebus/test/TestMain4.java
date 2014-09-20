package org.openhab.binding.ebus.test;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.UnsupportedCommOperationException;

import org.openhab.binding.ebus.serial.EBusSerialConnector;

public class TestMain4 {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		final EBusSerialConnector connector = new EBusSerialConnector();
		try {
			connector.open("COM10");
			
			byte[] data = TestMain3.convertString("30 76 50 22 03 CC 2B 0A BF 00 02 14 01 DE 00 AA");
			connector.send(data);
			
			data = TestMain3.convertString("10 76 50 22 03 CC 2B 0A BF 00 02 14 01 DE 00 AA");
			connector.send(data);
			
			Runnable r = new Runnable() {
				@Override
				public void run() {
					
					byte[] data2 = TestMain3.convertString("30 76 50 22 03 CC 2B 0A BF 00 02 14 01 DE 00 AA");
					connector.send(data2);
				}
			};
			
			Runnable r2 = new Runnable() {
				@Override
				public void run() {
					connector.sendSYN();
				}
			};
			
			Runnable r3 = new Runnable() {
				@Override
				public void run() {
					
					byte[] data2 = TestMain3.convertString("10 76 50 22 03 CC 2B 0A BF 00 02 14 01 DE 00 AA");
					connector.send(data2);
				}
			};
			
			
			ScheduledExecutorService newScheduledThreadPool = Executors.newScheduledThreadPool(2);
//			newScheduledThreadPool.scheduleAtFixedRate(r, 0, 5, TimeUnit.SECONDS);
//			newScheduledThreadPool.scheduleAtFixedRate(r, 0, 12, TimeUnit.SECONDS);
			newScheduledThreadPool.scheduleAtFixedRate(r2, 0, 3, TimeUnit.SECONDS);
			
			
			Thread.sleep(500);
			connector.sendSYN();
			
			Thread.sleep(60000);
			
			connector.close();
			System.out.println("Ende!");
			
		} catch (NoSuchPortException | PortInUseException
				| UnsupportedCommOperationException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
	}

}
