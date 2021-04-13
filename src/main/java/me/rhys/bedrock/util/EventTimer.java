package me.rhys.bedrock.util;

import me.rhys.bedrock.base.user.User;

public class EventTimer {
    private int tick;
    private final int max;
    private final User user;

    public EventTimer(int max, User user) {
        this.tick = 0;
        this.max = max;
        this.user = user;
        this.reset();
    }

    public boolean hasNotPassed() {
        int maxTick = this.max + this.user.getConnectionProcessor().getClientTick();
        return (this.user.getTick() > maxTick && (this.user.getTick() - tick) < maxTick);
    }

    public boolean passed() {
        int maxTick = this.max + this.user.getConnectionProcessor().getClientTick();
        return (this.user.getTick() > maxTick && (this.user.getTick() - tick) > maxTick);
    }

    public void reset() {
        this.tick = this.user.getTick();
    }
}