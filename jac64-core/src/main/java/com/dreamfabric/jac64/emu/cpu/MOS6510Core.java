/**
 * This file is a part of JaC64 - a Java C64 Emulator
 * Main Developer: Joakim Eriksson (Dreamfabric.com)
 * Contact: joakime@sics.se
 * Web: http://www.dreamfabric.com/c64
 * ---------------------------------------------------
 *
 *
 */

package com.dreamfabric.jac64.emu.cpu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MOS6510Core "implements" the 6510 processor in java code. Other classes are
 * intended to implement the specific write/read from memory for correct
 * emulation of RAM/ROM/IO handling
 *
 * @author Joakim Eriksson (joakime@sics.se)
 * @author Jan Blok (jblok@profdata.nl)
 * @version $Revision: $ $Date: $
 */
public abstract class MOS6510Core extends MOS6510Ops {
    private static Logger LOGGER = LoggerFactory.getLogger(MOS6510Core.class);

    public static final int NMI_DELAY = 2;
    public static final int IRQ_DELAY = 2;

    public static final int NMI_INT = 1;
    public static final int IRQ_INT = 2;

    // -------------------------------------------------------------------
    // Interrup signals
    // -------------------------------------------------------------------
    private boolean checkInterrupt = false;
    private boolean NMILow = false;
    private boolean NMILastLow = false;
    private boolean IRQLow = false;
    public long baLowUntil = 0;

    // The processor flags
    private boolean sign = false;
    private boolean zero = false;
    private boolean overflow = false;
    private boolean carry = false;
    private boolean decimal = false;
    private boolean brk = false;
    private boolean resetFlag = false;

    // registers
    private int acc = 0;
    private int x = 0;
    private int y = 0;
    private int s = 0xff; // The stackpointer ??? ff = top?

    private long nmiCycleStart = 0;
    private long irqCycleStart = 0;

    protected long currentCpuCycles = 0;

    // Some temporary and other variables...
    private int pc;
    private int interruptInExec = 0;

    private boolean disableInterupt = false;

    public void init() {
        super.init();
    }

    public long getCycles() {
        return currentCpuCycles;
    }

    public boolean getIRQLow() {
        return IRQLow;
    }

    public void setIRQLow(boolean low) {
        if (!IRQLow && low) {
            // If low -> will trigger an IRQ!
            checkInterrupt = true;
            irqCycleStart = currentCpuCycles + IRQ_DELAY;
        }
        IRQLow = low;
    }

    public boolean getNMILow() {
        return NMILow;
    }

    public void setNMILow(boolean low) {
        if (!NMILow && low) {
            // If going from "high" to low -> will trigger an NMI!
            checkInterrupt = true;
            nmiCycleStart = currentCpuCycles + NMI_DELAY;
            // System.out.println("*** NMI Goes low!");
        }
        NMILow = low;
        // If setting to non-low - both low and lastLow can be set?
        if (!low) {
            NMILastLow = low;
            // System.out.println("*** NMI Goes hi!");
        }
    }

    public int getPc() {
        return pc;
    }

    public void setPc(int pc) {
        this.pc = pc;
    }

    public int getInterruptInExec() {
        return interruptInExec;
    }

    public int getSP() {
        return s;
    }

