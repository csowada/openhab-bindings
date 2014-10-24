package org.openhab.binding.lacrosse.test;

import org.openhab.binding.lacrosse.connector.LaCrosseConnector;

public class TestMain {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		LaCrosseConnector connector = new LaCrosseConnector();
		connector.open("COM15");
		
		try {
			Thread.sleep(90000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
