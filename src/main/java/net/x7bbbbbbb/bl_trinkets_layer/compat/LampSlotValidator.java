package net.x7bbbbbbb.bl_trinkets_layer.compat;

import dev.emi.trinkets.api.TrinketsApi;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.resources.ResourceLocation;
import net.oxcodsnet.beltborne_lanterns.common.LampRegistry;
import net.x7bbbbbbb.bl_trinkets_layer.config.BLTrinketsLayerConfig;

public final class LampSlotValidator {
    private LampSlotValidator() {}

    public static void install() {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("trinkets", "tag");
        var previous = TrinketsApi.getTrinketPredicate(id);

        TrinketsApi.registerTrinketPredicate(id, (stack, ref, entity) -> {
            if (LampRegistry.isLamp(stack) && BLTrinketsLayerConfig.isAllowedSlot(ref.inventory().getSlotType())) {
                return TriState.TRUE;
            }
            return previous.map(p -> p.apply(stack, ref, entity)).orElse(TriState.DEFAULT);
        });
    }
}
