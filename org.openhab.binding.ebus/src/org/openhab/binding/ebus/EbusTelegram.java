package org.openhab.binding.ebus;

import java.nio.ByteBuffer;

public class EbusTelegram {

	public final static byte SYN = (byte)0xAA;
	public final static byte ACK_OK = (byte)0x00;
	public final static byte ACK_FAIL = (byte)0xFF;
	
	public static final byte BROADCAST = 1;
	public static final byte MASTER_SLAVE = 2;
	public static final byte MASTER_MASTER = 3;
	
	private ByteBuffer data;
	
	public EbusTelegram(ByteBuffer data) {
		this.data = data;
	}
	
	public EbusTelegram(byte[] data) {
		this.data = ByteBuffer.wrap(data);
	}
	
	public byte getSource() {
		return data.get(0);
	}
	
	public byte getDestination() {
		return data.get(1);
	}
	
	public short getCommand() {
		return data.getShort(2);
	}
	
	public int getDataLen() {
		return data.get(4);
	}
	
	public byte getCRC() {
		return data.get(getDataLen()+5);
	}
	
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
	
	public ByteBuffer getBuffer() {
		return data.asReadOnlyBuffer();
	}
	
	public byte[] getData() {
		int l = getDataLen();
		byte[] buffer = new byte[l];
		System.arraycopy(data.array(), 5, buffer, 0, l);
		return buffer;
	}
	
	public int getSlaveDataLen() {
		if(getType() == MASTER_SLAVE)
			return data.get(getDataLen()+7);
		return -1;
	}
	
	public byte[] getSlaveData() {
		int l = getSlaveDataLen();
		
		if(l == -1)
			return new byte[0];
		
		byte[] buffer = new byte[l];
		System.arraycopy(data.array(), getDataLen()+8, buffer, 0, l);
		return buffer;
	}
}