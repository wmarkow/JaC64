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
import com.dreamfabric.jac64.emu.bus.AddressableBus;
import com.dreamfabric.jac64.emu.pla.PLA;
import com.dreamfabric.jac64.emu.scheduler.TimeEvent;
import com.dreamfabric.jac64.emu.vic.C64Screen;

/**
 * CPU "implements" the C64s 6510 processor in java code. reimplemented from old
 * CPU.java
 *
 * @author Joakim Eriksson (joakime@sics.se)
 * @version $Revision:$, $Date:$
 */
public class CPU extends MOS6510Core {
    private static Logger LOGGER = LoggerFactory.getLogger(CPU.class);

    public static final boolean DEBUG_EVENT = false;
    // The IO RAM memory at 0x10000 (just since there is RAM there...)
    public static final int CHAR_ROM2 = 0x1d000;

    public static final int CH_PROTECT = 1;
    public static final int CH_MONITOR_WRITE = 2;
    public static final int CH_MONITOR_READ = 4;

    // The state of the program (runs if running = true)
    public boolean running = true;
    public boolean pause = false;

    private static final long CYCLES_PER_DEBUG = 10000000;
    public static final boolean DEBUG = false;

    private PLA pla;
    private AddressableBus addressableBus;
    protected C64Screen chips = null;
    private int memory[];

    public CPU(IMonitor m, String cb) {
        super(m, cb);
        memory = new int[getMemorySize()];
    }

    public void init(C64Screen scr) {
        super.init();
        this.chips = scr;
        C64Emulation.installROMs();
    }

    public void setPla(PLA pla) {
        this.pla = pla;
    }

    public void setAddressableBus(AddressableBus addressableBus) {
        this.addressableBus = addressableBus;
    }

    private final void schedule(long currentCpuCycles) {
        chips.clock(currentCpuCycles);
        while (currentCpuCycles >= scheduler.nextTime) {
            TimeEvent t = scheduler.popFirst();
            if (t != null) {
                if (DEBUG_EVENT) {
                    System.out.println("Executing event: " + t.getShort());
                }
                // Give it the actual time also!!!
                t.execute(currentCpuCycles);
            } else {
                if (DEBUG_EVENT)
                    System.out.println("Nothign to execute...");
                return;
            }
        }
    }

    // Reads the memory with all respect to all flags...
    protected final int fetchByte(int adr) {
        /* a cycles passes for this read */
        currentCpuCycles++;

        /* Chips work first, then CPU */
        schedule(currentCpuCycles);
        while (baLowUntil > currentCpuCycles) {
            currentCpuCycles++;
            schedule(currentCpuCycles);
        }

        Integer result = addressableBus.read(adr, currentCpuCycles);
        if (result != null) {
            return (int) result;
        }

        throw new IllegalArgumentException("Read operation should be handled by addressable bus!");
    }

    // A byte is written directly to memory or to ioChips
    protected final void writeByte(int adr, int data) {
        currentCpuCycles++;

        schedule(currentCpuCycles);

        if (adr == 0x01) {
            // setting CHAREN, HIRAM and LORAM of PLA
            pla.setCharenHiramLoram(data);
        }

        if (addressableBus.write(adr, data, currentCpuCycles)) {
            return;
        }

        throw new IllegalArgumentException("Write operation should be handled by addressable bus!");
    }

    public void poke(int address, int data) {
        writeByte(address & 0xffff, data & 0xff);
    }

    public void runBasic() {
        setMemory(631, (int) 'R');
        setMemory(632, (int) 'U');
        setMemory(633, (int) 'N');
        setMemory(634, 13);// enter
        setMemory(198, 4); // length
    }

    public void enterText(String txt) {
        System.out.println("Entering text into textbuffer: " + txt);
        txt = txt.toUpperCase();
        int len = txt.length();
        int pos = 0;
        for (int i = 0, n = len; i < n; i++) {
            char c = txt.charAt(i);
            if (c == '~')
                c = 13;
            setMemory(631 + pos, c);
            pos++;
            if (pos == 5) {
                setMemory(198, pos);
                pos = 0;
                int tries = 5;
                while (tries > 0 && getMemory(198) > 0) {
                    try {
                        Thread.sleep(50);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    tries--;
                    if (tries == 0) {
                        System.out.println("Buffer still full: " + getMemory(198));
                    }
                }
            }
        }
        setMemory(198, pos);
        int tries = 5;
        while (tries > 0 && getMemory(198) > 0) {
            try {
                Thread.sleep(50);
            } catch (Exception e) {
                e.printStackTrace();
            }
            tries--;
            if (tries == 0) {
                System.out.println("Buffer still full: " + getMemory(198));
            }
        }
    }

    public void run(int address) {
        reset();
        running = true;
        setPC(address);

        C64Emulation.getSid().start(getCycles());

        loop();
    }

    public void unknownInstruction(int pc, int op) {
        switch (op) {
            case SLEEP:
                currentCpuCycles += 100;
                break;
            case LOAD_FILE:
                if (acc == 0)
                    monitor.info(
                            "**** LOAD FILE! ***** PC = " + Integer.toString(pc, 16) + " => wmarkow unknown rindex");
                else
                    monitor.info("**** VERIFY!    ***** PC = " + pc + " => wmarkow unknown rindex");
                int len;
                int mptr = getMemory(0xbb) + (getMemory(0xbc) << 8);
                monitor.info("Filename len:" + (len = getMemory(0xb7)));
                String name = "";
                for (int i = 0; i < len; i++)
                    name += (char) getMemory(mptr++);
                name += '\n';
                int sec = getMemory(0xb9);
                monitor.info("name = " + name);
                monitor.info("Sec Address: " + sec);

                pc--;
                break;
        }
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

    public synchronized void stop() {
        // stop completely
        running = false;
        pause = false;
        C64Emulation.getSid().stop();
        notify();
    }

    public void reset() {
        writeByte(1, 0x7);
        super.reset();
        chips.reset();
        scheduler.empty();
        C64Emulation.getSid().reset();
    }

    public void setPC(int startAdress) {
        // The processor flags
        pc = startAdress;
    }

    public String getName() {
        return "C64 CPU";
    }

    /**
     * The main emulation <code>loop</code>.
     *
     * @param startAdress
     *            an <code>int</code> value that represent the starting address of
     *            the emulator
     */
    public void loop() {
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
                        monitor.disAssemble(getMemory(), 0, acc, x, y, (byte) getStatusByte(), interruptInExec,
                                lastInterrupt);
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
                        if (level > 2)
                            monitor.disAssemble(getMemory(), 0, acc, x, y, (byte) getStatusByte(), interruptInExec,
                                    lastInterrupt);
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
            e.printStackTrace();
            monitor.disAssemble(getMemory(), 0, acc, x, y, (byte) getStatusByte(), interruptInExec, lastInterrupt);
        }
    }

    public int[] getMemory() {
        return memory;
    }

    public void hardReset() {
        for (int i = 0; i < 0x10000; i++) {
            memory[i] = 0;
        }
        reset();
    }

    protected int getMemorySize() {
        return 0x20000;
    }

    protected int getMemory(int address) {
        validateAddress(address);

        return memory[address];
    }

    protected void setMemory(int address, int data) {
        validateAddress(address);

        memory[address] = data;
    }

    private void validateAddress(int address) {
        if (address >= 0x1D000 && address < 0x1DFFF) {
            LOGGER.warn(String.format("Invalid address 0x%05X", address));

            return;
        }
    }
}
