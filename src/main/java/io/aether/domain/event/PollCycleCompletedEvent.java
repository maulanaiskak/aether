package io.aether.domain.event;

import io.aether.domain.Location;
import org.springframework.context.ApplicationEvent;

public class PollCycleCompletedEvent extends ApplicationEvent {

    private final Location location;

    public PollCycleCompletedEvent(Object source, Location location) {
        super(source);
        this.location = location;
    }

    public Location location() {
        return location;
    }
}
