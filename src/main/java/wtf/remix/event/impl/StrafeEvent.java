package wtf.remix.event.impl;

import wtf.remix.event.base.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class StrafeEvent extends Event {
    private float yaw;
}