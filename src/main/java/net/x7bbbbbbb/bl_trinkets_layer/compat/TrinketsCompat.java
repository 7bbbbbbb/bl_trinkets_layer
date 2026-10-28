package net.x7bbbbbbb.bl_trinkets_layer.compat;

import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketsApi;
import dev.emi.trinkets.api.event.TrinketEquipCallback;
import dev.emi.trinkets.api.event.TrinketUnequipCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.oxcodsnet.beltborne_lanterns.common.BeltState;
import net.oxcodsnet.beltborne_lanterns.common.LampRegistry;
import net.oxcodsnet.beltborne_lanterns.common.compat.CompatibilityLayer;
import net.oxcodsnet.beltborne_lanterns.common.persistence.BeltLanternSave;
import net.oxcodsnet.beltborne_lanterns.fabric.BeltNetworking;
import net.x7bbbbbbb.bl_trinkets_layer.BLTrinketsLayer;
import net.x7bbbbbbb.bl_trinkets_layer.config.BLTrinketsLayerConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TrinketsCompat implements CompatibilityLayer {
    private static final String TRINKETS_MOD_ID = "trinkets";

    private final Set<UUID> pendingRespawn = ConcurrentHashMap.newKeySet();

    @Override
    public String getModId() {
        return TRINKETS_MOD_ID;
    }

    @Override
    public void onInitialize() {
        registerTrinketCallbacks();
        registerRespawnCallbacks();
        BLTrinketsLayer.LOGGER.info("[BL Trinkets Layer] Trinkets integration active");
    }

    @Override
    public boolean handlesItemOnDeath() {
        return true;
    }

    @Override
    public boolean tryToggleLantern(ServerPlayer player) {
        List<SlotAccess> slots = allowedSlots(player);

        for (SlotAccess slot : slots) {
            ItemStack stack = slot.get();
            if (!isLamp(stack)) continue;

            ItemStack toReturn = stack.copy();
            slot.set(ItemStack.EMPTY);
            if (!player.isCreative() && !toReturn.isEmpty()) {
                giveBack(player, toReturn);
            }
            syncMirrorFromSlots(player);
            return true;
        }

        if (BeltState.hasLamp(player)) return false;

        ItemStack held = player.getMainHandItem();
        if (!isLamp(held)) held = player.getOffhandItem();
        if (!isLamp(held)) return false;

        for (SlotAccess slot : slots) {
            if (!slot.get().isEmpty()) continue;
            slot.set(held.copyWithCount(1));
            if (!player.isCreative()) {
                held.shrink(1);
                player.getInventory().setChanged();
            }
            syncMirrorFromSlots(player);
            return true;
        }
        return false;
    }

    @Override
    public Optional<ItemStack> getBeltStack(ServerPlayer player) {
        for (SlotAccess slot : allowedSlots(player)) {
            ItemStack stack = slot.get();
            if (isLamp(stack)) return Optional.of(stack);
        }
        return Optional.empty();
    }

    @Override
    public void syncToggleOn(ServerPlayer player) {
        ItemStack stored = BeltState.getLampStack(player);
        if (stored == null || stored.isEmpty()) return;

        for (SlotAccess slot : allowedSlots(player)) {
            if (!slot.get().isEmpty()) continue;
            slot.set(stored.copy());
            syncMirrorFromSlots(player);
            return;
        }
    }

    private void registerTrinketCallbacks() {
        TrinketEquipCallback.EVENT.register((stack, ref, entity) -> {
            if (!(entity instanceof ServerPlayer player)) return;
            if (!BLTrinketsLayerConfig.isAllowedSlot(ref.inventory().getSlotType())) return;
            if (!isLamp(stack)) return;
            onLampEquipped(player);
        });

        TrinketUnequipCallback.EVENT.register((stack, ref, entity) -> {
            if (!(entity instanceof ServerPlayer player)) return;
            if (!BLTrinketsLayerConfig.isAllowedSlot(ref.inventory().getSlotType())) return;
            if (!isLamp(stack)) return;
            syncMirrorFromSlots(player);
        });
    }

    private void onLampEquipped(ServerPlayer player) {
        // The slots are authoritative: with several allowed slots the first lantern is the active one.
        Optional<ItemStack> worn = getBeltStack(player);
        if (worn.isEmpty()) return;
        ItemStack target = worn.get();

        ItemStack existing = BeltState.hasLamp(player) ? BeltState.getLampStack(player) : null;
        if (existing != null && !existing.isEmpty() && !stacksEqual(existing, target)
                && !player.isCreative() && !isWorn(player, existing)) {
            giveBack(player, existing.copy());
        }

        setMirroredLamp(player, target.copy());
        broadcastCurrent(player);
    }

    private void registerRespawnCallbacks() {
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            if (alive) return;
            if (!BeltState.hasLamp(oldPlayer)) return;
            pendingRespawn.add(newPlayer.getUUID());
            setMirroredLamp(oldPlayer, null);
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (alive) return;
            if (!pendingRespawn.remove(newPlayer.getUUID())) return;
            Optional<ItemStack> worn = getBeltStack(newPlayer);
            if (worn.isPresent()) {
                setMirroredLamp(newPlayer, worn.get().copy());
                broadcastCurrent(newPlayer);
            }
        });
    }

    private void syncMirrorFromSlots(ServerPlayer player) {
        Optional<ItemStack> worn = getBeltStack(player);
        setMirroredLamp(player, worn.map(ItemStack::copy).orElse(null));
        broadcastCurrent(player);
    }

    private static void setMirroredLamp(ServerPlayer player, ItemStack stack) {
        boolean clear = stack == null || stack.isEmpty();
        BeltState.setLamp(player, clear ? (ItemStack) null : stack);
        BeltLanternSave.get(player.server).set(player.getUUID(), clear ? (ItemStack) null : stack);
    }

    private void broadcastCurrent(ServerPlayer player) {
        ItemStack shown = getBeltStack(player).orElse(null);
        if (shown == null && BeltState.hasLamp(player)) {
            shown = BeltState.getLampStack(player);
        }
        BeltNetworking.broadcastBeltState(player, shown == null || shown.isEmpty() ? null : shown.getItem());
    }

    private boolean isWorn(ServerPlayer player, ItemStack stack) {
        for (SlotAccess slot : allowedSlots(player)) {
            ItemStack inSlot = slot.get();
            if (isLamp(inSlot) && stacksEqual(inSlot, stack)) return true;
        }
        return false;
    }

    private static void giveBack(ServerPlayer player, ItemStack stack) {
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
    }

    private static boolean isLamp(ItemStack stack) {
        return stack != null && !stack.isEmpty() && LampRegistry.isLamp(stack);
    }

    private static boolean stacksEqual(ItemStack first, ItemStack second) {
        if (first == second) return true;
        if (first == null || second == null) return false;
        return ItemStack.isSameItemSameComponents(first, second);
    }

    private static List<SlotAccess> allowedSlots(ServerPlayer player) {
        List<SlotAccess> result = new ArrayList<>();
        TrinketsApi.getTrinketComponent(player).ifPresent(component ->
                component.forEach((ref, stack) -> {
                    if (BLTrinketsLayerConfig.isAllowedSlot(ref.inventory().getSlotType())) {
                        result.add(new SlotAccess(ref));
                    }
                }));
        return result;
    }

    private record SlotAccess(SlotReference ref) {
        private boolean isValid() {
            return ref.index() >= 0 && ref.index() < ref.inventory().getContainerSize();
        }

        ItemStack get() {
            return isValid() ? ref.inventory().getItem(ref.index()) : ItemStack.EMPTY;
        }

        void set(ItemStack stack) {
            if (isValid()) ref.inventory().setItem(ref.index(), stack);
        }
    }
}
