package com.dreamfabric.jac64.emu.sid;

import com.dreamfabric.jac64.emu.SimulableIf;
import com.dreamfabric.jac64.emu.bus.AddressableIf;

public interface SIDIf extends AddressableIf, SimulableIf {

    public final static int START_ADDRESS = 0xD400;
    public final static int END_ADDRESS = 0xD41F;

    @Override
    default public int getStartAddress() {
        return START_ADDRESS;
    }

    @Override
    default public int getEndAddress() {
        return END_ADDRESS;
    }
}
