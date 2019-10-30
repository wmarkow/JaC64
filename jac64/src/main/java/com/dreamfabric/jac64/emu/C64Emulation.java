package com.dreamfabric.jac64.emu;

import com.dreamfabric.jac64.emu.bus.AddressableBus;
import com.dreamfabric.jac64.emu.cia.CIA1;
import com.dreamfabric.jac64.emu.io.IO;
import com.dreamfabric.jac64.emu.pla.PLA;
import com.dreamfabric.jac64.emu.sid.RESID;
import com.dreamfabric.jac64.emu.sid.SIDIf;

public class C64Emulation {

    public final static int CPUFrq = 985248;
    private static AddressableBus addressableBus = new AddressableBus();

    private static PLA pla = new PLA();
    private static IO io = new IO();
    private static SIDIf sid = new RESID();
    private static CIA1 cia1 = new CIA1();

    static {
        // prepare IO
        io.setSid(sid);
        io.setCia1(cia1);

        // prepare PLA
        pla.setIO(io);

        // prepare AddressableBus
        addressableBus.addAddressable(io);
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
}
