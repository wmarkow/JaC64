package com.dreamfabric.jac64.emu;

public interface SimulableIf {

    public void start(long currentCpuCycles);

    public void stop();

    public void reset();
}
