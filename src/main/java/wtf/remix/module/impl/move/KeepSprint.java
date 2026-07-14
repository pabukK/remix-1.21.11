package wtf.remix.module.impl.move;

import wtf.remix.module.Category;
import wtf.remix.module.Module;
import wtf.remix.module.value.impl.NumberValue;

public class KeepSprint extends Module {
    public final NumberValue motion = new NumberValue("Motion", 1, 0, 1, .1f);

    public KeepSprint() {
        super("KeepSprint", Category.Move);
    }
}
