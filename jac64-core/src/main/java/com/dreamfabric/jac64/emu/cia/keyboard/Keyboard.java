package com.dreamfabric.jac64.emu.cia.keyboard;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Keyboard implements KeyListener, Joy1KeyListener, Joy2KeyListener {
    private static Logger LOGGER = LoggerFactory.getLogger(Keyboard.class);

    private Set<Key> pressedKeys = new HashSet<Key>();
    private Set<Joy1Key> pressedJoy1Keys = new HashSet<Joy1Key>();
    private Set<Joy2Key> pressedJoy2Keys = new HashSet<Joy2Key>();

    private int pra;
    private int prb;
    private int ddra;
    private int ddrb;

    /***
     * Hold the value of the current PortA and PortB pin read. The pin read should
     * not be stored in the PRA and PRB registers.
     */
    private int praPinRead;
    private int prbPinRead;

    public Keyboard() {
        setDDRA(0x00);
        setPRA(0xFF);
        setDDRB(0x00);
        setPRB(0xFF);

        praPinRead = 255;
        prbPinRead = 255;
    }

    public void setPRA(int pra) {
        this.pra = pra;
    }

    public void setPRB(int prb) {
        this.prb = prb;
    }

    public void setDDRA(int ddra) {
        this.ddra = ddra;
    }

    public void setDDRB(int ddrb) {
        this.ddrb = ddrb;
    }

    public int getPRAPin() {
        updateKeyboardState();
        int portA = calculatePortAValueForJoy2();

        praPinRead &= portA;

        return praPinRead;
    }

    public int getPRBPin() {
        updateKeyboardState();
        int portB = calculatePortBValeForJoy1();

        prbPinRead &= portB;

        return prbPinRead;
    }

    public int getDDRA() {
        return ddra;
    }

    public int getDDRB() {
        return ddrb;
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

    private int getPRA() {
        return pra;
    }

    private int getPRB() {
        return prb;
    }
}
