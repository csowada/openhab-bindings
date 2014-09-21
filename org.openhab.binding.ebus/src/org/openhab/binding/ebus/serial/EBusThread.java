package org.openhab.binding.ebus.serial;

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

public class EBusThread extends Thread {

	private static final Logger logger = LoggerFactory
			.getLogger(EBusThread.class);
	
	private final Queue<byte[]> outputQueue = new LinkedBlockingQueue<byte[]>(20);
	private final List<IEBusEventListener> listeners = new ArrayList<IEBusEventListener>();
	
	/** serial receive buffer */
	private final ByteBuffer inputBuffer = ByteBuffer.allocate(50);
	
	protected InputStream inputStream;
	protected OutputStream outputStream;

	private int collisionCounter;

	public EBusThread() {
		super("eBus Connection Thread");
//		this.inputStream = inputStream;
//		this.outputStream = outputStream;
		this.setDaemon(true);
	}
	
	protected void connect() {
		
	}
	
	protected void disconnect() {
		
	}
	
	/**
	 * @param listener
	 */
	public void addEBusEventListener(IEBusEventListener listener) {
		listeners.add(listener);
	}
	
	/**
	 * @param listener
	 * @return
	 */
	public boolean removeEBusEventListener(IEBusEventListener listener) {
		return listeners.remove(listener);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		
		connect();
		
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
		
		disconnect();
	}
	
	/**
	 * @throws IOException
	 */
	protected void onEBusSyncReceived() throws IOException {

		if(inputBuffer.position() == 1 && inputBuffer.get(0) == EbusTelegram.SYN) {
			// auto sync byte
		}else if(inputBuffer.position() < 5) {
			System.out.println("EBusThread.onEBusSyncReceived() - Telegram to small");
		} else {
			byte[] copyOf = Arrays.copyOf(inputBuffer.array(), inputBuffer.position());
			EbusTelegram telegram = EBusUtils.processEBusData(copyOf);
			
			if(telegram != null) {
				onEBusTelegramReceived(telegram);
//				System.out.println(EBusUtils.toHexDumpString(telegram.getBuffer()));
				
			} else {
				System.out.println("EBusThread.onEBusTelegramReceived() - UPS");
			}
			
		}
		
		// datenbuffer zurück setzen
		inputBuffer.clear();
		
		// jetzt senden, wenn was da ist
		send(false);
	}

	/**
	 * @param telegram
	 */
	protected void onEBusTelegramReceived(final EbusTelegram telegram) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				for (IEBusEventListener listener : listeners) {
					listener.onTelegramReceived(telegram);
				}
			}
		}).start();;
	}
	
	/**
	 * @param data
	 * @return
	 */
	public boolean send(byte[] data) {
		logger.debug("Add to send queue: " + EBusUtils.toHexDumpString(data));
		return outputQueue.add(data);
	}
	
	/**
	 * @param secondTry
	 * @throws IOException
	 */
	protected void send(boolean secondTry) throws IOException {

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
				if(b != (byte) (read & 0xFF)) {
					// kollision
					if(collisionCounter++ > 10) {
						logger.error("More than 10 bus conflicts has ocoured, there is something wrong!");
						outputQueue.clear();
						
					} else {
						logger.warn("eBus Send Collision!");
					}

					return;
				}
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
					
					// länge der antwort
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
					
					// trotzdem syn senden, um fehlerhafte übertragung sauber abzuschließen
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

		// sauber gesendet, zurücksetzen
		collisionCounter = 0;
	}
}
