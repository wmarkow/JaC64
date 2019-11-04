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

import com.dreamfabric.c64utils.AutoStore;
import com.dreamfabric.jac64.DefaultIMon;
import com.dreamfabric.jac64.Hex;
import com.dreamfabric.jac64.IMonitor;
import com.dreamfabric.jac64.Loader;
import com.dreamfabric.jac64.PatchListener;
import com.dreamfabric.jac64.emu.C64Emulation;
import com.dreamfabric.jac64.emu.SlowDownCalculator;
import com.dreamfabric.jac64.emu.TimeEvent;
import com.dreamfabric.jac64.emu.bus.AddressableBus;
import com.dreamfabric.jac64.emu.pla.PLA;

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
    public static final int IO_OFFSET = 0x10000 - 0xd000;
    public static final int BASIC_ROM2 = 0x1a000;
    public static final int KERNAL_ROM2 = 0x1e000;
    public static final int CHAR_ROM2 = 0x1d000;

    public static final int CH_PROTECT = 1;
    public static final int CH_MONITOR_WRITE = 2;
    public static final int CH_MONITOR_READ = 4;

    // Defaults for the ROMs
    public boolean basicROM = true;
    public boolean kernalROM = true;
    public boolean charROM = false;
    public boolean ioON = true;

    // The state of the program (runs if running = true)
    public boolean running = true;
    public boolean pause = false;

    private static final long CYCLES_PER_DEBUG = 10000000;
    public static final boolean DEBUG = false;

    private Loader loader;
    private int windex = 0;

    private int cheatMon[];
    private AutoStore[] autoStore;

    private PLA pla;
    private AddressableBus addressableBus;

    public CPU(IMonitor m, String cb, Loader loader) {
        super(m, cb);
        this.loader = loader;
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

        // START: a new way of reading data from SID.
        Integer result = addressableBus.read(adr);
        if (result != null) {
            return (int) result;
        }
        // END: a new way of reading data from SID.

        if (basicROM && adr >= 0xA000 && adr <= 0xBFFF) {
            // should never happen because Basic ROM is handled by addressable bus
            throw new IllegalArgumentException("should never happen because Basic ROM is handled by addressable bus");
        }
        if (kernalROM && adr >= 0xE000 && adr <= 0xFFFF) {
            throw new IllegalArgumentException("should never happen because Kernal ROM is handled by addressable bus");
        }
        if (charROM && adr >= 0xD000 && adr <= 0xDFFF) {
            throw new IllegalArgumentException("should never happen because Char ROM is handled by addressable bus");
        }
        if (ioON && adr >= 0xD000 && adr <= 0xDFFF) {
            return chips.performRead(adr, currentCpuCycles);
        }
        return getMemory(adr);
    }

    // A byte is written directly to memory or to ioChips
    protected final void writeByte(int adr, int data) {
        currentCpuCycles++;

        schedule(currentCpuCycles);
        // Locking only on fetch byte...
        // System.out.println("Writing byte at: " + Integer.toString(adr, 16)
        // + " = " + data);
        if (adr <= 1) {
            setMemory(adr, data);
            int p = (getMemory(0) ^ 0xff) | getMemory(1);

            // pla.setCharenHiramLoram(p);

            kernalROM = ((p & 2) == 2); // Kernal on
            basicROM = ((p & 3) == 3); // Basic on

            charROM = ((p & 3) != 0) && ((p & 4) == 0);
            // ioON is probably not correct!!! Check against table...
            ioON = ((p & 3) != 0) && ((p & 4) != 0);

            // LOGGER.info(String.format("Setting basicROM = %s, kernalROM = %s, charROM =
            // %s, ioON = %s", basicROM,
            // kernalROM, charROM, ioON));
        }

        // START: a new way of writing data.
        if (adr == 0x01) {
            // setting CHAREN, HIRAM and LORAM of PLA
            pla.setCharenHiramLoram(data);
        }

        if (addressableBus.write(adr, data)) {
            adr &= 0xffff;
            if (ioON && adr >= 0xD000 && adr <= 0xDFFF) {
                // do nothing
            } else {
                windex = adr;
            }
            return;
        }
        // END: a new way of writing data.

        adr &= 0xffff;
        if (ioON && adr >= 0xD000 && adr <= 0xDFFF) {
            // System.out.println("IO Write at: " + Integer.toString(adr, 16));
            chips.performWrite(adr, data, currentCpuCycles);

            return;
        }

        // it should write to the underlying RAM:
        // https://www.c64-wiki.com/wiki/Memory_Map
        setMemory(windex = adr, data);
    }

    public void poke(int address, int data) {
        writeByte(address & 0xffff, data & 0xff);
    }

    public void patchROM(PatchListener list) {
        this.list = list;

        int pos = 0xf49e | 0x10000;
        setMemory(pos++, M6510Ops.JSR);
        setMemory(pos++, 0xd2);
        setMemory(pos++, 0xf5);

        System.out.println("Patched LOAD at: " + Hex.hex2(pos));
        setMemory(pos++, LOAD_FILE);
        setMemory(pos++, M6510Ops.RTS);
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

    protected void installROMS() {
        loadROM(loader.getResourceStream("/roms/chargen.c64"), CHAR_ROM2, 0x1000);

        C64Emulation.installROMs();
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
                int loadAdr = -1;
                if (sec == 0)
                    loadAdr = getMemory(0x2b) + (getMemory(0x2c) << 8);
                if (list != null) {
                    if (list.readFile(name, loadAdr)) {
                        acc = 0;
                    }
                }
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
        if (cheatMon != null) {
            cheatLoop();
            return;
        }
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

//                long delay = slowDownCalculator.calculateWaitInNanos(System.nanoTime(), currentCpuCycles);
//                if (delay == 0) {
//                    continue;
//                }
//
//                long sleepUntilNanos = System.nanoTime() + delay;
//                while (System.nanoTime() <= sleepUntilNanos) {
//                    // just empty loop to slow down the simulation
//                }
            }
        } catch (Exception e) {
            monitor.error("Exception in loop " + pc + " : " + e);
            e.printStackTrace();
            monitor.disAssemble(getMemory(), 0, acc, x, y, (byte) getStatusByte(), interruptInExec, lastInterrupt);
        }
    }

    // -------------------------------------------------------------------
    // Cheat loop!
    // Protection
    // + rule triggered auto get/store
    // Rule: xpr & xpr & xpr ...
    // rule: int[] adr, cmptype, cmpval ...
    // autostore: int[] adr, len => result in hex! from adr and on!
    // -------------------------------------------------------------------

    public void setAutoStore(int index, AutoStore au) {
        autoStore[index] = au;
    }

    public AutoStore getAutoStore(int index) {
        return autoStore[index];
    }

    public void setCheatEnabled(int maxAutostores) {
        cheatMon = new int[0x10000];
        autoStore = new AutoStore[maxAutostores];
    }

    public void protect(int address, int value) {
        cheatMon[address] = (cheatMon[address] & 0xff) | (value << 8) | CH_PROTECT;
    }

    public void monitorRead(int address) {
        cheatMon[address] |= CH_MONITOR_READ;
    }

    public void monitorWrite(int address) {
        cheatMon[address] |= CH_MONITOR_WRITE;
    }

    public void cheatLoop() {
        int t;
        try {
            while (running) {

                // Run one instruction!
                emulateOp();

                if (windex < 0x10000) {
                    if ((t = cheatMon[windex]) != 0) {
                        if ((t & CH_PROTECT) != 0) {
                            // Write back value from then protected...
                            setMemory(windex, (cheatMon[windex] >> 16) & 0xff);
                        }
                        if ((t & CH_MONITOR_WRITE) != 0) {
                            for (int i = 0, n = autoStore.length; i < n; i++) {
                                if (autoStore[i] != null)
                                    autoStore[i].checkRules(getMemory());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            monitor.error("Exception in loop " + pc + " : " + e);
            e.printStackTrace();
            monitor.disAssemble(getMemory(), 0, acc, x, y, (byte) getStatusByte(), interruptInExec, lastInterrupt);
        }
    }

    @Override
    protected int getMemorySize() {
        return 0x20000;
    }
}
