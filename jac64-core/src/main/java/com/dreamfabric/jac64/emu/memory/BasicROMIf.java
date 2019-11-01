package com.dreamfabric.jac64.emu.memory;

import com.dreamfabric.jac64.emu.bus.AddressableIf;

public interface BasicROMIf extends AddressableIf {

    public final static int START_ADDRESS = 0xA000;
    public final static int END_ADDRESS = 0xBFFF;

    @Override
    public default int getStartAddress() {
        return START_ADDRESS;
    }

    @Override
    public default int getEndAddress() {
        return END_ADDRESS;
    }
}
