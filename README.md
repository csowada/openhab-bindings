openhab-bindings
================

## EBus Binding
Das EBus Binding ist in der Lage die Kommunikation über den EBus einer Heizungsanlage direkt auszulesen.

### Installation

- Die Datei _org.openhab.binding.ebus_1.x.x.xxxxxxxxx.jar_ in das Openhab _addons_ Verzeichnis kopieren.
- Die Openhab Konfigurationsdatei _openhab.cfg_ muss angepasst werden. Fügen Sie dies der Datei hinzu und passen es entsprechen an.
```
ebus:serialPort=/dev/ttyUSB0
#ebus:parserUrl=platform:/base/../configurations/ebus-config.json
```
- Die eigene _.items_ Datei um die gewüschten Einträge erweitern. Hier ein Beispiel.
```
Group HeatingUnit	(All)
Group Solar_Chart													(HeatingUnit)

Number Temperature_Warm_Wather		"Warmwasser [%.1f °C]"	<temperature> 		(HeatingUnit) 	{ ebus="id:ww_temp"}
Number Temperature_T_Warm_Wather	"Warmwasser (Soll)[%.1f °C]"	<temperature> 		(HeatingUnit) 	{ ebus="id:soll_ww"}
Number Temperature_Heat_Vessel		"Kesseltemperatur [%.1f °C]"	<temperature> 		(HeatingUnit) 	{ ebus="id:temp_vessel"}
Number Temperature_Heat_Return		"Hz. Rücklauf [%.1f °C]"	<temperature> 	(HeatingUnit) 	{ ebus="id:temp_return"}

Number Temperature_Heat_Exhaust		"Abgastempertatur [%.1f °C]"	<temperature> 		(HeatingUnit) 	{ ebus="id:temp_exhaust"}
Number Pressure_Heating_System		"Anlagendruck [%.2f bar]"	<temperature> 		(HeatingUnit) 	{ ebus="id:system_pressure"}
Number Temperature_Outdoor			"Aussentemp. [%.1f °C]"	<temperature> 		(HeatingUnit) 	{ ebus="id:temp_outside"}

Switch Status_Heating_Alarm			"Alarm"	<siren> 		(HeatingUnit) 	{ ebus="id:state_alarm"}
Switch Status_Heating_Fire			"Hz. Flamme"	<fire> 		(HeatingUnit) 	{ ebus="id:state_flame"}
Number Status_Heating				"Hz. Status [%s]"	<settings>	(HeatingUnit) 	{ ebus="id:status_auto_stroker"}

Switch Solar_Pump					"Solar Pumpe"	<switch> 		(HeatingUnit,Solar_Chart) 	{ ebus="id:solar_pump"}
Number Temperature_Solar_Collector	"Sol. Kollektor Temp. [%.1f °C]"	<temperature> 		(HeatingUnit,Solar_Chart) 	{ ebus="id:temp_collector"}
Number Temperature_Solar_Return		"Sol. Rücklauf Temp. [%.1f °C]"	<temperature> 		(HeatingUnit,Solar_Chart) 	{ ebus="id:e1"}
Number Temperature_Solar_Reservoir	"Sol. Speicher Temp. [%.1f °C]"	<temperature> 		(HeatingUnit,Solar_Chart) 	{ ebus="id:temp_reservoir_1"}

Number Yield_Solar_Yield_Sum	"Sol. Gesamtertrag [%.1f kW/h]"	<chart> 		(HeatingUnit) 	{ ebus="id:yield_sum"}
Number Yield_Solar_Yield_Day	"Sol. Tagesertrag [%.2f kW/h]"	<chart> 		(HeatingUnit) 	{ ebus="id:yield_day"}
Number Yield_Solar_Current		"Aktueller Ertrag[%.2f kW]"	<chart> 		(HeatingUnit,Solar_Chart) 	{ ebus="id:solar_current"}
```
