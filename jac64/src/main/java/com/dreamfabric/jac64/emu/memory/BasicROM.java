package com.dreamfabric.jac64.emu.memory;

import com.dreamfabric.jac64.emu.bus.AddressableChip;

public class BasicROM extends AddressableChip implements BasicROMIf {

    @Override
    public boolean load(int address, int data) {
        write0(address, data);

        return true;
    }
}
