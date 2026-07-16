package injection;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket;
import net.minecraft.network.packet.s2c.play.*;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import wtf.remix.Client;
import wtf.remix.event.impl.PacketEvent;
import wtf.remix.module.impl.exploits.Disabler;
import wtf.remix.util.IMinecraft;
import net.minecraft.network.NetworkThreadUtils;
import net.minecraft.network.OffThreadException;
import net.minecraft.network.PacketApplyBatcher;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetworkThreadUtils.class)
public class MixinNetworkThreadUtils implements IMinecraft {
 /*
    @Inject(method = "forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V", at = @At("HEAD"), cancellable = true)
    private static <T extends PacketListener> void forceMainThread(Packet<T> packet, T listener, PacketApplyBatcher batcher, CallbackInfo ci) throws OffThreadException {
        if (!batcher.isOnThread()) {
            Disabler module = Client.instance.getModuleManager().getModule(Disabler.class);
            if (module != null && module.getMode().is("Grim") && module.isEnabled() && listener == MinecraftClient.getInstance().getNetworkHandler()) {
                module.getPostPackets().add((Packet<ClientPlayPacketListener>) instance);
                ci.cancel();
                throw OffThreadException.INSTANCE;
            } else {
                PacketEvent event = new PacketEvent(packet, PacketEvent.Type.Received);
                instance.getEventManager().call(event);

                if (event.isCancelled()) {
                    ci.cancel();
                    throw OffThreadException.INSTANCE;
                }
            }
        }
    }


  */


    /**
     * @author kev
     * @reason dis / pkt rev — 拦截网络线程上的包，支持延迟（Grim Post）和取消
     *
     *         合约：网络线程上必须始终 throw OffThreadException，
     *         否则 caller 会在网络线程上继续执行，导致线程安全问题。
     *         延迟/取消的包不调用 batcher.add()，但同样 throw 以阻止 caller。
     */
    @Overwrite
    public static <T extends PacketListener> void forceMainThread(Packet<T> packet, T listener, PacketApplyBatcher batcher) throws OffThreadException {
        if (!batcher.isOnThread()) {
            Disabler module = instance.getModuleManager().getModule(Disabler.class);

            if (module.isEnabled() && module.getGrimPost() && module.grimPostDelay(packet) && listener == MinecraftClient.getInstance().getNetworkHandler()) {
                // Grim Post：延迟到 releasePost() 处理，不入 batcher
                module.getPostPackets().add((Packet<ClientPlayPacketListener>) packet);
                throw OffThreadException.INSTANCE;  // 必须 throw 以中断网络线程
            }

            PacketEvent event = new PacketEvent(packet, PacketEvent.Type.Received);
            instance.getEventManager().call(event);

            if (event.isCancelled()) {
                // 被其他模块取消，不入 batcher
                throw OffThreadException.INSTANCE;  // 必须 throw 以中断网络线程
            }

            // 正常处理：入 batcher 队列 + throw OffThreadException
            batcher.add(listener, packet);
            throw OffThreadException.INSTANCE;
        }
    }

}