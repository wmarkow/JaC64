package com.dreamfabric.jac64.emu.bus;

import com.dreamfabric.jac64.emu.io.IO;
import com.dreamfabric.jac64.emu.memory.BasicROMIf;

public class AddressableBus implements AddressableIf {

    private final static int START_ADDRESS = 0x0000;
    private final static int END_ADDRESS = 0xFFFF;

    private BasicROMIf basicRom;
    private IO io;

    public void setBasicRom(BasicROMIf basicRom) {
        this.basicRom = basicRom;
    }

    public void setIO(IO io) {
        this.io = io;
    }

    @Override
    public boolean write(int address, int data) {
        boolean result = false;

        // it makes no point to write to ROMs
        result = io.write(address, data);
        if (result) {
            return result;
        }

        return result;
    }

    @Override
    public Integer read(int address) {
        Integer result = null;

        result = basicRom.read(address);
        if (result != null) {
            return result;
        }
        result = io.read(address);
        if (result != null) {
            return result;
        }

        return null;
    }

    @Override
    public int getStartAddress() {
        return START_ADDRESS;
    }

    @Override
    public int getEndAddress() {
        return END_ADDRESS;
    }

    @Override
    public void setEnabled(boolean enabled) {
        // nothing to do here
    }

    @Override
    public boolean isEnabled() {
        // always true
        return true;
    }
}
