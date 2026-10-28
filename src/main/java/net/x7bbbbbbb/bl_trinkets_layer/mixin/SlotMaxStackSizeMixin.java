package net.x7bbbbbbb.bl_trinkets_layer.mixin;

import dev.emi.trinkets.TrinketSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.oxcodsnet.beltborne_lanterns.common.LampRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public abstract class SlotMaxStackSizeMixin {
    @Inject(method = "getMaxStackSize(Lnet/minecraft/world/item/ItemStack;)I", at = @At("HEAD"), cancellable = true)
    private void bl_trinkets_limitLampStackSize(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if ((Object) this instanceof TrinketSlot && LampRegistry.isLamp(stack)) {
            cir.setReturnValue(1);
        }
    }
}
