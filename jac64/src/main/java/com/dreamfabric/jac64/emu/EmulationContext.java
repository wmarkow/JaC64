package com.dreamfabric.jac64.emu;

import java.io.BufferedInputStream;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dreamfabric.c64utils.Debugger;
import com.dreamfabric.jac64.Hex;
import com.dreamfabric.jac64.SELoader;
import com.dreamfabric.jac64.emu.bus.AddressableBus;
import com.dreamfabric.jac64.emu.bus.ControlBus;
import com.dreamfabric.jac64.emu.cia.CIA1;
import com.dreamfabric.jac64.emu.cia.CIA2;
import com.dreamfabric.jac64.emu.cpu.C64Cpu;
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
import com.dreamfabric.jac64.emu.vic.C64Screen;

public class EmulationContext {
    private static Logger LOGGER = LoggerFactory.getLogger(EmulationContext.class);

    public final static int CPUFrq = 985248;

    private Debugger monitor = new Debugger();
    private EventQueue scheduler = new EventQueue();
    private C64Cpu cpu = new C64Cpu();
    // One InterruptManager per named CPU. For now just one interrupt manager.
    private InterruptManager interruptManager = new InterruptManager(cpu);
    private PLA pla = new PLA();
    private ControlBus controlBus = new ControlBus(pla, interruptManager, cpu);

    private IO io = new IO();
    private SIDIf sid = new RESID(scheduler);
    private C64Screen vic = new C64Screen(monitor, true);
    private CIA1 cia1 = new CIA1(scheduler, controlBus);
    private CIA2 cia2 = new CIA2(scheduler, controlBus);

    private AddressableBus addressableBus = new AddressableBus();

    private BasicROM basicROM = new BasicROM();
    private KernalROM kernalROM = new KernalROM();
    private CharROM charROM = new CharROM();
    private RAM ram = new RAM();
    private ColorRAM colorRAM = new ColorRAM();

    public EmulationContext() {
        // prepare IO
        vic.init(controlBus, cia1);
        vic.setCia2(cia2);
        vic.setAddressableBus(addressableBus);
        io.setSid(sid);
        io.setVic(vic);
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
        cpu.init();
        cpu.setC64Screen(vic);
        cpu.setScheduler(scheduler);
        cpu.setControlBus(controlBus);
        cpu.setAddressableBus(addressableBus);

        installROMs();
    }

    public C64Cpu getCpu() {
        return cpu;
    }

    public AddressableBus getAddressableBus() {
        return addressableBus;
    }

    public SIDIf getSid() {
        return sid;
    }

    public void setSid(SIDIf sid) {
        this.sid = sid;
    }

    public CIA1 getCia1() {
        return cia1;
    }

    public CIA2 getCia2() {
        return cia2;
    }

    public RAM getRAM() {
        return ram;
    }

    public C64Screen getVic() {
        return vic;
    }

    public Debugger getMonitor() {
        return monitor;
    }

    public EventQueue getScheduler() {
        return scheduler;
    }

    public void installROMs() {
        loadROM("/roms/kernal.c64", kernalROM, 0x2000);
        patchKernalROM();
        loadROM("/roms/basic.c64", basicROM, 0x2000);
        loadROM("/roms/chargen.c64", charROM, 0x1000);
    }

    private void loadROM(String resource, ROM rom, int len) {
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

    private void patchKernalROM() {
        int address = 0xf49e;
        kernalROM.load(address++, M6510Ops.JSR);
        kernalROM.load(address++, 0xd2);
        kernalROM.load(address++, 0xf5);

        LOGGER.info("Patched LOAD at: " + Hex.hex2(address));
        kernalROM.load(address++, MOS6510Ops.LOAD_FILE);
        kernalROM.load(address++, M6510Ops.RTS);
    }
}
