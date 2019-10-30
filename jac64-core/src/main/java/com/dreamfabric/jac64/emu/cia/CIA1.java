package com.dreamfabric.jac64.emu.cia;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dreamfabric.jac64.emu.bus.AddressableChip;

public class CIA1 extends AddressableChip implements KeyListener {
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

    private Set<Key> pressedKeys = new HashSet<Key>();

    public CIA1() {
        super();
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

        if (result) {
            // LOGGER.info(String.format("CIA1 write %s = %s", address, data));
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

        if (address >= 0xdc00 && address <= 0xdc03) {
            updateKeyboardState();

            // read it again as the value could change
            result = read0(address);
        }

        if (result != null) {
            // LOGGER.info(String.format("CIA1 read %s = %s", address, result));
        }

        if (result != null && address >= 0xdc00 && address <= 0xdc03) {
            // for now return the correct result only for those registers: keypad and
            // joysticks
            return result;
        }

        // just for sniffing return null
        return null;
    }

    public KeyListener getKeyListener() {
        return this;
    }

    @Override
    public void keyPressed(Key key) {
        LOGGER.info(String.format("CIA1 key pressed %s", key));

        pressedKeys.add(key);
    }

    @Override
    public void keyReleased(Key key) {
        LOGGER.info(String.format("CIA1 key released %s", key));

        pressedKeys.remove(key);
    }

    private void updateKeyboardState() {
        Iterator<Key> iterator = pressedKeys.iterator();

        // set 1 on all PortB inputs
        int portB = getPRB() | (~getDDRB() & 0xFF);
        // set 1 on all PortA inputs
        int portA = getPRA() | (~getDDRA() & 0xFF);

        while (iterator.hasNext()) {
            Key key = iterator.next();

            portB = transferFromPortAtoPortB(key, portB);
            portA = transferFromPortBtoPortA(key, portA);
        }

        write0(PRB, portB);
        write0(PRA, portA);
    }

    private int transferFromPortAtoPortB(Key key, int currentPortB) {
        if ((getDDRA() & key.getPortAValue()) != 0) {
            // port A for the specified key is OUTPUT
            if ((getDDRB() & key.getPortBValue()) == 0) {
                // port B for the specified key is INPUT

                if ((getPRA() & key.getPortAValue()) == 0) {
                    // PortA contains 0 on the specified pin.
                    // Need to clear the specific bit.
                    return (currentPortB & (~key.getPortBValue() & 0xFF));
                } else {
                    // PortA contains 1 on the specified pin
                    // Need to set the specific bit.
                    return (currentPortB | key.getPortBValue());
                }
            }
        }

        return currentPortB;
    }

    private int transferFromPortBtoPortA(Key key, int currentPortA) {
        if ((getDDRB() & key.getPortBValue()) != 0) {
            // port B for the specified key is OUTPUT
            if ((getDDRA() & key.getPortAValue()) == 0) {
                // port A for the specified key is INPUT

                if ((getPRB() & key.getPortBValue()) == 0) {
                    // PortB contains 0 on the specified pin.
                    // Need to clear the specific bit.
                    return (currentPortA & (~key.getPortAValue() & 0xFF));
                } else {
                    // PortB contains 1 on the specified pin
                    // Need to set the specific bit.
                    return (currentPortA | key.getPortAValue());
                }
            }
        }

        return currentPortA;
    }

    private int getDDRA() {
        return read0(DDRA);
    }

    private int getDDRB() {
        return read0(DDRB);
    }

    private int getPRA() {
        return read0(PRA);
    }

    private int getPRB() {
        return read0(PRB);
    }
}
