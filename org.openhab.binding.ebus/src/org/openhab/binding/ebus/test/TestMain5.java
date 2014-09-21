package org.openhab.binding.ebus.test;

import org.openhab.binding.ebus.EbusTelegram;
import org.openhab.binding.ebus.parser.EBusUtils;
import org.openhab.binding.ebus.serial.EBusSerialThread;
import org.openhab.binding.ebus.serial.EBusThread;
import org.openhab.binding.ebus.serial.IEBusEventListener;

public class TestMain5 {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		System.out.println("TestMain5.main()");
		
		EBusThread t = new EBusSerialThread("COM6");
		
		t.addEBusEventListener(new IEBusEventListener() {
			@Override
			public void onTelegramReceived(EbusTelegram telegram) {
				System.err.println(EBusUtils.toHexDumpString(telegram.getBuffer()));
			}
		});
		
		t.start();
		
		byte[] data = TestMain3.convertString("30 76 50 22 03 CC 2B 0A BF 00 02 14 01 DE 00 AA");
		t.send(data);
		
		try {
			Thread.sleep(60000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
