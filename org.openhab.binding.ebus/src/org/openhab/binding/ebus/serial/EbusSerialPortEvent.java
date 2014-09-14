package org.openhab.binding.ebus.serial;

import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.openhab.binding.ebus.EbusTelegram;
import org.openhab.binding.ebus.parser.EBusUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class EbusSerialPortEvent implements SerialPortEventListener {

	private static final Logger logger = LoggerFactory
			.getLogger(EbusSerialPortEvent.class);

	/** serial receive buffer */
	private ByteBuffer dataBuffer = ByteBuffer.allocate(50);
	
	/**
	 * Process an ebus telegram after successful receiving (crc checks).
	 * @param telegram The correct received ebus telegram
	 */
	public abstract void onEBusTelegramAvailable(EbusTelegram telegram);
	
	/* (non-Javadoc)
	 * @see gnu.io.SerialPortEventListener#serialEvent(gnu.io.SerialPortEvent)
	 */
	@Override
	public void serialEvent(SerialPortEvent event) {
		if(event.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
			try {
				onDataAvailable(event);
			} catch (IOException e) {
				logger.error(e.toString(), e);
			}
		}
	}
	
	/**
	 * Process serial onDataAvailable event.
	 * @param event The event
	 * @throws IOException
	 */
	private void onDataAvailable(SerialPortEvent event) throws IOException {

		final SerialPort port = (SerialPort) event.getSource();
		final InputStream is = port.getInputStream();

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
					System.arraycopy(dataBuffer.array(), 0, b, 0, dataBuffer.position());
					
					EbusTelegram telegram = EBusUtils.processEBusData(b);
					onEBusTelegramAvailable(telegram);
				}

				dataBuffer.clear();
			} else {
				// write to buffer
				dataBuffer.put((byte) data);
			}
		}
	}
}