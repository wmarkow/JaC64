# JaC64
**Wmarkow** fork of **Cat's Eye Technologies** **JaC64** fork, a Java-based Commodore 64 emulator.

This fork introduces the following features/improvements:
 * introduce Maven as a build system
 * the code has been splited into smaller Java projects (Maven modules)
 * rework and refactor the sources so the C64 emulated units are more or less docoupled from each other:
   * introduce **CPU** unit
   * introduce **VIC** unit
   * introduce **SID** unit
   * introduce **CIA1** and **CIA2** units
   * introduce **PLA** unit
   * introduce **RAM**, **Color RAM**, **Basic ROM**, **Char ROM**, **Kernal ROM** units
   * introduce **Memory Bus** unit which allows to exchange data between other units
   * introduce **Control Bus** which takes over the interrupts and simulation event queue
 * the code looks much cleaner now
 * it would be easier to bugfixing or implement a new features
 * unfortunatelly the support for 1541 floppy disk has been abandoned during rework/refactor
 
 
With this rework the following are possible:
 * implement a silent **SID** which will generate no sound
 * implement a blind **VIC** which will generate no graphic
 * support for using external cartdrige
 * port the Java code into Arduino (like ESP8266), performance is a key word here
