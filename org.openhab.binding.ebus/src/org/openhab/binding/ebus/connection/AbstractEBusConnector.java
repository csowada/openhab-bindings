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

	protected byte senderId = (byte)0xFF;
	
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

	public void setSenderId(byte senderId) {
		senderId = this.senderId;
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

		// blocked for this send slot because a collision
		if(blockNextSend) {
			logger.trace("Sender was blocked for this SYN ...");
			blockNextSend = false;
			return;
		}

		// currently no data to send
		if(outputQueue.isEmpty()) {
			logger.trace("Send buffer is empty, nothing to send...");
			return;
		}

		// counter not zero, it's not allowed to send yet
		if(lockCounter > 0) {
			logger.trace("No access to ebus because the lock counter ...");
			return;
		}

		byte[] dataOutputBuffer = outputQueue.peek();
		logger.debug("EBusSerialPortEvent.send() data: {}", EBusUtils.toHexDumpString(dataOutputBuffer));

		// clear first
		inputBuffer.clear();

		boolean isMasterAddr = EBusUtils.isMasterAddress(dataOutputBuffer[1]);

		// send command
		for (int i = 0; i < dataOutputBuffer.length; i++) {
			byte b = dataOutputBuffer[i];
			outputStream.write(b);

			// directly read the current wrote byte from bus
			int read = inputStream.read();
			if(read != -1) {

				byte r = (byte) (read & 0xFF);
				inputBuffer.put(r);

				// do arbitation on on first byte only
				if(i == 0 && b != r) {

					// written and read byte not identical, that's
					// a collision
					logger.warn("eBus collision detected!");

					// last send try was a collision
					if(lastSendCollisionDetected) {
						logger.warn("A second collision occured!");
						resetSend();
						return;
					}
					// priority class identical
					else if((byte) (r & 0x0F) == (byte) (b & 0x0F)) {
						logger.trace("Priority class match, restart after next SYN ...");
						lastSendCollisionDetected = true;

					} else {
						logger.trace("Priority class doesn't match, blocked for next SYN ...");
						blockNextSend = true;
					}

					// stop after a collision
					return;
				}
			}
		}

		// sending master data finish

		// reset global variables
		lastSendCollisionDetected = false;
		blockNextSend = false;


		// if this telegram a broadcast?
		if(dataOutputBuffer[1] == (byte)0xFE) {
			logger.warn("Broadcast send ..............");

			// sende master sync
			outputStream.write(EbusTelegram.SYN);
			inputBuffer.put(EbusTelegram.SYN);

		} else {

			int read = inputStream.read();
			if(read != -1) {
				byte ack = (byte) (read & 0xFF);
				inputBuffer.put(ack);

				if(ack == EbusTelegram.ACK_OK) {

					// if the telegram is a slave telegram we will
					// get data from slave
					if(!isMasterAddr) {

						// len of answer
						byte nn2 = (byte) (inputStream.read() & 0xFF);
						inputBuffer.put(nn2);

						byte crc = EBusUtils.crc8_tab(nn2, (byte) 0);

						if(nn2 > 16) {
							logger.warn("slave data to lang, invalid!");

							// resend telegram (max. once)
							if(!resend(secondTry))
								return;
						}

						// read slave data, be aware of 0x0A bytes
						while(nn2 > 0) {
							byte d = (byte) (inputStream.read() & 0xFF);
							inputBuffer.put(d);
							crc = EBusUtils.crc8_tab(d, crc);

							if(d != (byte)0xA) {
								nn2--;
							}
						}

						// read slave crc
						byte crc2 = (byte) (inputStream.read() & 0xFF);
						inputBuffer.put(crc2);

						// check slave crc
						if(crc2 != crc) {
							logger.warn("Slave CRC wrong, resend!");

							// Resend telegram (max. once)
							if(!resend(secondTry))
								return;
						}

						// sende master sync
						outputStream.write(EbusTelegram.ACK_OK);
						inputBuffer.put(EbusTelegram.ACK_OK);
					} // isMasterAddr check

					// send SYN byte
					outputStream.write(EbusTelegram.SYN);
					inputBuffer.put(EbusTelegram.SYN);

				} else if(ack == EbusTelegram.ACK_FAIL) {
					// resend telegram (max. once)
					if(!resend(secondTry))
						return;

				} else if(ack == EbusTelegram.SYN) {
					logger.warn("No answer from slave, skip ...");
					resetSend();
					return;

				} else {
					// Wow, wrong answer, and now?
					logger.warn("Received wrong telegram: {}", EBusUtils.toHexDumpString(inputBuffer));

					// resend telegram (max. once)
					if(!resend(secondTry))
						return;
				}
			}
		}

		// Nach dem senden können wir uns um die empfangenen daten kümmern
		byte[] buffer = Arrays.copyOf(inputBuffer.array(), inputBuffer.position());
		final EbusTelegram telegram = EBusUtils.processEBusData(buffer);
		if(telegram != null) {
			onEBusTelegramReceived(telegram);

		} else {
			logger.debug("Received telegram was invalid, skip!");
		}

		// reset send module
		resetSend();
	}

	/**
	 * Resend data if it's the first try or call resetSend()
	 * @param secondTry
	 * @return
	 * @throws IOException
	 */
	private boolean resend(boolean secondTry) throws IOException {
		if(!secondTry) {
			send(true);
			return true;

		} else {
			logger.warn("Resend failed, remove data from sending queue ...");
			resetSend();
			return false;
		}
	}

	/**
	 * Reset the send routine
	 */
	private void resetSend() {
		
		// reset ebus counter
		lockCounter = LOCKOUT_COUNTER_MAX;

		// reset global variables
		lastSendCollisionDetected = false;
		blockNextSend = false;

		// remove entry from sending queue
		outputQueue.poll();
	}
}
