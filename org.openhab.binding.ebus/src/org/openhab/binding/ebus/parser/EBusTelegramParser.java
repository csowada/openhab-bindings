package org.openhab.binding.ebus.parser;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openhab.binding.ebus.EbusTelegram;

public class EBusTelegramParser {
	
	public static final int DEBUG_OFF = 0;
	public static final int DEBUG_UNKNOWN = 1;
	public static final int DEBUG_ALL = 2;
	
	private ArrayList<Map<String, Object>> telegramRegistry;
	private Map<String, Object> settings;
	private Compilable compEngine; 
	
	private int debugLevel = DEBUG_OFF;
	
	public EBusTelegramParser() {
        ScriptEngineManager mgr = new ScriptEngineManager();
        ScriptEngine engine = mgr.getEngineByName("JavaScript");
        
        if (engine instanceof Compilable) {
            compEngine = (Compilable) engine;
        }
	}
	
	public void setDebugLevel(int level) {
		debugLevel = level;
	}
	
	@SuppressWarnings("unchecked")
	public void loadConfigurationFile(URL url) {
		JSONParser parser=new JSONParser();

		try {
			InputStream inputStream = url.openConnection().getInputStream();
		    BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
			telegramRegistry = (JSONArray)parser.parse(in);
			
			for (Iterator<Map<String, Object>> iterator = telegramRegistry.iterator(); iterator.hasNext();) {
				JSONObject object = (JSONObject) iterator.next();
				
				if(object.get("cmd") instanceof String) {
					object.put("cmd", Short.decode((String) object.get("cmd")));
				}
				
				if(object.get("src") instanceof String) {
					object.put("src", Short.decode((String) object.get("src")));
				}
				
				if(object.get("dst") instanceof String) {
					object.put("dst", Short.decode((String) object.get("dst")));
				}
				
				if(object.get("filter") instanceof String) {
					String filter = (String)object.get("filter");
					filter = filter.replaceAll("\\?\\?", "[0-9A-Z]{2}");
					object.put("cfilter", Pattern.compile(filter));
				}
				
				// compile scipt's if available
				Map<String, Map<String, Object>> values = (Map<String, Map<String, Object>>) object.get("values");
				for (Entry<String, Map<String, Object>> entry : values.entrySet()) {
					if(entry.getValue().containsKey("script")) {
						String script = (String) entry.getValue().get("script");
						try {
							CompiledScript compile = compEngine.compile(script);
							entry.getValue().put("cscript", compile);
						} catch (ScriptException e) {
							e.printStackTrace();
						}
					}
				}
				
				// compile scipt's if available
				if(object.containsKey("computed_values")) {
					Map<String, Map<String, Object>> cvalues = (Map<String, Map<String, Object>>) object.get("computed_values");
					for (Entry<String, Map<String, Object>> entry : cvalues.entrySet()) {
						if(entry.getValue().containsKey("script")) {
							String script = (String) entry.getValue().get("script");
							try {
								CompiledScript compile = compEngine.compile(script);
								entry.getValue().put("cscript", compile);
							} catch (ScriptException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
	
	public Map<String, Object> parse(EbusTelegram telegram) {

		Map<String, Object> valueRegistry = new HashMap<String, Object>();
		
		ByteBuffer byteBuffer = telegram.getBuffer();
		int matchCount = 0;
		
		
		
		String bufferString = null;
		
//		if(debugLevel == DEBUG_ALL) {
//			bufferString = EBusUtils.toHexDumpString(byteBuffer).toString();
//			System.out.println("FULL DATA  : " + bufferString);
//		}
		
		telegramRegistryLoop:
		for (Map<String, Object> registryEntry : telegramRegistry) {
			
			// Is compiled pattern aka filter available, only use this
			if(registryEntry.containsKey("cfilter")) {
				Pattern pattern = (Pattern) registryEntry.get("cfilter");
				
				if(bufferString == null)
					bufferString = EBusUtils.toHexDumpString(byteBuffer).toString();
				
				Matcher matcher = pattern.matcher(bufferString);
				if(!matcher.matches()) {
					continue;
				}
				
			} else {
				
				if(registryEntry.containsKey("cmd")) {
					if(!registryEntry.get("cmd").equals(telegram.getCommand()))
						continue;
				}
				
				if(registryEntry.containsKey("src")) {
					if(!registryEntry.get("src").equals(telegram.getSource()))
						continue;
				}
				
				if(registryEntry.containsKey("dst")) {
					if(!registryEntry.get("dst").equals(telegram.getDestination()))
						continue;
				}

				
				for (Entry<String, Object> entry : registryEntry.entrySet()) {
					if(entry.getKey().startsWith("byte_")) {

						Short pos = Short.decode(entry.getKey().substring(5));
						Short val = Short.decode((String) entry.getValue());
						
						short b = (short) (telegram.getBuffer().get(pos-1) & 0xFF);
						
						if(!val.equals(b))
							continue telegramRegistryLoop;
					}
				}
				
			}
			
			matchCount++;

			@SuppressWarnings("unchecked")
			Map<String, Map<String, Object>> values = (Map<String, Map<String, Object>>) registryEntry.get("values");
			boolean debugShowResults = registryEntry.containsKey("debug");
			
			if(matchCount == 1 && (debugLevel == DEBUG_ALL || debugShowResults)) {
				bufferString = EBusUtils.toHexDumpString(byteBuffer).toString();
				System.out.println("FULL DATA  : " + bufferString);
//				System.out.println("EBusTelegramParser.parse()++++++++++++");
			}
			
			for (Entry<String, Map<String, Object>> entry : values.entrySet()) {
				
				settings = entry.getValue();

				String type = ((String) settings.get("type")).toLowerCase();
				int pos = settings.containsKey("pos") ? ((Long) settings.get("pos")).intValue() : -1;
				
				Object value = null;
				byte hByte = 0;
				byte lByte = 0;
				
				switch (type) {
				case "data2b":
					hByte = byteBuffer.get(pos);
					lByte = byteBuffer.get(pos-1);
					value = EBusUtils.decodeDATA2b(hByte, lByte);
					break;
					
				case "data2c":
					hByte = byteBuffer.get(pos);
					lByte = byteBuffer.get(pos-1);
					value = EBusUtils.decodeDATA2c(hByte, lByte);
					break;
					
					
				case "data1c":
					lByte = byteBuffer.get(pos-1);
					value = EBusUtils.decodeDATA1c(lByte);
					break;

				case "bcd":
					lByte = byteBuffer.get(pos-1);
					value = EBusUtils.decodeBCD(lByte);
					break;
					
				case "word":
					value = ((byteBuffer.get(pos) & 0xFF)<<8) + (byteBuffer.get(pos-1) & 0xFF);
					break;
					
				case "uchar":
				case "byte":
					value = byteBuffer.get(pos-1) & 0xFF;
					break;
					
				case "char":
				case "data1b":
					value = byteBuffer.get(pos-1);
					break;
					
				case "bit":
					int bit = ((Long) settings.get("bit")).intValue();
					byte n = (byte) (1 << bit);
					value = byteBuffer.get(pos-1) & n;
					break;
					
				default:
					System.out.println("Error: Unknown command type!" + type);
					break;
				}
				
				if(entry.getValue().containsKey("cscript")) {
					CompiledScript cscript = (CompiledScript) entry.getValue().get("cscript");
					Bindings bindings = cscript.getEngine().createBindings();
					bindings.put(entry.getKey(), value);
					bindings.put("thisValue", value);
					
					try {
						value = cscript.eval(bindings);
						
					} catch (ScriptException e) {
						e.printStackTrace();
					}
				}
				
				valueRegistry.put(entry.getKey(), value);

				if(debugShowResults) {
					String label = (String) (settings.containsKey("label") ? settings.get("label") : "");
					System.out.printf("   %-20s%-10s%s%n", entry.getKey(), value, label);
				}
			}
			
			
			if(!registryEntry.containsKey("computed_values"))
				continue;
			
			@SuppressWarnings("unchecked")
			Map<String, Map<String, Object>> cvalues = (Map<String, Map<String, Object>>) registryEntry.get("computed_values");
			for (Entry<String, Map<String, Object>> entry : cvalues.entrySet()) {
				
				CompiledScript cscript = (CompiledScript) entry.getValue().get("cscript");
				Bindings bindings = cscript.getEngine().createBindings();
				bindings.putAll(valueRegistry);
				
				try {
					Object value = cscript.eval(bindings);
					valueRegistry.put(entry.getKey(), value);

					if(debugShowResults) {
						String label = (String) (settings.containsKey("label") ? settings.get("label") : "");
						System.out.printf("   $%-20s%-10s%s%n", entry.getKey(), value, label);
					}
					

				} catch (ScriptException e) {
					e.printStackTrace();
				}
			}
		}

		if(matchCount == 0) {
			if(debugLevel == DEBUG_UNKNOWN) {
				if(bufferString == null)
					bufferString = EBusUtils.toHexDumpString(byteBuffer).toString();
				System.err.println("UNKNOWN TELEGRAM  : " + bufferString);
			}
		}
		
		return valueRegistry;
	}
	
}