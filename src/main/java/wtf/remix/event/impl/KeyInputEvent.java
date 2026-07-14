package wtf.remix.event.impl;

import wtf.remix.event.base.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class KeyInputEvent extends Event {
    private int key;
}
