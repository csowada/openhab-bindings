package org.openhab.binding.ebus.parser;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

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

	ArrayList<Map<String, Object>> telegramRegistry;
	private Map<String, Object> settings;
	private Compilable compEngine; 
	
	public EBusTelegramParser() {
        ScriptEngineManager mgr = new ScriptEngineManager();
        ScriptEngine engine = mgr.getEngineByName("JavaScript");
        
        if (engine instanceof Compilable) {
            compEngine = (Compilable) engine;
        }
	}
	
	@SuppressWarnings("unchecked")
	public void loadConfigurationFile(String filename) {
		JSONParser parser=new JSONParser();

		try {
			telegramRegistry = (JSONArray)parser.parse(new FileReader(filename));
			
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
	
	public Object parse(EbusTelegram telegram) {

		ByteBuffer byteBuffer = telegram.getBuffer();
		Map<String, Object> valueRegistry = new HashMap<String, Object>();
		
		telegramRegistryLoop:
		for (Map<String, Object> registryEntry : telegramRegistry) {
			
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
			
			
			@SuppressWarnings("unchecked")
			Map<String, Map<String, Object>> values = (Map<String, Map<String, Object>>) registryEntry.get("values");
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

				String label = (String) (settings.containsKey("label") ? settings.get("label") : "");
				System.out.printf("   %-20s%-10s%s%n", entry.getKey(), value, label);
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

					String label = (String) (settings.containsKey("label") ? settings.get("label") : "");
					System.out.printf("   $%-20s%-10s%s%n", entry.getKey(), value, label);

				} catch (ScriptException e) {
					e.printStackTrace();
				}
			}
		}

		
		return null;
	}
	
}
