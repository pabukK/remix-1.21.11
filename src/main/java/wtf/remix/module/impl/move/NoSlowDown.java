package wtf.remix.module.impl.move;

import net.minecraft.network.packet.c2s.play.SlotChangedStateC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import wtf.remix.event.base.annotation.EventTarget;
import wtf.remix.event.impl.MotionEvent;
import wtf.remix.event.impl.SlowEvent;
import wtf.remix.event.impl.UpdateEvent;
import wtf.remix.module.Category;
import wtf.remix.module.Module;
import wtf.remix.module.value.impl.BoolValue;
import wtf.remix.module.value.impl.ModeValue;

public class NoSlowDown extends Module {
    private final ModeValue mode = new ModeValue("Mode", "Vanilla", "Vanilla", "Grim");
    private final ModeValue grimMode = new ModeValue("Grim Mode", "Duo-Switch", () -> mode.is("Grim"), "Duo-Switch", "Tri-Switch");
    private final BoolValue keepSprint = new BoolValue("Keep Sprint", true);

    public NoSlowDown() {
        super("NoSlowDown", Category.Move);
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (mc.player == null || mc.world == null) return;

        setSuffix(mode.getValue());
    }

    @EventTarget
    public void onMotion(MotionEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (!mc.player.isUsingItem() || mc.player.getActiveItem().isEmpty()) return;

        if (mode.is("Grim")) {
            if (grimMode.is("Tri-Switch") || grimMode.is("Duo-Switch")) {
                mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().getSelectedSlot() % 8 + 1));
                if (grimMode.is("Tri-Switch")) {
                    mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().getSelectedSlot() % 7 + 2));
                }
                mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().getSelectedSlot()));
            }
        }
    }

    @EventTarget
    public void onSlow(SlowEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (mc.player.isUsingItem() && !mc.player.getActiveItem().isEmpty()) {
            if (mode.is("Vanilla") || mode.is("Grim")) {
                event.setCancelled(true);
            }

            if (keepSprint.getValue()) {
                mc.player.setSprinting(true);
            }
        }
    }
}