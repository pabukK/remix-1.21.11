package wtf.remix.module.impl.player;

import injection.accessor.MinecraftClientAccessor;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import wtf.remix.event.base.annotation.EventTarget;
import wtf.remix.event.impl.TickEvent;
import wtf.remix.module.Category;
import wtf.remix.module.Module;
import wtf.remix.util.network.PacketUtil;
import wtf.remix.util.player.ItemUtil;

/**
 * @author kevin
 * @since 2026/7/14
 */

public class Gapple extends Module {
    public Gapple() {
        super("AutoGapple", Category.Player);
    }

    @Override
    public void onEnable() {
        int itemInHotbar = ItemUtil.getItemInHotbar(Items.GOLDEN_APPLE);
        PacketUtil.sendPacket(new UpdateSelectedSlotC2SPacket(itemInHotbar));
        PacketUtil.sendSequencedPacket(sequence -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, sequence, mc.player.getYaw(), mc.player.getPitch()));
        // ((MinecraftClientAccessor) mc).idoItemUse();
        for (int i = 0; i < 35; i++) {
            PacketUtil.sendPacket(new PlayerMoveC2SPacket.Full(
                    mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                    mc.player.getYaw(), mc.player.getPitch(),
                    mc.player.isOnGround(), mc.player.horizontalCollision));
            PacketUtil.sendSequencedPacket(sequence -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, sequence, mc.player.getYaw(), mc.player.getPitch()));
        }
        PacketUtil.sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().getSelectedSlot()));

        toggle();
    }
}
