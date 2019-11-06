package com.dreamfabric.jac64.emu.vic;

import com.dreamfabric.jac64.emu.SimulableIf;
import com.dreamfabric.jac64.emu.bus.AddressableIf;

public interface VICIf extends AddressableIf, SimulableIf {

    public final static int START_ADDRESS = 0xD000;
    public final static int END_ADDRESS = 0xD3FF;

    @Override
    default public int getStartAddress() {
        return START_ADDRESS;
    }

    @Override
    default public int getEndAddress() {
        return END_ADDRESS;
    }

}