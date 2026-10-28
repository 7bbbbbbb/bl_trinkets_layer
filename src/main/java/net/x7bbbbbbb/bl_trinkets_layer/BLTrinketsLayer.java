package net.x7bbbbbbb.bl_trinkets_layer;

import net.fabricmc.api.ModInitializer;
import net.x7bbbbbbb.bl_trinkets_layer.compat.LampSlotValidator;
import net.x7bbbbbbb.bl_trinkets_layer.config.BLTrinketsLayerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BLTrinketsLayer implements ModInitializer {
    public static final String MOD_ID = "bl_trinkets_layer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        BLTrinketsLayerConfig.load();
        LampSlotValidator.install();
    }
}
