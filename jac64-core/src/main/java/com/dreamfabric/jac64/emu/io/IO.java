package com.dreamfabric.jac64.emu.io;

import com.dreamfabric.jac64.emu.bus.AddressableVoid;
import com.dreamfabric.jac64.emu.sid.SIDIf;

public class IO extends AddressableVoid {

    private final static int START_ADDRESS = 0xD000;
    private final static int END_ADDRESS = 0xDFFF;

    private SIDIf sid;

    public void setSid(SIDIf sid) {
        this.sid = sid;
    }

    @Override
    public boolean write(int address, int data) {
        if (sid.write(address, data)) {
            return true;
        }

        return false;
    }

    @Override
    public Integer read(int address) {
        Integer result = null;

        result = sid.read(address);
        if (result != null) {
            return result;
        }

        return null;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        sid.setEnabled(enabled);
    }

    @Override
    public int getStartAddress() {
        return START_ADDRESS;
    }

    @Override
    public int getEndAddress() {
        return END_ADDRESS;
    }
}
