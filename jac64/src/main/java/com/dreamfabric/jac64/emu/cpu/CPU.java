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
package com.dreamfabric.jac64.emu.cpu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dreamfabric.jac64.IMonitor;
import com.dreamfabric.jac64.emu.C64Emulation;
import com.dreamfabric.jac64.emu.SlowDownCalculator;
import com.dreamfabric.jac64.emu.vic.C64Screen;

/**
 * CPU "implements" the C64s 6510 processor in java code. reimplemented from old
 * CPU.java
 *
 * @author Joakim Eriksson (joakime@sics.se)
 * @version $Revision:$, $Date:$
 */
public class CPU extends C64Cpu {
    private static Logger LOGGER = LoggerFactory.getLogger(CPU.class);

    // The state of the program (runs if running = true)
    public boolean running = true;
    public boolean pause = false;

    private static final long CYCLES_PER_DEBUG = 10000000;
    public static final boolean DEBUG = false;

    public CPU(IMonitor m, String cb) {
        super(m, cb);
    }

    public void init(C64Screen scr) {
        super.init();
        this.setC64Screen(scr);
        C64Emulation.installROMs();
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
        C64Emulation.getSid().stop();
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
        writeByte(1, 0x7);
        super.reset();
        C64Emulation.getSid().reset();
    }

    public void runBasic() {
        super.runBasic();
    }

    private void run(int address) {
        reset();
        running = true;
        setPc(address);

        C64Emulation.getSid().start(getCycles());

        loop();
    }

    /**
     * The main emulation <code>loop</code>.
     *
     * @param startAdress
     *            an <code>int</code> value that represent the starting address of
     *            the emulator
     */
    protected void loop() {
        long next_print = currentCpuCycles + CYCLES_PER_DEBUG;
        // How much should this be???
        monitor.info("Starting CPU at: " + Integer.toHexString(pc));
        try {
            SlowDownCalculator slowDownCalculator = new SlowDownCalculator(C64Emulation.CPUFrq);

            while (running) {
                slowDownCalculator.markLoopStart(System.nanoTime(), currentCpuCycles);

                // Debugging?
                if (monitor.isEnabled()) { // || interruptInExec > 0) {
                    if (baLowUntil <= currentCpuCycles) {
                        // TODO: wmarkow fix disassemble
                        // monitor.disAssemble(getMemory(), 0, acc, x, y, (byte) getStatusByte(),
                        // interruptInExec,
                        // lastInterrupt);
                    }
                }

                // Run one instruction!
                emulateOp();

                nr_ins++;
                if (next_print < currentCpuCycles) {
                    long sec = System.currentTimeMillis() - lastMillis;
                    int level = monitor.getLevel();

                    if (DEBUG && level > 1) {
                        monitor.info("--------------------------");
                        monitor.info("Nr ins:" + nr_ins + " sec:" + (sec) + " -> " + ((nr_ins * 1000) / sec) + " ins/s"
                                + "  " + " clk: " + currentCpuCycles + " clk/s: " + ((CYCLES_PER_DEBUG * 1000) / sec)
                                + "\n" + ((nr_irq * 1000) / sec));
                        // TODO: wmarkow fix disassemble
                        // if (level > 2)
                        // monitor.disAssemble(getMemory(), 0, acc, x, y, (byte) getStatusByte(),
                        // interruptInExec,
                        // lastInterrupt);
                        monitor.info("--------------------------");
                    }
                    nr_irq = 0;
                    nr_ins = 0;
                    lastMillis = System.currentTimeMillis();
                    next_print = currentCpuCycles + CYCLES_PER_DEBUG;
                }

                // long delay = slowDownCalculator.calculateWaitInNanos(System.nanoTime(),
                // currentCpuCycles);
                // if (delay == 0) {
                // continue;
                // }
                //
                // long sleepUntilNanos = System.nanoTime() + delay;
                // while (System.nanoTime() <= sleepUntilNanos) {
                // // just empty loop to slow down the simulation
                // }
            }
        } catch (Exception e) {
            monitor.error("Exception in loop " + pc + " : " + e);
            LOGGER.error(e.getMessage(), e);
            // TODO: wmarkow fix disassemble
            // monitor.disAssemble(getMemory(), 0, acc, x, y, (byte) getStatusByte(),
            // interruptInExec, lastInterrupt);
        }
    }
}
