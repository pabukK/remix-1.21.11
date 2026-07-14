package wtf.remix.event.impl;

import wtf.remix.event.base.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class LookEvent extends Event {
    private float[] rotation;
    private float[] lastRotation;
}