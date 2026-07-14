package wtf.remix;

import wtf.remix.command.CommandManager;
import wtf.remix.config.ConfigManager;
import wtf.remix.event.base.EventManager;
import wtf.remix.module.ModuleManager;
import wtf.remix.ui.font.FontManager;
import wtf.remix.util.IMinecraft;
import wtf.remix.management.*;
import lombok.Getter;
import org.apache.logging.log4j.Logger;

@Getter
public class Client implements IMinecraft {
    public static Client instance;
    public static Logger logger;

    public static String name = "Remix";
    public static String version = "v1.7.1";

    private EventManager eventManager;
    private ModuleManager moduleManager;
    private CommandManager commandManager;
    private ConfigManager configManager;
    private RotationManager rotationManager;
    private TargetManager targetManager;
    private FriendManager friendManager;
    private FontManager fontManager;
    private PacketManager packetManager;
    private ViaVersionManager viaVersionManager;

    public void init() {

        // Why did you do that?
        eventManager = new EventManager();
        moduleManager = new ModuleManager();
        commandManager = new CommandManager();
        configManager = new ConfigManager();
        rotationManager = new RotationManager();
        targetManager = new TargetManager();
        friendManager = new FriendManager();
        fontManager = new FontManager();
        packetManager = new PacketManager();
        viaVersionManager = new ViaVersionManager();
    }

    public void shutdown() {
        configManager.saveAll();
    }
}