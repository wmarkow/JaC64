package com.dreamfabric.jac64.emu.memory;

import com.dreamfabric.jac64.emu.bus.AddressableChip;

public class BasicROM extends AddressableChip {

    @Override
    public int getStartAddress() {
        return VoidBasicROM.START_ADDRESS;
    }

    @Override
    public int getEndAddress() {
        return VoidBasicROM.END_ADDRESS;
    }
}
