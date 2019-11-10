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

    /***
     * An entry point to fill up the RAM with initial data. The methods
     * {@link isWritable} and {@link isEnabled} are not taken into account here.
     * 
     * @param address
     * @param data
     * @return
     */
    public void write0(int address, int data) {
        super.write0(address, data);
    }
    
    public Integer read0(int address) {
        return super.read0(address);
    }
}
