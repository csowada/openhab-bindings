package org.openhab.binding.ebus.test;

import javax.xml.bind.DatatypeConverter;

import org.openhab.binding.ebus.EbusTelegram;
import org.openhab.binding.ebus.parser.EBusTelegramParser;
import org.openhab.binding.ebus.parser.EBusUtils;

public class TestMain {

	public static byte[] convertString(String string) {
		return DatatypeConverter.parseHexBinary(string.replaceAll(" ", ""));
	}
	
	public static void main(String[] args) {

		final EBusTelegramParser parser = new EBusTelegramParser();
//		parser.loadConfigurationFile("C:\\openhab\\MINIMAL\\workspace\\org.openhab.binding.ebus\\META-INF\\test.json");
		
//		byte[] a0 = convertString("71 FE 50 17 10 A9 00 B5 50 03 A3 02 00 80 00 80 00 80 00 80 00 80 0E AA");
//		byte[] a0 = convertString("30 50 50 14 07 20 80 21 00 00 00 64 05 00 09 00 00 00 80 00 80 00 B4 05 3C 00 AA");
		byte[] a0 = convertString("71 FE 50 17 10 41 B5 A9 01 03 05 03 00 80 00 80 00 80 00 80 00 80 C2 AA");
		EbusTelegram convertData2 = EBusUtils.convertData2(a0);
		byte[] slaveData0 = convertData2.getSlaveData();
		System.out.println(EBusUtils.toHexDumpString(convertData2.getCRC()));
		
		a0 = convertString("30 50 50 14 07 20 80 21 00 00 00 64 05 00 09 00 00 00 80 00 80 00 B4 05 3C 00 AA");
		EbusTelegram ebusCmd = EBusUtils.convertData2(a0);
		short command = ebusCmd.getCommand();
		byte[] data2 = ebusCmd.getData();
		byte[] slaveData = ebusCmd.getSlaveData();
		if(command == 0x5014) {
			System.out.println("TestMain.main()");
		}
		System.out.println(EBusUtils.toHexDumpString(ebusCmd.getCRC()));

		
		//                          0  1  2  3  4  5  6  7  8  9 10 11 12 13 14 15 16 17 18 19 20 21 
		byte[] a1 = convertString("71 FE 50 17 10 41 B5 2A 05 85 03 00 80 00 80 00 80 00 80 00 80 05 aa");
//		byte[] a1 = convertString("71 FE 50 17 10 41 B5 50 03 A3 02 00 80 00 80 00 80 00 80 00 80 0E AA");
		EbusTelegram convertData22 = EBusUtils.convertData2(a1);
		
		parser.parse(convertData22);
		
//		if(true)
//			return;
		
//		EBusCommand eBusCommand = EBusUtils.convertData(a1);
		byte[] data = new byte[0];//eBusCommand.data;
//		CommandXXX.readCommand(eBusCommand);
		
//		if(true)
//			return;
		
//		System.out.println("=== Solardaten ===");
//		System.out.println("Pumpe an:                   " + "BCD " + decodeBCD(data[0]));
//		System.out.println("???:                        " + unsignedInt(data[1]));
//		System.out.println("???:                        " + decodeDATA2b(data[1], data[0]));
//		System.out.println("Kollektortemperatur:      x " + decodeDATA2c(data[3], data[2]));	//7,8
//		System.out.println("Warmwassertemperatur Sol: x " + decodeDATA2c(data[5], data[4]));	//9,10
		
		/*
		 * 5,6		? Statisch, jeweils Solar an				???
		 * 7,8		Kollektor Temp. 							DATA2c
		 * 9,10		Warmwasser Solar							DATA2c
		 * 11,12	???, Ersatzwert für DATA2b/c				??? DATA2b/c
		 * 13,14	???, Ersatzwert für DATA2b/c				??? DATA2b/c
		 * 15,16	???, Ersatzwert für DATA2b/c				??? DATA2b/c
		 * 17,18	???, Ersatzwert für DATA2b/c				??? DATA2b/c
		 * 19,20	???, Ersatzwert für DATA2b/c				??? DATA2b/c
		 */
		
		System.out.println("=== Solardaten ===");
		System.out.println("Pumpe an:                   " + "BCD " + decodeBCD(a1[5]));
		System.out.println("???:                        " + unsignedInt(a1[6]));
		System.out.println("???:                        " + decodeDATA2b(a1[6], a1[5]));
		System.out.println("Kollektortemperatur:      x " + decodeDATA2c(a1[8], a1[7]));	//7,8
		System.out.println("Warmwassertemperatur Sol: x " + decodeDATA2c(a1[10], a1[9]));	//9,10
//		bruteFaceCommands(a1);
		
//		if(true)
//			return;
		
		
		//                          0  1  2  3  4  5  6  7  8  9 10 11 12 13 14 15 16 17 18 19
//		byte[] a2 = convertString("71 FE 50 18 0E 3E 00 1C 03 0A 00 6A 03 67 03 01 00 00 00 DB");
		byte[] a2 = convertString("71 FE 50 18 0E 1E 00 65 02 00 00 2B 03 77 03 01 00 00 00 A9");
		
		/*
		 * 5,6		? Akt. Leistung								??? DATA2b
		 * 7,8		Tagesertrag	Kommastellen in Wh				WORD
		 * 9,10		Tagesertrag	in Wh							WORD
		 * 11,12	?Gesamtertrag								??? DATA2b/c
		 * 13,14	Gesamtertrag								WORD
		 * 15,16	Gesamtertrag	*1000						WORD
		 * 17,18	? Gesamtertrag  *1000*1000					???
		 */
		
		System.out.println("=== Solarertrag ===");
		
		// 71 FE 50 18 0E 
		// 3E 00				-> akt. leitung
		// 1C 03 0A 00			-> Tagesertrag
		// 6A 03				-> ??
		// 67 03 01 00			-> Gesamtertrag
		// 00 00
		// DB
		
		int m = (a2[8]<<8) + a2[7];
		int m2 = (a2[10]<<8) + a2[9];
		
		int mm = m2*1000 + m;
		int mm2 = (a2[10]<<24) + (a2[9]<<16) + (a2[8]<<8) + a2[7];
		
		
		int g = (a2[12]<<8) + a2[11];
		int g2 = (a2[14]<<8) + a2[13];
		int g3 = (a2[16]<<8) + a2[15];
		int g4 = g3*1000 + g2;
		
		// byte 5 ist ein bit? 1E und 3E
		System.out.println("Tagesertrag kW/h:         x " + (float)mm/1000); //7-10
		System.out.println("Gesamtertrag kW/h:        x " + (float)g4/1000); //13-16
		System.out.println("Aktuelle Leitung:         ? " + decodeDATA2b(a2[6], a2[5]));
		System.out.println("Aktuelle Leitung:         ? " + decodeDATA2c(a2[6], a2[5]));
		
		System.out.println("???:                        " + decodeDATA2b(a2[12], a2[11]));
		System.out.println("???:                        " + decodeDATA2c(a2[12], a2[11]));
		bruteFaceCommands(a2);
		
		//                          0  1  2  3  4  5  6  7  8  9 10 11 12 13 14
//		byte[] a3 = convertString("71 FE 50 23 09 98 62 09 01 00 5D 01 00 00 3E");
		byte[] a3 = convertString("71 30 50 23 09 B0 F4 02 00 04 5D 01 00 00 80");
		
		System.out.println("=== Solar an BM ===");
		bruteFaceCommands(a3);
		
//		System.out.println("Test 1:  0        " + decodeDATA2c((byte) 0x00, (byte)0x00));
//		System.out.println("Test 2:  1/16     " + decodeDATA2c((byte) 0x00, (byte)0x01));
//		System.out.println("Test 3:  -1/16    " + decodeDATA2c((byte) 0xFF, (byte)0xFF));
//		System.out.println("Test 4:  -1       " + decodeDATA2c((byte) 0xFF, (byte)0xF0));
//		System.out.println("Test 5:  -2048    " + decodeDATA2c((byte) 0x80, (byte)0x00));
//		System.out.println("Test 6:  -2047,9  " + decodeDATA2c((byte) 0x80, (byte)0x01));
//		System.out.println("Test 7:  2047,9   " + decodeDATA2c((byte) 0x7F, (byte)0xFF));
//		
//		
//		System.out.println("Test 1:           " + decodeDATA2b((byte) 0x00, (byte)0x00));
//		System.out.println("Test 2:           " + decodeDATA2b((byte) 0x00, (byte)0x01));
//		System.out.println("Test 3:           " + decodeDATA2b((byte) 0xFF, (byte)0xFF));
//		System.out.println("Test 4:           " + decodeDATA2b((byte) 0xFF, (byte)0xF0));
//		System.out.println("Test 6:           " + decodeDATA2b((byte) 0x80, (byte)0x00));
//		System.out.println("Test 5:           " + decodeDATA2b((byte) 0x80, (byte)0x01));
//		System.out.println("Test 6:           " + decodeDATA2b((byte) 0x7F, (byte)0xFF));
		
		
		//                          0  1  2  3  4  5  6  7  8  9 10 11 12 13
		byte[] a5 = convertString("03 FE 05 03 08 01 00 00 FF 51 2F 44 13 96");
		if(a5[5] == 1) {
			System.out.println("State-Nummer:             x " + unsignedInt(a5[6])); //7
			System.out.println("Zustände BIT:             x " + unsignedInt(a5[7])); //7
			System.out.println("Stellgrad %:              x " + unsignedInt(a5[8])); //8
			System.out.println("Kesseltemp.:              x " + decodeDATA1c(a5[9])); //9
			System.out.println("Rücklauftemp..:           x " + unsignedInt(a5[10])); //10
			System.out.println("Boilertemp..:             x " + unsignedInt(a5[11])); //11
			System.out.println("Aussentemp.:              x " + a5[12]); //12
		}

		//                          0  1  2  3  4  5  6  7  8  9 10 11 12 13
		byte[] a6 = convertString("03 F1 08 00 08 00 80 99 13 80 00 00 05 F6");
		
		System.out.println("Kesselsollwert:           x " + decodeDATA2b(a6[6], a6[5])); //7
		System.out.println("Aussentemp.:              x " + decodeDATA2b(a6[8], a6[7])); //7
		System.out.println("Leistungszwang:           x " + unsignedInt(a5[9])); //8
		System.out.println("Status:                   x " + unsignedInt(a5[10])); //9
		System.out.println("Brauchwasser Soll:        x " + decodeDATA2b(a6[12], a6[11])); //10
		
		
		
	}