    public void emulateOp() {
        // Before executing an operation - check for interrupts!!!
        if (checkInterrupt) {
            // Trigger on negative edge!
            if ((NMILow && !NMILastLow) && (currentCpuCycles >= nmiCycleStart)) {
                LOGGER.debug("NMI interrupt at " + currentCpuCycles);
                doInterrupt(0xfffa, getStatusByte() & 0xef);
                disableInterupt = true;
                // prevent irq during nmi,RTI will clear by poping status back
                // checkInterrupt = false;
                // Remember last NMI state in order to check on next...
                NMILastLow = NMILow;
                // Just the interrupt handling... do nothing more...
                return;
            }

            if ((IRQLow && currentCpuCycles >= irqCycleStart) || brk) {
                if (disableInterupt) {
                    brk = false;
                    checkInterrupt = (NMILow && !NMILastLow);
                } else {
                    LOGGER.debug("IRQ interrupt > " + IRQLow + " BRK: " + brk);
                    // checkInterrupt = false; //does not make sense to leave more
                    int status = getStatusByte();
                    if (brk) {
                        status |= 0x10;
                        pc++;
                    } else
                        status &= 0xef;
                    doInterrupt(0xfffe, status);
                    disableInterupt = true;
                    // prevent irq during irq, RTI will clear by poping status back
                    brk = false;

                    // Just the interrupt handling... do nothing more...
                    // Remember last NMI state in order to check on next...
                    // NMILastLow = NMILow;
                    return;
                }
            } else if (resetFlag) {
                doReset();
            }
        }

        // Ok no interrupts, execute instruction
        // fetch instruction!
        int data = INSTRUCTION_SET[fetchByte(pc++)];
        int op = data & OP_MASK;
        int addrMode = data & ADDRESSING_MASK;
        boolean read = (data & READ) != 0;
        boolean write = (data & WRITE) != 0;
        int adr = 0;
        int tmp = 0;
        boolean nxtcarry = false;

        // System.out.println("AddrMode:" + Hex.hex2(addrMode) +
        // " op: " + Hex.hex2(op)
        // + " data: " + Hex.hex2(data));

        // fetch first argument (always fetched...?) - but not always pc++!!
        int p1 = fetchByte(pc);

        // Fetch addres, and read if it should be done!
        switch (addrMode) {
            // never any address when immediate
            case IMMEDIATE:
                pc++;
                data = p1;
                break;
            case ABSOLUTE:
                pc++;
                adr = (fetchByte(pc++) << 8) + p1;
                if (read) {
                    data = fetchByte(adr);
                }
                break;
            case ZERO:
                pc++;
                adr = p1;
                if (read) {
                    data = fetchByte(adr);
                }
                break;
            case ZERO_X:
            case ZERO_Y:
                pc++;
                // Read from wrong address first...
                fetchByte(p1);

                if (addrMode == ZERO_X)
                    adr = (p1 + x) & 0xff;
                else
                    adr = (p1 + y) & 0xff;

                if (read) {
                    data = fetchByte(adr);
                }
                break;
            case ABSOLUTE_X:
            case ABSOLUTE_Y:
                pc++;
                // Fetch hi byte!
                adr = fetchByte(pc++) << 8;

                // add x/y to low byte & possibly faulty fetch!
                if (addrMode == ABSOLUTE_X)
                    p1 += x;
                else
                    p1 += y;

                data = fetchByte(adr + (p1 & 0xff));
                adr += p1;

                // If read - a fifth cycle patches the incorrect address...
                // Always done if RMW!
                if (read && (p1 > 0xff || write)) {
                    data = fetchByte(adr);
                }
                break;
            case RELATIVE:
                pc++;
                adr = pc + (byte) p1;
                if (((adr ^ pc) & 0xff00) > 0) {
                    // loose one cycle since adr is on another page...
                    tmp = 2;
                } else {
                    tmp = 1;
                }
                break;
            case ACCUMULATOR:
                data = acc;
                write = false;
                break;
            case INDIRECT_X:
                pc++;
                // unneccesary read... fetchByte(p1);
                fetchByte(p1);
                tmp = (p1 + x) & 0xff;

                adr = (fetchByte(tmp + 1) << 8);
                adr |= fetchByte(tmp);

                if (read) {
                    data = fetchByte(adr);
                }
                break;
            case INDIRECT_Y:
                pc++;
                // Fetch hi and lo
                adr = (fetchByte(p1 + 1) << 8);
                p1 = fetchByte(p1);
                p1 += y;

                data = fetchByte(adr + (p1 & 0xff));
                adr += p1;

                // If read - a sixth cycle patches the incorrect address...
                // Always done if RMW!
                if (read && (p1 > 0xff || write)) {
                    data = fetchByte(adr);
                }
                break;
            case INDIRECT:
                pc++;
                // Fetch pointer
                adr = (fetchByte(pc) << 8) + p1;

                // Calculate address
                tmp = (adr & 0xfff00) | ((adr + 1) & 0xff);
                // fetch the real address
                adr = fetchByte(adr);
                adr += (fetchByte(tmp) << 8);
                break;
        }

        // -------------------------------------------------------------------
        // Addressing handled! now on to instructions in order of appearance
        // -------------------------------------------------------------------

        // If RMW - it will write before proceeding
        if (read && write) {
            writeByte(adr, data);
        }

        switch (op) {
            case BRK:
                brk = true;
                checkInterrupt = true;
                break;
            case AND:
                acc = acc & data;
                setZS(acc);
                break;
            case ADC:
                opADCimp(data);
                break;
            case SBC:
                opSBCimp(data);
                break;
            case ORA:
                acc = acc | data;
                setZS(acc);
                break;
            case EOR:
                acc = acc ^ data;
                setZS(acc);
                break;
            case BIT:
                sign = data > 0x7f;
                overflow = (data & 0x40) > 0;
                zero = (acc & data) == 0;
                break;
            case LSR:
                carry = (data & 0x01) != 0;
                data = data >> 1;
                zero = (data == 0);
                sign = false;
                break;
            case ROL:
                data = (data << 1) + (carry ? 1 : 0);
                carry = (data & 0x100) != 0;
                data = data & 0xff;
                setZS(data);
                break;
            case ROR:
                nxtcarry = (data & 0x01) != 0;
                data = (data >> 1) + (carry ? 0x80 : 0);
                carry = nxtcarry;
                setZS(data);
                break;
            case TXA:
                acc = x;
                setZS(acc);
                break;
            case TAX:
                x = acc;
                setZS(x);
                break;
            case TYA:
                acc = y;
                setZS(acc);
                break;
            case TAY:
                y = acc;
                setZS(y);
                break;
            case TSX:
                x = s;
                setZS(x);
                break;
            case TXS:
                s = x & 0xff;
                break;
            case DEC:
                data = (data - 1) & 0xff;
                setZS(data);
                break;
            case INC:
                data = (data + 1) & 0xff;
                setZS(data);
                break;
            case INX:
                x = (x + 1) & 0xff;
                setZS(x);
                break;
            case DEX:
                x = (x - 1) & 0xff;
                setZS(x);
                break;
            case INY:
                y = (y + 1) & 0xff;
                setZS(y);
                break;
            case DEY:
                y = (y - 1) & 0xff;
                setZS(y);
                break;
            // Jumps
            case JSR:
                pc++;
                adr = (fetchByte(pc) << 8) + p1;
                fetchByte(s | 0x100);
                push((pc & 0xff00) >> 8); // HI
                push(pc & 0x00ff); // LOW
                pc = adr;
                break;
            case JMP:
                pc = adr;
                break;
            case RTS:
                fetchByte(s | 0x100);
                pc = pop() + (pop() << 8);
                pc++;
                fetchByte(pc);
                break;
            case RTI:
                fetchByte(s | 0x100);
                tmp = pop();
                setStatusByte(tmp);
                pc = pop() + (pop() << 8);
                brk = false;
                interruptInExec--;
                // Need to check for interrupts
                checkInterrupt = true;
                break;

            case TRP:
                LOGGER.info("TRAP Instruction executed");
                break;
            case NOP:
                break;
            case ASL:
                setCarry(data);
                data = (data << 1) & 0xff;
                setZS(data);
                break;
            case PHA:
                push(acc);
                break;
            case PLA:
                fetchByte(s | 0x100);
                acc = pop();
                setZS(acc);
                break;
            case PHP:
                brk = true;
                push(getStatusByte());
                brk = false;
                break;
            case PLP:
                tmp = pop();
                setStatusByte(tmp);
                brk = false;
                checkInterrupt = true;
                break;
            case ANC:
                acc = acc & data;
                setZS(acc);
                carry = (acc & 0x80) != 0;
                break;
            case CMP:
                data = acc - data;
                carry = data >= 0;
                setZS((data & 0xff));
                break;
            case CPX:
                data = x - data;
                carry = data >= 0;
                setZS((data & 0xff));
                break;
            case CPY:
                data = y - data;
                carry = data >= 0;
                setZS((data & 0xff));
                break;
            // Branch instructions
            case BCC:
                branch(!carry, adr, tmp);
                break;
            case BCS:
                branch(carry, adr, tmp);
                break;
            case BEQ:
                branch(zero, adr, tmp);
                break;
            case BNE:
                branch(!zero, adr, tmp);
                break;
            case BVC:
                branch(!overflow, adr, tmp);
                break;
            case BVS:
                branch(overflow, adr, tmp);
                break;
            case BPL:
                branch(!sign, adr, tmp);
                break;
            case BMI:
                branch(sign, adr, tmp);
                break;
            // Modify flags
            case CLC:
                carry = false;
                break;
            case SEC:
                carry = true;
                break;
            case CLD:
                decimal = false;
                break;
            case SED:
                decimal = true;
                break;
            case CLV:
                overflow = false;
                break;
            case SEI:
                disableInterupt = true;
                break;
            case CLI:
                disableInterupt = false;
                checkInterrupt = true;
                break;
            // Load / Store instructions
            case LDA:
                acc = data;
                setZS(data);
                break;
            case LDX:
                x = data;
                setZS(data);
                break;
            case LDY:
                y = data;
                setZS(data);
                break;
            case STA:
                data = acc;
                break;
            case STX:
                data = x;
                break;
            case STY:
                data = y;
                break;

            // -------------------------------------------------------------------
            // Undocumented ops
            // -------------------------------------------------------------------
            case ANE:
                acc = p1 & x & (acc | 0xee);
                setZS(acc);
                break;
            case ARR: // ARR = AND + ROR ??? - not???
                // A'la frodo
                tmp = p1 & acc;
                acc = (carry ? (tmp >> 1) | 0x80 : tmp >> 1);
                if (!decimal) {
                    setZS(acc);
                    carry = (acc & 0x40) != 0;
                    overflow = ((acc & 0x40) ^ ((acc & 0x20) << 1)) != 0;
                } else {
                    sign = carry;
                    zero = acc == 0;
                    overflow = ((tmp ^ acc) & 0x40) != 0;
                    if ((tmp & 0x0f) + (tmp & 0x01) > 5)
                        acc = acc & 0xf0 | (acc + 6) & 0x0f;
                    if (carry = ((tmp + (tmp & 0x10)) & 0x1f0) > 0x50)
                        acc += 0x60;
                }
                break;

            case ASR: // AND + LSR
                acc = acc & data;
                nxtcarry = (acc & 0x01) != 0;
                acc = (acc >> 1);
                carry = nxtcarry;
                setZS(acc);
                break;

            case DCP:
                data = (data - 1) & 0xff;
                setZS(data);
                tmp = acc - data;
                carry = tmp >= 0;
                setZS((tmp & 0xff));
                break;

            case ISB:
                data = (data + 1) & 0xff;
                // SBC PART!
                opSBCimp(data);
                break;
            case LAX:
                acc = x = data;
                setZS(acc);
                break;

            case LAS: // A,X,S:={adr}&S
                acc = x = s = (data & s);
                setZS(acc);
                break;

            case LXA:
                x = acc = (acc | 0xee) & p1;
                setZS(acc);
                break;

            case RLA:
                data = (data << 1) + (carry ? 1 : 0);
                carry = (data & 0x100) != 0;
                data = data & 0xff;
                // AND PART
                acc = acc & data;
                zero = (acc == 0);
                sign = (acc > 0x7f);
                break;

            case RRA: // RRA ROR + ADC
                nxtcarry = (data & 0x01) != 0;
                data = (data >> 1) + (carry ? 0x80 : 0);
                carry = nxtcarry;
                // ADC PART!
                opADCimp(data);
                break;

            case SBX:
                x = ((acc & x) - p1);
                carry = x >= 0;
                x = x & 0xff;
                setZS(x);
                break;
            case SHA:
                data = acc & x & ((adr >> 8) + 1);
                break;

            case SHS:
                data = acc & x & ((adr >> 8) + 1);
                s = acc & x;
                break;

            case SHX:
                data = x & ((adr >> 8) + 1);
                break;
            case SHY:
                data = y & ((adr >> 8) + 1);
                break;

            case SAX:
                data = acc & x;
                break;

            case SRE:
                carry = (data & 0x01) != 0;
                data = data >> 1;
                // EOR PART
                acc = acc ^ data;
                setZS(acc);
                break;

            case SLO:
                // ASL
                setCarry(data);
                data = (data << 1) & 0xff;
                // Written later...
                // THE ORA PART
                acc = acc | data;
                setZS(acc);
                break;
            default:
                unknownInstruction(pc, op);
        }

        if (write) {
            writeByte(adr, data);
        } else if (addrMode == ACCUMULATOR) {
            acc = data;
        }
    }

