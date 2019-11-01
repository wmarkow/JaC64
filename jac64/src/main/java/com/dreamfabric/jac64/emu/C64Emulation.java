package com.dreamfabric.jac64.emu;

import java.io.BufferedInputStream;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dreamfabric.jac64.SELoader;
import com.dreamfabric.jac64.emu.bus.AddressableBus;
import com.dreamfabric.jac64.emu.bus.AddressableChip;
import com.dreamfabric.jac64.emu.cia.CIA1;
import com.dreamfabric.jac64.emu.io.IO;
import com.dreamfabric.jac64.emu.memory.BasicROM;
import com.dreamfabric.jac64.emu.pla.PLA;
import com.dreamfabric.jac64.emu.sid.RESID;
import com.dreamfabric.jac64.emu.sid.SIDIf;

public class C64Emulation {
    private static Logger LOGGER = LoggerFactory.getLogger(C64Emulation.class);

    public final static int CPUFrq = 985248;
    private static AddressableBus addressableBus = new AddressableBus();

    private static PLA pla = new PLA();
    private static IO io = new IO();
    private static SIDIf sid = new RESID();
    private static CIA1 cia1 = new CIA1();
    // private static KernalROM kernalROM = new KernalROM();
    private static BasicROM basicROM = new BasicROM();

    static {
        // prepare IO
        io.setSid(sid);
        io.setCia1(cia1);

        // prepare PLA
        pla.setIO(io);
        pla.setBasicROM(basicROM);

        // prepare AddressableBus
        addressableBus.addAddressable(io);
        // addressableBus.addAddressable(kernalROM);
        addressableBus.addAddressable(basicROM);
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

    public static void installROMs() {
        // loadROM("/roms/kernal.c64", kernalROM, 0x2000);
        // patchKernalROM();
        loadROM("/roms/basic.c64", basicROM, 0x2000);
    }

    private static void loadROM(String resource, AddressableChip addressableChip, int len) {
        addressableChip.setEnabled(true);

        try {
            SELoader loader = new SELoader();
            InputStream ins = loader.getResourceStream(resource);

            BufferedInputStream stream = new BufferedInputStream(ins);
            if (stream != null) {
                byte[] charBuf = new byte[len];
                int pos = 0;
                int t;
                try {
                    int startMem = addressableChip.getStartAddress();
                    while ((t = stream.read(charBuf, pos, len - pos)) > 0) {
                        pos += t;
                    }
                    LOGGER.info("Installing rom at :" + Integer.toString(startMem, 16) + " size:" + pos);
                    for (int i = 0; i < charBuf.length; i++) {
                        int data = ((int) charBuf[i]) & 0xff;
                        boolean result = addressableChip.write(startMem + i, data);
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

    // private static void patchKernalROM() {
    // int address = 0xf49e;
    // kernalROM.write(address++, M6510Ops.JSR);
    // kernalROM.write(address++, 0xd2);
    // kernalROM.write(address++, 0xf5);
    //
    // LOGGER.info("Patched LOAD at: " + Hex.hex2(address));
    // kernalROM.write(address++, MOS6510Ops.LOAD_FILE);
    // kernalROM.write(address++, M6510Ops.RTS);
    // }
}
