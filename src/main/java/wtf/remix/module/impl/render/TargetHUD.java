package wtf.remix.module.impl.render;

import net.minecraft.client.gui.screen.ChatScreen;
import wtf.remix.Client;
import wtf.remix.module.impl.combat.KillAura;
import wtf.remix.ui.font.TrueTypeFont;
import wtf.remix.ui.hud.Drag;
import wtf.remix.util.animation.Easing;
import wtf.remix.util.animation.EasingAnimation;
import wtf.remix.util.render.Render2D;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.awt.*;

public final class TargetHUD extends Drag {

    /* ── Layout ────────────────────────────────────────── */
    private static final int WIDTH = 180;
    private static final int HEIGHT = 55;

    /* face */
    private static final int AVATAR_SIZE = 36;
    private static final int AVATAR_MARGIN_LEFT = 5;
    private static final int AVATAR_TOP = 5;

    /* name (right of face) */
    private static final int TEXT_GAP = 8;
    private static final int NAME_TOP = 5;
    private static final int FONT_SIZE = 18;              // ↑ bigger

    /* armour (below name): slot background + item icon */
    private static final int ARMOR_GAP = 3;
    private static final int SLOT_SIZE = 18;               // gray square behind item
    private static final int ITEM_OFFSET = 1;              // centre 16×16 icon inside 18×18 slot
    private static final int SLOT_SPACING = 2;

    /* ping (vanilla sprite, scaled up with font) */
    private static final int PING_ICON_W = 12;             // ↑ bigger (from 10)
    private static final int PING_ICON_H = 10;             // ↑ bigger (from 8)
    private static final int PING_TOP = 6;
    private static final int PING_RIGHT_MARGIN = 6;

    /* health bar (bottom) */
    private static final int HEALTH_BAR_HEIGHT = 6;
    private static final int HEALTH_BAR_MARGIN = 5;
    private static final int HEALTH_BAR_BOTTOM = 5;

    /* ── Ping texture Identifiers (same as vanilla PlayerListHud) ── */
    private static final Identifier[] PING_TEXTURES = {
            Identifier.of("icon/ping_unknown"),       // 0  – offline
            Identifier.of("icon/ping_1"),              // 1  – ≥1000 ms
            Identifier.of("icon/ping_2"),              // 2  – 600…999
            Identifier.of("icon/ping_3"),              // 3  – 300…599
            Identifier.of("icon/ping_4"),              // 4  – 150…299
            Identifier.of("icon/ping_5"),              // 5  – ≤149
    };

    /* ── Armour slot order ── */
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    /* ── Animation ─────────────────────────────────────── */
    private final EasingAnimation healthAnimation = new EasingAnimation(Easing.EASE_OUT_QUART, 200);

    public TargetHUD() {
        super("TargetHUD");
        percentX = 0.95f;   // right-edge aligned
        percentY = 0.65f;   // lower-mid area
        this.width = WIDTH;
        this.height = HEIGHT;
    }

    @Override
    public boolean isRightAnchored() {
        return true;
    }

    /* ================================================================
       RENDER
    ================================================================ */
    @Override
    public void render(DrawContext context) {
        PlayerEntity player;
        KillAura ka = Client.instance.getModuleManager().getModule(KillAura.class);
        if (!ka.isEnabled() && !(mc.currentScreen instanceof ChatScreen)) return;

        if (ka.isEnabled()) {
            LivingEntity target = ka.getTarget();
            if (target == null || !target.isAlive() || target.getHealth() <= 0) return;
            if (!(target instanceof PlayerEntity playerW)) return;
            else player = playerW;
        } else {
            player = mc.player;
        }

        /* ── right-anchored position ── */
        final float rx = renderX - WIDTH;
        final float ry = renderY;

        /* ── font ── */
        final TrueTypeFont font = Client.instance.getFontManager().getFont(FONT_SIZE);

        /* ── 1. Background + border ── */
        Render2D.drawRect(context, rx, ry, WIDTH, HEIGHT, new Color(0, 0, 0, 160).getRGB());
        Render2D.drawOutline(context, rx, ry, WIDTH, HEIGHT, 1, new Color(30, 30, 35).getRGB());

        /* ── 2. Player face ── */
        final float faceX = rx + AVATAR_MARGIN_LEFT;
        final float faceY = ry + AVATAR_TOP;
        drawPlayerFace(context, player, faceX, faceY);

        /* ── 3. Player name ── */
        final float nameX = faceX + AVATAR_SIZE + TEXT_GAP;
        final float nameY = ry + NAME_TOP;
        final boolean whiteMode = Client.instance.getModuleManager().getModule(HUD.class).getWhiteMode().getValue();
        final int nameColor = whiteMode ? -1 : Client.instance.getModuleManager().getModule(HUD.class).getColor();
        font.drawStringWithShadow(context, player.getName().getString(), nameX, nameY, nameColor);

        /* ── 4. Armour items ── */
        final float armorY = nameY + font.getHeight() + ARMOR_GAP;
        drawArmor(context, player, nameX, armorY);

        /* ── 5. Ping icon ── */
        final float pingX = rx + WIDTH - PING_RIGHT_MARGIN - PING_ICON_W;
        final float pingY = ry + PING_TOP;
        drawPing(context, player, pingX, pingY);

        /* ── 6. Health bar ── */
        final float hbX = rx + HEALTH_BAR_MARGIN;
        final float hbY = ry + HEIGHT - HEALTH_BAR_HEIGHT - HEALTH_BAR_BOTTOM;
        final float hbW = WIDTH - HEALTH_BAR_MARGIN * 2;
        drawHealthBar(context, player, hbX, hbY, hbW);
    }

