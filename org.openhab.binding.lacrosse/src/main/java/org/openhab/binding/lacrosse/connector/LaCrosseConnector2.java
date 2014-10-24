package org.openhab.binding.lacrosse.connector;


import org.openhab.binding.lacrosse.LaCrosseBinding;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LaCrosseConnector2 extends LaCrosseConnector {

	private static final Logger logger = LoggerFactory
			.getLogger(LaCrosseConnector2.class);
	
	private LaCrosseBinding binding;

	public LaCrosseConnector2(LaCrosseBinding binding) {
		this.binding = binding;
	}

	@Override
	public void onDataReceived(int address, float temperature, int humidity,
			boolean batteryNew, boolean batteryWeak) {

		logger.info("Data for sensor " + address);

		binding.postUpdate(String.valueOf(address), "temperature", new DecimalType(String.valueOf(temperature)));
		binding.postUpdate(String.valueOf(address), "humidity", new DecimalType(humidity));
		binding.postUpdate(String.valueOf(address), "batteryNew", batteryNew ? OnOffType.ON : OnOffType.OFF);
		binding.postUpdate(String.valueOf(address), "batteryLow", batteryWeak ? OnOffType.ON : OnOffType.OFF);
	}
}
