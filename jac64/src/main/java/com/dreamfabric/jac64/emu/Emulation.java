/**
 * This file is a part of JaC64 - a Java C64 Emulator
 * Main Developer: Joakim Eriksson (Dreamfabric.com)
 * Contact: joakime@sics.se
 * Web: http://www.dreamfabric.com/c64
 * ---------------------------------------------------
 * This is the CPU file for Commodore 64 with its
 * ROM files and memory management, etc.
 *
 * @(#)cpu.java	Created date: 99-5-17
 *
 */
package com.dreamfabric.jac64.emu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dreamfabric.jac64.emu.cpu.C64Cpu;

/**
 * CPU "implements" the C64s 6510 processor in java code. reimplemented from old
 * CPU.java
 *
 * @author Joakim Eriksson (joakime@sics.se)
 * @version $Revision:$, $Date:$
 */
public class Emulation {
    private static Logger LOGGER = LoggerFactory.getLogger(Emulation.class);

    // The state of the program (runs if running = true)
    public boolean running = true;
    public boolean pause = false;
    EmulationContext emulationContext;

    public Emulation(EmulationContext emulationContext) {
        this.emulationContext = emulationContext;
    }

    // Takes the thread and loops!!!
    public void start() {
        run(0xfce2); // Power UP reset routine!

        if (pause) {
            while (pause) {
                System.out.println("Entering pause mode...");
                synchronized (this) {
                    try {
                        wait();
                    } catch (Exception e) {
                    }
                }
                System.out.println("Exiting pause mode...");
                loop();
            }
        }
    }

    public synchronized void stop() {
        // stop completely
        running = false;
        pause = false;
        emulationContext.getSid().stop();
        notify();
    }

    // Should pause the application!
    public synchronized void setPause(boolean p) {
        if (p) {
            pause = true;
            running = false;
        } else {
            pause = false;
            running = true;
        }
        notify();
    }

    public void reset() {
        getCpu().reset();
        emulationContext.getSid().reset();
    }

    public void runBasic() {
        getCpu().runBasic();
    }

    private void run(int address) {
        reset();
        running = true;
        getCpu().setPc(address);

        emulationContext.getSid().start(getCpu().getCycles());

        loop();
    }

    protected void loop() {
        try {
            while (running) {
                // Run one instruction!
                getCpu().emulateOp();
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    public C64Cpu getCpu() {
        return emulationContext.getCpu();
    }
}
