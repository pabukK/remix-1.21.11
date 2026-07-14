package wtf.remix.management;

import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import wtf.remix.event.base.annotation.EventPriority;
import wtf.remix.event.base.annotation.EventTarget;
import wtf.remix.event.impl.*;
import wtf.remix.management.movement.MovementCorrection;
import wtf.remix.module.impl.combat.KillAura;
import wtf.remix.module.impl.world.Scaffold;
import wtf.remix.util.IMinecraft;
import wtf.remix.util.player.MovementUtil;
import wtf.remix.util.player.RotationUtil;

/**
 * RotationManager
 * @author DSJ
 */
public class RotationManager implements IMinecraft {
    public static float[] currentRotations;
    public static float[] targetRotations;
    public static float[] lastRotations;

    public static MovementCorrection correctMovement;
    private static double rotationSpeed;
    private static boolean enabled;

    public RotationManager() {
        instance.getEventManager().register(this);
    }

    public static void setRotations(float[] rotations, double rotationSpeed, MovementCorrection correctMovement) {
        RotationManager.targetRotations = rotations;
        RotationManager.rotationSpeed = rotationSpeed;
        RotationManager.correctMovement = correctMovement;

        enabled = true;
    }

    @EventTarget
    @EventPriority(999)
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (mc.player == null) return;

        KillAura killAura = instance.getModuleManager().getModule(KillAura.class);
        Scaffold scaffold = instance.getModuleManager().getModule(Scaffold.class);

        if (scaffold.isEnabled() && scaffold.isCanRotation() && scaffold.getRotations() != null) {
            if (!scaffold.getInstantRot().getValue()) {
                setRotations(scaffold.getRotations(), scaffold.getRotationSpeed().getValue(), scaffold.getMovementFix().getValue() ? MovementCorrection.Silent : MovementCorrection.None);
            } else {
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getEntityPos(), scaffold.getRotations()[0], scaffold.getRotations()[1], mc.player.isOnGround(), mc.player.horizontalCollision));
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getEntityPos(), mc.player.getYaw(), mc.player.getPitch(), mc.player.isOnGround(), mc.player.horizontalCollision));
            }
        } else if (killAura.isEnabled() && killAura.getTarget() != null && killAura.getRotations() != null) {
            setRotations(killAura.getRotations(), killAura.getRotationSpeed().getValue(), killAura.getMovementFixMode().is("None") ? MovementCorrection.None : (killAura.getMovementFixMode().is("Silent") ? MovementCorrection.Silent : MovementCorrection.Strict));
        } else {
            enabled = false;
        }


        lastRotations = currentRotations;
        currentRotations = RotationUtil.getSmoothRotation(lastRotations, targetRotations, rotationSpeed + Math.random());
        mc.gameRenderer.updateCrosshairTarget(1.0f);
    }

    @EventTarget
    @EventPriority(999)
    public void onLook(LookEvent e) {
        if (mc.player == null) return;

        if (canRotation()) {
            e.setRotation(currentRotations);
            e.setLastRotation(lastRotations);
        }
    }

    @EventTarget
    @EventPriority(999)
    public void onStrafe(StrafeEvent e) {
        if (mc.player == null) return;

        if (canRotation() && correctMovement != MovementCorrection.None) {
            e.setYaw(currentRotations[0]);
        }
    }

    @EventTarget
    @EventPriority(999)
    public void onJump(JumpEvent e) {
        if (mc.player == null) return;

        if (canRotation() && correctMovement != MovementCorrection.None) {
            e.setYaw(currentRotations[0]);
        }
    }

    @EventTarget
    @EventPriority(999)
    public void onMotion(MotionEvent e) {
        if (mc.player == null) return;

        if (e.isPre()) {
            if (!enabled || currentRotations == null || lastRotations == null || targetRotations == null) {
                currentRotations = targetRotations = lastRotations = new float[]{mc.player.getYaw(), mc.player.getPitch()};
            }

            if (canRotation()) {
                e.setYaw(currentRotations[0]);
                e.setPitch(currentRotations[1]);
            }
        }
    }

    @EventTarget
    @EventPriority(999)
    public void onMoveInput(MoveInputEvent e) {
        if (canRotation() && correctMovement == MovementCorrection.Silent) {
            MovementUtil.fixMovement(e, currentRotations[0]);
        }
    }

    @EventTarget
    @EventPriority(999)
    public void onRotation(RenderRotationEvent e) {
        if (mc.player == null) return;

        if (canRotation()) {
            e.setRotation(currentRotations);
            e.setLastRotation(lastRotations);
        }
    }

    public boolean canRotation() {
        return enabled && currentRotations != null && lastRotations != null && targetRotations != null;
    }
}
