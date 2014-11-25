/**
* Copyright (c) 2010-2014, openHAB.org and others.
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*/
package org.openhab.binding.ebus;

import java.nio.ByteBuffer;

/**
* @author Christian Sowada
* @since 1.7.0
*/
public class EBusTelegram {

	/** The SYN byte */
	public final static byte SYN = (byte)0xAA;
	
	/** The ACK OK answer byte */
	public final static byte ACK_OK = (byte)0x00;
	
	/** The ACK FAIL answer byte */
	public final static byte ACK_FAIL = (byte)0xFF;
	
	/** Telegram type broadcast */
	public static final byte BROADCAST = 1;
	
	/** Telegram type Master-Slave */
	public static final byte MASTER_SLAVE = 2;
	
	/** Telegram type Master-Master */
	public static final byte MASTER_MASTER = 3;
	
	private ByteBuffer data;
	
	/**
	 * @param data
	 */
	public EBusTelegram(ByteBuffer data) {
		this.data = data;
	}
	
	/**
	 * @param data
	 */
	public EBusTelegram(byte[] data) {
		this.data = ByteBuffer.wrap(data);
	}
	
	/**
	 * Get source id
	 * @return
	 */
	public byte getSource() {
		return data.get(0);
	}
	
	/**
	 * Get destionation id
	 * @return
	 */
	public byte getDestination() {
		return data.get(1);
	}
	
	/**
	 * Get command as short
	 * @return
	 */
	public short getCommand() {
		return data.getShort(2);
	}
	
	/**
	 * Get the master data len
	 * @return
	 */
	public int getDataLen() {
		return data.get(4);
	}
	
	
	/**
	 * Get master crc
	 * @return
	 */
	public byte getCRC() {
		return data.get(getDataLen()+5);
	}
	
	/**
	 * Get the telegram type
	 * @return
	 * @see EBusTelegram.BROADCAST
	 * @see EBusTelegram.MASTER_SLAVE
	 * @see EBusTelegram.MASTER_MASTER
	 */
	public byte getType() {
		int pos = getDataLen()+6;
		byte b = data.get(pos);
		if(b == SYN) {
			return BROADCAST;
		} else if(b == ACK_OK && data.get(pos+1) == SYN) {
			return MASTER_MASTER;
		}
		
		return MASTER_SLAVE;
	}
	
	
	/**
	 * Get master data as read only ByteBuffer
	 * @return
	 */
	public ByteBuffer getBuffer() {
		return data.asReadOnlyBuffer();
	}
	
	/**
	 * Get master data as byte array
	 * @return
	 */
	public byte[] getData() {
		int l = getDataLen();
		byte[] buffer = new byte[l];
		System.arraycopy(data.array(), 5, buffer, 0, l);
		return buffer;
	}
	
	/**
	 * Get slave data len
	 * @return
	 */
	public int getSlaveDataLen() {
		if(getType() == MASTER_SLAVE)
			return data.get(getDataLen()+7);
		return -1;
	}
	
	/**
	 * Get slave data byte array
	 * @return
	 */
	public byte[] getSlaveData() {
		int l = getSlaveDataLen();
		
		if(l == -1)
			return new byte[0];
		
		byte[] buffer = new byte[l];
		System.arraycopy(data.array(), getDataLen()+8, buffer, 0, l);
		return buffer;
	}
}