package net.x7bbbbbbb.bl_trinkets_layer.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.oxcodsnet.beltborne_lanterns.common.config.BLClientConfigAccess;
import net.oxcodsnet.beltborne_lanterns.common.config.BLConfig;
import net.oxcodsnet.beltborne_lanterns.common.config.BLConfigs;
import net.x7bbbbbbb.bl_trinkets_layer.BLTrinketsLayer;

public final class BLTrinketsLayerClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> preloadBeltborneConfig());
    }

    private static void preloadBeltborneConfig() {
        try {
            BLClientConfigAccess.get(); // BL bugfix: load the config to correctly apply the lantern offsets.
        } catch (RuntimeException | LinkageError e) {
            BLTrinketsLayer.LOGGER.warn("[BL Trinkets Layer] Could not preload the Beltborne Lanterns client config; lantern offsets may "
                    + "stay at defaults until its config screen is opened", e);
        }
    }
}
