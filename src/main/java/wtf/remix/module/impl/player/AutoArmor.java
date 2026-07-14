package wtf.remix.module.impl.player;

import wtf.remix.event.base.annotation.EventTarget;
import wtf.remix.event.impl.MotionEvent;
import wtf.remix.module.Category;
import wtf.remix.module.Module;
import wtf.remix.module.value.impl.BoolValue;
import wtf.remix.module.value.impl.NumberValue;
import wtf.remix.util.misc.TimerUtil;
import wtf.remix.util.player.ClickSlotUtil;
import wtf.remix.util.player.ItemUtil;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.screen.slot.Slot;

public class AutoArmor extends Module {

    private final BoolValue inventoryOnly = new BoolValue("Inventory Only", false);
    private final NumberValue delay = new NumberValue("Delay", 50, 0, 1000, 10);

    private final TimerUtil armorTimer = new TimerUtil();

    public AutoArmor() {
        super("AutoArmor", Category.Player);
    }

    @EventTarget
    public void onMotion(MotionEvent event) {
        if (mc.player == null || event.isPost()) return;

        boolean instant = delay.getValue().doubleValue() == 0.0;
        setSuffix(String.format("%.1f", delay.getValue()));

        if (inventoryOnly.getValue()) {
            if (!(mc.currentScreen instanceof InventoryScreen)) return;
        } else {
            if (mc.currentScreen != null && !(mc.currentScreen instanceof InventoryScreen)) return;
        }

        if (instant || armorTimer.hasTimeElapsed(delay.getValue().longValue())) {
            EquipmentSlot[] armorSlots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

            for (EquipmentSlot slot : armorSlots) {
                if (equipArmor(slot, ItemUtil.getBestArmorSlot(slot)) && !instant) {
                    break;
                }
            }
        }
    }

    private boolean equipArmor(EquipmentSlot equipmentSlot, int bestSlot) {
        if (mc.player == null) return false;

        if (bestSlot >= 9 && bestSlot <= 44) {
            int targetSlot = switch (equipmentSlot) {
                case HEAD -> 5;
                case CHEST -> 6;
                case LEGS -> 7;
                case FEET -> 8;
                default -> -1;
            };

            if (targetSlot != -1) {
                Slot currentArmorSlot = mc.player.currentScreenHandler.getSlot(targetSlot);
                if (currentArmorSlot.hasStack()) {
                    ClickSlotUtil.dropAll(targetSlot);
                    armorTimer.reset();
                    return true;
                }
            }

            ClickSlotUtil.shiftClick(bestSlot);
            armorTimer.reset();
            return true;
        }
        return false;
    }
}