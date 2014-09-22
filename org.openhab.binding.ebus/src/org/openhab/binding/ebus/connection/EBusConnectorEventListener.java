package org.openhab.binding.ebus.connection;

import org.openhab.binding.ebus.EbusTelegram;

/**
 * @author CSo
 *
 */
public interface EBusConnectorEventListener {

	/**
	 * @param telegram
	 */
	public void onTelegramReceived(EbusTelegram telegram);
	
}
