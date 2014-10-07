package org.openhab.binding.ebus.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EBusConfigurationProvider {

	private static final Logger logger = LoggerFactory
			.getLogger(EBusConfigurationProvider.class);

	private ArrayList<Map<String, Object>> telegramRegistry = new ArrayList<>();

	private Compilable compEngine; 

	public EBusConfigurationProvider() {
		ScriptEngineManager mgr = new ScriptEngineManager();
		ScriptEngine engine = mgr.getEngineByName("JavaScript");

		if (engine instanceof Compilable) {
			compEngine = (Compilable) engine;
		}
	}

	@SuppressWarnings("unchecked")
	public void loadConfigurationFile(URL url) throws IOException, ParseException {
		final JSONParser parser = new JSONParser();

		InputStream inputStream = url.openConnection().getInputStream();
		BufferedReader in = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
		ArrayList<Map<String, Object>> loadedTelegramRegistry = (JSONArray)parser.parse(in);

		for (Iterator<Map<String, Object>> iterator = loadedTelegramRegistry.iterator(); iterator.hasNext();) {
			JSONObject object = (JSONObject) iterator.next();
			transformDataTypes(object);
		}

		if(loadedTelegramRegistry != null && !loadedTelegramRegistry.isEmpty()) {
			telegramRegistry.addAll(loadedTelegramRegistry);
		}
	}

	@SuppressWarnings("unchecked")
	protected void transformDataTypes(JSONObject configurationEntry) {
		
		if(configurationEntry.get("filter") instanceof String) {
			String filter = (String)configurationEntry.get("filter");
			filter = filter.replaceAll("\\?\\?", "[0-9A-Z]{2}");
			logger.trace("Compile RegEx filter: {}", filter);
			configurationEntry.put("cfilter", Pattern.compile(filter));
		} else {
			String filter = "[0-9A-Z]{2} [0-9A-Z]{2}";
			if(configurationEntry.containsKey("command")) {
				filter += " " + configurationEntry.get("command");
				filter += " [0-9A-Z]{2}";
			}
			
			if(configurationEntry.containsKey("data")) {
				filter += " " + configurationEntry.get("data");
			}
			
			filter += " .*";
			
			logger.trace("Compile RegEx filter: {}", filter);
			configurationEntry.put("cfilter", Pattern.compile(filter));
		}
		
		// compile scipt's if available
		if(configurationEntry.containsKey("values")) {
			Map<String, Map<String, Object>> values = (Map<String, Map<String, Object>>) configurationEntry.get("values");
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
		}
		
		// compile scipt's if available
		if(configurationEntry.containsKey("computed_values")) {
			Map<String, Map<String, Object>> cvalues = (Map<String, Map<String, Object>>) configurationEntry.get("computed_values");
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
	
	public List<Map<String, Object>> getCommandsByFilter(String bufferString) {

		final List<Map<String, Object>> matchedTelegramRegistry = new ArrayList<Map<String, Object>>();

		/** select matching telegram registry entries */
		for (Map<String, Object> registryEntry : telegramRegistry) {
			Pattern pattern = (Pattern) registryEntry.get("cfilter");
			Matcher matcher = pattern.matcher(bufferString);
			if(matcher.matches()) {
				matchedTelegramRegistry.add(registryEntry);
			}
		}

		return matchedTelegramRegistry;
	}

	public Map<String, Object> getCommandById(String commandId, String commandClass) {
		for (Map<String, Object> entry : telegramRegistry) {
			if(entry.containsKey("id") && entry.get("id").equals(commandId) && 
					entry.containsKey("class") && entry.get("class").equals(commandClass)) {
				return entry;
			}
		}

		return null;
	}

}