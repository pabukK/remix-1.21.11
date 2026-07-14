package wtf.remix.event.impl;

import wtf.remix.event.base.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.entity.Entity;

@Getter
@AllArgsConstructor
public class AttackEvent extends Event {
    private final Entity entity;
}
