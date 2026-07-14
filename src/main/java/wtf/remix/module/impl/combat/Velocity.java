package wtf.remix.module.impl.combat;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import wtf.remix.event.base.annotation.EventTarget;
import wtf.remix.event.impl.MoveInputEvent;
import wtf.remix.event.impl.PacketEvent;
import wtf.remix.event.impl.TickEvent;
import wtf.remix.event.impl.WorldEvent;
import wtf.remix.module.Category;
import wtf.remix.module.Module;
import wtf.remix.module.value.impl.ModeValue;
import wtf.remix.module.value.impl.NumberValue;
import wtf.remix.util.player.RotationUtil;
import injection.accessor.EntityVelocityUpdateS2CPacketAccessor;
import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;

@Getter
public class Velocity extends Module {
    private final ModeValue mode = new ModeValue("Mode", "Normal", "Normal", "Packet", "Reduce", "Grim Exempt");
    private final NumberValue horizontal = new NumberValue("Horizontal", 0, 0, 100, 1, () -> mode.is("Packet"));
    private final NumberValue vertical = new NumberValue("Vertical", 0, 0, 100, 1, () -> mode.is("Packet"));
    private LivingEntity attackTarget = null;
    private boolean jump = false;
    private boolean attacking;
    private int reduceTicks;
    private int resetTicks;
    private boolean canCancel;
    private int lastLoggedTick = -1;

    public Velocity() {
        super("Velocity", Category.Combat);
    }

    @Override
    public void onEnable() {
        reset();
    }

    @Override
    public void onDisable() {
        reset();
    }

    @EventTarget
    public void onWorld(WorldEvent event) {
        reset();
    }

    private void reset() {
        attackTarget = null;
        attacking = false;
        reduceTicks = 0;
        resetTicks = 0;
        jump = false;
        canCancel = false;
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (mc.player == null) return;

        if (jump) {
            event.setJumping(true);
            jump = false;
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (mc.player == null) return;
        setSuffix(mode.getValue());
        Packet<?> packet = event.getPacket();
        if (event.getType() == PacketEvent.Type.Received) {
            if (packet instanceof EntityVelocityUpdateS2CPacket velocity) {
                if (velocity.getEntityId() == mc.player.getId()) {
                    switch (mode.getValue()) {
                        case "Normal" ->
                                event.setCancelled(true);

                        case "Packet" -> {
                            EntityVelocityUpdateS2CPacketAccessor accessor = (EntityVelocityUpdateS2CPacketAccessor) velocity;
                            double x = velocity.getVelocity().x * (horizontal.getValue() / 100.0);
                            double y = velocity.getVelocity().y * (vertical.getValue() / 100.0);
                            double z = velocity.getVelocity().z * (horizontal.getValue() / 100.0);
                            accessor.setVelocity(new Vec3d(x, y, z));
                        }

                        case "Reduce" -> {
                            if (velocity.getEntityId() == mc.player.getId() && velocity.getVelocity().y > 0) {
                                Entity entity = getEntity();
                                if (entity instanceof PlayerEntity livingEntity) {
                                    reduceTicks = 5;
                                    attackTarget = livingEntity;
                                    jump = true;
                                }
                            }
                        }

                        case "Grim Exempt" -> {
                            if (canCancel) {
                                event.setCancelled(true);
                                lastLoggedTick = mc.player.age;
                            }
                        }
                    }
                }
            }

            if (packet instanceof EntityDamageS2CPacket damage && damage.entityId() == mc.player.getId()) {
                canCancel = true;
            }
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.interactionManager == null) return;

        if (mode.is("Grim Exempt") && canCancel && lastLoggedTick != -1) {
            if (mc.player.age - lastLoggedTick >= 1) {
                int repeatCount = 4;
                ClientPlayNetworkHandler network = mc.getNetworkHandler();
                ClientPlayerEntity player = mc.player;
                for (int i = 0; i < repeatCount; i++) {
                    network.sendPacket(new PlayerMoveC2SPacket.Full(player.getX(), player.getY(), player.getZ(),
                            player.getYaw(), player.getPitch(), player.isOnGround(),
                            player.horizontalCollision));
                }
                network.sendPacket(
                        new PlayerActionC2SPacket(
                                PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK,
                                player.getBlockPos(),
                                player.getHorizontalFacing().getOpposite()
                        )
                );
                canCancel = false;
                lastLoggedTick = -1;
            }
        }

        if (mode.is("Reduce")) {
            if (resetTicks > 0) {
                resetTicks--;
                if (resetTicks <= 0) {
                    attacking = false;
                }
            }

            if (attackTarget != null && reduceTicks > 0) {
                if (RotationUtil.getDistanceToEntity(attackTarget) >= 3.0) {
                    return;
                }

                if (mc.player.isSprinting()) {
                    mc.player.setSprinting(false);
                    mc.interactionManager.attackEntity(mc.player, attackTarget);
                    mc.player.swingHand(Hand.MAIN_HAND);
                    Vec3d velocity = mc.player.getVelocity();
                    mc.player.setVelocity(velocity.x * 0.6, velocity.y, velocity.z * 0.6);
                    attackTarget = null;
                    reduceTicks--;
                    resetTicks = 3;
                    attacking = true;
                }
            }
        }
    }

    private Entity getEntity() {
        KillAura killAura = getModule(KillAura.class);
        HitResult hitResult = mc.crosshairTarget;
        Entity entity = null;

        if (killAura.isEnabled() && killAura.getTarget() != null) {
            entity = killAura.getTarget();
        } else {
            if (hitResult != null && hitResult.getType() == HitResult.Type.ENTITY) {
                entity = ((EntityHitResult) hitResult).getEntity();
            }
        }
        return entity;
    }
}