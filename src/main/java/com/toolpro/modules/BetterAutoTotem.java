package com.toolpro.modules;

import com.toolpro.ToolPro;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;

/**
 * An improved Auto Totem. On top of the usual "keep a totem in your offhand" behaviour this module
 * predicts incoming explosion and fall damage, reacts instantly the tick a totem pops, and reports
 * the number of totems left. It never needs an inventory screen to be open.
 */
public class BetterAutoTotem extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("Smart only holds a totem when needed, Strict always holds one.")
        .defaultValue(Mode.Smart)
        .build()
    );

    private final Setting<Integer> health = sgGeneral.add(new IntSetting.Builder()
        .name("health")
        .description("Equip a totem when your effective health drops to or below this value.")
        .defaultValue(12)
        .range(1, 36)
        .sliderMax(36)
        .visible(() -> mode.get() == Mode.Smart)
        .build()
    );

    private final Setting<Boolean> predictExplosion = sgGeneral.add(new BoolSetting.Builder()
        .name("predict-explosion")
        .description("Equip a totem when a nearby crystal, bed or anchor could kill you.")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.Smart)
        .build()
    );

    private final Setting<Boolean> predictFall = sgGeneral.add(new BoolSetting.Builder()
        .name("predict-fall")
        .description("Equip a totem when fall damage could kill you.")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.Smart)
        .build()
    );

    private final Setting<Boolean> elytra = sgGeneral.add(new BoolSetting.Builder()
        .name("elytra")
        .description("Always hold a totem while gliding with an elytra.")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.Smart)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Ticks between totem swaps.")
        .defaultValue(0)
        .min(0)
        .sliderMax(20)
        .build()
    );

    private int totems;
    private int ticks;

    public BetterAutoTotem() {
        super(ToolPro.CATEGORY, "better-auto-totem", "Smarter, faster totem swapping with damage prediction.");
    }

    @Override
    public void onActivate() {
        ticks = 0;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;

        FindItemResult totem = InvUtils.find(Items.TOTEM_OF_UNDYING);
        totems = totem.count();
        if (totems <= 0) return;

        if (ticks < delay.get()) {
            ticks++;
            return;
        }
        ticks = 0;

        if (mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) return;
        if (!shouldHold()) return;

        InvUtils.move().from(totem.slot()).toOffhand();
    }

    private boolean shouldHold() {
        if (mode.get() == Mode.Strict) return true;

        float effectiveHealth = PlayerUtils.getTotalHealth() - PlayerUtils.possibleHealthReductions(predictExplosion.get(), predictFall.get());
        if (effectiveHealth <= health.get()) return true;

        boolean gliding = mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA) && mc.player.isFallFlying();
        return elytra.get() && gliding;
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onPacket(PacketEvent.Receive event) {
        if (!(event.packet instanceof ClientboundEntityEventPacket packet)) return;
        if (packet.getEventId() != EntityEvent.PROTECTED_FROM_DEATH) return;

        Entity entity = packet.getEntity(mc.level);
        if (entity == null || !entity.equals(mc.player)) return;

        // A totem just popped: swap a new one in as soon as possible.
        ticks = delay.get();
    }

    @Override
    public String getInfoString() {
        return String.valueOf(totems);
    }

    public enum Mode {
        Smart,
        Strict
    }
}
