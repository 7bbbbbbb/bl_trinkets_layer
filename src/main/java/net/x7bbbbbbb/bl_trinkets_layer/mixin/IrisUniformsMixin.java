package net.x7bbbbbbb.bl_trinkets_layer.mixin;

import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.oxcodsnet.beltborne_lanterns.common.LampRegistry;
import net.x7bbbbbbb.bl_trinkets_layer.config.BLTrinketsLayerConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;

@Pseudo
@Mixin(targets = "net.irisshaders.iris.uniforms.IdMapUniforms$HeldItemSupplier")
public class IrisUniformsMixin {

    @Redirect(
            method = "update",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;getItemInHand(Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/item/ItemStack;"
            ),
            require = 0
    )
    private ItemStack bl_trinkets$beltLanternAsOffhand(LocalPlayer player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND || !BLTrinketsLayerConfig.isIrisCompatEnabled()) {
            return player.getItemInHand(hand);
        }

        ItemStack lantern = bl_trinkets$findBeltLantern(player);
        return lantern.isEmpty() ? player.getItemInHand(hand) : lantern;
    }

    @Unique
    private static ItemStack bl_trinkets$findBeltLantern(LocalPlayer player) {
        TrinketComponent component = TrinketsApi.getTrinketComponent(player).orElse(null);
        if (component == null) return ItemStack.EMPTY;

        for (Map<String, TrinketInventory> group : component.getInventory().values()) {
            for (TrinketInventory inventory : group.values()) {
                if (!BLTrinketsLayerConfig.isAllowedSlot(inventory.getSlotType())) continue;
                for (int i = 0; i < inventory.getContainerSize(); i++) {
                    ItemStack stack = inventory.getItem(i);
                    if (!stack.isEmpty() && (LampRegistry.isLamp(stack) || stack.is(LampRegistry.EXTRA_LAMPS_TAG))) {
                        return stack;
                    }
                }
            }
        }
        return ItemStack.EMPTY;
    }
}
