package com.dreamfabric.jac64.emu.cia;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dreamfabric.jac64.emu.bus.AddressableChip;

public class CIA1 extends AddressableChip {
    private static Logger LOGGER = LoggerFactory.getLogger(CIA1.class);

    public final static int START_ADDRESS = 0xDC00;
    public final static int END_ADDRESS = 0xDCFF;

    static final int PRA = 0x00 + START_ADDRESS;
    static final int PRB = 0x01 + START_ADDRESS;
    static final int DDRA = 0x02 + START_ADDRESS;
    static final int DDRB = 0x03 + START_ADDRESS;
    private static final int TIMALO = 0x04 + START_ADDRESS;
    private static final int TIMAHI = 0x05 + START_ADDRESS;
    private static final int TIMBLO = 0x06 + START_ADDRESS;
    private static final int TIMBHI = 0x07 + START_ADDRESS;
    private static final int TODTEN = 0x08 + START_ADDRESS;
    private static final int TODSEC = 0x09 + START_ADDRESS;
    private static final int TODMIN = 0x0a + START_ADDRESS;
    private static final int TODHRS = 0x0b + START_ADDRESS;
    private static final int SDR = 0x0c + START_ADDRESS;
    private static final int ICR = 0x0d + START_ADDRESS;
    private static final int CRA = 0x0e + START_ADDRESS;
    private static final int CRB = 0x0f + START_ADDRESS;

    private Keyboard keyboard;

    public CIA1() {
        super();

        write0(CIA1.DDRA, 0x00); // input
        write0(CIA1.PRA, 0xFF); // HIGH
        write0(CIA1.DDRB, 0x00); // input
        write0(CIA1.PRB, 0xFF); // HIGH

        keyboard = new Keyboard();
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
    public boolean write(int address, int data) {
        boolean result = super.write(address, data);

        if (result == false) {
            return false;
        }

        switch (address) {
            case PRA:
                keyboard.setPra(data);
                return true;
            case PRB:
                keyboard.setPrb(data);
                return true;
            case DDRA:
                keyboard.setDdra(data);
                return true;
            case DDRB:
                keyboard.setDdrb(data);
                return true;
        }

        // just for sniffing return false
        return false;
    }

    @Override
    public Integer read(int address) {
        Integer result = super.read(address);
        if (result == null) {
            return null;
        }

        switch (address) {
            case PRA:
                return keyboard.getPraPin();
            case PRB:
                return keyboard.getPrbPin();
            case DDRA:
                return result;
            case DDRB:
                return result;
        }

        // just for sniffing return null
        return null;
    }

    public KeyListener getKeyListener() {
        return keyboard;
    }

    public Joy1KeyListener getJoy1KeyListener() {
        return keyboard;
    }

    public Joy2KeyListener getJoy2KeyListener() {
        return keyboard;
    }

}
