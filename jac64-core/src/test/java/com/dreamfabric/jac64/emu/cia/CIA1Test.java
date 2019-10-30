package com.dreamfabric.jac64.emu.cia;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class CIA1Test {

    private CIA1 subject;

    @Before
    public void init() {
        subject = new CIA1();
        subject.setEnabled(true);
    }

    /* DEFAULT TESTS FOR PORT A */

    @Test
    public void testPortA_forDefault() {
        assertEquals(0, subject.read(CIA1.DDRA).intValue()); // input
        assertEquals(255, subject.read(CIA1.PRA).intValue()); // all high
    }

    @Test
    public void testPortA_forDefaultWhenInput() {
        subject.write(CIA1.DDRA, 0x00); // input

        assertEquals(255, subject.read(CIA1.PRA).intValue());
    }

    @Test
    public void testPortA_forDefaultWhenOutput() {
        subject.write(CIA1.DDRA, 0xFF); // output

        assertEquals(255, subject.read(CIA1.PRA).intValue());
    }

    @Test
    public void testPortA_whenInputAndAllHigh() {
        subject.write(CIA1.DDRA, 0x00); // input
        subject.write(CIA1.PRA, 255); // all low

        assertEquals(255, subject.read(CIA1.PRA).intValue());
    }

    @Test
    public void testPortA_whenInputAndAllLow() {
        subject.write(CIA1.DDRA, 0x00); // input
        subject.write(CIA1.PRA, 0); // all low

        // what here?
    }

    @Test
    public void testPortA_whenOutputAndAllHigh() {
        subject.write(CIA1.DDRA, 0xFF); // output
        subject.write(CIA1.PRA, 255); // all high

        assertEquals(255, subject.read(CIA1.PRA).intValue());
    }

    @Test
    public void testPortA_whenOutputAndAllLow() {
        subject.write(CIA1.DDRA, 0xFF); // output
        subject.write(CIA1.PRA, 0); // all low

        assertEquals(0, subject.read(CIA1.PRA).intValue());
    }

    /* DEFAULT TESTS FOR PORT B */

    @Test
    public void testPortB_forDefault() {
        assertEquals(0, subject.read(CIA1.DDRB).intValue()); // input
        assertEquals(255, subject.read(CIA1.PRB).intValue()); // all high
    }

    @Test
    public void testPortB_forDefaultWhenInput() {
        subject.write(CIA1.DDRB, 0x00);

        assertEquals(255, subject.read(CIA1.PRB).intValue());
    }

    @Test
    public void testPortB_forDefaultWhenOutput() {
        subject.write(CIA1.DDRB, 0xFF); // output

        assertEquals(255, subject.read(CIA1.PRB).intValue());
    }

    @Test
    public void testPortB_whenInputAndAllHigh() {
        subject.write(CIA1.DDRB, 0x00); // input
        subject.write(CIA1.PRB, 255); // all low

        assertEquals(255, subject.read(CIA1.PRB).intValue());
    }

    @Test
    public void testPortB_whenInputAndAllLow() {
        subject.write(CIA1.DDRB, 0x00); // input
        subject.write(CIA1.PRB, 0); // all low

        // what here?
    }

    @Test
    public void testPortB_whenOutputAndAllHigh() {
        subject.write(CIA1.DDRB, 0xFF); // output
        subject.write(CIA1.PRB, 255); // all high

        assertEquals(255, subject.read(CIA1.PRB).intValue());
    }

    @Test
    public void testPortB_whenOutputAndAllLow() {
        subject.write(CIA1.DDRB, 0xFF); // output
        subject.write(CIA1.PRB, 0); // all low

        assertEquals(0, subject.read(CIA1.PRB).intValue());
    }

    /* TESTS FOR PORTS AND JOYSTICKS */
    /* https://www.c64-wiki.com/wiki/Joystick#BASIC */

    @Test
    public void testPortA_forJoy2WhenInput() {
        subject.write(CIA1.DDRA, 0x00);
        subject.write(CIA1.PRA, 0xFF);
        
        subject.joy2KeyPressed(Joy2Key.VK_UP);
        assertEquals(254, subject.read(CIA1.PRA).intValue());
        subject.joy2KeyReleased(Joy2Key.VK_UP);

        subject.joy2KeyPressed(Joy2Key.VK_DOWN);
        assertEquals(253, subject.read(CIA1.PRA).intValue());
        subject.joy2KeyReleased(Joy2Key.VK_DOWN);

        subject.joy2KeyPressed(Joy2Key.VK_LEFT);
        assertEquals(251, subject.read(CIA1.PRA).intValue());
        subject.joy2KeyReleased(Joy2Key.VK_LEFT);

        subject.joy2KeyPressed(Joy2Key.VK_RIGHT);
        assertEquals(247, subject.read(CIA1.PRA).intValue());
        subject.joy2KeyReleased(Joy2Key.VK_RIGHT);

        subject.joy2KeyPressed(Joy2Key.VK_FIRE);
        assertEquals(239, subject.read(CIA1.PRA).intValue());
        subject.joy2KeyReleased(Joy2Key.VK_FIRE);
    }

    @Test
    public void testPortA_forJoy2WhenOutput() {
        subject.write(CIA1.DDRA, 0xFF);
        subject.write(CIA1.PRA, 0xFF);
        
        subject.joy2KeyPressed(Joy2Key.VK_UP);
        assertEquals(254, subject.read(CIA1.PRA).intValue());
        subject.joy2KeyReleased(Joy2Key.VK_UP);

        subject.joy2KeyPressed(Joy2Key.VK_DOWN);
        assertEquals(253, subject.read(CIA1.PRA).intValue());
        subject.joy2KeyReleased(Joy2Key.VK_DOWN);

        subject.joy2KeyPressed(Joy2Key.VK_LEFT);
        assertEquals(251, subject.read(CIA1.PRA).intValue());
        subject.joy2KeyReleased(Joy2Key.VK_LEFT);

        subject.joy2KeyPressed(Joy2Key.VK_RIGHT);
        assertEquals(247, subject.read(CIA1.PRA).intValue());
        subject.joy2KeyReleased(Joy2Key.VK_RIGHT);

        subject.joy2KeyPressed(Joy2Key.VK_FIRE);
        assertEquals(239, subject.read(CIA1.PRA).intValue());
        subject.joy2KeyReleased(Joy2Key.VK_FIRE);
    }

    @Test
    public void testPortB_forJoy1WhenInput() {
        subject.write(CIA1.DDRB, 0x00);
        subject.write(CIA1.PRB, 0xFF);
        
        subject.joy1KeyPressed(Joy1Key.VK_UP);
        assertEquals(254, subject.read(CIA1.PRB).intValue());
        subject.joy1KeyReleased(Joy1Key.VK_UP);

        subject.joy1KeyPressed(Joy1Key.VK_DOWN);
        assertEquals(253, subject.read(CIA1.PRB).intValue());
        subject.joy1KeyReleased(Joy1Key.VK_DOWN);

        subject.joy1KeyPressed(Joy1Key.VK_LEFT);
        assertEquals(251, subject.read(CIA1.PRB).intValue());
        subject.joy1KeyReleased(Joy1Key.VK_LEFT);

        subject.joy1KeyPressed(Joy1Key.VK_RIGHT);
        assertEquals(247, subject.read(CIA1.PRB).intValue());
        subject.joy1KeyReleased(Joy1Key.VK_RIGHT);

        subject.joy1KeyPressed(Joy1Key.VK_FIRE);
        assertEquals(239, subject.read(CIA1.PRB).intValue());
        subject.joy1KeyReleased(Joy1Key.VK_FIRE);
    }

    @Test
    public void testPortB_forJoy1WhenOutput() {
        subject.write(CIA1.DDRB, 0xFF);
        subject.write(CIA1.PRB, 0xFF);
        
        subject.joy1KeyPressed(Joy1Key.VK_UP);
        assertEquals(254, subject.read(CIA1.PRB).intValue());
        subject.joy1KeyReleased(Joy1Key.VK_UP);

        subject.joy1KeyPressed(Joy1Key.VK_DOWN);
        assertEquals(253, subject.read(CIA1.PRB).intValue());
        subject.joy1KeyReleased(Joy1Key.VK_DOWN);

        subject.joy1KeyPressed(Joy1Key.VK_LEFT);
        assertEquals(251, subject.read(CIA1.PRB).intValue());
        subject.joy1KeyReleased(Joy1Key.VK_LEFT);

        subject.joy1KeyPressed(Joy1Key.VK_RIGHT);
        assertEquals(247, subject.read(CIA1.PRB).intValue());
        subject.joy1KeyReleased(Joy1Key.VK_RIGHT);

        subject.joy1KeyPressed(Joy1Key.VK_FIRE);
        assertEquals(239, subject.read(CIA1.PRB).intValue());
        subject.joy1KeyReleased(Joy1Key.VK_FIRE);
    }

    /***
     * 
     * Abacus Software Peeks & Pokes for the Commodore 64
     *
     * If the RUN/STOP key is not turned off, then you can use location 145 for a
     * another purpose. This is where the operating system produces a copy of
     * location 56321, so joystick port 1 can also be read with PEEK (145). It’s a
     * bit more complicated with joysitck port 2. It’s in memory cell 56320, but
     * this location is really designed for column selection (i.e., output), and
     * joystick reading is dependent upon input. Therefore, this port must be
     * switched to the CIA; you can do this with POKE 56322,224. This command does
     * the following:
     *
     * 1) Memory location 56320 now reads the joystick movement in 56321;
     *
     * 2) the keyboard is turned off, which can be rectified by either
     * RUN/STOP-RESTORE or POKE 56322,255.
     *
     * Joystick operation is simple. Basically, it consists of five or fewer
     * separate switches. One is used for the fire button, while the remainder are
     * underneath the stick handle, set in four different directions. The position
     * of the stick turns on the corresponding key—and the 64 notes that in the
     * proper memory locations.
     * 
     * SUMMARY: Joysticks
     *
     * Read joystick port 1: PEEK (56321)
     *
     * Location 56320 must contain 127 to do so
     *
     * Read joystick port 2: POKE 56322,224: REM PORT SWITCHED
     *
     * PEEK (56320)
     *
     * @see https://archive.org/stream/peeks-and-pokes-for-the-commodore-64/PeeksAndPokesForTheCommodore64_djvu.txt
     * @see https://www.c64-wiki.com/wiki/Joystick#BASIC
     */
    @Test
    public void testPortA_forJoy2_whenKeyboardPartiallyDisabled() {
        subject.write(CIA1.DDRA, 224); // output and input
        subject.write(CIA1.PRA, 127);

        subject.joy2KeyPressed(Joy2Key.VK_UP);
        assertEquals(126, subject.read(CIA1.PRA).intValue());
        subject.joy2KeyReleased(Joy2Key.VK_UP);

        subject.joy2KeyPressed(Joy2Key.VK_DOWN);
        assertEquals(125, subject.read(CIA1.PRA).intValue());
        subject.joy2KeyReleased(Joy2Key.VK_DOWN);

        subject.joy2KeyPressed(Joy2Key.VK_LEFT);
        assertEquals(123, subject.read(CIA1.PRA).intValue());
        subject.joy2KeyReleased(Joy2Key.VK_LEFT);

        subject.joy2KeyPressed(Joy2Key.VK_RIGHT);
        assertEquals(119, subject.read(CIA1.PRA).intValue());
        subject.joy2KeyReleased(Joy2Key.VK_RIGHT);

        subject.joy2KeyPressed(Joy2Key.VK_FIRE);
        assertEquals(111, subject.read(CIA1.PRA).intValue());
        subject.joy2KeyReleased(Joy2Key.VK_FIRE);
    }
    
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
        subject.write(CIA1.PRB, 0b11011111); // testing column 5 (COL5) of the matrix

        // perform a click of S key
        subject.keyPressed(Key.VK_S);
        // check if PortA contains a correct value
        assertEquals(0b11111101, subject.read(CIA1.PRA).intValue());

        // release S key
        subject.keyReleased(Key.VK_S);
        // check if PortB contains a correct value
        assertEquals(0b11111111, subject.read(CIA1.PRA).intValue());
    }

    /* TEST RICK DANGEROUS REAL LIFE SCENARIO */
    @Test
    public void testPortA_forJoy2_forRickDangerous2CreditsScreen_whenNoKeyPressed() {
        subject.write(CIA1.DDRA, 0xFF); // PortA as output
        subject.write(CIA1.DDRB, 0x00); // PortB as input
        subject.write(CIA1.PRA, 127);
        subject.write(CIA1.PRB, 251);

        assertEquals(0b01111111, subject.read(CIA1.PRA).intValue());
    }

    @Test
    public void testPortA_forJoy2_forRickDangerous2CreditsScreen_whenFirePressed() {
        // configure Port A and B
        subject.write(CIA1.DDRA, 0xFF); // PortA as output
        subject.write(CIA1.DDRB, 0x00); // PortB as input
        subject.write(CIA1.PRA, 127);
        subject.write(CIA1.PRB, 251);

        subject.joy2KeyPressed(Joy2Key.VK_FIRE);
        assertEquals(111, subject.read(CIA1.PRA).intValue());
    }

    @Test
    public void testPortB_forJoy1_forRickDangerous2CreditsScreen_whenNoKeyPressed() {
        subject.write(CIA1.DDRB, 0xFF); // PortA as output
        subject.write(CIA1.DDRA, 0x00); // PortB as input
        subject.write(CIA1.PRB, 127);
        subject.write(CIA1.PRA, 251);

        assertEquals(0b01111111, subject.read(CIA1.PRB).intValue());
    }

    @Test
    public void testPortB_forJoy1_forRickDangerous2CreditsScreen_whenFirePressed() {
        subject.write(CIA1.DDRB, 0xFF); // PortA as output
        subject.write(CIA1.DDRA, 0x00); // PortB as input
        subject.write(CIA1.PRB, 127);
        subject.write(CIA1.PRA, 251);

        subject.joy1KeyPressed(Joy1Key.VK_FIRE);
        assertEquals(111, subject.read(CIA1.PRB).intValue());
    }
}