    // Reset the MOS6510Core!!!
    // This can be called with any thread!!!
    public void reset() {
        // Clear and copy!
        // The processor flags
        NMILow = false;
        brk = false;
        IRQLow = false;
        LOGGER.debug("Set IRQLOW to false...");
        resetFlag = true;
        checkInterrupt = true;
    }

    protected abstract int fetchByte(int adr);

    protected abstract void writeByte(int adr, int data);

    protected void unknownInstruction(int pc, int op) {
        LOGGER.error("Unknown instruction: " + op);
    }

    protected int getACC() {
        return acc;
    }

    private final void doInterrupt(int adr, int status) {
        // System.out.println("Doing Interrupt disableInterrupt before: " +
        // disableInterupt);
        fetchByte(pc);
        fetchByte(pc + 1);
        push((pc & 0xff00) >> 8); // HI ??
        push(pc & 0x00ff); // LOW ??
        push(status);
        interruptInExec++;
        pc = (fetchByte(adr + 1) << 8);
        pc += fetchByte(adr);
    }

    private final int getStatusByte() {
        return ((carry ? 0x01 : 0) + (zero ? 0x02 : 0) + (disableInterupt ? 0x04 : 0) + (decimal ? 0x08 : 0)
                + (brk ? 0x10 : 0) + 0x20 + (overflow ? 0x40 : 0) + (sign ? 0x80 : 0));
    }

