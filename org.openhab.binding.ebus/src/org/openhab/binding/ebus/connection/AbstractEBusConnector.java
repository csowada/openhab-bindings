/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.ebus.connection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.openhab.binding.ebus.EbusTelegram;
import org.openhab.binding.ebus.parser.EBusUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christian Sowada
 * @since 1.6.0
 */
public abstract class AbstractEBusConnector extends Thread {

	private static final Logger logger = LoggerFactory
			.getLogger(AbstractEBusConnector.class);

	private final Queue<byte[]> outputQueue = new LinkedBlockingQueue<byte[]>(20);
	private final List<EBusConnectorEventListener> listeners = new ArrayList<EBusConnectorEventListener>();

	/** serial receive buffer */
	private final ByteBuffer inputBuffer = ByteBuffer.allocate(50);

	/** input stream for eBus communication*/
	protected InputStream inputStream;

	/** output stream for eBus communication*/
	protected OutputStream outputStream;

	private int lockCounter = 0;

	private boolean blockNextSend;
	private boolean lastSendCollisionDetected = false;
	private static int LOCKOUT_COUNTER_MAX = 3;

	/**
	 * Constructor
	 */
	public AbstractEBusConnector() {
		super("eBus Connection Thread");
		this.setDaemon(true);
	}

	/**
	 * Connects the connector to it's backend system. It's important
	 * to connect before start the thread.
	 * @return
	 * @throws IOException
	 */
	public boolean connect() throws IOException {
		return true;
	}

	/**
	 * Disconnects the connector from it's backend system.
	 * @return
	 * @throws IOException
	 */
	public boolean disconnect() throws IOException {
		return true;
	}

	/**
	 * Add a listener
	 * @param listener
	 */
	public void addEBusEventListener(EBusConnectorEventListener listener) {
		listeners.add(listener);
	}

	/**
	 * Remove a listener
	 * @param listener
	 * @return
	 */
	public boolean removeEBusEventListener(EBusConnectorEventListener listener) {
		return listeners.remove(listener);
	}

	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {

		while (!isInterrupted()) {
			try {
				int read = inputStream.read();
				if(read != -1) {
					byte receivedByte = (byte)(read & 0xFF);

					inputBuffer.put(receivedByte);
					if(receivedByte == EbusTelegram.SYN) {
						onEBusSyncReceived();
					}
				}

			} catch (IOException e) {
				logger.error(e.toString(), e);
			}
		}

		try {
			disconnect();
		} catch (IOException e) {
			logger.error(e.toString(), e);
		}
	}

	/**
	 * Called if a SYN packet was received
	 * @throws IOException
	 */
	protected void onEBusSyncReceived() throws IOException {

		if(inputBuffer.position() == 1 && inputBuffer.get(0) == EbusTelegram.SYN) {
			if(lockCounter > 0) lockCounter--;
			logger.trace("Auto-SYN byte received");

			// jetzt senden, wenn was da ist
			send(false);
			
		} else if(inputBuffer.position() == 2 && inputBuffer.get(0) == EbusTelegram.SYN) {
			logger.warn("Collision on eBus detected (SYN DATA SYNC Sequence) ...");
			blockNextSend = true;
			
			// jetzt senden, wenn was da ist
			send(false);

		}else if(inputBuffer.position() < 5) {
			if(lockCounter > 0) lockCounter--;
			logger.trace("Telegram to small, skip!");
			
			// jetzt senden, wenn was da ist
			send(false);

		} else {
			if(lockCounter > 0) lockCounter--;
			byte[] copyOf = Arrays.copyOf(inputBuffer.array(), inputBuffer.position());
			
			// erst senden, dass ist zeitkritisch
			send(false);
			
			// Nach dem senden können wir uns um die empfangenen daten kümmern
			final EbusTelegram telegram = EBusUtils.processEBusData(copyOf);
			if(telegram != null) {
				onEBusTelegramReceived(telegram);

			} else {
				logger.debug("Received telegram was invalid, skip!");
			}

		}

		// datenbuffer zurück setzen
		inputBuffer.clear();
	}

