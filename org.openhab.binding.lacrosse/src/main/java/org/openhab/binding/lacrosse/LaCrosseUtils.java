package org.openhab.binding.lacrosse;

public class LaCrosseUtils {

	static final int COMFORTABLE_MAP[][] =
		{
		    {1,1,1,1,1,1,1,1,1,1,1,1,1,2,2,2,1,1,1,1},    // 14.0 °C bis 14.4 °C
		    {1,1,1,1,1,1,1,1,1,2,2,2,2,2,2,2,2,1,1,1},    // 14.5 °C bis 14.9 °C
		    {1,1,1,1,1,1,1,2,2,2,2,2,2,2,2,2,2,2,1,1},    // 15.0 °C bis 15.4 °C
		    {1,1,1,1,1,1,1,2,2,2,2,2,2,2,3,2,2,2,1,1},    // 15.5 °C bis 15.9 °C
		    {1,1,1,1,1,1,1,2,2,2,2,2,2,3,3,2,2,1,1,1},    // 16.0 °C bis 16.4 °C
		    {1,1,1,1,1,1,2,2,2,2,2,2,3,3,3,2,2,1,1,1},    // 16.5 °C bis 16.9 °C
		    {1,1,1,1,1,1,2,2,2,2,2,3,3,3,3,2,2,1,1,1},    // 17.0 °C bis 17.4 °C
		    {1,1,1,1,1,1,2,2,2,2,3,3,3,3,2,2,2,1,1,1},    // 17.5 °C bis 17.9 °C
		    {1,1,1,1,1,2,2,2,2,3,3,3,3,3,2,2,2,1,1,1},    // 18.0 °C bis 18.4 °C
		    {1,1,1,1,1,2,2,2,3,3,3,3,3,3,2,2,2,1,1,1},    // 18.5 °C bis 18.9 °C
		    {1,1,1,1,2,2,2,3,3,3,3,3,3,3,2,2,2,1,1,1},    // 19.0 °C bis 19.4 °C
		    {1,1,1,1,2,2,2,3,3,3,3,3,3,3,2,2,2,1,1,1},    // 19.5 °C bis 19.9 °C
		    {1,1,1,1,2,2,2,2,3,3,3,3,3,3,2,2,2,1,1,1},    // 20.0 °C bis 20.4 °C
		    {1,1,1,1,2,2,2,3,3,3,3,3,3,2,2,2,1,1,1,1},    // 20.5 °C bis 20.9 °C
		    {1,1,1,1,2,2,2,3,3,3,3,3,3,2,2,2,1,1,1,1},    // 21.0 °C bis 21.4 °C
		    {1,1,1,2,2,2,2,3,3,3,3,3,3,2,2,1,1,1,1,1},    // 21.5 °C bis 21.9 °C
		    {1,1,1,2,2,2,2,3,3,3,3,3,3,2,2,1,1,1,1,1},    // 22.0 °C bis 22.4 °C
		    {1,1,1,2,2,2,2,3,3,3,3,3,2,2,1,1,1,1,1,1},    // 22.5 °C bis 22.9 °C
		    {1,1,1,2,2,2,2,3,3,3,2,2,2,2,1,1,1,1,1,1},    // 23.0 °C bis 23.4 °C
		    {1,1,1,2,2,2,2,3,3,2,2,2,2,1,1,1,1,1,1,1},    // 23.5 °C bis 23.9 °C
		    {1,1,1,2,2,2,2,3,2,2,2,2,2,1,1,1,1,1,1,1},    // 24.0 °C bis 24.4 °C
		    {1,1,1,2,2,2,2,3,2,2,2,2,1,1,1,1,1,1,1,1},    // 24.5 °C bis 24.9 °C
		    {1,1,1,2,2,2,2,2,2,2,2,1,1,1,1,1,1,1,1,1},    // 25.0 °C bis 25.4 °C
		    {1,1,1,1,2,2,2,2,2,2,1,1,1,1,1,1,1,1,1,1},    // 25.5 °C bis 25.9 °C
		    {1,1,1,1,1,2,2,2,2,1,1,1,1,1,1,1,1,1,1,1},    // 26.0 °C bis 26.4 °C
		    {1,1,1,1,1,1,2,2,1,1,1,1,1,1,1,1,1,1,1,1}     // 26.5 °C bis 26.9 °C
		};
	
	public static final int COMFORTABLE_LEVEL_HIGH = 3;
	public static final int COMFORTABLE_LEVEL_MEDIUM = 2;
	public static final int COMFORTABLE_LEVEL_LOW = 1;
	
	public static final String getComfortableLevelStr(double temperature, double humidity) {
		int comfortableLevel = getComfortableLevel(temperature, humidity);
		
		if(comfortableLevel == COMFORTABLE_LEVEL_LOW) {
			return "comflvl_low";
			
		} else if(comfortableLevel == COMFORTABLE_LEVEL_MEDIUM) {
			return "comflvl_medium";
			
		} else {
			return "comflvl_heigh";
		}
	}
	
	public static final int getComfortableLevel(double temperature, double humidity) {
		
		if(temperature < 14 || temperature > 26.9) {
			return COMFORTABLE_LEVEL_LOW;
		}
		
		double x = (temperature / 0.5f) - 28;	
		double y = humidity / 5;
		
		
		
		try {
			return COMFORTABLE_MAP[(int) Math.round(x)][(int) Math.round(y)];
		} catch (Exception e) {
			return 0;
		}
	}
	
	public static final double getPS(double k1, double k2, double k3, double temperature) {
		return k1 * Math.exp((k2*temperature)/(k3+temperature));
	}
	
	public static final double calculateAbsoluteHumidity(double relHum, double temp) {
		
		// für T < 0 über Wasser (Taupunkt)
		final double a = 7.6;
		final double b = 240.7;
		
		final double r = 8314.3;	// J/(kmol*K) (universelle Gaskonstante)
		final double mw = 18.016;	// kg/kmol (Molekulargewicht des Wasserdampfes)
		
		
		// Sättigungsdampfdruck in hPa
		// SDD(T) = 6.1078 * 10^((a*T)/(b+T))
		double sddt = 6.1078 * Math.pow(10, ((a*temp)/(b+temp)));
		
		// Dampfdruck in hPa
		// DD(r,T) = r/100 * SDD(T)
		double ddrt = relHum/100 * sddt;

		// v(r,T) = log10(DD(r,T)/6.1078)
		//double vrt = Math.log10(ddrt/6.1078);
		
		// TD(r,T) = b*v/(a-v)
		//double tdrt = b * vrt/(a-vrt);
		
		// Temperatur in Kelvin (TK = T + 273.15)
		double tk = temp + 273.15;
		
		// AF(r,TK) = 10^5 * mw/R* * DD(r,T)/TK
		double afrtk = Math.pow(10, 5) * mw/r * ddrt/tk;
		
		return afrtk;
	}
	
}
