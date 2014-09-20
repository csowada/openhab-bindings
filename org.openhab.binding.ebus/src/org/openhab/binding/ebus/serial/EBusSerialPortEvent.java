package org.openhab.binding.ebus.serial;

import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.openhab.binding.ebus.EbusTelegram;
import org.openhab.binding.ebus.parser.EBusUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class EBusSerialPortEvent implements SerialPortEventListener {

	private static final Logger logger = LoggerFactory
			.getLogger(EBusSerialPortEvent.class);

	private Queue<byte[]> outputQueue = new LinkedBlockingQueue<byte[]>(20);
	private boolean isReSendData = false;
	private int collisionCounter = 0;
	
	private InputStream is;
	private OutputStream out;

	/** serial receive buffer */
	private ByteBuffer dataBuffer = ByteBuffer.allocate(50);

	private SerialPort serialPort;

	public EBusSerialPortEvent(SerialPort serialPort) {
		this.serialPort = serialPort;
		try {
			is = serialPort.getInputStream();
			out = serialPort.getOutputStream();
		} catch (IOException e) {
			logger.error(e.toString(), e);
		}
	}

	/**
	 * Process an ebus telegram after successful receiving (crc checks).
	 * @param telegram The correct received ebus telegram
	 */
	public abstract void onEBusTelegramAvailable(EbusTelegram telegram);

	private void onEBusAvailable() {
		try {
			send();
		} catch (IOException e) {
			logger.error(e.toString(), e);
		}
	}

	private void send() throws IOException {

		if(outputQueue.isEmpty()) {
			logger.trace("Send buffer is empty, nothing to send...");
			return;
		}

		byte[] dataOutputBuffer = outputQueue.peek();
		logger.debug("EBusSerialPortEvent.send()" + EBusUtils.toHexDumpString(dataOutputBuffer));

		// events kurz deaktivieren, wir schreiben und lesen gleichzeitig
		// um kollisionen zu erkennen.
		serialPort.notifyOnDataAvailable(false);

		for (int i = 0; i < dataOutputBuffer.length; i++) {
			byte b = dataOutputBuffer[i];
			out.write(b);
			
			// gerade geschriebenes wieder vom bus einlesen
			int read = is.read();
			if(read != -1) {
				// geschriebenes byte und gelesenes byte nicht identisch,
				// das ist dann eine kollision
				if(b != (byte) (read & 0xFF)) {
					// kollision
					if(collisionCounter++ > 10) {
						logger.error("More than 10 bus conflicts has ocoured, there is something wrong!");
						outputQueue.clear();
					} else {
						logger.warn("eBus Send Collision!");
					}
					
					// events wieder aktivieren
					serialPort.notifyOnDataAvailable(true);
					return;
				}
			}
		}

		// sauber gesendet, zurücksetzen
		collisionCounter = 0;
		
		// daten sauber an den bus übermittelt!
		processReceivedData(dataOutputBuffer, dataOutputBuffer.length, true);
		serialPort.notifyOnDataAvailable(true);
	}

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

	protected void send(byte[] data) {
		logger.debug("Add to send queue: " + EBusUtils.toHexDumpString(data));
		outputQueue.add(data);
	}

	/**
	 * Process serial onDataAvailable event.
	 * @param event The event
	 * @throws IOException
	 */
	private void onDataAvailable(SerialPortEvent event) throws IOException {

		logger.trace("EBusSerialPortEvent.onDataAvailable()");

		byte[] buffer = new byte[50];
		int bufLen = is.read(buffer);

		processReceivedData(buffer, bufLen, false);
	}

	private void processReceivedData(byte[] buffer, int len, boolean ownCommand) {

		byte data = 0;

		if(ownCommand) {
			// gesendete befehle kommen als ganze an,
			// daher alte daten löschen
			dataBuffer.clear();
		}

		for (int i = 0; i < len; i++) {

			data = buffer[i];

			// sync byte, next could be a new data package
			if(data == (byte)EbusTelegram.SYN) {
				
				// min length of ebus telegrams
				if(dataBuffer.position() > 5) {

					// sync byte
					dataBuffer.put((byte)EbusTelegram.SYN);

					byte[] b = Arrays.copyOf(dataBuffer.array(), dataBuffer.position());
					
					// finally, clear the receive buffer to be ready for new data
					dataBuffer.clear();
					
					final EbusTelegram telegram = EBusUtils.processEBusData(b);
					if(telegram != null) {
						onEBusTelegramAvailable(telegram);
					}

					if(ownCommand) {
						if(telegram != null) {
							// telegram korrekt
							logger.debug("Alles OK");
							isReSendData = false;
							outputQueue.poll();

						} else if(isReSendData) {
							logger.error("Nicht geklappt, verwerfen");
							isReSendData = false;
							outputQueue.poll();

						} else {
							// telegram ungültig
							logger.warn("Befehl erneut senden");
							isReSendData = true;
						}
					}
				}

				// now it's time to send if something is in the queue
				onEBusAvailable();

				
			} else {
				// write to buffer
				dataBuffer.put((byte) data);
			}
		}
	}


	protected void sendSYN() {
		try {
			out.write(EbusTelegram.SYN);
		} catch (IOException e) {
			logger.error(e.toString(), e);
		}
	}

}