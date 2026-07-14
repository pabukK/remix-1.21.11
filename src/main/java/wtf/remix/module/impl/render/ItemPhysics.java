package wtf.remix.module.impl.render;

import wtf.remix.module.Category;
import wtf.remix.module.Module;
import lombok.Getter;

@Getter
public class ItemPhysics extends Module {

    public ItemPhysics() {
        super("ItemPhysics", Category.Render);
    }
}