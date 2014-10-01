/**
* Copyright (c) 2010-2014, openHAB.org and others.
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*/
package org.openhab.binding.ebus.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author Christian Sowada
* @since 1.6.0
*/
public class EBusTelegramParser {

	private static final Logger logger = LoggerFactory
			.getLogger(EBusTelegramParser.class);
	
	private static final Logger logger2 = LoggerFactory
			.getLogger(EBusTelegramParser.class.getPackage().getName() + ".Analyses");
	
	private static final Logger logger3 = LoggerFactory
			.getLogger(EBusTelegramParser.class.getPackage().getName() + ".BruteForce");

	private ArrayList<Map<String, Object>> telegramRegistry = new ArrayList<>();
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
	public void loadConfigurationFile(URL url) throws IOException, ParseException {
		JSONParser parser=new JSONParser();

		InputStream inputStream = url.openConnection().getInputStream();
		BufferedReader in = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
		ArrayList<Map<String, Object>> loadedTelegramRegistry = (JSONArray)parser.parse(in);

		for (Iterator<Map<String, Object>> iterator = loadedTelegramRegistry.iterator(); iterator.hasNext();) {
			JSONObject object = (JSONObject) iterator.next();

			if(object.get("filter") instanceof String) {
				String filter = (String)object.get("filter");
				filter = filter.replaceAll("\\?\\?", "[0-9A-Z]{2}");
				logger.trace("Compile RegEx filter: {}", filter);
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
		
		if(loadedTelegramRegistry != null && !loadedTelegramRegistry.isEmpty()) {
			telegramRegistry.addAll(loadedTelegramRegistry);
		}
	}
	
	private Object getValue(ByteBuffer byteBuffer, String type, int pos) {
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
			hByte = byteBuffer.get(pos);
			lByte = byteBuffer.get(pos-1);
			value = EBusUtils.decodeWORD(hByte, lByte);
			break;

		case "uchar":
		case "byte":
			value = byteBuffer.get(pos-1) & 0xFF;
			break;

		case "char":
			value = byteBuffer.get(pos-1);
			if((byte)value == (byte)0xFF)
				value = null;
			break;

		case "data1b":
			value = byteBuffer.get(pos-1);
			if((byte)value == (byte)0x80)
				value = null;
			break;

		case "bit":
			int bit = ((Long) settings.get("bit")).intValue();
			value = byteBuffer.get(pos-1);

			boolean isSet = ((byte)value >> bit& 0x1) == 1;
			value = isSet;
			break;

		default:
			logger.warn("Configuration Error: Unknown command type! {}", type);
			break;
		}
		
		return value;
	}
	
	private Object evaluateScript(Entry<String, Map<String, Object>> entry, Map<String, Object> bindings2) throws ScriptException {
		Object value = null;
		if(entry.getValue().containsKey("cscript")) {
			CompiledScript cscript = (CompiledScript) entry.getValue().get("cscript");

			// Add global varaibles thisValue and keyName to JavaScript context
			Bindings bindings = cscript.getEngine().createBindings();
			bindings.putAll(bindings2);
			value = cscript.eval(bindings);
		}
		
		return value;
	}
	
	private void bruteforceEBusTelegram(EbusTelegram telegram) {

//		logger3.trace(telegram.getBuffer());
		
		byte[] data = telegram.getData();
		
		String format = String.format("%-4s%-13s%-13s%-13s%-13s%-13s%-13s", "Pos", "WORD", "UInt", "DATA2B", "DATA2C", "DATA1c", "BCD");
		logger3.trace("    " + format);
		logger3.trace("    -----------------------------------------------------------------------------");
		
		for (int i = 0; i < data.length; i++) {

			Object word = i == data.length-1 ? "---" : EBusUtils.decodeWORD(data[i+1], data[i]);
			Object data2b = i == data.length-1 ? "---" : EBusUtils.decodeDATA2b(data[i+1], data[i]);
			Object data2c = i == data.length-1 ? "---" : EBusUtils.decodeDATA2c(data[i+1], data[i]);
			Object data1c = i == data.length-1 ? "---" : EBusUtils.decodeDATA1c(data[i+1]);
			int bcd = EBusUtils.decodeBCD(data[i]);
			int uint = EBusUtils.unsignedInt(data[i]);
			
			format = String.format("%-4s%-13s%-13s%-13s%-13s%-13s%-13s", i+6, word, uint, data2b, data2c, data1c, bcd);
			logger3.trace("    " + format);
		}
		
		if(telegram.getType() == EbusTelegram.MASTER_SLAVE) {
			data = telegram.getSlaveData();
			
			logger3.trace("    ---------------------------------- Answer ----------------------------------");
			
			for (int i = 0; i < data.length; i++) {

				Object word = i == data.length-1 ? "---" : EBusUtils.decodeWORD(data[i+1], data[i]);
				Object data2b = i == data.length-1 ? "---" : EBusUtils.decodeDATA2b(data[i+1], data[i]);
				Object data2c = i == data.length-1 ? "---" : EBusUtils.decodeDATA2c(data[i+1], data[i]);
				Object data1c = i == data.length-1 ? "---" : EBusUtils.decodeDATA1c(data[i+1]);
				int bcd = EBusUtils.decodeBCD(data[i]);
				int uint = EBusUtils.unsignedInt(data[i]);
				
				format = String.format("%-4s%-13s%-13s%-13s%-13s%-13s%-13s", i+6, word, uint, data2b, data2c, data1c, bcd);
				logger3.trace("    " + format);
			}
			
		}
	}
	
	public Map<String, Object> parse(EbusTelegram telegram) {

		if(telegramRegistry == null) {
			logger.error("Configuration not loaded, can't parse telegram!");
			return null;
		}

		final Map<String, Object> valueRegistry = new HashMap<String, Object>();

		if(telegram == null) {
			return null;
		}

		final ByteBuffer byteBuffer = telegram.getBuffer();
		final String bufferString = EBusUtils.toHexDumpString(byteBuffer).toString();

		final List<Map<String, Object>> matchedTelegramRegistry = new ArrayList<Map<String, Object>>();
		
		/** select matching telegram registry entries */
		for (Map<String, Object> registryEntry : telegramRegistry) {
			Pattern pattern = (Pattern) registryEntry.get("cfilter");
			Matcher matcher = pattern.matcher(bufferString);
			if(matcher.matches()) {
				matchedTelegramRegistry.add(registryEntry);
			}
		}

		logger2.info(bufferString);
		
		if(matchedTelegramRegistry.isEmpty()) {
			logger2.debug("  >>> Unknown ----------------------------------------");
			if(logger3.isTraceEnabled()) {
				logger2.trace(bufferString);
				bruteforceEBusTelegram(telegram);
			}
			
			return null;
		}

		for (Map<String, Object> registryEntry : matchedTelegramRegistry) {

			int debugLevel = 0;
			if(registryEntry.containsKey("debug")) {
				debugLevel = ((Long)registryEntry.get("debug")).intValue();
			}
//			debugLevel = 0;
			
			@SuppressWarnings("unchecked")
			Map<String, Map<String, Object>> values = (Map<String, Map<String, Object>>) registryEntry.get("values");
			
//			if(debugLevel >= 1) {
				logger2.debug("  >>> {}", registryEntry.containsKey("comment") ? 
						registryEntry.get("comment") : "<No comment available>");
//			}

			for (Entry<String, Map<String, Object>> entry : values.entrySet()) {

				settings = entry.getValue();

				String type = ((String) settings.get("type")).toLowerCase();
				int pos = settings.containsKey("pos") ? ((Long) settings.get("pos")).intValue() : -1;

				Object value = getValue(byteBuffer, type, pos);

				// Add global variables thisValue and keyName to JavaScript context
				HashMap<String, Object> bindings = new HashMap<String, Object>();
				bindings.put(entry.getKey(), value);
				bindings.put("thisValue", value);
				
				if(settings.containsKey("cscript")) {
					try {
						value = evaluateScript(entry, bindings);
					} catch (ScriptException e) {
						logger.error("Error on evaluating JavaScript!", e);
						break;
					}
				}
				
//				if(debugLevel >= 2) {
//				if(logger2.isTraceEnabled()) {
					String label = (String) (settings.containsKey("label") ? settings.get("label") : "");
					String format = String.format("%-22s%-10s%s", entry.getKey(), value, label);
					if(debugLevel >= 2) {
						logger2.debug("    >>> " + format);
					} else {
						logger2.trace("    >>> " + format);
					}
					
//				}

				valueRegistry.put(entry.getKey(), value);
			}

			// computes values available? if not exit here
			if(!registryEntry.containsKey("computed_values"))
				continue;

			@SuppressWarnings("unchecked")
			Map<String, Map<String, Object>> cvalues = (Map<String, Map<String, Object>>) registryEntry.get("computed_values");
			for (Entry<String, Map<String, Object>> entry : cvalues.entrySet()) {

				HashMap<String, Object> bindings = new HashMap<String, Object>();
				bindings.putAll(valueRegistry);
				Object value;
				try {
					value = evaluateScript(entry, bindings);
					valueRegistry.put(entry.getKey(), value);
					
					if(debugLevel >= 2) {
						String label = (String) (settings.containsKey("label") ? settings.get("label") : "");
						String format = String.format("%-22s%-10s%s", entry.getKey(), value, label);
						logger2.debug("    >>> " + format);
					}
					
				} catch (ScriptException e) {
					logger.error("Error on evaluating JavaScript!", e);
				}
			}
		}

		return valueRegistry;
	}

}