package org.openhab.binding.ebus.test;

import org.openhab.binding.ebus.EbusTelegram;
import org.openhab.binding.ebus.connection.EBusSerialConnector;
import org.openhab.binding.ebus.connection.AbstractEBusConnector;
import org.openhab.binding.ebus.connection.EBusConnectorEventListener;
import org.openhab.binding.ebus.parser.EBusUtils;

public class TestMain5 {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		System.out.println("TestMain5.main()");
		
		AbstractEBusConnector t = new EBusSerialConnector("COM6");
		
		t.addEBusEventListener(new EBusConnectorEventListener() {
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