    /* ================================================================
       SUB-RENDERERS
    ================================================================ */

    /** Draw the 8×8 face region of the player's skin texture. */
    private void drawPlayerFace(DrawContext context, PlayerEntity player, float x, float y) {
        try {
            PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
            if (entry != null) {
                SkinTextures textures = entry.getSkinTextures();
                PlayerSkinDrawer.draw(context, textures, (int) x, (int) y, AVATAR_SIZE);
            } else {
                Render2D.drawRect(context, x, y, AVATAR_SIZE, AVATAR_SIZE, new Color(50, 50, 55).getRGB());
            }
        } catch (Exception ignored) {
            Render2D.drawRect(context, x, y, AVATAR_SIZE, AVATAR_SIZE, new Color(50, 50, 55).getRGB());
        }
    }

    /**
     * Draw the 4 armour pieces in a horizontal row.
     * Each piece sits inside an 18×18 dark-grey slot; the 16×16 item
     * icon is centred inside.
     *
     * <p>We call {@link DrawContext#drawItem(ItemStack, int, int)}
     * directly because {@link Render2D#drawItem} uses a matrix
     * translate that doesn't affect the absolute screen coordinates
     * expected by {@code drawItem} in 1.21.4+.
     */
    private void drawArmor(DrawContext context, PlayerEntity player, float x, float y) {
        for (int i = 0; i < ARMOR_SLOTS.length; i++) {
            final float slotX = x + i * (SLOT_SIZE + SLOT_SPACING);
            final float slotY = y;

            // ── dark-grey slot background ──
            Render2D.drawRect(context, slotX, slotY, SLOT_SIZE, SLOT_SIZE,
                    new Color(30, 30, 35, 200).getRGB());

            // ── item icon centred inside the slot ──
            ItemStack stack = player.getEquippedStack(ARMOR_SLOTS[i]);
            if (!stack.isEmpty()) {
                context.drawItem(stack, (int) (slotX + ITEM_OFFSET), (int) (slotY + ITEM_OFFSET));
            }
        }
    }

    /**
     * Draw a vanilla-style ping icon using the same sprite identifiers
     * and thresholds as {@code net.minecraft.client.gui.hud.PlayerListHud}.
     */
    private void drawPing(DrawContext context, PlayerEntity player, float x, float y) {
        try {
            PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
            if (entry == null) return;

            final int pingMs = entry.getLatency();
            final int idx;

            if (pingMs < 0) {
                idx = 0;                          // unknown
            } else if (pingMs <= 150) {
                idx = 5;                          // excellent
            } else if (pingMs <= 300) {
                idx = 4;                          // good
            } else if (pingMs <= 600) {
                idx = 3;                          // medium
            } else if (pingMs <= 1000) {
                idx = 2;                          // poor
            } else {
                idx = 1;                          // terrible
            }

            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, PING_TEXTURES[idx],
                    (int) x, (int) y, PING_ICON_W, PING_ICON_H);

        } catch (Exception ignored) {
            // network handler may be null in single-player
        }
    }

    /** Draw a smooth health bar (red background, yellow fill). */
    private void drawHealthBar(DrawContext context, LivingEntity entity, float x, float y, float width) {
        final float health = entity.getHealth();
        final float maxHealth = entity.getMaxHealth();
        final float ratio = Math.min(1f, Math.max(0f, health / maxHealth));

        healthAnimation.run(ratio);
        final float animatedRatio = healthAnimation.getValue().floatValue();

        // Background (dark red)
        Render2D.drawRect(context, x, y, width, HEALTH_BAR_HEIGHT,
                new Color(80, 0, 0).getRGB());
        // Fill (yellow)
        if (animatedRatio > 0.005f) {
            Render2D.drawRect(context, x, y, width * animatedRatio, HEALTH_BAR_HEIGHT,
                    new Color(255, 255, 0).getRGB());
        }
    }
}
