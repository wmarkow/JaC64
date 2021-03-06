/**
 * This file is a part of JaC64 - a Java C64 Emulator
 * Main Developer: Joakim Eriksson (Dreamfabric.com)
 * Contact: joakime@sics.se
 * Web: http://www.dreamfabric.com/c64
 * ---------------------------------------------------
 */

package com.dreamfabric.jac64.emu.vic;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.awt.image.MemoryImageSource;

import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dreamfabric.jac64.C64Canvas;
import com.dreamfabric.jac64.IMonitor;
import com.dreamfabric.jac64.emu.bus.AddressableBus;
import com.dreamfabric.jac64.emu.bus.AddressableChip;
import com.dreamfabric.jac64.emu.bus.ControlBus;
import com.dreamfabric.jac64.emu.cia.CIA1;
import com.dreamfabric.jac64.emu.cia.CIA2;
import com.dreamfabric.jac64.emu.cpu.MOS6510Core;
import com.dreamfabric.jac64.emu.interrupt.InterruptManager;

/**
 * Implements the VIC chip + some other HW
 *
 * @author Joakim Eriksson (joakime@sics.se) / main developer, still active
 * @author Jan Blok (jblok@profdata.nl) / co-developer during ~2001
 * @version $Revision: 1.11 $, $Date: 2006/05/02 16:26:26 $
 */

public class C64Screen extends AddressableChip implements VICIf, MouseMotionListener {
    private static Logger LOGGER = LoggerFactory.getLogger(C64Screen.class);

    private final static int START_ADDRESS = 0xD000;
    private final static int END_ADDRESS = 0xD3FF;

    public static final String version = "1.11";

    public static final int IO_UPDATE = 37;
    // This is PAL speed! - will be called each scan line...

    private static final int VIC_IRQ = 1;

    // This might be solved differently later!!!
    public static final int CYCLES_PER_LINE = VICConstants.SCAN_RATE;

    // ALoow the IO to write in same as RAM
    public static final boolean SOUND_AVAIABLE = true;

    public static final Color TRANSPARENT_BLACK = new Color(0, 0, 0x40, 0x40);
    public static final Color DARKER_0 = new Color(0, 0, 0x40, 0x20);
    public static final Color LIGHTER_0 = new Color(0xe0, 0xe0, 0xff, 0x30);
    public static final Color DARKER_N = new Color(0, 0, 0x40, 0x70);
    public static final Color LIGHTER_N = new Color(0xe0, 0xe0, 0xff, 0xa0);

    public static final Color LED_ON = new Color(0x60, 0xdf, 0x60, 0xc0);
    public static final Color LED_OFF = new Color(0x20, 0x60, 0x20, 0xc0);
    public static final Color LED_BORDER = new Color(0x40, 0x60, 0x40, 0xa0);

    public static final int LABEL_COUNT = 32;
    private Color[] darks = new Color[LABEL_COUNT];
    private Color[] lites = new Color[LABEL_COUNT];
    private int colIndex = 0;

    // This is the screen width and height used...
    private final static int SC_WIDTH = 384; // 403;
    private final static int SC_HEIGHT = 284;
    private final int SC_XOFFS = 32;
    // Done: this should be - 24!
    private final int SC_SPXOFFS = SC_XOFFS - 24;
    private final int FIRST_VISIBLE_VBEAM = 15;
    private final int SC_SPYOFFS = FIRST_VISIBLE_VBEAM + 1;

    private IMonitor monitor;

    private boolean DOUBLE = false;
    private int reset = 100;
    private C64Canvas canvas;

    private int[] cbmcolor = VICConstants.COLOR_SETS[0];

    // -------------------------------------------------------------------
    // VIC-II variables
    // -------------------------------------------------------------------
    public int charSetBaseAddress; // address of character's glyphs definitions. It can be read from $1000 of
                                   // Character ROM
    public int videoMatrixBaseAddress; // address of the screen matrix 25x40 (rows x cols): range 1024-2023
                                       // ($0400-$07E7)
    public int videoMode;

    // VIC Registers
    int irqMask = 0;
    int irqFlags = 0;
    int control1 = 0;
    int control2 = 0;
    int sprXMSB = 0;
    int sprEN = 0;
    int sprYEX = 0;
    int sprXEX = 0;
    int sprPri = 0;
    int sprMul = 0;
    int sprCol = 0;
    int sprBgCol = 0;
    int sprMC0 = 0;
    int sprMC1 = 0;
    int vicMemoryPointers = 0;
    int vicMemDDRA = 0;
    int vicMemDATA = 0;
    // Read for debugging on other places...
    public int vbeam = 0; // read at d012
    public int raster = 0;
    int bCol = 0;
    int bgCol[] = new int[4];

    private int vicBaseAddress = 0;
    private boolean badLine = false;
    private int spr0BlockSel;

    // New type of position in video matrix - Video Counter (VIC II docs)
    int vc = 0;
    int vcBase = 0;
    int rc = 0;
    int vmli = 0;
    // The current vBeam pos - 9... => used for keeping track of memory
    // position to write to...
    int vPos = 0;
    int mpos = 0;

    int displayWidth = SC_WIDTH;
    int displayHeight = SC_HEIGHT;
    int offsetX = 0;
    int offsetY = 0;

    // Cached variables...
    boolean gfxVisible = false;
    boolean paintBorder = false;
    boolean paintSideBorder = false;

    int borderColor = cbmcolor[0];
    int bgColor = cbmcolor[1];

    private boolean extended = false;
    private boolean multiCol = false;
    private boolean blankRow = false;
    private boolean hideColumn = false;

    int multiColor[] = new int[4];

    // 48 extra for the case of an expanded sprite byte
    int collissionMask[] = new int[SC_WIDTH + 48];

    Sprite sprites[] = new Sprite[8];

    private Color colors[] = null;

    private int horizScroll = 0;
    private int vScroll = 0;

    private Image image;
    private Graphics g2;

    // Caching all 40 chars (or whatever) each "bad-line"
    private int[] vicCharCache = new int[40];// this is the whole current row with 40 characters (columns) inside
    private int[] vicColCache = new int[40];// this is the whole current row with 40 characters colors (columns) inside

    public Image screen = null;
    private MemoryImageSource mis = null;

    // The array to generate the screen in Extra rows for sprite clipping
    // And for clipping when scrolling (smooth)
    int mem[] = new int[SC_WIDTH * (SC_HEIGHT + 10)];

    int rnd = 754;
    String message;
    String tmsg = "";

    int frame = 0;
    private boolean updating = false;
    boolean displayEnabled = true;
    boolean irqTriggered = false;
    long lastLine = 0;
    long firstLine = 0;

    int potx = 0;
    int poty = 0;

