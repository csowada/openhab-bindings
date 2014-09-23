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

	private static final java.util.logging.Logger logger = LoggerFactory
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

		} else if(inputBuffer.position() == 2 && inputBuffer.get(0) == EbusTelegram.SYN) {
			logger.warning("Collision on eBus detected (SYN DATA SYNC Sequence) ...");
			blockNextSend = true;
			
		}else if(inputBuffer.position() < 5) {
			if(lockCounter > 0) lockCounter--;
			logger.trace("Telegram to small, skip!");

		} else {
			if(lockCounter > 0) lockCounter--;
			byte[] copyOf = Arrays.copyOf(inputBuffer.array(), inputBuffer.position());
			EbusTelegram telegram = EBusUtils.processEBusData(copyOf);

			if(telegram != null) {
				onEBusTelegramReceived(telegram);

			} else {
				logger.debug("Received telegram was invalid, skip!");
			}

		}

		// datenbuffer zur�ck setzen
		inputBuffer.clear();

		// jetzt senden, wenn was da ist
		send(false);

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
		logger.debug("Add to send queue: {}", EBusUtils.toHexDumpString(data));
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
			logger.trace("Sender was blocked for this SYN ...");
			blockNextSend = false;
			return;
		}
		
		// Z�hler noch nicht auf 0, daher darf aktuell nicht gesendet werden.
		if(lockCounter > 0) {
			logger.trace("No access to ebus because the lock counter ...");
			return;
		}
		
		// Es gibt keine Daten zum senden
		if(outputQueue.isEmpty()) {
			logger.trace("Send buffer is empty, nothing to send...");
			return;
		}

		byte[] dataOutputBuffer = outputQueue.peek();
		logger.debug("EBusSerialPortEvent.send()" + EBusUtils.toHexDumpString(dataOutputBuffer));


		// befehl senden
		for (int i = 0; i < dataOutputBuffer.length; i++) {
			byte b = dataOutputBuffer[i];
			outputStream.write(b);

			// gerade geschriebenes wieder vom bus einlesen
			int read = inputStream.read();
			if(read != -1) {
				
				// geschriebenes byte und gelesenes byte nicht identisch,
				// das ist dann eine kollision
				logger.warn("eBus collision detected!");
				
				// arbitrierung nur beim ersten byte durchf�hren
				if(i == 0) {
					byte r = (byte) (read & 0xFF);
					if(b != r) {
						// priority class identy
						if((byte) (r & 0x0F) == (byte) (b & 0x0F)) {
							// nach dem n�chsten syn wieder senden
							logger.info("Priority class match, restart after next SYN ...");
							
						} else {
							// wir sind raus, die anderen senden erst mal
							logger.info("Priority class doesn't match, blocked for next SYN ...");
							blockNextSend = true;
						}
						
					}
				}
				
				// stop after a collision
				return;
			}
		}

		// hier angekommen? dann war das senden i.O.
		inputBuffer.put(dataOutputBuffer);

		// gffls. wartet auf antwort
		if(dataOutputBuffer[dataOutputBuffer.length] != EbusTelegram.SYN) {
			int read = inputStream.read();
			if(read != -1) {
				byte ack = (byte) (read & 0xFF);
				inputBuffer.put(ack);

				if(ack == EbusTelegram.ACK_OK) {
					// Slave sagt ok

					// l�nge der antwort
					byte nn2 = (byte) (inputStream.read() & 0xFF);
					inputBuffer.put(nn2);

					while(nn2 > 0) {
						byte d = (byte) (inputStream.read() & 0xFF);
						inputBuffer.put(d);

						if(d != (byte)0xA) {
							nn2--;
						}
					}

					byte crc2 = (byte) (inputStream.read() & 0xFF);
					inputBuffer.put(crc2);

					outputStream.write(EbusTelegram.SYN);
					inputBuffer.put(EbusTelegram.SYN);

				} else if(ack == EbusTelegram.ACK_FAIL) {

					// trotzdem syn senden, um fehlerhafte �bertragung sauber abzuschlie�en
					outputStream.write(EbusTelegram.SYN);

					// Nochmal senden (max. 1x)
					if(!secondTry)
						send(true);
					else
						logger.warn("Das war nichts, senden einstellen");

				} else {
					// Falsche Daten, keine Antwort vom Slave ?

				}
			}
		}

		// eBus lock counter zur�cksetzen 
		lockCounter = LOCKOUT_COUNTER_MAX;
	}
}
