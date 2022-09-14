# Battery capacity tester
Kotlin kode which controls an [electronic load](https://en.wikipedia.org/wiki/Electrical_load) via [SCPI](https://en.wikipedia.org/wiki/Standard_Commands_for_Programmable_Instruments)-commands to
discharge a battery in a controlled manner. This lets you calculate the
actual capacity of the battery.

SCPI commands are sent/received via UDP and works for example with:
- [RND 320-KEL103 - Electronic DC Load](https://www.elfadistrelec.no/en/electronic-dc-load-120v-30a-300w-rnd-lab-rnd-320-kel103/p/30126024)
- [Korad KEL103 programmable DC electronic load](https://eleshop.eu/korad-kel103-programmable-dc-electronic-load.html)

## Example
     % java -jar battery-capacity-tester.jar 192.168.1.17:18190 18190 3.7 1.0 3600 0.05
     18:42:16.941 [main] INFO  c.d.b.BatteryCapacityTester -  mode=CC duration=1027 voltage=3.723 current=0.999 sumMilliAmpereHours=0.285 milliAmpereHours=0.285
     18:42:17.976 [main] INFO  c.d.b.BatteryCapacityTester -  mode=CC duration=1046 voltage=3.721 current=0.999 sumMilliAmpereHours=0.575 milliAmpereHours=0.290
     18:42:19.006 [main] INFO  c.d.b.BatteryCapacityTester -  mode=CC duration=1033 voltage=3.720 current=0.999 sumMilliAmpereHours=0.861 milliAmpereHours=0.287
     18:42:20.035 [main] INFO  c.d.b.BatteryCapacityTester -  mode=CC duration=1027 voltage=3.719 current=0.999 sumMilliAmpereHours=1.146 milliAmpereHours=0.285
     18:42:21.062 [main] INFO  c.d.b.BatteryCapacityTester -  mode=CC duration=1028 voltage=3.718 current=0.999 sumMilliAmpereHours=1.431 milliAmpereHours=0.285
     18:42:22.090 [main] INFO  c.d.b.BatteryCapacityTester -  mode=CC duration=1026 voltage=3.717 current=0.999 sumMilliAmpereHours=1.716 milliAmpereHours=0.285
     18:42:23.124 [main] INFO  c.d.b.BatteryCapacityTester -  mode=CC duration=1034 voltage=3.716 current=0.999 sumMilliAmpereHours=2.003 milliAmpereHours=0.287
     18:42:24.157 [main] INFO  c.d.b.BatteryCapacityTester -  mode=CC duration=1034 voltage=3.715 current=0.999 sumMilliAmpereHours=2.290 milliAmpereHours=0.287
     18:42:25.182 [main] INFO  c.d.b.BatteryCapacityTester -  mode=CC duration=1025 voltage=3.715 current=0.999 sumMilliAmpereHours=2.574 milliAmpereHours=0.284
     18:42:26.215 [main] INFO  c.d.b.BatteryCapacityTester -  mode=CC duration=1032 voltage=3.714 current=0.999 sumMilliAmpereHours=2.860 milliAmpereHours=0.286
     18:42:27.247 [main] INFO  c.d.b.BatteryCapacityTester -  mode=CC duration=1033 voltage=3.714 current=0.999 sumMilliAmpereHours=3.147 milliAmpereHours=0.287
     18:42:28.277 [main] INFO  c.d.b.BatteryCapacityTester -  mode=CC duration=1031 voltage=3.713 current=0.999 sumMilliAmpereHours=3.433 milliAmpereHours=0.286
