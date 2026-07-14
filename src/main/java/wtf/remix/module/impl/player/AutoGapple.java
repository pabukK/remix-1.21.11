package wtf.remix.module.impl.player;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import wtf.remix.Client;
import wtf.remix.event.base.annotation.EventTarget;
import wtf.remix.event.impl.UpdateEvent;
import wtf.remix.module.Category;
import wtf.remix.module.Module;
import wtf.remix.module.impl.world.Scaffold;
import wtf.remix.module.value.impl.BoolValue;
import wtf.remix.module.value.impl.NumberValue;
import wtf.remix.util.Util;
import wtf.remix.util.misc.TimerUtil;
import wtf.remix.util.network.PacketUtil;

/**
 * Auto eat golden apples when health is low.
 *
 * Reference: LiquidBounce ModuleAutoBuff — Gapple feature
 * Design: state machine with silent hotbar switching,
 * offhand detection, and proper eat-completion tracking.
 *
 * @author kevin
 * @since 2026/7/14
 */

public class AutoGapple extends Module {

    private final BoolValue auto = new BoolValue("Auto", true);
    private final NumberValue health = new NumberValue("Health", 10, 1, 20, 1, auto::getValue);
    private final NumberValue delay = new NumberValue("Delay", 150, 0, 1000, 50, auto::getValue);
    private final BoolValue silent = new BoolValue("Silent", true);

    private final TimerUtil timer = new TimerUtil();

    // ── state machine ──────────────────────────────────────────
    private enum State { IDLE, EATING }

    private State state = State.IDLE;
    private int tickCounter = 0;         // ticks spent in current state
    private int prevSlot = -1;           // slot to restore after eat

    // Golden apple takes 32 ticks (1.6 s) to eat; allow margin.
    private static final int GAPPLE_EAT_TICKS = 32;
    private static final int EAT_TIMEOUT   = 60;

    public AutoGapple() {
        super("AutoGapple", Category.Player);
    }

    // ── lifecycle ──────────────────────────────────────────────

    @Override
    public void onEnable() {
        timer.reset();
        resetState();
    }

    @Override
    public void onDisable() {
        restoreSlot();
        resetState();
    }

    private void resetState() {
        state = State.IDLE;
        tickCounter = 0;
        prevSlot = -1;
    }

    // ── update loop ────────────────────────────────────────────

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (mc.player == null || mc.world == null) return;

        // Pause when dead, creative, spectator, or inventory open
        if (shouldSkip()) {
            abortIfEating();
            return;
        }

        switch (state) {
            case IDLE   -> tickIdle();
            case EATING -> tickEating();
        }
    }

    // ── idle: find gapple & start eating ───────────────────────

    private void tickIdle() {
        // Auto-mode guards
        if (auto.getValue()) {
            if (!timer.finished(delay.getValue().longValue())) return;
            if (mc.player.getHealth() > health.getValue())       return;
        }

        // Don't interrupt existing item use
        if (mc.player.isUsingItem()) return;

        // Priority: offhand → main hand → hotbar
        if (isGapple(mc.player.getOffHandStack())) {
            beginEat(Hand.OFF_HAND, false);
            return;
        }

        if (isGapple(mc.player.getMainHandStack())) {
            beginEat(Hand.MAIN_HAND, false);
            return;
        }

        int slot = findGappleInHotbar();
        if (slot == -1) {
            if (!auto.getValue()) {
                Util.log("No golden apples found in hotbar!");
                toggle();
            }
            return;   // nothing to eat
        }

        // Switch to the gapple slot
        prevSlot = mc.player.getInventory().getSelectedSlot();

        if (silent.getValue()) {
            // Server-only switch (client doesn't visually move)
            PacketUtil.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        } else {
            mc.player.getInventory().setSelectedSlot(slot);
        }

        beginEat(Hand.MAIN_HAND, true);
    }

    /**
     * Send the sequenced interact packet to start eating.
     */
    private void beginEat(Hand hand, boolean switched) {
        PacketUtil.sendSequencedPacket(sequence ->
                new PlayerInteractItemC2SPacket(hand, sequence,
                        mc.player.getYaw(), mc.player.getPitch()));

        tickCounter = 0;
        state = State.EATING;
    }

    // ── eating: wait for completion ────────────────────────────

    private void tickEating() {
        tickCounter++;

        // Periodically sync position so the server doesn't desync
        if (tickCounter % 5 == 0) {
            PacketUtil.sendPacket(new PlayerMoveC2SPacket.Full(
                    mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                    mc.player.getYaw(), mc.player.getPitch(),
                    mc.player.isOnGround(), mc.player.horizontalCollision));
        }

        // Two completion signals:
        //   Client-side: player stopped using item (only reliable in visible mode)
        //   Server-side: enough ticks have passed for the gapple to be consumed
        boolean clientDone = !mc.player.isUsingItem() && tickCounter >= GAPPLE_EAT_TICKS;
        boolean timedOut    = tickCounter >= EAT_TIMEOUT;

        if (clientDone || timedOut) {
            finishEat();
        }
    }

    private void finishEat() {
        restoreSlot();
        timer.reset();

        if (!auto.getValue()) {
            // Manual mode: one-shot
            toggle();
        } else {
            state = State.IDLE;
        }
    }

    // ── helpers ────────────────────────────────────────────────

    private boolean shouldSkip() {
        return mc.player.isDead()
                || mc.player.isCreative()
                || mc.player.isSpectator()
                || mc.currentScreen != null || Client.instance.getModuleManager().getModule(Scaffold.class).isEnabled();
    }

    /** If we were eating and something interrupted us, clean up. */
    private void abortIfEating() {
        if (state != State.IDLE) {
            restoreSlot();
            resetState();
        }
    }

    private int findGappleInHotbar() {
        for (int i = 0; i < 9; i++) {
            if (isGapple(mc.player.getInventory().getStack(i))) {
                return i;
            }
        }
        return -1;
    }

    private boolean isGapple(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.getItem() == Items.GOLDEN_APPLE
                ||  stack.getItem() == Items.ENCHANTED_GOLDEN_APPLE);
    }

    /** Restore the previously-held server slot. */
    private void restoreSlot() {
        if (prevSlot == -1) return;

        PacketUtil.sendPacket(new UpdateSelectedSlotC2SPacket(prevSlot));
        if (!silent.getValue()) {
            mc.player.getInventory().setSelectedSlot(prevSlot);
        }
        prevSlot = -1;
    }
}
