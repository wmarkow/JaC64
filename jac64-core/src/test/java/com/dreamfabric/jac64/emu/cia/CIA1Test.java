package com.dreamfabric.jac64.emu.cia;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CIA1Test {

    /***
     * https://www.c64-wiki.com/wiki/Keyboard#direct_addressing_of_a_key
     */
    @Test
    public void testKeyboardForS_fromtAtoB() {
        CIA1 subject = new CIA1();
        subject.setEnabled(true);

        // configure ports A and B
        subject.write(CIA1.DDRA, 0xFF); // CIA#1 port A = outputs
        subject.write(CIA1.DDRB, 0x00); // CIA#1 port B = inputs
        // put a sequence for port A
        subject.write(CIA1.PRA, 0b1111101); // testing row 1 (ROW1) of the matrix

        // press S key
        subject.keyPressed(Key.VK_S);
        // check if PortB contains a correct value
        assertEquals(0b11011111, subject.read(CIA1.PRB).intValue());

        // release S key
        subject.keyReleased(Key.VK_S);
        // check if PortB contains a correct value
        assertEquals(0b11111111, subject.read(CIA1.PRB).intValue());
    }

    @Test
    public void testKeyboardForS_fromBtoA() {
        CIA1 subject = new CIA1();
        subject.setEnabled(true);

        // configure ports A and B
        subject.write(CIA1.DDRB, 0xFF); // CIA#1 port B = outputs
        subject.write(CIA1.DDRA, 0x00); // CIA#1 port A = inputs
        // put a sequence for port B
        subject.write(CIA1.PRA, 0b11011111); // testing column 5 (COL5) of the matrix

        // perform a click of S key
        subject.keyPressed(Key.VK_S);
        // check if PortA contains a correct value
        assertEquals(0b11111101, subject.read(CIA1.PRA).intValue());

        // release S key
        subject.keyReleased(Key.VK_S);
        // check if PortB contains a correct value
        assertEquals(0b11111111, subject.read(CIA1.PRA).intValue());
    }

    @Test
    public void testKeyboardWhenNoKeyPressed() {
        CIA1 subject = new CIA1();
        subject.setEnabled(true);

        // configure ports A and B
        subject.write(CIA1.DDRA, 0xFF); // CIA#1 port A = outputs
        subject.write(CIA1.DDRB, 0b10101010); // CIA#1 port B = mix of outputs and inputs

        // all input pins of PortB should be HIGH
        assertEquals(0b01010101, subject.read(CIA1.PRB).intValue() & ~0b10101010);
    }
}
