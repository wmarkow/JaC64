package com.dreamfabric.jac64.emu;

public abstract class TimeEvent {
  // For linking events...
  TimeEvent nextEvent;
  TimeEvent prevEvent;
  boolean scheduled = false;

String name;
  
  protected long time;

  public TimeEvent(long time) {
    this.time = time;
  }

  public TimeEvent(long time, String name) {
    this.time = time;
    this.name = name;
  }

  public final long getTime() {
    return time;
  }
  
  public void setTime(long time)
  {
      this.time = time;
  }

  public abstract void execute(long t);

  public String getShort() {
    return "" + time + (name != null ? ": " + name : "");
  }

  public boolean isScheduled() {
      return scheduled;
  }
} // TimeEvent