package com.mapsyncer.server;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;

@EventBusSubscriber(value = {Dist.CLIENT, Dist.DEDICATED_SERVER}, bus = EventBusSubscriber.Bus.FORGE)
public class PlayerJoinHandler {
    @SubscribeEvent public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        ServerPlayer player = (ServerPlayer) event.getEntity();
        PlayerJoinHandlerLogic.onPlayerJoin(player, player.getServer());
    }
    @SubscribeEvent public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        PlayerJoinHandlerLogic.onPlayerLeave(event.getEntity().getUUID());
        com.mapsyncer.network.impl.ForgeNetworkHandler.onPlayerDisconnect(event.getEntity().getUUID());
    }
    @SubscribeEvent public static void onServerStopped(ServerStoppedEvent event) { PlayerJoinHandlerLogic.onServerStopped(); }

}