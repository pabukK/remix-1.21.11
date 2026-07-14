package wtf.remix.module.impl.render;

import wtf.remix.module.Category;
import wtf.remix.module.Module;
import wtf.remix.ui.clickgui.ClickGuiScreen;
import org.lwjgl.glfw.GLFW;

public final class ClickGui extends Module {

    public ClickGui() {
        super("ClickGui", Category.Render);
        setKey(GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    @Override
    public void onEnable() {
        mc.setScreen(new ClickGuiScreen());
        setEnabled(false);
    }
}