	private static void bruteFaceCommands(byte[] data) {
		
		System.out.println("==== Int ====");
		for (int i = 5; i < data.length-1; i++) {
			System.out.println(i + ": " + unsignedInt(data[i]));
		}
		
		System.out.println("==== BCD ====");
		for (int i = 5; i < data.length-1; i++) {
			System.out.println(""+i + ": " + decodeBCD(data[i]));
		}
		
		System.out.println("==== DATA1c ====");
		for (int i = 5; i < data.length-1; i++) {
			System.out.println(""+i + ": " + decodeDATA1c(data[i]));
		}
		
		
		
		System.out.println("==== DATA2b ====");
		for (int i = 5; i < data.length-2; i++) {
			System.out.println(""+i+","+(i+1) + ": " + decodeDATA2b(data[i], data[i+1]));
		}
		
		System.out.println("==== DATA2c ====");
		for (int i = 5; i < data.length-2; i++) {
			System.out.println(""+i+","+(i+1) + ": " + decodeDATA2c(data[i], data[i+1]));
		}
		
		System.out.println("==== Word ====");
		for (int i = 5; i < data.length-2; i++) {
			int m = (data[i+1]<<8) + data[1];
			System.out.println(""+i+","+(i+1) + ": " + m);
		}
	}
	