	/**
	 * Called if a valid eBus telegram was received. Send to event
	 * listeners in a seperate thread.
	 * @param telegram
	 */
	protected void onEBusTelegramReceived(final EbusTelegram telegram) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				for (EBusConnectorEventListener listener : listeners) {
					listener.onTelegramReceived(telegram);
				}
			}
		}).start();;
	}

	/**
	 * Add a byte array to send queue.
	 * @param data
	 * @return
	 */
	public boolean send(byte[] data) {
		byte crc = 0;
		for (int i = 0; i < data.length-1; i++) {
			byte b = data[i];
			crc = EBusUtils.crc8_tab(b, crc);
		}
		data[data.length-1] = crc;
		
		//		logger.debug("Add to send queue: {}", EBusUtils.toHexDumpString(data));
		return outputQueue.add(data);
	}

	/**
	 * Internal send function. Send and read to detect byte collisions.
	 * @param secondTry
	 * @throws IOException
	 */
	protected void send(boolean secondTry) throws IOException {

		// Es gab eine Kollision, daher darf gerade nicht gesendet werden
		if(blockNextSend) {
			logger.debug("Sender was blocked for this SYN ...");
			blockNextSend = false;
			return;
		}

		// Es gibt keine Daten zum senden
		if(outputQueue.isEmpty()) {
			logger.trace("Send buffer is empty, nothing to send...");
			return;
		}

		// Zähler noch nicht auf 0, daher darf aktuell nicht gesendet werden.
		if(lockCounter > 0) {
			logger.debug("No access to ebus because the lock counter ...");
			return;
		}

		byte[] dataOutputBuffer = outputQueue.peek();
		logger.debug("EBusSerialPortEvent.send()" + EBusUtils.toHexDumpString(dataOutputBuffer));

		// erst mal bereinigen
		inputBuffer.clear();
		
		// befehl senden
		for (int i = 0; i < dataOutputBuffer.length; i++) {
			byte b = dataOutputBuffer[i];
			outputStream.write(b);
			
			// gerade geschriebenes wieder vom bus einlesen
			int read = inputStream.read();
			if(read != -1) {

				inputBuffer.put((byte) (read & 0xFF));
				
				// arbitrierung nur beim ersten byte durchführen
				if(i == 0) {
					byte r = (byte) (read & 0xFF);
					if(b != r) {

						// geschriebenes byte und gelesenes byte nicht identisch,
						// das ist dann eine kollision
						logger.warn("eBus collision detected!");

						// 
						if(lastSendCollisionDetected) {
							logger.warn("das wars, ende");
							outputQueue.poll();

							lastSendCollisionDetected = false;
							blockNextSend = false;
//							return;

						// priority class identy
						} else if((byte) (r & 0x0F) == (byte) (b & 0x0F)) {
							// nach dem nächsten syn wieder senden
							logger.info("Priority class match, restart after next SYN ...");
							lastSendCollisionDetected = true;

						} else {
							// wir sind raus, die anderen senden erst mal
							logger.info("Priority class doesn't match, blocked for next SYN ...");
							blockNextSend = true;
							lastSendCollisionDetected = true;
						}

						// stop after a collision
						return;
					}
				}
			}
		}

		// hier angekommen? dann war das senden i.O.

		// ggfls. wartet auf antwort
		if(dataOutputBuffer[1] != (byte)0xFE) {

			int read = inputStream.read();
			if(read != -1) {
				byte ack = (byte) (read & 0xFF);
				inputBuffer.put(ack);

				if(ack == EbusTelegram.ACK_OK) {

					// länge der antwort
					byte nn2 = (byte) (inputStream.read() & 0xFF);
					inputBuffer.put(nn2);

					if(nn2 > 16) {
						logger.warn("slave data to lang, invalid!");
						return;
					}

					while(nn2 > 0) {
						byte d = (byte) (inputStream.read() & 0xFF);
						inputBuffer.put(d);

						if(d != (byte)0xA) {
							nn2--;
						}
					}

					byte crc2 = (byte) (inputStream.read() & 0xFF);
					inputBuffer.put(crc2);

					// sende master sync
					outputStream.write(EbusTelegram.ACK_OK);
					outputStream.write(EbusTelegram.SYN);
					inputBuffer.put(EbusTelegram.ACK_OK);
					inputBuffer.put(EbusTelegram.SYN);

				} else if(ack == EbusTelegram.ACK_FAIL) {

					// trotzdem syn senden, um fehlerhafte übertragung sauber abzuschließen
					outputStream.write(EbusTelegram.SYN);

					// Nochmal senden (max. 1x)
					if(!secondTry)
						send(true);
					else
						logger.warn("Das war nichts, senden einstellen");
					
				} else if(ack == EbusTelegram.SYN) {
					logger.warn("Keine Antwort ...");
					
				} else {
					// Falsche Daten, keine Antwort vom Slave ?
					logger.debug("Falsche Antwort vom SLAVE -> " + EBusUtils.toHexDumpString(ack));
					logger.debug("Received telegram {}", EBusUtils.toHexDumpString(inputBuffer));
					
					// trotzdem syn senden, um fehlerhafte übertragung sauber abzuschließen
					outputStream.write(EbusTelegram.SYN);

					for (int i = 0; i < 16; i++) {
						byte d = (byte) (inputStream.read() & 0xFF);
						logger.debug("-->" + EBusUtils.toHexDumpString(d));
					}
					
					// Nochmal senden (max. 1x)
					if(!secondTry)
						send(true);
					else
						logger.warn("Das war nichts, senden einstellen");

				}
			}
		}

		// eBus lock counter zurücksetzen 
		lockCounter = LOCKOUT_COUNTER_MAX;

		// globale variablen zurücksetzen
		lastSendCollisionDetected = false;
		blockNextSend = false;

		logger.debug("Received telegram {}", EBusUtils.toHexDumpString(inputBuffer));

		// eintrag aus der queue entfernen
		outputQueue.poll();
	}
}
