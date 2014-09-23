## Anschlussmöglichekeiten an den eBus

Bei der Heizung Wolf CSZ-2 kann man sich auf der Rückseite der Klappe die das Bedienmoduls BM2 trägt den eBus (X32) abgreifen.
Um die Anschlüsse zu bestimmen, habe ich die Platine aus der Heizung ausgebaut und durchgemessen. So sieht die kleine Platine aus.
![BM-Kontaktplatine](images/IMG_20140903_121551.jpg)

### Pinbelegung

- X30 - Verbunde mit der Hauptplatine HCM-2
- X31 - Anschlussmöglichkeit des Modules ISM7i
- X32 - Service Schnittstelle
- X33 - Anschluss des Bedienmodules BM2

Belegung | X30 | X31 | X32 | X33
---      | --- | --- | --- | ---
+23V     | 1   | 1   | -   | -
CLR      | 2   | -   | -   | -
+3,3V    | 3   | 3   | 1   | 2
eBus     | 4   | 5   | 7   | 3
GND      | 5   | 2   | 8   | 5
wBus     | 6   | 6   | 9   | 4
VIS TX   | 7   | -   | 2   | -
VIS RX   | 8   | -   | 3   | -
