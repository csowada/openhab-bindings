package org.openhab.binding.ebus.serial;

import org.openhab.binding.ebus.EbusTelegram;

public interface IEBusEventListener {

	public void onTelegramReceived(EbusTelegram telegram);
	
}
