package com.dreamfabric.jac64.emu.cia;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dreamfabric.jac64.emu.bus.AddressableChip;

public class CIA1 extends AddressableChip implements KeyListener, Joy1KeyListener, Joy2KeyListener {
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
    private Set<Joy1Key> pressedJoy1Keys = new HashSet<Joy1Key>();
    private Set<Joy2Key> pressedJoy2Keys = new HashSet<Joy2Key>();

    /***
     * Hold the value of the current PortA and PortB pin read. The pin read should
     * not be stored in the PRA and PRB registers.
     */
    private int praPinRead;
    private int prbPinRead;

    public CIA1() {
        super();

        write0(CIA1.DDRA, 0x00); // input
        write0(CIA1.PRA, 0xFF); // HIGH
        write0(CIA1.DDRB, 0x00); // input
        write0(CIA1.PRB, 0xFF); // HIGH

        praPinRead = 255;
        prbPinRead = 255;
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

        if (result && address >= 0xdc00 && address <= 0xdc03) {
            // LOGGER.info(String.format("CIA1 write %s = %s", address, data));
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

        if (address >= 0xdc00 && address <= 0xdc03) {
            updateKeyboardState();
            int portB = calculatePortBValeForJoy1();
            int portA = calculatePortAValueForJoy2();

            praPinRead &= portA;
            prbPinRead &= portB;

            if (address == CIA1.PRA) {
                return praPinRead;
            }

            if (address == CIA1.PRB) {
                return prbPinRead;
            }
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

    public Joy1KeyListener getJoy1KeyListener() {
        return this;
    }

    public Joy2KeyListener getJoy2KeyListener() {
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

    @Override
    public void joy1KeyPressed(Joy1Key key) {
        LOGGER.info(String.format("CIA1 Joy1 key pressed %s", key));

        pressedJoy1Keys.add(key);
    }

    @Override
    public void joy1KeyReleased(Joy1Key key) {
        LOGGER.info(String.format("CIA1 Joy1 key released %s", key));

        pressedJoy1Keys.remove(key);
    }

    @Override
    public void joy2KeyPressed(Joy2Key key) {
        LOGGER.info(String.format("CIA1 Joy2 key pressed %s", key));

        pressedJoy2Keys.add(key);
    }

    @Override
    public void joy2KeyReleased(Joy2Key key) {
        LOGGER.info(String.format("CIA1 Joy2 key released %s", key));

        pressedJoy2Keys.remove(key);
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

        praPinRead = portA;
        prbPinRead = portB;
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

    private int calculatePortBValeForJoy1() {
        Iterator<Joy1Key> iterator = pressedJoy1Keys.iterator();

        // joy1 is connected to Port B
        int portB = getPRB();

        while (iterator.hasNext()) {
            Joy1Key key = iterator.next();

            // clear the bit because joy key is pressed
            portB = (portB & (~key.getPortBValue() & 0xFF));
        }

        return portB;
    }

    private int calculatePortAValueForJoy2() {
        Iterator<Joy2Key> iterator = pressedJoy2Keys.iterator();

        // joy2 is connected to Port A
        int portA = getPRA();

        while (iterator.hasNext()) {
            Joy2Key key = iterator.next();

            // clear the bit because joy key is pressed
            portA = (portA & (~key.getPortAValue() & 0xFF));
        }

        return portA;
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