    private final void setStatusByte(int status) {
        carry = (status & 0x01) != 0;
        zero = (status & 0x02) != 0;
        disableInterupt = (status & 0x04) != 0;
        decimal = (status & 0x08) != 0;
        brk = (status & 0x10) != 0;
        overflow = (status & 0x40) != 0;
        sign = (status & 0x80) != 0;
    }

    private final void setZS(int data) {
        zero = data == 0;
        sign = data > 0x7f;
    }

    private final void setCarry(int data) {
        carry = data > 0x7f;
    }

    // -------------------------------------------------------------------
    // Old m4 macros as methods... should be replaced some day (with above)
    // -------------------------------------------------------------------
    // Stack operations...
    // can access array directly 's' is filed with one byte
    private final int pop() {
        int r = fetchByte((s = (s + 1) & 0xff) | 0x100);
        return r;
    }

    // can access array directly 's' is filed with one byte
    private final void push(int data) {
        writeByte((s & 0xff) | 0x100, data);
        s = (s - 1) & 0xff;
    }

    private final void opADCimp(int data) {
        int tmp = data + acc + (carry ? 1 : 0);
        zero = (tmp & 0xff) == 0; // not valid in decimal mode

        if (decimal) {
            tmp = (acc & 0xf) + (data & 0xf) + (carry ? 1 : 0);
            if (tmp > 0x9)
                tmp += 0x6;
            if (tmp <= 0x0f)
                tmp = (tmp & 0xf) + (acc & 0xf0) + (data & 0xf0);
            else
                tmp = (tmp & 0xf) + (acc & 0xf0) + (data & 0xf0) + 0x10;

            overflow = (((acc ^ data) & 0x80) == 0) && (((acc ^ tmp) & 0x80) != 0);

            sign = (tmp & 0x80) > 0;

            if ((tmp & 0x1f0) > 0x90)
                tmp += 0x60;
            carry = tmp > 0x99;
        } else {
            overflow = (((acc ^ data) & 0x80) == 0) && (((acc ^ tmp) & 0x80) != 0);
            carry = tmp > 0xff;
            sign = (tmp & 0x80) > 0;
        }
        acc = tmp & 0xff;
    }