	private static int unsignedInt(int signedInt) {
		return (signedInt << 24) >>> 24;
	}

	private static int decodeBCD(byte data) {
		return (data >> 4)*10 + (data & (byte) 0x0F);
	}
	
	private static int signedInt(int unsignedInt) {
		return (unsignedInt << 24) >> 24;
	}

	private static float decodeDATA1c(int data) {
		int psuedoUnsigned = (data << 24) >>> 24;
		return (psuedoUnsigned / 2);
	}

	private static float decodeDATA2c(byte highData, byte lowData) {

		int h = unsignedInt(highData);
		int l = unsignedInt(lowData);
//		int h = highData;
//		int l = lowData;
		int x = (h<<8) + l;
		int y = (x>>4);
		int g = x & (byte)0x0F;
		
		float g2 = (float)g / 16;
		
		float z = y + g2;
		
		if((highData & (byte) 0x80) == (byte) 0x80) {
			return -9999;
		}
		
		return z;
		
//		if((highData & (byte) 0x80) == (byte) 0x80) {
//			float a = 1- (h*16);
//			float b = 1- (l & 0x0F);
//			float c = 1- ((float)(l >> 4) / 16);
//			
//			return (1- (h*16)) - (1- (l & 0x0F)) - (1- ((float)(l >> 4) / 16));
//		} else {
//			return (h*16) + (l & 0x0F) + ((float)(l >> 4) / 16);
//		}
	}
	
	private static float decodeDATA2b(byte highData, byte lowData) {
		float h = unsignedInt(highData);
		float l = unsignedInt(lowData);
		if((highData & (byte) 0x80) == (byte) 0x80) {
			//return (1 - h) - (1- (l / 256));
			return -9999;
		} else {
			return h + (l / 256);
		}
	}

}
