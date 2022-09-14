# Battery capacity tester
Kotlin kode which controls an [electronic load](https://en.wikipedia.org/wiki/Electrical_load) via [SCPI](https://en.wikipedia.org/wiki/Standard_Commands_for_Programmable_Instruments)-commands to
discharge a battery in a controlled manner. This lets you calculate the
actual capacity of the battery.

SCPI commands are sent/received via UDP and works for example with [RND 320-KEL103 - Electronic DC Load](https://www.elfadistrelec.no/en/electronic-dc-load-120v-30a-300w-rnd-lab-rnd-320-kel103/p/30126024)
