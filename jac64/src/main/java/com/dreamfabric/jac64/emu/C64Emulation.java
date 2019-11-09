package com.dreamfabric.jac64.emu;

import java.io.BufferedInputStream;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dreamfabric.c64utils.Debugger;
import com.dreamfabric.jac64.Hex;
import com.dreamfabric.jac64.SELoader;
import com.dreamfabric.jac64.emu.bus.AddressableBus;
import com.dreamfabric.jac64.emu.cia.CIA1;
import com.dreamfabric.jac64.emu.cia.CIA2;
import com.dreamfabric.jac64.emu.cpu.CPU;
import com.dreamfabric.jac64.emu.cpu.M6510Ops;
import com.dreamfabric.jac64.emu.cpu.MOS6510Ops;
import com.dreamfabric.jac64.emu.interrupt.InterruptManager;
import com.dreamfabric.jac64.emu.io.IO;
import com.dreamfabric.jac64.emu.memory.BasicROM;
import com.dreamfabric.jac64.emu.memory.CharROM;
import com.dreamfabric.jac64.emu.memory.ColorRAM;
import com.dreamfabric.jac64.emu.memory.KernalROM;
import com.dreamfabric.jac64.emu.memory.RAM;
import com.dreamfabric.jac64.emu.memory.ROM;
import com.dreamfabric.jac64.emu.pla.PLA;
import com.dreamfabric.jac64.emu.scheduler.EventQueue;
import com.dreamfabric.jac64.emu.sid.RESID;
import com.dreamfabric.jac64.emu.sid.SIDIf;

public class C64Emulation {
    private static Logger LOGGER = LoggerFactory.getLogger(C64Emulation.class);

    public final static int CPUFrq = 985248;

    private static Debugger monitor = new Debugger();
    private static EventQueue scheduler = new EventQueue();
    private static CPU cpu = new CPU(monitor, "", new SELoader());
    // One InterruptManager per named CPU. For now just one interrupt manager.
    private static InterruptManager interruptManager = new InterruptManager(cpu);
    private static AddressableBus addressableBus = new AddressableBus();

    private static PLA pla = new PLA();
    private static IO io = new IO();
    private static SIDIf sid = new RESID(scheduler);
    private static CIA1 cia1 = new CIA1(scheduler, interruptManager);
    private static CIA2 cia2 = new CIA2(scheduler, interruptManager);

    private static BasicROM basicROM = new BasicROM();
    private static KernalROM kernalROM = new KernalROM();
    private static CharROM charROM = new CharROM();
    private static RAM ram = new RAM();
    private static ColorRAM colorRAM = new ColorRAM();

    static {
        // prepare IO
        io.setSid(sid);
        io.setCia1(cia1);
        io.setCia2(cia2);
        io.setColorRAM(colorRAM);

        // prepare PLA
        pla.setIO(io);
        pla.setBasicROM(basicROM);
        pla.setKernalROM(kernalROM);
        pla.setCharROM(charROM);

        // prepare AddressableBus
        addressableBus.setIO(io);
        addressableBus.setBasicRom(basicROM);
        addressableBus.setKernalRom(kernalROM);
        addressableBus.setCharRom(charROM);
        ram.setEnabled(true);
        addressableBus.setRAM(ram);

        // prepare CPU
        cpu.setScheduler(scheduler);
        cpu.setPla(pla);
        cpu.setAddressableBus(addressableBus);
    }

    public static CPU getCpu() {
        return cpu;
    }

    public static InterruptManager getInterruptManager() {
        return interruptManager;
    }

    public static PLA getPla() {
        return pla;
    }

    public static AddressableBus getAddressableBus() {
        return addressableBus;
    }

    public static SIDIf getSid() {
        return sid;
    }

    public static void setSid(SIDIf sid) {
        C64Emulation.sid = sid;
    }

    public static CIA1 getCia1() {
        return cia1;
    }

    public static CIA2 getCia2() {
        return cia2;
    }

    public static RAM getRAM() {
        return ram;
    }

    public static Debugger getMonitor() {
        return monitor;
    }

    public static EventQueue getScheduler() {
        return scheduler;
    }

    public static void installROMs() {
        loadROM("/roms/kernal.c64", kernalROM, 0x2000);
        patchKernalROM();
        loadROM("/roms/basic.c64", basicROM, 0x2000);
        loadROM("/roms/chargen.c64", charROM, 0x1000);
    }

    private static void loadROM(String resource, ROM rom, int len) {
        try {
            SELoader loader = new SELoader();
            InputStream ins = loader.getResourceStream(resource);

            BufferedInputStream stream = new BufferedInputStream(ins);
            if (stream != null) {
                byte[] charBuf = new byte[len];
                int pos = 0;
                int t;
                try {
                    int startMem = rom.getStartAddress();
                    while ((t = stream.read(charBuf, pos, len - pos)) > 0) {
                        pos += t;
                    }
                    LOGGER.info("Installing rom at :" + Integer.toString(startMem, 16) + " size:" + pos);
                    for (int i = 0; i < charBuf.length; i++) {
                        int data = ((int) charBuf[i]) & 0xff;
                        boolean result = rom.load(startMem + i, data);
                        if (!result) {
                            throw new RuntimeException(
                                    "Problem writing rom file: can't write to addressable. Addres out of range or addressable is disabled.");
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Problem reading rom file ");
                    e.printStackTrace();
                } finally {
                    try {
                        stream.close();
                    } catch (Exception e2) {
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error loading resource" + e);
        }
    }

    private static void patchKernalROM() {
        int address = 0xf49e;
        kernalROM.load(address++, M6510Ops.JSR);
        kernalROM.load(address++, 0xd2);
        kernalROM.load(address++, 0xf5);

        LOGGER.info("Patched LOAD at: " + Hex.hex2(address));
        kernalROM.load(address++, MOS6510Ops.LOAD_FILE);
        kernalROM.load(address++, M6510Ops.RTS);
    }
}
