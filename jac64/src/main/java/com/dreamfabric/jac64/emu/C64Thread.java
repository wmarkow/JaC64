package com.dreamfabric.jac64.emu;

public abstract class C64Thread extends Thread {

    private boolean running = false;

    public C64Thread(String name) {
        super(name);
    }

    public abstract void executeInLoop();

    @Override
    public void run() {
        while (running) {
            executeInLoop();
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
