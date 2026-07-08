package com.toolpro.modules;

import com.toolpro.ToolPro;
import com.toolpro.util.CombatUtils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Automatically performs a "D-Tap" sequence on a nearby opponent: it hits the target to launch
 * them into the air and then places (and optionally breaks) end crystals while they are airborne.
 *
 * <p>When {@code throw-pearls} is enabled and ender pearls are present in the hotbar the module will
 * throw a pearl towards the target while they are airborne, just before placing crystals.</p>
 */
public class AutoDTap extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgCrystal = settings.createGroup("Crystal");
    private final SettingGroup sgPearl = settings.createGroup("Pearl");
    private final SettingGroup sgRotate = settings.createGroup("Rotation");

    // General

    private final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("target-range")
        .description("Maximum distance to search for a target.")
        .defaultValue(5.0)
        .min(0.0)
        .sliderMax(10.0)
        .build()
    );

    private final Setting<SortPriority> priority = sgGeneral.add(new EnumSetting.Builder<SortPriority>()
        .name("target-priority")
        .description("How the target is selected when several players are in range.")
        .defaultValue(SortPriority.LowestHealth)
        .build()
    );

    private final Setting<Integer> hitDelay = sgGeneral.add(new IntSetting.Builder()
        .name("hit-delay")
        .description("Ticks between attacks used to launch the target.")
        .defaultValue(2)
        .min(0)
        .sliderMax(20)
        .build()
    );

    private final Setting<Boolean> requireAirborne = sgGeneral.add(new BoolSetting.Builder()
        .name("require-airborne")
        .description("Only place crystals once the target has left the ground.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> swing = sgGeneral.add(new BoolSetting.Builder()
        .name("swing")
        .description("Renders a hand swing for attacks, placements and pearls.")
        .defaultValue(true)
        .build()
    );

    // Crystal

    private final Setting<Integer> placeDelay = sgCrystal.add(new IntSetting.Builder()
        .name("place-delay")
        .description("Ticks between crystal placements.")
        .defaultValue(1)
        .min(0)
        .sliderMax(20)
        .build()
    );

    private final Setting<Boolean> autoBreak = sgCrystal.add(new BoolSetting.Builder()
        .name("auto-break")
        .description("Automatically break placed crystals that are within reach of the target.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> breakDelay = sgCrystal.add(new IntSetting.Builder()
        .name("break-delay")
        .description("Ticks between crystal breaks.")
        .defaultValue(0)
        .min(0)
        .sliderMax(20)
        .visible(autoBreak::get)
        .build()
    );

    private final Setting<Double> searchRadius = sgCrystal.add(new DoubleSetting.Builder()
        .name("search-radius")
        .description("Horizontal radius around the target to look for a crystal base.")
        .defaultValue(4.0)
        .min(1.0)
        .sliderMax(6.0)
        .build()
    );

    private final Setting<Double> minDamage = sgCrystal.add(new DoubleSetting.Builder()
        .name("min-damage")
        .description("Minimum damage a placement must deal to the target.")
        .defaultValue(4.0)
        .min(0.0)
        .sliderMax(20.0)
        .build()
    );

    private final Setting<Double> maxSelfDamage = sgCrystal.add(new DoubleSetting.Builder()
        .name("max-self-damage")
        .description("Do not place crystals that would deal more than this much damage to you.")
        .defaultValue(8.0)
        .min(0.0)
        .sliderMax(20.0)
        .build()
    );

    private final Setting<Boolean> swapBack = sgCrystal.add(new BoolSetting.Builder()
        .name("swap-back")
        .description("Switch back to the previously selected slot after placing.")
        .defaultValue(true)
        .build()
    );

    // Pearl

    private final Setting<Boolean> throwPearls = sgPearl.add(new BoolSetting.Builder()
        .name("throw-pearls")
        .description("When ender pearls are in the hotbar, throw one near the target while airborne before placing crystals.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> pearlDelay = sgPearl.add(new IntSetting.Builder()
        .name("pearl-delay")
        .description("Minimum ticks between pearl throws.")
        .defaultValue(30)
        .min(0)
        .sliderMax(100)
        .visible(throwPearls::get)
        .build()
    );

    private final Setting<Boolean> pearlAirborneOnly = sgPearl.add(new BoolSetting.Builder()
        .name("airborne-only")
        .description("Only throw pearls while the target is off the ground.")
        .defaultValue(true)
        .visible(throwPearls::get)
        .build()
    );

    // Rotation

    private final Setting<Boolean> rotate = sgRotate.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Rotate towards the target and placements. Disabling makes the module fully silent but easier to detect as illegal.")
        .defaultValue(true)
        .build()
    );

    private int hitTimer, placeTimer, pearlTimer, breakTimer;

    public AutoDTap() {
        super(ToolPro.CATEGORY, "auto-d-tap", "Hits an opponent to launch them, then crystals them while airborne. Optionally pearls them first.");
    }

    @Override
    public void onActivate() {
        hitTimer = placeTimer = pearlTimer = breakTimer = 0;
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        if (hitTimer > 0) hitTimer--;
        if (placeTimer > 0) placeTimer--;
        if (pearlTimer > 0) pearlTimer--;
        if (breakTimer > 0) breakTimer--;

        PlayerEntity target = TargetUtils.getPlayerTarget(targetRange.get(), priority.get());
        if (target == null) return;

        boolean airborne = !target.isOnGround();

        // Launch the target by attacking it.
        if (hitTimer <= 0 && PlayerUtils.isWithinReach(target)) {
            attack(target);
            hitTimer = hitDelay.get();
        }

        // Optionally throw a pearl near the target while airborne.
        if (throwPearls.get() && pearlTimer <= 0 && (!pearlAirborneOnly.get() || airborne)) {
            if (throwPearlAt(target)) pearlTimer = pearlDelay.get();
        }

        // Place a crystal in the best spot around the target.
        if (placeTimer <= 0 && (!requireAirborne.get() || airborne)) {
            if (placeBestCrystal(target)) placeTimer = placeDelay.get();
        }

        // Break the crystal closest to the target.
        if (autoBreak.get() && breakTimer <= 0) {
            if (breakBestCrystal(target)) breakTimer = breakDelay.get();
        }
    }

    private void attack(Entity target) {
        Hand hand = InvUtils.findInHotbar(Items.END_CRYSTAL).getHand();
        if (hand == null) hand = Hand.MAIN_HAND;
        Hand finalHand = hand;

        if (rotate.get()) {
            Rotations.rotate(Rotations.getYaw(target), Rotations.getPitch(target), 50, () -> CombatUtils.attackEntity(target, finalHand, swing.get()));
        } else {
            CombatUtils.attackEntity(target, finalHand, swing.get());
        }
    }

    private boolean throwPearlAt(PlayerEntity target) {
        FindItemResult pearl = InvUtils.findInHotbar(Items.ENDER_PEARL);
        if (!pearl.found()) return false;

        Vec3d aim = target.getPos().add(0, target.getStandingEyeHeight(), 0);
        Runnable action = () -> {
            int prev = mc.player.getInventory().selectedSlot;
            boolean swapped = false;

            Hand hand;
            if (pearl.isOffhand()) hand = Hand.OFF_HAND;
            else {
                InvUtils.swap(pearl.slot(), false);
                swapped = true;
                hand = Hand.MAIN_HAND;
            }

            CombatUtils.useItem(hand);
            if (swing.get()) CombatUtils.swing(hand);

            if (swapped && swapBack.get()) InvUtils.swap(prev, false);
        };

        if (rotate.get()) Rotations.rotate(Rotations.getYaw(aim), Rotations.getPitch(aim), 45, action);
        else action.run();

        return true;
    }

    private boolean placeBestCrystal(PlayerEntity target) {
        FindItemResult crystal = InvUtils.findInHotbar(Items.END_CRYSTAL);
        if (!crystal.found()) return false;

        BlockPos base = findBestBase(target);
        if (base == null) return false;

        Vec3d crystalTop = new Vec3d(base.getX() + 0.5, base.getY() + 1.0, base.getZ() + 0.5);
        BlockPos finalBase = base;

        Runnable action = () -> {
            int prev = mc.player.getInventory().selectedSlot;
            boolean swapped = false;

            Hand hand;
            if (crystal.isOffhand()) hand = Hand.OFF_HAND;
            else {
                InvUtils.swap(crystal.slot(), false);
                swapped = true;
                hand = Hand.MAIN_HAND;
            }

            CombatUtils.placeCrystal(finalBase, hand, swing.get());

            if (swapped && swapBack.get()) InvUtils.swap(prev, false);
        };

        if (rotate.get()) Rotations.rotate(Rotations.getYaw(crystalTop), Rotations.getPitch(crystalTop), 40, action);
        else action.run();

        return true;
    }

    private BlockPos findBestBase(PlayerEntity target) {
        BlockPos feet = target.getBlockPos();
        int r = (int) Math.ceil(searchRadius.get());

        BlockPos best = null;
        double bestDamage = minDamage.get();

        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dy = -3; dy <= 1; dy++) {
                    pos.set(feet.getX() + dx, feet.getY() + dy, feet.getZ() + dz);

                    if (!CombatUtils.isCrystalBase(pos)) continue;

                    BlockPos above = pos.up();
                    if (!mc.world.getBlockState(above).isAir()) continue;

                    Vec3d crystalTop = new Vec3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
                    if (!PlayerUtils.isWithinReach(crystalTop)) continue;
                    if (isCrystalAt(above)) continue;

                    double damage = DamageUtils.crystalDamage(target, crystalTop);
                    double self = DamageUtils.crystalDamage(mc.player, crystalTop);
                    if (damage < minDamage.get() || self > maxSelfDamage.get()) continue;

                    if (damage > bestDamage) {
                        bestDamage = damage;
                        best = pos.toImmutable();
                    }
                }
            }
        }

        return best;
    }

    private boolean isCrystalAt(BlockPos pos) {
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof EndCrystalEntity && entity.getBlockPos().equals(pos)) return true;
        }
        return false;
    }

    private boolean breakBestCrystal(PlayerEntity target) {
        EndCrystalEntity best = null;
        double bestDistance = Double.MAX_VALUE;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof EndCrystalEntity crystal)) continue;
            if (!PlayerUtils.isWithinReach(crystal)) continue;

            double distance = crystal.squaredDistanceTo(target);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = crystal;
            }
        }

        if (best == null) return false;

        EndCrystalEntity finalBest = best;
        Hand hand = InvUtils.findInHotbar(Items.END_CRYSTAL).getHand();
        if (hand == null) hand = Hand.MAIN_HAND;
        Hand finalHand = hand;

        if (rotate.get()) {
            Rotations.rotate(Rotations.getYaw(best), Rotations.getPitch(best), 35, () -> CombatUtils.attackEntity(finalBest, finalHand, swing.get()));
        } else {
            CombatUtils.attackEntity(best, finalHand, swing.get());
        }

        return true;
    }
}
