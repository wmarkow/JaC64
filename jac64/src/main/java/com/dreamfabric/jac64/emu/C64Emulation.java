package com.dreamfabric.jac64.emu;

import com.dreamfabric.jac64.emu.bus.AddressableBus;
import com.dreamfabric.jac64.emu.io.IO;
import com.dreamfabric.jac64.emu.pla.PLA;
import com.dreamfabric.jac64.emu.sid.RESID;
import com.dreamfabric.jac64.emu.sid.SIDIf;
import com.dreamfabric.jac64.emu.sid.VoidSID;

public class C64Emulation {

    private static AddressableBus addressableBus = new AddressableBus();

    private static PLA pla = new PLA();
    private static IO io = new IO();
//     private static SIDIf sid = new VoidSID();
    private static SIDIf sid = new RESID();

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
    
    public static SIDIf getSid() {
        return sid;
    }
}