    private final void branch(boolean branch, int adr, int cycDiff) {
        if (branch) {
            int oldPC = pc;
            pc = adr;
            /* correct branch */
            if (cycDiff == 1) {
                fetchByte(pc);
            } else {
                if (pc < oldPC)
                    fetchByte(pc + 0x100);
                else
                    fetchByte(pc - 0x100);
                fetchByte(pc); // Should be fwd or backwd...
            }
        }
    }

    private final void opSBCimp(int data) {
        int tmp = acc - data - (carry ? 0 : 1);
        boolean nxtcarry = (tmp >= 0);
        tmp = tmp & 0x1ff; // Carry is set!
        sign = (tmp & 0x80) == 0x80; // Invalid in decimal mode??
        zero = ((tmp & 0xff) == 0);
        overflow = (((acc ^ tmp) & 0x80) != 0) && (((acc ^ data) & 0x80) != 0);
        if (decimal) {
            tmp = (acc & 0xf) - (data & 0xf) - (carry ? 0 : 1);
            if ((tmp & 0x10) > 0)
                tmp = ((tmp - 6) & 0xf) | ((acc & 0xf0) - (data & 0xf0) - 0x10);
            else
                tmp = (tmp & 0xf) | ((acc & 0xf0) - (data & 0xf0));
            if ((tmp & 0x100) > 0)
                tmp -= 0x60;
        }
        acc = tmp & 0xff;
        carry = nxtcarry;
    }

    private void doReset() {
        sign = false;
        zero = false;
        overflow = false;
        carry = false;
        decimal = false;
        brk = false;

        disableInterupt = false;
        interruptInExec = 0;

        checkInterrupt = false;
        NMILow = false;
        NMILastLow = false;
        IRQLow = false;
        LOGGER.debug("Set IRQLOW to false...");
        resetFlag = false;

        pc = fetchByte(0xfffc) + (fetchByte(0xfffd) << 8);

        LOGGER.debug("Reset to: " + pc);
    }
}
