package com.dreamfabric.jac64.emu.cia;

public enum Joy1Key {
    VK_FIRE(0x10), VK_RIGHT(0x08), VK_LEFT(0x04), VK_DOWN(0x02), VK_UP(0x01);

    private int portBValue;

    private Joy1Key(int portBValue) {
        this.portBValue = portBValue;
    }

    public int getPortBValue() {
        return portBValue;
    }
}
