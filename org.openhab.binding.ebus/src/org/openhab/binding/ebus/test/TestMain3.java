package org.openhab.binding.ebus.test;

import java.net.URL;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

import org.openhab.binding.ebus.EbusTelegram;
import org.openhab.binding.ebus.parser.EBusTelegramParser;
import org.openhab.binding.ebus.parser.EBusUtils;

public class TestMain3 {

	public static void go(String data, EBusTelegramParser parser) {
		byte[] buffer = convertString(data);
		EbusTelegram telegram = EBusUtils.convertData2(buffer);
		parser.parse(telegram);
	}
	
	public static void main(String[] args) {
		
		final EBusTelegramParser parser = new EBusTelegramParser();
		final URL configurationUrl = ClassLoader.getSystemResource("META-INF/ebus-configuration.json");
		
//		url = new URL("platform:/plugin/de.vogella.rcp.plugin.filereader/files/test.txt")
		parser.loadConfigurationFile(configurationUrl);

		byte[] buffer = null;
		EbusTelegram telegram = null;

		
		
		
//		buffer = convertString("30 76 50 22 03 CC 2B 0A BF 00 02 14 01 DE 00 AA");
//		telegram = EBusUtils.convertData2(buffer);
//		Map<String, Object> parse = parser.parse(telegram);
//		
//		
//		
//		buffer = convertString("30 08 50 22 03 CC 0D 00 BB 00 02 2A 01 FD 00 AA");
//		telegram = EBusUtils.convertData2(buffer);
//		parse = parser.parse(telegram);
		
		go("30 08 50 22 03 CC 0D 00 BB 00 02 2A 01 FD 00 AA", parser);	// Temp ?
		go("30 08 50 22 03 CC 6F 01 BB 00 02 00 00 2C 00 AA", parser);	// 0
		go("30 08 50 22 03 CC 1A 27 59 00 02 9A 00 A1 00 AA", parser);	// Anlagendruck WORD / 10			1,54
		go("30 08 50 22 03 CC 53 27 13 00 02 0D 00 6F 00 AA", parser);	// 13
		go("30 08 50 22 03 CC 54 27 E4 00 02 00 00 2C 00 AA", parser);	// 0
		go("30 08 50 22 03 CC 16 00 A6 00 02 6F 01 6F 00 AA", parser);	// Temp ?
		go("30 08 50 22 03 CC 6C 01 8D 00 02 00 00 2C 00 AA", parser);	// 0
		go("30 08 50 22 03 CC 9A 01 74 00 02 82 00 8A 00 AA", parser);	// Softwareversion WORD				130
		go("30 08 50 22 03 CC 26 02 F2 00 02 30 1E 64 00 AA", parser);	// Anz. Zündungen WORD				7728
		go("30 08 50 22 03 CC 27 02 69 00 02 00 00 2C 00 AA", parser);	// 0
		go("30 08 50 22 03 CC 2A 02 2A 00 02 D0 00 DD 00 AA", parser);	// Betriebsstunden Brenner WORD		208
		go("30 08 50 22 03 CC 16 27 81 00 02 40 FE 1A 00 AA", parser);	// 
		go("30 08 50 22 03 CC 1D 27 AE 00 02 9A 01 A0 00 AA", parser);	// Temp ?
		go("30 08 50 22 03 CC 4F 27 F9 00 02 03 00 1A 00 AA", parser);	// Ionisation WORD					3
		go("30 08 50 22 03 CC 57 27 D2 00 02 00 00 2C 00 AA", parser);	// 0
		go("30 08 50 22 03 CC 5E 27 50 00 02 29 00 CA 00 AA", parser);	// Anz. Netz-Ein WORD o. Byte		41
		
		go("30 08 50 22 03 CC 0E 00 8D 00 02 F8 01 A1 00 AA", parser);
		go("30 08 50 22 03 CC 29 02 1C 00 02 00 00 2C 00 AA", parser);
		go("30 08 50 22 03 CC 16 27 81 00 02 40 FE 1A 00 AA", parser);
		
//		go("30 76 50 22 03 CC AE 09 ED 00 02 02 00 81 00 AA", parser);
//		go("30 76 50 22 03 CC F8 02 70 00 02 00 00 2C 00 AA", parser);
//		go("30 76 50 22 03 CC FA 02 DD 00 02 00 00 2C 00 AA", parser);
		
		
		/*
		 * 
unknown -48 Value
FULL DATA : 30 08 50 22 03 CC 16 27 81 00 02 40 FE 1A 00 AA
unknown -448 Value
FULL DATA : 30 08 50 22 03 CC 1D 27 AE 00 02 9A 01 A0 00 AA
unknown 154 Value
FULL DATA : 30 08 50 22 03 CC 4F 27 F9 00 02 03 00 1A 00 AA
unknown 3 Value
FULL DATA : 30 08 50 22 03 CC 57 27 D2 00 02 00 00 2C 00 AA
unknown 0 Value
FULL DATA : 30 08 50 22 03 CC 5E 27 50 00 02 29 00 CA 00 AA
unknown 41 Value
		 */
		
		
		
//		go("30 76 50 22 03 CC FA 02 DD 00 02 00 00 2C 00 AA", parser);
//		go("30 76 50 22 03 CC FA 02 DD 00 02 00 00 2C 00 AA", parser);
//		go("30 76 50 22 03 CC FA 02 DD 00 02 00 00 2C 00 AA", parser);
		
		if(true)
			return;
		
		// 0503
		buffer = convertString("03 FE 05 03 08 01 00 00 FF 51 2F 44 13 96 AA");
		telegram = EBusUtils.convertData2(buffer);
		parser.parse(telegram);

		// 0800
		buffer = convertString("03 F1 08 00 08 00 80 99 13 80 00 00 05 F6 AA");
		telegram = EBusUtils.convertData2(buffer);
		parser.parse(telegram);



		// 0514
		buffer = convertString("30 50 50 14 07 20 80 21 00 00 00 64 05 00 09 00 00 00 80 00 80 00 B4 05 3C 00 AA");
		telegram = EBusUtils.convertData2(buffer);
		parser.parse(telegram);

		// 5017
		buffer = convertString("71 FE 50 17 10 41 B5 2A 05 85 03 00 80 00 80 00 80 00 80 00 80 05 AA");
		telegram = EBusUtils.convertData2(buffer);
		parser.parse(telegram);


		// 5018
		buffer = convertString("71 FE 50 18 0E 1E 00 65 02 00 00 2B 03 77 03 01 00 00 00 A9 AA");
		telegram = EBusUtils.convertData2(buffer);
		parser.parse(telegram);

		// 5022
		buffer = convertString("30 08 50 22 03 CC 6F 01 BB 00 02 00 00 2C 00 AA");
		telegram = EBusUtils.convertData2(buffer);
		parser.parse(telegram);

		// 5023
		buffer = convertString("71 30 50 23 09 B0 F4 02 00 04 5D 01 00 00 80 AA");
		telegram = EBusUtils.convertData2(buffer);
		parser.parse(telegram);


	}

	public static byte[] convertString(String string) {
		return DatatypeConverter.parseHexBinary(string.replaceAll(" ", ""));
	}

}
