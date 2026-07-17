package wtf.remix.module.impl.move;

import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import wtf.remix.event.base.annotation.EventTarget;
import wtf.remix.event.impl.MotionEvent;
import wtf.remix.event.impl.PacketEvent;
import wtf.remix.event.impl.WorldEvent;
import wtf.remix.module.Category;
import wtf.remix.module.Module;
import wtf.remix.module.impl.combat.TargetStrafe;
import wtf.remix.module.impl.exploits.Disabler;
import wtf.remix.module.value.impl.ModeValue;
import wtf.remix.module.value.impl.NumberValue;
import wtf.remix.util.Util;
import wtf.remix.util.network.PacketUtil;
import wtf.remix.util.player.MovementUtil;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import java.util.LinkedList;
import java.util.Queue;

public final class Fly extends Module {
    public final ModeValue mode = new ModeValue("Mode", "Vanilla", "Vanilla", "Sentinel", "Grim");
    private final NumberValue horizontalSpeed = new NumberValue("Horizontal Speed", 3.5, .1, 10, .1);
    private final NumberValue verticalSpeed = new NumberValue("Vertical Speed", .7, .1, 5, .1);
    private int tick;

    // Grim mode state
    private boolean delayingPackets = false;
    private boolean receivedVelocity = false;
    private int grimTicks = 0;
    private final Queue<Packet<?>> delayedPackets = new LinkedList<>();

    public Fly() {
        super("Fly", Category.Move);
    }

    @Override
    public void onEnable() {
        tick = 0;
        grimTicks = 0;
        delayingPackets = false;
        receivedVelocity = false;
        delayedPackets.clear();
    }

    @Override
    public void onDisable() {
        instance.getPacketManager().getBlink().dispatch(this);
        MovementUtil.stop();

        if (mode.is("Grim")) {
            while (!delayedPackets.isEmpty()) {
                PacketUtil.sendPacketNoEvent(delayedPackets.poll());
            }
            delayingPackets = false;
            receivedVelocity = false;
            grimTicks = 0;
        }
    }

    @EventTarget
    public void onWorld(WorldEvent event) {
        if (mode.is("Grim")) {
            toggle();
            grimTicks = 0;
            delayingPackets = false;
            receivedVelocity = false;
            delayedPackets.clear();
        }
    }

    @EventTarget
    public void onMotion(MotionEvent event) {
        if (mc.player == null || check()) return;
        setSuffix(mode.getValue());

        TargetStrafe ts = getModule(TargetStrafe.class);
        boolean strafing = ts.isEnabled() && ts.getTarget() != null && (!ts.getSpace().getValue() || mc.options.jumpKey.isPressed());

        double targetY = 0.0;
        if (!strafing) {
            if (mc.options.jumpKey.isPressed()) {
                targetY = verticalSpeed.getValue().doubleValue();
            } else if (mc.options.sneakKey.isPressed()) {
                targetY = -verticalSpeed.getValue().doubleValue();
            }
        }

        switch (mode.getValue()) {
            case "Vanilla" -> {
                if (!strafing) {
                    mc.player.setVelocity(0.0, targetY, 0.0);
                } else {
                    mc.player.setVelocity(mc.player.getVelocity().x, 0.0, mc.player.getVelocity().z);
                }
                MovementUtil.strafe(horizontalSpeed.getValue().doubleValue());
            }

            case "Sentinel" -> {
                if (tick++ % 6 == 0) {
                    instance.getPacketManager().getBlink().start(this);
                    mc.player.setVelocity(strafing ? mc.player.getVelocity().x : 0.0, mc.player.getVelocity().y, strafing ? mc.player.getVelocity().z : 0.0);
                    MovementUtil.strafe(horizontalSpeed.getValue().doubleValue());
                } else if (!MovementUtil.isMoving()) {
                    mc.player.setVelocity(0.0, mc.player.getVelocity().y, 0.0);
                } else {
                    instance.getPacketManager().getBlink().dispatch(this);
                }
                mc.player.setVelocity(mc.player.getVelocity().x, targetY, mc.player.getVelocity().z);
            }

            case "Grim" -> {
                // Grim delay logic: after velocity, wait ~8 ticks then send START_FALL_FLYING
                if (event.isPre() && delayingPackets) {
                    grimTicks++;
                    if (grimTicks >= 8) {
                        PacketUtil.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                        grimTicks = 0;
                        delayingPackets = false;
                    }
                }

                // Vanilla-style flight control
                if (!strafing) {
                    mc.player.setVelocity(0.0, targetY, 0.0);
                } else {
                    mc.player.setVelocity(mc.player.getVelocity().x, 0.0, mc.player.getVelocity().z);
                }
                MovementUtil.strafe(horizontalSpeed.getValue().doubleValue());
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!mode.is("Grim") || mc.player == null) return;

        Packet<?> packet = event.getPacket();

        if (event.getType() == PacketEvent.Type.Send) {
            // Delay KeepAlive after velocity (replaces 1.8 C0FPacketConfirmTransaction)
            if (packet instanceof CommonPongC2SPacket && receivedVelocity) {
                event.setCancelled(true);
                if (delayedPackets.isEmpty()) {
                    delayingPackets = true;
                }
                delayedPackets.add(packet);
                Util.debug("Omg i delayed");
            }

            // Attack → flush delayed packets (replaces 1.8 C02PacketUseEntity)
            if (packet instanceof PlayerInteractEntityC2SPacket) {
                if (receivedVelocity && !delayedPackets.isEmpty()) {
                    while (!delayedPackets.isEmpty()) {
                        PacketUtil.sendPacketNoEvent(delayedPackets.poll());
                    }
                }
            }
        }

        if (event.getType() == PacketEvent.Type.Received) {
            // Position sync → flush delayed packets (replaces 1.8 S08PacketPlayerPosLook)
            if (packet instanceof PlayerPositionLookS2CPacket) {
                if (receivedVelocity && !delayedPackets.isEmpty()) {
                    while (!delayedPackets.isEmpty()) {
                        PacketUtil.sendPacketNoEvent(delayedPackets.poll());
                    }
                }
            }

            // Cancel velocity for ourselves (replaces 1.8 S12PacketEntityVelocity)
            if (packet instanceof EntityVelocityUpdateS2CPacket v) {
                if (v.getEntityId() == mc.player.getId()) {
                    if (receivedVelocity || !delayedPackets.isEmpty()) {
                        return;
                    }
                    grimTicks = 0;
                    receivedVelocity = true;
                    event.setCancelled(true);
                }
            }
        }
    }

    private boolean check() {
        return mode.is("Sentinel") && getModule(Disabler.class).isWaiting();
    }
}