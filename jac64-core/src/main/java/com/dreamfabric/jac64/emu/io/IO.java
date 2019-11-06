package com.dreamfabric.jac64.emu.io;

import com.dreamfabric.jac64.emu.bus.AddressableVoid;
import com.dreamfabric.jac64.emu.cia.CIA1;
import com.dreamfabric.jac64.emu.cia.CIA2;
import com.dreamfabric.jac64.emu.memory.ColorRAM;
import com.dreamfabric.jac64.emu.sid.SIDIf;
import com.dreamfabric.jac64.emu.vic.VICIf;

public class IO extends AddressableVoid {

    private final static int START_ADDRESS = 0xD000;
    private final static int END_ADDRESS = 0xDFFF;

    private SIDIf sid;
    private VICIf vic;
    private CIA1 cia1;
    private CIA2 cia2;
    private ColorRAM colorRAM;

    public void setSid(SIDIf sid) {
        this.sid = sid;
    }

    public void setVic(VICIf vic) {
        this.vic = vic;
    }

    public void setCia1(CIA1 cia1) {
        this.cia1 = cia1;
    }

    public void setCia2(CIA2 cia2) {
        this.cia2 = cia2;
    }

    public void setColorRAM(ColorRAM colorRAM) {
        this.colorRAM = colorRAM;
    }

    public ColorRAM getColorRAM() {
        return colorRAM;
    }

    @Override
    public boolean write(int address, int data, long currentCpuCycles) {
        if (sid.write(address, data, currentCpuCycles)) {
            return true;
        }

        if (vic.write(address, data, currentCpuCycles)) {
            return true;
        }

        if (cia1.write(address, data, currentCpuCycles)) {
            return true;
        }

        if (cia2.write(address, data, currentCpuCycles)) {
            return true;
        }

        if (colorRAM.write(address, data, currentCpuCycles)) {
            return true;
        }

        return false;
    }

    @Override
    public Integer read(int address, long currentCpuCycles) {
        Integer result = null;

        result = sid.read(address, currentCpuCycles);
        if (result != null) {
            return result;
        }

        result = vic.read(address, currentCpuCycles);
        if (result != null) {
            return result;
        }

        result = cia1.read(address, currentCpuCycles);
        if (result != null) {
            return result;
        }

        result = cia2.read(address, currentCpuCycles);
        if (result != null) {
            return result;
        }

        result = colorRAM.read(address, currentCpuCycles);
        if (result != null) {
            return result;
        }

        return null;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        sid.setEnabled(enabled);
        vic.setEnabled(enabled);
        cia1.setEnabled(enabled);
        cia2.setEnabled(enabled);
        colorRAM.setEnabled(enabled);
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
