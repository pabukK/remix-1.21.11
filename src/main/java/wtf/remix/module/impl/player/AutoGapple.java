package wtf.remix.module.impl.player;

import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import wtf.remix.event.base.annotation.EventTarget;
import wtf.remix.event.impl.UpdateEvent;
import wtf.remix.module.Category;
import wtf.remix.module.Module;
import wtf.remix.module.value.impl.BoolValue;
import wtf.remix.module.value.impl.NumberValue;
import wtf.remix.util.Util;
import wtf.remix.util.misc.TimerUtil;
import wtf.remix.util.network.PacketUtil;
import wtf.remix.util.player.ItemUtil;

/**
 * @author kevin
 * @since 2026/7/13
 */

public class AutoGapple extends Module {
    private final BoolValue auto = new BoolValue("Auto", true);
    private final NumberValue health = new NumberValue("Health", 10, 1, 20, 1, auto::getValue);
    private final NumberValue delay = new NumberValue("Delay", 150, 0, 1000, 50, auto::getValue);

    private final TimerUtil timer = new TimerUtil();

    public AutoGapple() {
        super("AutoGapple", Category.Player);
    }

    @Override
    public void onEnable() {
        timer.reset();
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (mc.player == null) return;

        if (auto.getValue()) {
            if (!timer.finished(delay.getValue().longValue()) || mc.player.getHealth() > health.getValue()) {
                return;
            }

            doEat();
            Util.debug("Should eat");
            timer.reset();
        } else {
            doEat();
            toggle();
        }
    }

    private void doEat() {
        int itemInHotbar = ItemUtil.getItemInHotbar(Items.GOLDEN_APPLE);

        if (itemInHotbar != -1) {
            final float yaw = mc.player.getYaw();
            final float pitch = mc.player.getPitch();

            PacketUtil.sendPacket(new UpdateSelectedSlotC2SPacket(itemInHotbar));
            PacketUtil.sendSequencedPacket(sequence -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, sequence, yaw, pitch));
            for (int i = 1; i <= 35; i++) {
                PacketUtil.sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getEntityPos(), yaw, pitch, mc.player.isOnGround(), mc.player.horizontalCollision));
            }
            mc.player.stopUsingItem();
            PacketUtil.sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().getSelectedSlot()));
        } else {
            Util.log("Gapple not found in hotbar!");
            toggle();
        }
    }
}