package wtf.remix.util;

import wtf.remix.Client;
import net.minecraft.client.MinecraftClient;

public interface IMinecraft {
    MinecraftClient mc = MinecraftClient.getInstance();
    Client instance = Client.instance;
}
