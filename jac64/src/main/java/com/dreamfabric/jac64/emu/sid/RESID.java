package com.dreamfabric.jac64.emu.sid;

import com.dreamfabric.jac64.emu.bus.AddressableChip;
import com.dreamfabric.jac64.emu.sid.SIDIf;
import com.dreamfabric.jac64.emu.vic.C64Screen;
import com.dreamfabric.resid.ISIDDefs;
import com.dreamfabric.resid.ISIDDefs.sampling_method;
import com.dreamfabric.resid.SID;

/***
 * Implementation of {@link SIDIf} which uses reSID algorithm to emulate SID
 * chip.
 * 
 * @see {@link com.dreamfabric.resid.*}
 * 
 * @author Witold Markowski
 *
 */
public class RESID extends AddressableChip implements SIDIf {

    static final int SAMPLE_RATE = 44000;
    static final int DL_BUFFER_SIZE = 44000;

    int BUFFER_SIZE = 256;
    byte[] buffer = new byte[BUFFER_SIZE * 2];

    private SID sid;
    private int CPUFrq = 985248;
    private int clocksPerSample = CPUFrq / SAMPLE_RATE;
    private long nanosPerSample = (long) (1e9 / SAMPLE_RATE);
    private int pos = 0;
    private long nextExecInNanos = 0;
    private AudioDriver audioDriver;

    private RESIDThread thread;

    public RESID() {
        audioDriver = new AudioDriverSE();
        audioDriver.init(44000, 22000);
        audioDriver.setSoundOn(true);

        sid = new SID();

        sid.set_sampling_parameters(CPUFrq, sampling_method.SAMPLE_FAST, SAMPLE_RATE, -1, 0.97);
        setChipVersion(C64Screen.RESID_6581);

    }

    public void clock(long cycles) {
    }

    @Override
    public boolean write(int address, int data) {
        if (super.write(address, data)) {
            sid.write(address - getStartAddress(), data);

            return true;
        }

        return false;
    }

    @Override
    public Integer read(int address) {
        Integer result = super.read(address);

        if (result != null) {
            return sid.read(address - getStartAddress());
        }

        return null;
    }

    @Override
    public void start() {
        if (thread != null && thread.isAlive()) {
            return;
        }

        thread = new RESIDThread();
        thread.start();
    }

    @Override
    public void stop() {
        thread.forceStop();
    }

    @Override
    public void reset() {
        sid.reset();
    }

    public void setChipVersion(int version) {
        if (version == C64Screen.RESID_6581) {
            sid.set_chip_model(ISIDDefs.chip_model.MOS6581);
        } else {
            sid.set_chip_model(ISIDDefs.chip_model.MOS8580);
        }
    }

    private void execute() {
        long nanos = System.nanoTime();
        if (nanos < nextExecInNanos) {
            return;
        }

        // Clock resid!
        for (int q = 0; q < clocksPerSample; q++) {
            sid.clock();
        }

        // and take the sample!
        int sample = sid.output();
        buffer[pos++] = (byte) (sample & 0xff);
        buffer[pos++] = (byte) ((sample >> 8));
        if (pos == buffer.length) {
            writeSamples();
        }

        nextExecInNanos = nanos + (long) (nanosPerSample * 0.95);
    }

    private void writeSamples() {
        audioDriver.write(buffer);
        pos = 0;
    }

    private class RESIDThread extends Thread {

        private boolean running = false;

        public RESIDThread() {
            super("reSID Thread");
        }

        @Override
        public void run() {
            while (running) {
                execute();
            }
        }

        @Override
        public synchronized void start() {
            running = true;
            super.start();
        }

        public synchronized void forceStop() {
            running = false;
        }
    }
}