    // This variable changes when Kernal has installed
    // a working ISR that is reading the keyboard
    private boolean isrRunning = false;

    private AddressableBus addressableBus;
    private ControlBus controlBus;

    public C64Screen(IMonitor m, boolean dob) {
        monitor = m;
        DOUBLE = dob;

        makeColors(darks, DARKER_0, DARKER_N);
        makeColors(lites, LIGHTER_0, LIGHTER_N);
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
    public void start(long currentCpuCycles) {

    }

    public void setAutoscale(boolean val) {
        DOUBLE = val;
        canvas.setAutoscale(val);
    }

    public void setAddressableBus(AddressableBus addressableBus) {
        this.addressableBus = addressableBus;
    }

    private void makeColors(Color[] colors, Color c1, Color c2) {
        int a0 = c1.getAlpha();
        int r0 = c1.getRed();
        int g0 = c1.getGreen();
        int b0 = c1.getBlue();
        int an = c2.getAlpha();
        int rn = c2.getRed();
        int gn = c2.getGreen();
        int bn = c2.getBlue();
        int lc = LABEL_COUNT / 2;
        for (int i = 0, n = lc; i < n; i++) {
            colors[i] = colors[LABEL_COUNT - i - 1] = new Color(((lc - i) * r0 + (i * rn)) / lc,
                    ((lc - i) * g0 + (i * gn)) / lc, ((lc - i) * b0 + (i * bn)) / lc, ((lc - i) * a0 + (i * an)) / lc);
        }
    }

    public void setColorSet(int c) {
        if (c >= 0 && c < VICConstants.COLOR_SETS.length) {
            cbmcolor = VICConstants.COLOR_SETS[c];
            borderColor = cbmcolor[bCol];
            bgColor = cbmcolor[bgCol[0]];
            for (int i = 0, n = 8; i < n; i++) {
                sprites[i].color[0] = bgColor;
                sprites[i].color[1] = cbmcolor[sprMC0];
                sprites[i].color[3] = cbmcolor[sprMC1];
            }
        }
    }

    public void setIntegerScaling(boolean yes) {
        canvas.setIntegerScaling(yes);
    }

    public JPanel getScreen() {
        return canvas;
    }

    public boolean ready() {
        return isrRunning;
    }

    public void setDisplayFactor(double f) {
        displayWidth = (int) (SC_WIDTH * f);
        displayHeight = (int) (SC_HEIGHT * f);
        crtImage = null;
    }

    public void setDisplayOffset(int x, int y) {
        offsetX = x;
        offsetY = y;
    }

    public void dumpGfxStat() {
        monitor.info("Char CharSetBaseAddress: 0x" + Integer.toString(charSetBaseAddress, 16));
        monitor.info("CharSet adr: 0x" + Integer.toString(charSetBaseAddress, 16));
        monitor.info("VideoMode: " + videoMode);
        monitor.info("Video Matrix: 0x" + Integer.toString(videoMatrixBaseAddress, 16));

        monitor.info("Text: extended = " + extended + " multicol = " + multiCol);

        monitor.info("24 Rows on? " + (((control1 & 0x08) == 0) ? "yes" : "no"));

        monitor.info("YScroll = " + (control1 & 0x7));
        monitor.info("$d011 = " + control1);

        monitor.info("IRQ Latch: " + Integer.toString(irqFlags, 16));
        monitor.info("IRQ  Mask: " + Integer.toString(irqMask, 16));
        monitor.info("IRQ RPos : " + raster);

        for (int i = 0, n = 8; i < n; i++) {
            monitor.info("Sprite " + (i + 1) + " pos = " + sprites[i].x + ", " + sprites[i].y);
        }
    }

    public void init(ControlBus controlBus) {
        this.controlBus = controlBus;

        for (int i = 0, n = sprites.length; i < n; i++) {
            sprites[i] = new Sprite();
            sprites[i].spriteNo = i;
        }

        canvas = new C64Canvas(this, DOUBLE, controlBus);
        canvas.addMouseMotionListener(this);

        for (int i = 0; i < SC_WIDTH * SC_HEIGHT; i++) {
            mem[i] = cbmcolor[6];
        }

        mis = new MemoryImageSource(SC_WIDTH, SC_HEIGHT, mem, 0, SC_WIDTH);

        mis.setAnimated(true);
        mis.setFullBufferUpdates(true);
        screen = canvas.createImage(mis);

        // to fix bug
        // http://developer.java.sun.com/developer/bugParade/bugs/4464723.html
        // setRequestFocusEnabled(true);
        // addMouseListener(new MouseAdapter() {
        // public void mousePressed(MouseEvent e) {
        // requestFocus();
        // }
        // });
        initUpdate();
    }

    public void restoreKey(boolean down) {
        if (down)
            getControlBus().setNMI(InterruptManager.KEYBOARD_NMI);
        else
            getControlBus().clearNMI(InterruptManager.KEYBOARD_NMI);
    }

    // Should be checked up!!!
    private static final int[] IO_ADDRAND = new int[] { 0xd03f, 0xd03f, 0xd03f, 0xd03f, 0xd41f, 0xd41f, 0xd41f, 0xd41f,
            0xd8ff, 0xd9ff, 0xdaff, 0xdbff, // Color ram
            0xdc0f, 0xdd0f, 0xdeff, 0xdfff, // CIA + Expansion...
    };

    @Override
    public Integer read(int address, long currentCpuCycles) {

        // dX00 => and address
        // d000 - d3ff => &d063
        int pos = (address >> 8) & 0xf;
        int originalAddress = address;

        Integer result = super.read(address, currentCpuCycles);
        if (result == null) {
            return null;
        }

        // monitor.info("Address before: " + address);
        address = address & IO_ADDRAND[pos];
        int val = 0;
        switch (address) {
            case 0xd000:
            case 0xd002:
            case 0xd004:
            case 0xd006:
            case 0xd008:
            case 0xd00a:
            case 0xd00c:
            case 0xd00e:
                return sprites[(address - 0xd000) >> 1].x & 0xff;
            case 0xd001:
            case 0xd003:
            case 0xd005:
            case 0xd007:
            case 0xd009:
            case 0xd00b:
            case 0xd00d:
            case 0xd00f:
                return sprites[(address - 0xd000) >> 1].y;
            case 0xd010:
                return sprXMSB;
            case 0xd011:
                return control1 & 0x7f | ((vbeam & 0x100) >> 1);
            case 0xd012:
                return vbeam & 0xff;
            // Sprite collission registers - zeroed after read!
            case 0xd013:
            case 0xd014:
                // Lightpen x/y
                return 0;
            case 0xd015:
                return sprEN;
            case 0xd016:
                return control2;
            case 0xd017:
                return sprYEX;
            case 0xd018:
                return vicMemoryPointers;
            case 0xd019:
                return irqFlags;
            case 0xd01a:
                return irqMask;
            case 0xd01b:
                return sprPri;
            case 0xd01c:
                return sprMul;
            case 0xd01d:
                return sprXEX;
            case 0xd01e:
                val = sprCol;
                LOGGER.debug("Reading sprite collission: " + Integer.toString(address, 16) + " => " + val);
                sprCol = 0;
                return val;
            case 0xd01f:
                val = sprBgCol;
                LOGGER.debug("Reading sprite collission: " + Integer.toString(address, 16) + " => " + val);

                sprBgCol = 0;
                return val;
            case 0xd020:
                return bCol | 0xf0;
            case 0xd021:
            case 0xd022:
            case 0xd023:
            case 0xd024:
                return bgCol[address - 0xd021] | 0xf0;
            case 0xd025:
                return sprMC0 | 0xf0;
            case 0xd026:
                return sprMC1 | 0xf0;
            case 0xd027:
            case 0xd028:
            case 0xd029:
            case 0xd02a:
            case 0xd02b:
            case 0xd02c:
            case 0xd02d:
            case 0xd02e:
                return sprites[address - 0xd027].col | 0xf0;
            case 0xd419:
                return potx;
            case 0xd41A:
                return poty;
            default:
                if (pos >= 0x8) {
                    // this looks weired
                    throw new IllegalArgumentException(
                            String.format("Weired read from originalAddress = 0x%05X, changedAddress = 0x%05X",
                                    originalAddress, address));
                }
                return 0xff;
        }
    }

    @Override
    public boolean write(int address, int data, long currentCpuCycles) {
        if (super.write(address, data, currentCpuCycles) == false) {

            return false;
        }

        int pos = (address >> 8) & 0xf;
        int originalAddress = address;
        address = address & IO_ADDRAND[pos];

        switch (address) {
            // -------------------------------------------------------------------
            // VIC related
            // -------------------------------------------------------------------
            case 0xd000:
            case 0xd002:
            case 0xd004:
            case 0xd006:
            case 0xd008:
            case 0xd00a:
            case 0xd00c:
            case 0xd00e:
                int sprite = (address - 0xd000) >> 1;
                sprites[sprite].x &= 0x100;
                sprites[sprite].x += data;
                break;
            case 0xd001:
            case 0xd003:
            case 0xd005:
            case 0xd007:
            case 0xd009:
            case 0xd00b:
            case 0xd00d:
            case 0xd00f:
                sprites[(address - 0xd000) >> 1].y = data;
                // System.out.println("Setting sprite " + (address - 0xd000)/2 + " to " + data);
                break;
            case 0xd010:
                sprXMSB = data;
                for (int i = 0, m = 1, n = 8; i < n; i++, m = m << 1) {
                    sprites[i].x &= 0xff;
                    sprites[i].x |= (data & m) != 0 ? 0x100 : 0;
                }
                break;
            // d011 -> high address of raster pos
            case 0xd011:
                raster = (raster & 0xff) | ((data << 1) & 0x100);
                control1 = data;
                // monitor.info("Setting blank: " +
                // ((memory[ + 0xd011] & 0x08) == 0) +
                // " at " + vbeam);

                if (vScroll != (data & 7)) {
                    // update vScroll and badLine!
                    vScroll = data & 0x7;
                    boolean oldBadLine = badLine;
                    badLine = (displayEnabled && vbeam >= 0x30 && vbeam <= 0xf7) && (vbeam & 0x7) == vScroll;
                    if (oldBadLine != badLine) {
                        LOGGER.debug("#### BadLC diff@" + vbeam + " => " + badLine + " vScroll: " + vScroll + " vmli: "
                                + vmli + " vc: " + vc + " rc: " + rc);
                    }
                }

                extended = (data & 0x40) != 0;
                blankRow = (data & 0x08) == 0;

                // 000 => normal text, 001 => multicolor text
                // 010 => extended text, 011 => illegal mode...
                // 100 => hires gfx, 101 => multi hires
                // 110, 111 => ?
                videoMode = (extended ? 0x02 : 0) | (multiCol ? 0x01 : 0) | (((data & 0x20) != 0) ? 0x04 : 0x00);

                // System.out.println("Extended set to: " + extended + " at " +
                // vbeam + " d011: " + Hex.hex2(data));

                LOGGER.debug("d011 = " + data + " at " + vbeam + " => YScroll = " + (data & 0x7));
                LOGGER.debug("Setting raster position (hi) to: " + (data & 0x80));

                break;

            // d012 -> raster position
            case 0xd012:
                raster = (raster & 0x100) | data;
                LOGGER.debug("Setting Raster Position (low) to " + data);
                break;
            case 0xd013:
            case 0xd014:
                // Write to lightpen...
                break;
            case 0xd015:
                sprEN = data;
                for (int i = 0, m = 1, n = 8; i < n; i++, m = m << 1) {
                    sprites[i].enabled = (data & m) != 0;
                }
                // System.out.println("Setting sprite enable to " + data);

                break;
            case 0xd016:
                control2 = data;
                horizScroll = data & 0x7;
                multiCol = (data & 0x10) != 0;

                // if (hideColumn != ((data & 0x08) == 0)) {
                // System.out.println("38 chars on: " + hideColumn + " at " + vbeam + " cycle: "
                // +
                // (cycles - lastLine) + " borderstate:" + borderState);
                // }

                hideColumn = (data & 0x08) == 0;

                // Set videmode...
                videoMode = (extended ? 0x02 : 0) | (multiCol ? 0x01 : 0) | (((control1 & 0x20) != 0) ? 0x04 : 0x00);

                // System.out.println("HorizScroll set to: " + horizScroll + " at "
                // + vbeam);

                // System.out.println("MultiColor set to: " + multiCol + " at " + vbeam);
                break;

            case 0xd017:
                sprYEX = data;
                for (int i = 0, m = 1, n = 8; i < n; i++, m = m << 1) {
                    sprites[i].expandY = (data & m) != 0;
                }
                break;

            case 0xd018:
                vicMemoryPointers = data;
                setVideoMem();
                break;

            case 0xd019: {
                if ((data & 0x80) != 0)
                    data = 0xff;
                int latchval = 0xff ^ data;
                LOGGER.debug("Latching VIC-II: " + Integer.toString(data, 16) + " on " + Integer.toString(irqFlags, 16)
                        + " latch: " + Integer.toString(latchval, 16));

                irqFlags &= latchval;

                // Is this "flagged" off?
                if ((irqMask & 0x0f & irqFlags) == 0) {
                    getControlBus().clearIRQ(VIC_IRQ);
                }
            }
                break;
            case 0xd01a:
                irqMask = data;

                // Check if IRQ should trigger or clear!
                if ((irqMask & 0x0f & irqFlags) != 0) {
                    irqFlags |= 0x80;
                    getControlBus().setIRQ(VIC_IRQ);
                } else {
                    getControlBus().clearIRQ(VIC_IRQ);
                }

                LOGGER.debug("Changing IRQ mask to: " + Integer.toString(irqMask, 16) + " vbeam: " + vbeam);
                break;

            case 0xd01b:
                sprPri = data;
                for (int i = 0, m = 1, n = 8; i < n; i++, m = m << 1) {
                    sprites[i].priority = (data & m) != 0;
                }
                break;
            case 0xd01c:
                sprMul = data;
                for (int i = 0, m = 1, n = 8; i < n; i++, m = m << 1) {
                    sprites[i].multicolor = (data & m) != 0;
                }
                break;
            case 0xd01d:
                sprXEX = data;
                for (int i = 0, m = 1, n = 8; i < n; i++, m = m << 1) {
                    sprites[i].expandX = (data & m) != 0;
                }
                break;
            case 0xd01e:
                sprCol = data;
                break;
            case 0xd01f:
                sprBgCol = data;
                break;
            case 0xd020:
                borderColor = cbmcolor[bCol = data & 15];
                break;
            case 0xd021:
                bgColor = cbmcolor[bgCol[0] = data & 15];
                for (int i = 0, n = 8; i < n; i++) {
                    sprites[i].color[0] = bgColor;
                }
                break;
            case 0xd022:
            case 0xd023:
            case 0xd024:
                bgCol[address - 0xd021] = data & 15;
                break;
            case 0xd025:
                sprMC0 = data & 15;
                for (int i = 0, n = 8; i < n; i++) {
                    sprites[i].color[1] = cbmcolor[sprMC0];
                }
                break;
            case 0xd026:
                sprMC1 = data & 15;
                for (int i = 0, n = 8; i < n; i++) {
                    sprites[i].color[3] = cbmcolor[sprMC1];
                }
                break;
            case 0xd027:
            case 0xd028:
            case 0xd029:
            case 0xd02a:
            case 0xd02b:
            case 0xd02c:
            case 0xd02d:
            case 0xd02e:
                sprites[address - 0xd027].color[2] = cbmcolor[data & 15];
                sprites[address - 0xd027].col = data & 15;
                // System.out.println("Sprite " + (address - 0xd027) + " color set to: " + (data
                // & 15));
                break;
            default:
                throw new IllegalArgumentException(String.format(
                        "Weired write to originalAddress = 0x%05X, changedAddress = 0x%05X", originalAddress, address));
        }

        return true;
    }

    private void setVideoMem() {
        // Set-up vars for screen rendering
        charSetBaseAddress = ((vicMemoryPointers & 0x0e) << 10);
        videoMatrixBaseAddress = ((vicMemoryPointers & 0xf0) << 6);
        vicBaseAddress = ((vicMemoryPointers & 0x08) << 10);
        spr0BlockSel = 0x03f8 + videoMatrixBaseAddress;
    }

    private void initUpdate() {
        vc = 0;
        vcBase = 0;
        vmli = 0;
        // rc = 0;
        updating = true;

        // // First rendered line will start at cpu.cycles - no at next_scan!
        // firstLine = nextScanLine;

        for (int i = 0; i < 8; i++) {
            sprites[i].nextByte = 0;
            sprites[i].painting = false;
            sprites[i].spriteReg = 0;
        }

        if (colors == null) {
            colors = new Color[16];
            for (int i = 0; i < 16; i++) {
                colors[i] = new Color(cbmcolor[i]);
            }
        }
        canvas.setBackground(colors[bCol & 15]);
    }

    // -------------------------------------------------------------------
    // Screen rendering!
    // -------------------------------------------------------------------
    // keep track of if the border is to be painted...
    private int borderState = 0;
    private boolean notVisible = false;
    private int xPos = 0;
    private long lastCycle = 0;

    @Override
    public void clock(long currentCpuCycles) {

        if (lastCycle + 1 < currentCpuCycles) {
            LOGGER.debug("More than one cycle passed: " + (currentCpuCycles - lastCycle) + " at " + currentCpuCycles);

            if (lastCycle == currentCpuCycles) {
                LOGGER.debug(
                        "No diff since last update!!!: " + (currentCpuCycles - lastCycle) + " at " + currentCpuCycles);
            }
            lastCycle = currentCpuCycles;
        }

        // Delta is cycles into the current raster line!
        int vicCycle = (int) (currentCpuCycles - lastLine);

        if (notVisible) {
            if (vicCycle < 62)
                return;
        }

        // Each cycle is 8 pixels (a byte)
        // Cycle 16 (if first cycle is 0) is the first visible gfx cycle
        // Cycle 12 is first visible border cycle => 12 x 8 = 96
        // Last to "draw" is cycle 59 => 59 * 8 = 472 => 376 visible pixels?

        if (badLine) {
            gfxVisible = true;
        }

        switch (vicCycle) {
            case 0:
                // Increase the vbeam - rendering is started
                vbeam = (vbeam + 1) % 312;
                if (vbeam == 0)
                    frame++;
                vPos = vbeam - (FIRST_VISIBLE_VBEAM + 1);

                if (vbeam == FIRST_VISIBLE_VBEAM) {
                    colIndex++;
                    if (colIndex >= LABEL_COUNT)
                        colIndex = 0;
                    // Display enabled?
                    initUpdate();
                }

                // Check for interrupts, etc...
                // Sprite collission interrupts - why only once a line?
                if (((irqMask & 2) != 0) && (sprBgCol != 0) && (irqFlags & 2) == 0) {
                    LOGGER.debug("*** Sprite collission IRQ (d01f): " + sprBgCol + " at " + vbeam);
                    irqFlags |= 82;
                    getControlBus().setIRQ(VIC_IRQ);
                }
                if (((irqMask & 4) != 0) && (sprCol != 0) && (irqFlags & 4) == 0) {
                    LOGGER.debug("*** Sprite collission IRQ (d01e): " + sprCol + " at " + vbeam);
                    irqFlags |= 84;
                    getControlBus().setIRQ(VIC_IRQ);
                }

                int irqComp = raster;
                // Not nice... FIX THIS!!!
                if (irqComp > 312)
                    irqComp &= 0xff;

                if ((irqFlags & 1) == 0 && (irqComp == vbeam)) {
                    irqFlags |= 0x1;

                    if ((irqMask & 1) != 0) {
                        irqFlags |= 0x80;
                        irqTriggered = true;
                        getControlBus().setIRQ(VIC_IRQ);
                        LOGGER.debug("Generating IRQ at " + vbeam + " req:" + raster + " flags: " + irqFlags);
                    }
                } else {
                    irqTriggered = false;
                }
                notVisible = false;
                if (vPos < 0 || vPos >= 284) {
                    controlBus.setCpuBALowUntil(0);
                    notVisible = true;
                    LOGGER.debug("FINISH next at " + vbeam);
                    // Jump directly to VS_FINISH and wait for end of line...
                    break;
                }

                // Check if display should be enabled...
                if (vbeam == 0x30) {
                    displayEnabled = (control1 & 0x10) != 0;
                    if (displayEnabled) {
                        borderState &= ~0x04;
                    } else {
                        borderState |= 0x04;
                    }
                }

                badLine = (displayEnabled && vbeam >= 0x30 && vbeam <= 0xf7) && (vbeam & 0x7) == vScroll;

                // Clear the collission masks each line... - not needed???
                for (int i = 0, n = SC_WIDTH; i < n; i++) {
                    collissionMask[i] = 0;
                }
                break;
            case 1: // Sprite data - sprite 3
                if (sprites[3].dma) {
                    sprites[3].readSpriteData(); // reads all 3 bytes here (one should be prev).
                }
                if (sprites[5].dma) {
                    controlBus.setCpuBALowUntil(lastLine + VICConstants.BA_SP5);
                }
                break;
            case 2:
                // here some of the bytes for sprite 4 should be read...
                break;
            case 3:
                if (sprites[4].dma) {
                    sprites[4].readSpriteData();
                }
                if (sprites[6].dma) {
                    controlBus.setCpuBALowUntil(lastLine + VICConstants.BA_SP6);
                }
                break;
            case 4:
                // here some of the bytes for sprite 5 should be read...
                break;
            case 5:
                if (sprites[5].dma) {
                    sprites[5].readSpriteData();
                }
                if (sprites[7].dma) {
                    controlBus.setCpuBALowUntil(lastLine + VICConstants.BA_SP7);
                }
                break;
            case 6:
                // here some of the bytes for sprite 6 should be read..
                break;
            case 7:
                if (sprites[6].dma) {
                    sprites[6].readSpriteData();
                }
                break;
            case 8:
                // here some of the bytes for sprite 7 should be read...
                break;
            case 9:
                if (sprites[7].dma) {
                    sprites[7].readSpriteData();
                }

                // Border management! (at another cycle maybe?)
                if (blankRow) {
                    if (vbeam == 247) {
                        borderState |= 1;
                    }
                } else {
                    if (vbeam == 251) {
                        borderState |= 1;
                    }
                    if (vbeam == 51) {
                        borderState &= 0xfe;

                        // Reset sprite data to avoid garbage since they are not painted...
                        for (int i = 0, n = 7; i < n; i++) {
                            if (!sprites[i].painting) {
                                sprites[i].lineFinished = true;
                            }
                        }
                    }
                }
                // No border after vbeam 55 (ever?)
                if (vbeam == 55) {
                    borderState &= 0xfe;

                    // Reset sprite data to avoid garbage since they are not painted...
                    for (int i = 0, n = 7; i < n; i++) {
                        if (!sprites[i].painting)
                            sprites[i].lineFinished = true;
                    }
                }
                break;
            case 10:
                break;
            case 11: // Set badline fetching...
                if (badLine) {
                    controlBus.setCpuBALowUntil(lastLine + VICConstants.BA_BADLINE);
                }
                break;
            case 12: // First visible cycle (on screen)
                // calculate mpos before starting the rendering!
                mpos = vPos * SC_WIDTH;
                drawBackground();

                xPos = 16;
                mpos += 8;

                break;
            case 13:
                drawBackground();
                drawSprites();
                mpos += 8;

                // Set vc, reset vmli...
                vc = vcBase;
                vmli = 0;
                if (badLine) {
                    controlBus.setCpuBALowUntil(lastLine + VICConstants.BA_BADLINE);
                    LOGGER.debug("#### RC = 0 (" + rc + ") at " + vbeam + " vc: " + vc);
                    rc = 0;
                }
                break;
            case 14:
                drawBackground();
                drawSprites();
                mpos += 8;
                if (badLine) {
                    controlBus.setCpuBALowUntil(lastLine + VICConstants.BA_BADLINE);
                }
                break;
            case 15:

                drawBackground();
                drawSprites();
                mpos += 8;

                if (badLine) {
                    controlBus.setCpuBALowUntil(lastLine + VICConstants.BA_BADLINE);
                }

                // Turn off sprite DMA if finished reading!
                for (int i = 0, n = 8; i < n; i++) {
                    if (sprites[i].nextByte == 63)
                        sprites[i].dma = false;
                }

                break;
            case 16:
                if (!hideColumn) {
                    borderState &= 0xfd;
                }
                if (badLine) {
                    controlBus.setCpuBALowUntil(lastLine + VICConstants.BA_BADLINE);
                    // Fetch first char into cache! (for the below draw...)
                    vicCharCache[vmli] = getMemory(videoMatrixBaseAddress + (vcBase & 0x3ff));
                    vicColCache[vmli] = getFromColorRAM(vcBase & 0x3ff);// getMemory(IO_OFFSET + 0xd800 + (vcBase &
                                                                        // 0x3ff)); // read from color RAM?
                }

                // Draw one character here!
                drawGraphics(mpos + horizScroll);
                drawSprites();
                if (borderState != 0)
                    drawBackground();
                mpos += 8;

                // System.out.println("Cycle: " + vicCycle + " VMLI: " + vmli + " => " + mpos);
                break;
            case 17:
                if (hideColumn) {
                    borderState &= 0xfd;
                }

                if (badLine) {
                    controlBus.setCpuBALowUntil(lastLine + VICConstants.BA_BADLINE);
                    // Fetch a some chars into cache! (for the below draw...)
                    vicCharCache[vmli] = getMemory(videoMatrixBaseAddress + ((vcBase + vmli) & 0x3ff));
                    vicColCache[vmli] = getFromColorRAM((vcBase + vmli) & 0x3ff);// getMemory(IO_OFFSET + 0xd800 +
                                                                                 // ((vcBase + vmli) & 0x3ff));
                }
                // draw the graphics. (should probably handle sprites also??)
                drawGraphics(mpos + horizScroll);
                drawSprites();
                mpos += 8;
                break;
            // Cycle 18 - 53
            default:
                if (badLine) {
                    controlBus.setCpuBALowUntil(lastLine + VICConstants.BA_BADLINE);
                    // Fetch a some chars into cache! (for the below draw...)
                    vicCharCache[vmli] = getMemory(videoMatrixBaseAddress + ((vcBase + vmli) & 0x3ff));
                    vicColCache[vmli] = getFromColorRAM((vcBase + vmli) & 0x3ff);// getMemory(IO_OFFSET + 0xd800 +
                                                                                 // ((vcBase + vmli) & 0x3ff));
                }
                // draw the graphics. (should probably handle sprites also??)
                drawGraphics(mpos + horizScroll);
                drawSprites();

                mpos += 8;
                // System.out.println("Cycle: " + vicCycle + " VMLI: " + vmli + " => " + mpos);
                break;
            case 54:
                // Then Check if it is time to start up the sprites!
                // Does not matter in which order this is done ?=
                if (badLine) {
                    controlBus.setCpuBALowUntil(lastLine + VICConstants.BA_BADLINE);
                    // Fetch a some chars into cache! (for the below draw...)
                    vicCharCache[vmli] = getMemory(videoMatrixBaseAddress + ((vcBase + vmli) & 0x3ff));
                    vicColCache[vmli] = getFromColorRAM((vcBase + vmli) & 0x3ff);// getMemory(IO_OFFSET + 0xd800 +
                                                                                 // ((vcBase + vmli) & 0x3ff));
                }
                int mult = 1;
                int ypos = vPos + SC_SPYOFFS;

                for (int i = 0, n = 8; i < n; i++) {
                    Sprite sprite = sprites[i];
                    if (sprite.enabled) {
                        // If it is time to start drawing this sprite!
                        if (sprite.y == (ypos & 0xff) && (ypos < 270)) {
                            sprite.nextByte = 0;
                            sprite.dma = true;
                            sprite.expFlipFlop = true;
                            LOGGER.debug("Starting painting sprite " + i + " on " + vbeam + " first visible at "
                                    + (ypos + 1));
                        }
                    }
                    mult = mult << 1;
                }
                if (sprites[0].dma) {
                    controlBus.setCpuBALowUntil(lastLine + VICConstants.BA_SP0);
                }

                drawGraphics(mpos + horizScroll);
                drawSprites();

                mpos += 8;
                // System.out.println("Cycle: " + vicCycle + " VMLI: " + vmli + " => " + mpos);

                break;
            case 55:
                if (hideColumn) {
                    borderState |= 2;
                }
                if (badLine) {
                    controlBus.setCpuBALowUntil(lastLine + VICConstants.BA_BADLINE);
                    // Fetch a some chars into cache! (for the below draw...)
                    vicCharCache[vmli] = getMemory(videoMatrixBaseAddress + ((vcBase + vmli) & 0x3ff));
                    vicColCache[vmli] = getFromColorRAM((vcBase + vmli) & 0x3ff);// getMemory(IO_OFFSET + 0xd800 +
                                                                                 // ((vcBase + vmli) & 0x3ff));
                }
                drawGraphics(mpos + horizScroll);
                drawSprites();
                if (borderState != 0)
                    drawBackground();
                mpos += 8;
                // System.out.println("Cycle: " + vicCycle + " VMLI: " + vmli + " => " + mpos);

                break;
            case 56:
                if (!hideColumn) {
                    borderState |= 2;
                }

                drawBackground();
                drawSprites();
                mpos += 8;

                // If time to turn of sprite display...
                for (int i = 0, n = 8; i < n; i++) {
                    Sprite sprite = sprites[i];
                    if (!sprite.dma) {
                        sprite.painting = false;
                        LOGGER.debug("Stopped painting sprite " + i + " at (after): " + vbeam);
                    }
                }

                // Here we should check if sprite dma should start...
                // - probably need to add a dma variable to sprites, and not
                // only use the painting variable for better emulation
                // Bus not available if sp0 or sp1 is painting
                if (sprites[1].dma) {
                    controlBus.setCpuBALowUntil(lastLine + VICConstants.BA_SP1);
                }
                break;
            case 57:
                // Paint border, check sprite for display and read sprite 0 data.
                for (int i = 0, n = 8; i < n; i++) {
                    Sprite sprite = sprites[i];
                    if (sprite.dma)
                        sprite.painting = true;
                }

                drawBackground();
                drawSprites();
                mpos += 8;

                if (rc == 7) {
                    vcBase = vc;
                    gfxVisible = false;
                    LOGGER.debug("#### RC7 ==> vc = " + vc + " at " + vbeam + " vicCycle = " + vicCycle);
                    if (vc == 1000) {
                        LOGGER.debug("--------------- last line ----------------");
                    }
                }

                if (badLine || gfxVisible) {
                    rc = (rc + 1) & 7;
                    gfxVisible = true;
                }

                if (sprites[0].painting) {
                    sprites[0].readSpriteData();
                }

                if (sprites[2].dma) {
                    controlBus.setCpuBALowUntil(lastLine + VICConstants.BA_SP2);
                }

                break;
            case 58:
                drawBackground();
                drawSprites();
                mpos += 8;

                break;
            case 59:
                drawBackground();
                drawSprites();
                mpos += 8;

                if (sprites[1].painting) {
                    sprites[1].readSpriteData();
                }
                break;
            case 60:
                drawSprites();
                break;
            case 61:
                if (sprites[2].painting) {
                    sprites[2].readSpriteData();
                }
                if (sprites[3].dma) {
                    controlBus.setCpuBALowUntil(lastLine + VICConstants.BA_SP3);
                }
                break;
            case 62:
                // Should this be made??? or should sprite 0 be repaintable
                // same line?
                // Reset sprites so that they can be repainted again...
                for (int i = 0; i < sprites.length; i++) {
                    sprites[i].reset();
                }
                lastLine += VICConstants.SCAN_RATE;
                // Update screen
                if (updating) {
                    if (vPos == 285) {
                        mis.newPixels();
                        canvas.repaint();
                        updating = false;
                    }
                }
                notVisible = false;
                break;
        }
    }

    // Used to draw background where either border or background should be
    // painted...
    private void drawBackground() {
        int bpos = mpos;
        int currentBg = borderState > 0 ? borderColor : bgColor;
        for (int i = 0; i < 8; i++) {
            mem[bpos++] = currentBg;
        }
    }

    /**
     * <code>drawGraphics</code> - draw the VIC graphics (text/bitmap) Note that
     * sprites are not drawn here... (yet?)
     *
     *
     * @param mpos
     *            an <code>int</code> value representing position to draw the
     *            graphics from (already fixed with hscroll)
     */
    private final void drawGraphics(int mpos) {
        if (!gfxVisible || paintBorder || (borderState & 1) == 1) {
            // We know that display is not enabled, and that mpos is already
            // at a correct place, except horizScroll...
            mpos -= horizScroll;
            int color = (paintBorder || (borderState > 0)) ? borderColor : bgColor;
            for (int i = mpos, n = mpos + 8; i < n; i++) {
                mem[i] = color;
            }
            // trick to use vmli as a if var even when no gfx.
            vmli++;
            return;
        }

        int collX = (vmli << 3) + horizScroll + SC_XOFFS;

        // Paint background if first col (should maybe be made later also...)
        if (vmli == 0) {
            for (int i = mpos - horizScroll, n = i + 8; i < n; i++) {
                mem[i] = bgColor;
            }
        }

        // position is an starting address (i.e. in Character ROM) of the character
        // glyphs data. Each character has 8 bytes data.
        // https://www.c64-wiki.com/wiki/Character_set#Character_sequence
        int position = 0;
        int data = 0, penColor = 0, bgcol = bgColor;

        if ((control1 & 0x20) == 0) {
            // here we have a text mode
            int tmp;
            int pcol;

            // This should be in a cache some where...
            if (multiCol) {
                multiColor[0] = bgColor;
                multiColor[1] = cbmcolor[bgCol[1]];
                multiColor[2] = cbmcolor[bgCol[2]];
            }

            penColor = cbmcolor[pcol = vicColCache[vmli] & 15];
            if (extended) {
                position = charSetBaseAddress + (((data = vicCharCache[vmli]) & 0x3f) << 3);
                bgcol = cbmcolor[bgCol[(data >> 6)]];
            } else {
                position = charSetBaseAddress + (vicCharCache[vmli] << 3);
            }

            // rc is the row offset for the character that is drawn. RC = 0...7
            data = getMemory(position + rc); // data should contain the specific value for a specific character

            if (multiCol && pcol > 7) {
                multiColor[3] = cbmcolor[pcol & 7];
                for (int pix = 0; pix < 8; pix += 2) {
                    tmp = (data >> pix) & 3;
                    mem[mpos + 6 - pix] = mem[mpos + 7 - pix] = multiColor[tmp];
                    // both 00 and 01 => no collission!?
                    // but what about priority?
                    if (tmp > 0x01) {
                        tmp = 256;
                    } else {
                        tmp = 0;
                    }
                    collissionMask[collX + 7 - pix] = collissionMask[collX + 6 - pix] = tmp;
                }
            } else {
                for (int pix = 0; pix < 8; pix++) {
                    if ((data & (1 << pix)) > 0) {
                        mem[mpos + 7 - pix] = penColor;
                        collissionMask[collX + 7 - pix] = 256;
                    } else {
                        mem[mpos + 7 - pix] = bgcol;
                        collissionMask[collX + 7 - pix] = 0;
                    }
                }
            }

            if (multiCol && extended) {
                // Illegal mode => all black!
                for (int pix = 0; pix < 8; pix++) {
                    mem[mpos + 7 - pix] = 0xff000000;
                }
            }

            // if (BAD_LINE_DEBUG && badLine) {
            // for (int pix = 0; pix < 8; pix += 4) {
            // mem[mpos + 7 - pix] = (mem[mpos + 7 - pix] & 0xff7f7f7f) | 0x0fff;
            // }
            // }
        } else {
            // -------------------------------------------------------------------
            // Bitmap mode!
            // -------------------------------------------------------------------
            position = vicBaseAddress + (vc & 0x3ff) * 8 + rc;
            if (multiCol) {
                multiColor[0] = bgColor;
            }
            int vmliData = vicCharCache[vmli];
            penColor = cbmcolor[(vmliData & 0xf0) >> 4];
            bgcol = cbmcolor[vmliData & 0x0f];

            data = getMemory(position);

            if (multiCol) {
                multiColor[1] = cbmcolor[(vmliData >> 4) & 0x0f];
                multiColor[2] = cbmcolor[vmliData & 0x0f];
                multiColor[3] = cbmcolor[vicColCache[vmli] & 0x0f];

                // Multicolor
                int tmp;
                for (int pix = 0; pix < 8; pix += 2) {
                    mem[mpos + 6 - pix] = mem[mpos + 7 - pix] = multiColor[tmp = (data >> pix) & 3];
                    if (tmp > 0x01) {
                        tmp = 256;
                    } else {
                        tmp = 0;
                    }
                    collissionMask[collX + 7 - pix] = collissionMask[collX + 6 - pix] = tmp;
                }
            } else {
                // Non multicolor
                for (int pix = 0; pix < 8; pix++) {
                    if ((data & (1 << pix)) > 0) {
                        mem[7 - pix + mpos] = penColor;
                        collissionMask[collX + 7 - pix] = 256;
                    } else {
                        mem[7 - pix + mpos] = bgcol;
                        collissionMask[collX + 7 - pix] = 0;
                    }
                }
            }

            if (extended) {
                // Illegal mode => all black!
                for (int pix = 0; pix < 8; pix++) {
                    mem[mpos + 7 - pix] = 0xff000000;
                }
            }

            // if (BAD_LINE_DEBUG && badLine) {
            // for (int pix = 0; pix < 8; pix += 4) {
            // mem[mpos + 7 - pix] = (mem[mpos + 7 - pix] & 0xff3f3f3f) | 0x0fff;
            // }
            // }
        }
        vc++;
        vmli++;
    }

    // -------------------------------------------------------------------
    // Sprites...
    // -------------------------------------------------------------------
    private final void drawSprites() {
        int smult = 0x100;
        int lastX = xPos - 8;

        for (int i = 7; i >= 0; i--) {
            Sprite sprite = sprites[i];
            // Done before the continue...
            smult = smult >> 1;
            if (sprite.lineFinished || !sprite.painting) {
                continue;
            }
            int x = sprite.x + SC_SPXOFFS; // 0 in sprite x => xPos = 8
            int mpos = vPos * SC_WIDTH;

            if (x < xPos) {
                // Ok, we should write some data...
                int minX = lastX > x ? lastX : x;
                // if (i == 0) monitor.info("Writing sprite " + i + " first pixel at " +
                // minX + " vPos = " + vPos);

                for (int j = minX, m = xPos; j < m; j++) {
                    int c = sprite.getPixel();
                    if (c != 0 && borderState == 0) {
                        int tmp = (collissionMask[j] |= smult);
                        if (!sprite.priority || (tmp & 0x100) == 0) {
                            mem[mpos + j] = sprite.color[c];
                        }

                        if (tmp != smult) {
                            // If collission with bg then notice!
                            if ((tmp & 0x100) != 0) {
                                sprBgCol |= smult;
                                // monitor.info("***** Sprite x Bkg collission!");
                            }
                            // If collission with sprite, all colls must
                            // be registered!
                            if ((tmp & 0xff) != smult) {
                                sprCol |= tmp & 0xff;
                                // monitor.info("***** Sprite x Sprite collission: d01e = " +
                                // sprCol + " sprite: " + i + " => " +
                                // smult + " at " + j + "," + vbeam);
                            }
                        }
                    }

                    // if (SPRITEDEBUG) {
                    // if ((sprite.nextByte == 3) && ((j & 4) == 0)) {
                    // mem[mpos + j] = 0xff00ff00;
                    // }
                    // if ((sprite.nextByte == 63) && ((j & 4) == 0)) {
                    // mem[mpos + j] = 0xffff0000;
                    // }
                    //
                    // if (j == x) {
                    // mem[mpos + j] = 0xff000000 + sprite.pointer;
                    // }
                    // }
                }
            }
        }
        xPos += 8;
    }

    public void stop() {
    }

    public void reset() {
        // Clear a lot of stuff...???
        initUpdate();
        lastLine = 0;

        for (int i = 0; i < mem.length; i++)
            mem[i] = 0;
        reset = 100;

        sprCol = 0;
        sprBgCol = 0;

        // c1541.reset();
        isrRunning = false;
    }

    public static final int IMG_TOTWIDTH = SC_WIDTH;
    public static final int IMG_TOTHEIGHT = SC_HEIGHT;

    public Image crtImage;

    // Will be called from the c64canvas class
    long repaint = 0;

    public void paint(Graphics g) {
        if (g == null)
            return;

        if (image == null) {
            image = canvas.createImage(IMG_TOTWIDTH, IMG_TOTHEIGHT);
            g2 = image.getGraphics();
            g2.setFont(new Font("Monospaced", Font.PLAIN, 11));
        }

        if (crtImage == null) {
            crtImage = new BufferedImage(displayWidth, displayHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics gcrt = crtImage.getGraphics();
            gcrt.setColor(TRANSPARENT_BLACK);
            for (int i = 0, n = displayHeight; i < n; i += 2) {
                gcrt.drawLine(0, i, displayWidth, i);
            }
        }

        // Why is there transparency?
        g2.drawImage(screen, 0, 0, null);

        if (reset > 0) {
            g2.setColor(darks[colIndex]);
            int xp = 44;
            if (reset < 44) {
                xp = reset;
            }
            g2.drawString("JaC64 " + version + " - Java C64 - www.jac64.com", xp + 1, 9);
            g2.setColor(lites[colIndex]);
            g2.drawString("JaC64 " + version + " - Java C64 - www.jac64.com", xp, 8);
            reset--;
        } else {
            String msg = "JaC64 ";
            if ((message != null) && (message != "")) {
                msg += message;
            } else {
                colIndex = 0;
            }
            msg += tmsg;

            g2.setColor(darks[colIndex]);
            g2.drawString(msg, 1, 9);
            g2.setColor(lites[colIndex]);
            g2.drawString(msg, 0, 8);

            g2.fillRect(372, 3, 7, 1);
            g2.setColor(LED_BORDER);
            g2.drawRect(371, 2, 8, 2);
        }

        g.fillRect(0, 0, offsetX, displayHeight + offsetY * 2);
        g.fillRect(offsetX + displayWidth, 0, offsetX, displayHeight + offsetY * 2);
        g.fillRect(0, 0, displayWidth + offsetX * 2, offsetY);
        g.fillRect(0, displayHeight + offsetY, displayWidth + offsetX * 2, offsetY);
        Graphics2D g2d = (Graphics2D) g;
        // g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
        // RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(image, offsetX, offsetY, displayWidth, displayHeight, null);
        // g.drawImage(crtImage, offsetX, offsetY, displayWidth, displayHeight, null);

        // monitor.info("Repaint: " + (System.currentTimeMillis() - repaint) + " " +
        // memory[IO_OFFSET + 55296 + 6 * 40]);
        // repaint = System.currentTimeMillis();
    }

    // -------------------------------------------------------------------
    // Internal sprite class to handle all data for sprites
    // Just a collection of data registers... so far...
    // -------------------------------------------------------------------
    private class Sprite {

        boolean painting = false; // If sprite is "on" or not (visible)
        boolean dma = false; // Sprite DMA on/off

        int nextByte;
        int pointer;
        int x;
        int y;

        int spriteNo;
        // Contains the sprite data to be outshifted
        int spriteReg;

        boolean enabled;
        boolean expFlipFlop;
        boolean multicolor = false;
        boolean expandX = false;
        boolean expandY = false;
        boolean priority = false;
        boolean lineFinished = false;

        int pixelsLeft = 0;
        int currentPixel = 0;

        // The sprites color value (col)
        int col;
        // Sprites real colors
        int[] color = new int[4];

        int getPixel() {
            if (lineFinished)
                return 0;
            pixelsLeft--;
            if (pixelsLeft > 0)
                return currentPixel;
            // Indicate finished!
            if (pixelsLeft <= 0 && spriteReg == 0) {
                currentPixel = 0;
                lineFinished = true;
                return 0;
            }

            if (multicolor) {
                // The 23rd and 22nd pixel => data!
                currentPixel = (spriteReg & 0xc00000) >> 22;
                spriteReg = (spriteReg << 2) & 0xffffff;
                pixelsLeft = 2;
            } else {
                // Only the 23rd bit is pixel data!
                currentPixel = (spriteReg & 0x800000) >> 22;
                spriteReg = (spriteReg << 1) & 0xffffff;
                pixelsLeft = 1;
            }
            // Double the number of pixels if expanded!
            if (expandX) {
                pixelsLeft = pixelsLeft << 1;
            }

            return currentPixel;
        }

        void reset() {
            lineFinished = false;
        }

        void readSpriteData() {
            // Read pointer + the three sprite data pointers...
            pointer = getMemory(spr0BlockSel + spriteNo) * 0x40;
            spriteReg = ((getMemory(pointer + nextByte++) & 0xff) << 16)
                    | ((getMemory(pointer + nextByte++) & 0xff) << 8) | getMemory(pointer + nextByte++);

            // For debugging... seems to be err on other place than the
            // Memoryfetch - since this is also looking very odd...???
            // spriteReg = 0xf0f0f0;

            if (!expandY)
                expFlipFlop = false;

            if (expFlipFlop) {
                nextByte = nextByte - 3;
            }

            expFlipFlop = !expFlipFlop;
            pixelsLeft = 0;

        }
    }

    // -------------------------------------------------------------------
    // MouseListener
    // -------------------------------------------------------------------
    public void mouseDragged(MouseEvent e) {
        potx = e.getX() & 0xff;
        poty = 0xff - (e.getY() & 0xff);
    }

    public void mouseMoved(MouseEvent e) {
        potx = e.getX() & 0xff;
        poty = 0xff - (e.getY() & 0xff);
    }

    private int getMemory(int address) {
        if (address > 16384) {
            throw new IllegalArgumentException(
                    String.format("VIC wants to read out of its range; from address 0x%05X", address));
        }

        setVideoMem();

        return addressableBus.readVicExclusive(address);
    };

    protected int getFromColorRAM(int localColorRAMAddress) {
        setVideoMem();

        return addressableBus.readVicExclusiveFromColorRAM(localColorRAMAddress);
    }

    private ControlBus getControlBus() {
        return controlBus;
    }
}