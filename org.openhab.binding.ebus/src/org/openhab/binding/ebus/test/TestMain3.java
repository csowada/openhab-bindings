package org.openhab.binding.ebus.test;

import javax.xml.bind.DatatypeConverter;

import org.openhab.binding.ebus.EbusTelegram;
import org.openhab.binding.ebus.parser.EBusTelegramParser;
import org.openhab.binding.ebus.parser.EBusUtils;

public class TestMain3 {

	public static void main(String[] args) {
		final EBusTelegramParser parser = new EBusTelegramParser();
		parser.loadConfigurationFile("C:\\openhab\\MINIMAL\\workspace\\org.openhab.binding.ebus\\META-INF\\test.json");


		byte[] a1 = null;
		EbusTelegram telegram = null;

		
		
		
		a1 = convertString("30 76 50 22 03 CC 2B 0A BF 00 02 14 01 DE 00 AA");
		telegram = EBusUtils.convertData2(a1);
		parser.parse(telegram);
		
		if(true)
			return;
		
		// 0503
		a1 = convertString("03 FE 05 03 08 01 00 00 FF 51 2F 44 13 96 AA");
		telegram = EBusUtils.convertData2(a1);
		parser.parse(telegram);

		// 0800
		a1 = convertString("03 F1 08 00 08 00 80 99 13 80 00 00 05 F6 AA");
		telegram = EBusUtils.convertData2(a1);
		parser.parse(telegram);



		// 0514
		a1 = convertString("30 50 50 14 07 20 80 21 00 00 00 64 05 00 09 00 00 00 80 00 80 00 B4 05 3C 00 AA");
		telegram = EBusUtils.convertData2(a1);
		parser.parse(telegram);

		// 5017
		a1 = convertString("71 FE 50 17 10 41 B5 2A 05 85 03 00 80 00 80 00 80 00 80 00 80 05 AA");
		telegram = EBusUtils.convertData2(a1);
		parser.parse(telegram);


		// 5018
		a1 = convertString("71 FE 50 18 0E 1E 00 65 02 00 00 2B 03 77 03 01 00 00 00 A9 AA");
		telegram = EBusUtils.convertData2(a1);
		parser.parse(telegram);

		// 5022
		a1 = convertString("30 08 50 22 03 CC 6F 01 BB 00 02 00 00 2C 00 AA");
		telegram = EBusUtils.convertData2(a1);
		parser.parse(telegram);

		// 5023
		a1 = convertString("71 30 50 23 09 B0 F4 02 00 04 5D 01 00 00 80 AA");
		telegram = EBusUtils.convertData2(a1);
		parser.parse(telegram);


	}

	public static byte[] convertString(String string) {
		return DatatypeConverter.parseHexBinary(string.replaceAll(" ", ""));
	}

}
