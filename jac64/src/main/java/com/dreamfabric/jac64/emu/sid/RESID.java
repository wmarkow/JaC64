package com.dreamfabric.jac64.emu.sid;

import com.dreamfabric.jac64.emu.bus.AddressableChip;
import com.dreamfabric.jac64.emu.scheduler.EventQueue;
import com.dreamfabric.jac64.emu.scheduler.TimeEvent;
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

    public static final int RESID_6581 = 1;
    public static final int RESID_8580 = 2;

    static final int SAMPLE_RATE = 44000;
    static final int DL_BUFFER_SIZE = 44000;

    int BUFFER_SIZE = 256;
    byte[] buffer = new byte[BUFFER_SIZE * 2];

    private SID sid;
    private int CPUFrq = 985248;
    private int clocksPerSample = CPUFrq / SAMPLE_RATE;
    private int pos = 0;
    private AudioDriver audioDriver;
    private EventQueue scheduler;

    public RESID(EventQueue scheduler) {
        audioDriver = new AudioDriverSE();
        audioDriver.init(44000, 22000);
        audioDriver.setSoundOn(true);

        sid = new SID();

        sid.set_sampling_parameters(CPUFrq, sampling_method.SAMPLE_FAST, SAMPLE_RATE, -1, 0.97);
        setChipVersion(RESID_6581);

        this.scheduler = scheduler;
    }

    TimeEvent updateEvent = new TimeEvent(0) {
        public void execute(long currentCpuCycles) {
            RESID.this.execute();

            scheduler.addEvent(this, currentCpuCycles + clocksPerSample);
        }
    };

    public void clock(long cycles) {
    }

    @Override
    public boolean write(int address, int data, long currentCpuCycles) {
        if (super.write(address, data, currentCpuCycles)) {
            sid.write(address - getStartAddress(), data);

            return true;
        }

        return false;
    }

    @Override
    public Integer read(int address, long currentCpuCycles) {
        Integer result = super.read(address, currentCpuCycles);

        if (result != null) {
            return sid.read(address - getStartAddress());
        }

        return null;
    }

    @Override
    public void start(long currentCpuCycles) {
        scheduler.addEvent(updateEvent, currentCpuCycles + clocksPerSample);
    }

    @Override
    public void stop() {
        scheduler.removeEvent(updateEvent);
    }

    @Override
    public void reset() {
        sid.reset();
        scheduler.addEvent(updateEvent, clocksPerSample);
    }

    public void setChipVersion(int version) {
        if (version == RESID_6581) {
            sid.set_chip_model(ISIDDefs.chip_model.MOS6581);
        } else {
            sid.set_chip_model(ISIDDefs.chip_model.MOS8580);
        }
    }

    private void execute() {
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
    }

    private void writeSamples() {
        audioDriver.write(buffer);
        pos = 0;
    }
}
