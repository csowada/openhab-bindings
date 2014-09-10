package org.openhab.binding.ebus.serial;

import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.openhab.binding.ebus.EbusTelegram;
import org.openhab.binding.ebus.parser.EBusTelegramParser;
import org.openhab.binding.ebus.parser.EBusUtils;

public class EbusSerialPortEvent implements SerialPortEventListener {

//	final static int SYN = 0xAA;
//	final static int ACK_OK = 0x00;
//	final static int ACK_FAIL = 0xFF;
	
	ByteBuffer dataBuffer = ByteBuffer.allocate(50);
	
//	int dataLen = -1;
	private EBusTelegramParser parser;
	
	public EbusSerialPortEvent() {
		parser = new EBusTelegramParser();
		parser.loadConfigurationFile("C:\\Users\\CSo\\git\\openhab-bindings\\org.openhab.binding.ebus\\META-INF\\test.json");
	}
	
	@Override
	public void serialEvent(SerialPortEvent event) {
		if(event.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
			try {
				onDataAvailable(event);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else if(event.getEventType() == SerialPortEvent.OUTPUT_BUFFER_EMPTY) {
			System.out.println("EbusSerialPortEvent.serialEvent() -> OUTPUT_BUFFER_EMPTY");
		}
	}

//	private StringBuilder toHexDumpString(byte[] data) {
//		StringBuilder sb = new StringBuilder();
//		for (int i = 0; i < data.length; i++) {
//			byte c = data[i];
//			if(i > 0) sb.append(' ');
//			sb.append(String.format("%02X", (0xFF & c)));
//		}
//		return sb;
//	}
//	
//	private void analyseData(byte[] data) {
//		System.out.println("DATA: " + toHexDumpString(data).toString());
//		
//		byte qq  = data[0];		// Source Address
//		byte zz  = data[1];		// Destination Address
//		byte pb  = data[3];		// Primary Command
//		byte sb  = data[3];		// Secondary Command
//		byte nn  = data[4];		// Number of data
//		byte crc = 0;			// crc
//		
//		System.out.println("EbusSerialPortEvent.analyseData()");
//		
//		for (int i = 5; i < data.length; i++) {
//			
//		}
//	}
	
	private void onDataAvailable(SerialPortEvent event) throws IOException {

		SerialPort port = (SerialPort) event.getSource();
		InputStream is = port.getInputStream();

		byte[] buffer = new byte[50];
		int bufLen = is.read(buffer);
		
		byte data = 0;
		
		for (int i = 0; i < bufLen; i++) {
			
			data = buffer[i];
			
			
			
			// sync byte, next could be a new data package
			if(data == (byte)0xAA) {
				


				if(dataBuffer.position() > 5) {
					
					dataBuffer.put((byte) 0xAA);
					
					byte[] b = new byte[dataBuffer.position()];
//					dataBuffer.get(b, 0, b.length);
					System.arraycopy(dataBuffer.array(), 0, b, 0, dataBuffer.position());
					
					EbusTelegram telegram = EBusUtils.convertData2(b);
					parser.parse(telegram);
				}

				dataBuffer.clear();
			} else {
				// write to buffer
//				System.out.println(EBusUtils.toHexDumpString(data));
				dataBuffer.put((byte) data);
			}
		}
	}
}