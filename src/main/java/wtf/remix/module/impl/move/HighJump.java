package wtf.remix.module.impl.move;

import wtf.remix.event.base.annotation.EventTarget;
import wtf.remix.event.impl.*;
import wtf.remix.module.Category;
import wtf.remix.module.Module;
import wtf.remix.module.value.impl.BoolValue;
import wtf.remix.module.value.impl.ModeValue;
import wtf.remix.module.value.impl.NumberValue;
import wtf.remix.util.network.PacketUtil;
import wtf.remix.util.player.MovementUtil;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;

public class HighJump extends Module {

    private final ModeValue mode = new ModeValue("Mode", "Grim", "Grim");

    private final BoolValue highPingMode = new BoolValue("High Ping Mode (May be slower)", false, () -> mode.is("Grim"));
    private final NumberValue speed = new NumberValue("Speed", 1.0f, 0.0f, 1.0f, 0.001f, () -> mode.is("Grim"));

    private int tickCounter;
    private int groundTicks;
    private boolean shouldJump;
    private boolean hasSentPacketsRecently;

    public HighJump() {
        super("HighJump", Category.Move);
    }

    @Override
    public void onEnable() {
        // 立即发送初始 C03 包，让服务端位置跟踪失效
        sendDesyncPackets();

        tickCounter = 0;
        groundTicks = 0;
        shouldJump = false;
        hasSentPacketsRecently = false;
    }

    @Override
    public void onDisable() {
        // Grim 模式无需特殊清理
    }

    @EventTarget
    public void onStrafe(StrafeEvent event) {
        if (!isGrim() || mc.player == null) return;

        groundTicks = mc.player.isOnGround() ? groundTicks + 1 : 0;
    }

    @EventTarget
    public void onMotion(MotionEvent event) {
        if (!isGrim() || mc.player == null) return;

        if (event.isPre()) {
            // 每 tick 重置，依赖 onPacket → velocity 来重设
            shouldJump = false;

            // 应用速度：偶数 tick 给予更强加速 (0.085)，奇数 tick 弱加速 (0.03)
            if (tickCounter > -1) {
                double speedMultiplier = 0.03;
                if (tickCounter % 2 == 0) {
                    speedMultiplier = mc.player.isOnGround() ? 0.085 : 0.03;
                }
                MovementUtil.strafe(speedMultiplier * speed.getValue().doubleValue());
            }
            tickCounter++;
        } else {
            // 偶数 tick 发伪造 C03 包，混淆服务端位置追踪
            if (tickCounter % 2 == 0) {
                sendDesyncPackets();
                hasSentPacketsRecently = true;
            }
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (!isGrim() || mc.player == null) return;

        if (shouldJump) {
            event.setJumping(true);
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!isGrim() || mc.player == null) return;

        if (event.getType() != PacketEvent.Type.Received) return;

        // 服务端拉回 / 位置同步 → 调整 tick 使其继续错开
        if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
            if (tickCounter % 2 == 1) {
                tickCounter++;
            }
        }

        // 受到击退 → 标记下次移动输入时跳跃
        if (event.getPacket() instanceof EntityVelocityUpdateS2CPacket velocity) {
            hasSentPacketsRecently = false;
            if (velocity.getEntityId() == mc.player.getId()) {
                shouldJump = true;
            }
        }
    }

    private boolean isGrim() {
        return mode.is("Grim");
    }

    private void sendDesyncPackets() {
        if (highPingMode.getValue()) {
            PacketUtil.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(false, false));
            PacketUtil.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(false, false));
        } else {
            PacketUtil.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true, false));
            PacketUtil.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(false, false));
        }
    }
}
