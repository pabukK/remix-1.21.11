package wtf.remix.management;

import wtf.remix.event.base.annotation.EventTarget;
import wtf.remix.event.impl.PacketEvent;
import wtf.remix.event.impl.WorldEvent;
import wtf.remix.management.packet.impl.Blink;
import wtf.remix.management.packet.impl.Delay;
import wtf.remix.util.IMinecraft;
import lombok.Getter;

@Getter
public class PacketManager implements IMinecraft {
    private final Delay delay = new Delay();
    private final Blink blink = new Blink();

    public PacketManager() {
        instance.getEventManager().register(this);
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (mc.player == null) {
            delay.clear();
            blink.clear();
            return;
        }

        if (mc.player.isDead() || mc.getNetworkHandler() == null || mc.isInSingleplayer()) {
            delay.dispatch(true);
            blink.dispatch(true);
            return;
        }

        switch (event.getType()) {
            case Received -> delay.handle(event);
            case Send -> blink.handle(event);
        }
    }

    @EventTarget
    public void onWorld(WorldEvent event) {
        delay.dispatch(true);
        blink.dispatch(true);
    }
}