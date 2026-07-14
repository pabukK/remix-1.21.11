package wtf.remix.management;

import wtf.remix.event.base.annotation.EventTarget;
import wtf.remix.event.impl.UpdateEvent;
import wtf.remix.util.IMinecraft;
import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

@Getter
public class TargetManager implements IMinecraft {
    private final List<LivingEntity> targets = new ArrayList<>();

    public TargetManager() {
        instance.getEventManager().register(this);
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (mc.world == null) return;

        targets.clear();
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof LivingEntity livingEntity) {
                targets.add(livingEntity);
            }
        }
    }
}