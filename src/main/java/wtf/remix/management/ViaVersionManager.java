package wtf.remix.management;

import com.viaversion.viafabricplus.ViaFabricPlus;
import com.viaversion.viafabricplus.api.ViaFabricPlusBase;
import lombok.Getter;
import net.fabricmc.loader.api.FabricLoader;
import wtf.remix.util.IMinecraft;

/**
 * @author kevin
 * @since 2026/7/13
 */

@Getter
public class ViaVersionManager implements IMinecraft {
    private final ViaFabricPlusBase via;

    public ViaVersionManager() {
        via = isViaValid() ? ViaFabricPlus.getImpl() : null;
    }

    private boolean isViaValid() {
        return FabricLoader.getInstance().isModLoaded("viafabricplus") || FabricLoader.getInstance().isModLoaded("viafabric");
    }
}
