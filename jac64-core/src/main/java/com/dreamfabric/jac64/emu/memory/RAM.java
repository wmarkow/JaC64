package com.dreamfabric.jac64.emu.memory;

import com.dreamfabric.jac64.emu.bus.AddressableChip;

public class RAM extends AddressableChip {
    public final static int START_ADDRESS = 0x0000;
    public final static int END_ADDRESS = 0xFFFF;

    @Override
    public int getStartAddress() {
        return START_ADDRESS;
    }

    @Override
    public int getEndAddress() {
        return END_ADDRESS;
    }

}
