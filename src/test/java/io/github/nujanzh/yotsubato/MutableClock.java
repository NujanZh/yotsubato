package io.github.nujanzh.yotsubato;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

public class MutableClock extends Clock {

    private volatile Instant currentInstant;
    private final ZoneId zone;

    public MutableClock(Instant currentInstant, ZoneId zone) {
        this.currentInstant = currentInstant;
        this.zone = zone;
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new MutableClock(currentInstant, zone);
    }

    @Override
    public Instant instant() {
        return currentInstant;
    }

    public void advance(Duration duration) {
        currentInstant = currentInstant.plus(duration);
    }

    public void setInstant(Instant instant) {
        currentInstant = instant;
    }
}
