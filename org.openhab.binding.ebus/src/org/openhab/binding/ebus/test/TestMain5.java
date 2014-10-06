package org.openhab.binding.ebus.test;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.openhab.binding.ebus.EBusTelegram;
import org.openhab.binding.ebus.connection.EBusSerialConnector;
import org.openhab.binding.ebus.connection.AbstractEBusConnector;
import org.openhab.binding.ebus.connection.EBusConnectorEventListener;
import org.openhab.binding.ebus.parser.EBusUtils;

public class TestMain5 {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		System.out.println("TestMain5.main()");
		
		final AbstractEBusConnector t = new EBusSerialConnector("COM6");
		
		t.addEBusEventListener(new EBusConnectorEventListener() {
			@Override
			public void onTelegramReceived(EBusTelegram telegram) {
				System.err.println(EBusUtils.toHexDumpString(telegram.getBuffer()));
			}
		});
		try {
			t.connect();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		t.start();
		
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
//		byte[] data = TestMain3.convertString("30 76 50 22 03 CC 2B 0A BF 00 02 14 01 DE 00 AA");
		final byte[] data = TestMain3.convertString("00 76 50 22 03 CC 2B 0A BF");
		
		
		Runnable r = new Runnable() {
			@Override
			public void run() {
				t.send(data);
			}
		};
		
		ScheduledExecutorService newSingleThreadScheduledExecutor = Executors.newSingleThreadScheduledExecutor();
		newSingleThreadScheduledExecutor.scheduleAtFixedRate(r, 5, 6, TimeUnit.SECONDS);
		
		try {
			Thread.sleep(60000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
