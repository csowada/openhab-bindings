#eBus Binding

## Anschlussmöglichekeiten an den eBus

Bei der Heizung Wolf CSZ-2 kann man sich auf der Rückseite des Bedienmoduls BM2 Zugriff auf den eBus beschaffen.
Um die Anschlüsse zu bestimmen, habe ich die Platine aus der Heizung ausgebaut und durchgemessen.
![BM-Kontaktplatine](images/IMG_20140903_121551.jpg)

### Pinbelegung

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
