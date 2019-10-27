package com.dreamfabric.jac64.emu;

import com.dreamfabric.jac64.emu.bus.AddressableBus;
import com.dreamfabric.jac64.emu.io.IO;
import com.dreamfabric.jac64.emu.pla.PLA;
import com.dreamfabric.jac64.emu.sid.SID;

public class C64Emulation {

    private static AddressableBus addressableBus = new AddressableBus();

    private static PLA pla = new PLA();
    private static IO io = new IO();
    private static SID sid = new SID();

    static {
        // prepare IO
        io.setSid(sid);

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
}
