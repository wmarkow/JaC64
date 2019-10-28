package com.dreamfabric.jac64.emu.sid;

import com.dreamfabric.jac64.AudioDriver;
import com.dreamfabric.jac64.AudioDriverSE;
import com.dreamfabric.jac64.emu.vic.C64Screen;
import com.dreamfabric.resid.ISIDDefs;
import com.dreamfabric.resid.ISIDDefs.sampling_method;
import com.dreamfabric.resid.SID;

public class RESID extends VoidSID {

    static final int SAMPLE_RATE = 44000;
    static final int DL_BUFFER_SIZE = 44000;

    int BUFFER_SIZE = 256;
    byte[] buffer = new byte[BUFFER_SIZE * 2];

    SID sid;
    int CPUFrq = 985248;
    int clocksPerSample = CPUFrq / SAMPLE_RATE;
    private long nanosPerSample = (long) (1e9 / SAMPLE_RATE);
    private int pos = 0;
    private AudioDriver audioDriver;

    private Thread thread;
    private long nextExecInNanos = 0;

    public RESID() {
        audioDriver = new AudioDriverSE();
        audioDriver.init(44000, 22000);
        audioDriver.setSoundOn(true);

        sid = new SID();

        sid.set_sampling_parameters(CPUFrq, sampling_method.SAMPLE_FAST, SAMPLE_RATE, -1, 0.97);

        setChipVersion(C64Screen.RESID_6581);
        thread = new Thread(new Runnable() {

            @Override
            public void run() {
                while (true) {
                    execute();
                }
            }
        });

        thread.start();
    }

    public void clock(long cycles) {
    }

    @Override
    public boolean write(int address, byte data) {
        if (super.write(address, data)) {
            sid.write(address - getStartAddress(), data);

            return true;
        }

        return false;
    }

    @Override
    public Byte read(int address) {
        Byte result = super.read(address);

        if (result != null) {
            Integer integer = sid.read(address - getStartAddress());
            return integer.byteValue();
        }

        return null;
    }

    public void reset() {
        sid.reset();
    }

    public void stop() {
        // Called from any thread!
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

         nextExecInNanos = nanos + nanosPerSample;
    }

    private void writeSamples() {
        audioDriver.write(buffer);
        pos = 0;
    }
}
