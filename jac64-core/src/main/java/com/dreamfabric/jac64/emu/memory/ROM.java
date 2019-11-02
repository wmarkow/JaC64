package com.dreamfabric.jac64.emu.memory;

import com.dreamfabric.jac64.emu.bus.AddressableChip;

public abstract class ROM extends AddressableChip implements ROMIf {

    @Override
    public boolean load(int address, int data) {
        write0(address, data);

        return true;
    }
}
