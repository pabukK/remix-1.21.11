package wtf.remix.ui.clickgui.component;

import wtf.remix.module.value.Value;
import wtf.remix.ui.clickgui.ModuleButton;
import wtf.remix.util.IMinecraft;
import lombok.Getter;
import net.minecraft.client.gui.DrawContext;

@Getter
public abstract class Component implements IMinecraft {
    protected final ModuleButton parent;
    private final Value value;
    protected float x, y, width, height;

    public Component(ModuleButton parent, Value value) {
        this.parent = parent;
        this.value = value;
    }

    public void render(DrawContext context, float x, float y, float width, int mouseX, int mouseY, float globalAlpha) {
        this.x = x;
        this.y = y;
        this.width = width;
    }

    public void mouseClicked(double mouseX, double mouseY, int button) {}
    public void mouseReleased(double mouseX, double mouseY, int button) {}

    protected boolean hovered(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}