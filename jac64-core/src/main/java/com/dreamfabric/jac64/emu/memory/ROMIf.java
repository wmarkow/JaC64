package com.dreamfabric.jac64.emu.memory;

import com.dreamfabric.jac64.emu.bus.AddressableIf;

public interface ROMIf extends AddressableIf {

    /***
     * An entry point to fill up the ROM (which is read only) with initial data. The
     * methods {@link isWritable} and {@link isEnabled} are not taken into account
     * here.
     * 
     * @param address
     * @param data
     * @return
     */
    public boolean load(int address, int data);
}
