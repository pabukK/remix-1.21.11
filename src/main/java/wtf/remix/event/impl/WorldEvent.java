package wtf.remix.event.impl;

import wtf.remix.event.base.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.world.ClientWorld;

@Getter
@Setter
@AllArgsConstructor
public class WorldEvent extends Event {
    public final ClientWorld world;
}
