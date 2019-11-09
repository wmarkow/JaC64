package com.dreamfabric.jac64.emu.memory;

import com.dreamfabric.jac64.emu.bus.AddressableChip;

public class ColorRAM extends AddressableChip {
    public final static int START_ADDRESS = 0xD800;
    public final static int END_ADDRESS = 0xDBFF;

    @Override
    public int getStartAddress() {
        return START_ADDRESS;
    }

    @Override
    public int getEndAddress() {
        return END_ADDRESS;
    }

}
