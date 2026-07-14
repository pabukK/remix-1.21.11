package wtf.remix.module.impl.render;

import wtf.remix.event.base.annotation.EventTarget;
import wtf.remix.event.impl.UpdateEvent;
import wtf.remix.module.Category;
import wtf.remix.module.Module;
import wtf.remix.module.value.impl.BoolValue;
import wtf.remix.module.value.impl.ModeValue;
import wtf.remix.module.value.impl.NumberValue;

public class Animation extends Module {
    public final NumberValue swingSpeed = new NumberValue("Swing Speed", 0, -4, 20, 1);
    public final ModeValue swingMode = new ModeValue("Swing Mode", "Vanilla", "Vanilla", "Smooth");
    public final ModeValue blockMode = new ModeValue("Block Mode", "Flux", "Flux", "1.7", "Stella", "SideDown", "Leaked", "Styles", "Spin", "Screw", "Swang");
    public final BoolValue equipProgress = new BoolValue("Equip Progress", true);

    public Animation() {
        super("Animation", Category.Render);
        setEnabled(true);
        setHidden(true);
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        setSuffix(blockMode.getValue());
    }

    @Override
    public void onDisable() {
        setEnabled(true);
    }
}