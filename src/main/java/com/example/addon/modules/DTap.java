package com.yourname.addon.modules;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.CrystalAura;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class DTap extends Module {
    public DTap() {
        super("dtap", "Auto D-Tap crystals with Crystal Aura settings.");
    }

    // Copy most settings from Crystal Aura
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPlace = settings.createGroup("Place");
    private final SettingGroup sgBreak = settings.createGroup("Break");
    private final SettingGroup sgDTap = settings.createGroup("D-Tap");

    // General
    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-switch")
        .description("Automatically switches to crystals.")
        .defaultValue(true)
        .build());

    // Place settings (simplified - you can expand)
    private final Setting<Double> placeRange = sgPlace.add(new DoubleSetting.Builder()
        .name("place-range")
        .description("Range for placing crystals.")
        .defaultValue(5.5)
        .min(0)
        .max(6)
        .build());

    // Break settings
    private final Setting<Double> breakRange = sgBreak.add(new DoubleSetting.Builder()
        .name("break-range")
        .description("Range for breaking crystals.")
        .defaultValue(5.5)
        .min(0)
        .max(6)
        .build());

    // D-Tap specific
    private final Setting<Boolean> dTapEnabled = sgDTap.add(new BoolSetting.Builder()
        .name("d-tap")
        .description("Enable double tap on crystals.")
        .defaultValue(true)
        .build());

    private final Setting<Integer> dTapDelay = sgDTap.add(new IntSetting.Builder()
        .name("d-tap-delay")
        .description("Ticks between first and second hit.")
        .defaultValue(1)
        .min(0)
        .max(5)
        .build());

    private Entity targetCrystal = null;
    private int dTapTimer = 0;

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!dTapEnabled.get()) return;

        FindItemResult crystal = InvUtils.find(Items.END_CRYSTAL);
        if (!crystal.found()) return;

        // Find best crystal to D-Tap
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof EndCrystalEntity crystalEntity) {
                if (mc.player.distanceTo(crystalEntity) <= breakRange.get()) {
                    targetCrystal = crystalEntity;
                    break;
                }
            }
        }

        if (targetCrystal != null) {
            if (dTapTimer == 0) {
                // First hit
                mc.interactionManager.attackEntity(mc.player, targetCrystal);
                mc.player.swingHand(Hand.MAIN_HAND);
                dTapTimer = dTapDelay.get();
            } else if (dTapTimer > 0) {
                dTapTimer--;
                if (dTapTimer == 0) {
                    // Second hit (D-Tap)
                    mc.interactionManager.attackEntity(mc.player, targetCrystal);
                    mc.player.swingHand(Hand.MAIN_HAND);
                    targetCrystal = null;
                }
            }
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        // Optional: render indicator on target crystal
    }
}
