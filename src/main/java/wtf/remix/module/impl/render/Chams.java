package wtf.remix.module.impl.render;

import wtf.remix.module.Category;
import wtf.remix.module.Module;
import wtf.remix.module.value.impl.BoolValue;
import wtf.remix.module.value.impl.ColorValue;
import wtf.remix.module.value.impl.ModeValue;
import wtf.remix.module.value.impl.NumberValue;
import lombok.Getter;

import java.awt.*;

@Getter
public class Chams extends Module {
    private final ModeValue renderMode = new ModeValue("Render Mode", "Tint", "Tint", "Flat");
    private final ModeValue colorMode = new ModeValue("Color Mode", "Aura", "Aura", "Custom");
    private final ColorValue customColor = new ColorValue("Custom Color", new Color(255, 255, 255));
    private final BoolValue throughWalls = new BoolValue("Through Walls", true);
    private final NumberValue alpha = new NumberValue("Alpha", 150, 0, 255, 1);

    public Chams() {
        super("Chams", Category.Render);
    }
}