package com.toolpro.modules;

import com.toolpro.ToolPro;
import com.toolpro.util.CombatUtils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * When ender pearls are present in the hotbar this module pearls the player into the deepest,
 * most protected hole nearby to maximise defensive positioning. It scans the surrounding area for
 * holes surrounded by blast resistant blocks (obsidian / bedrock) and throws a pearl into the
 * deepest one it can find.
 */
public class PearlAnchor extends Module {
    private static final float MIN_BLAST_RESISTANCE = 600f;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> radius = sgGeneral.add(new DoubleSetting.Builder()
        .name("radius")
        .description("Horizontal radius to scan for holes.")
        .defaultValue(3.0)
        .min(1.0)
        .sliderMax(6.0)
        .build()
    );

    private final Setting<Integer> depth = sgGeneral.add(new IntSetting.Builder()
        .name("depth")
        .description("How many blocks below your feet to look for the bottom of a hole.")
        .defaultValue(4)
        .min(1)
        .sliderMax(8)
        .build()
    );

    private final Setting<Boolean> requireWalls = sgGeneral.add(new BoolSetting.Builder()
        .name("require-walls")
        .description("Only consider spots whose four horizontal neighbours are blast resistant (a true hole).")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Minimum ticks between pearl throws.")
        .defaultValue(20)
        .min(0)
        .sliderMax(100)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Rotate down into the hole before throwing.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> swing = sgGeneral.add(new BoolSetting.Builder()
        .name("swing")
        .description("Render a hand swing when throwing.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> swapBack = sgGeneral.add(new BoolSetting.Builder()
        .name("swap-back")
        .description("Switch back to the previously selected slot after throwing.")
        .defaultValue(true)
        .build()
    );

    private int timer;

    public PearlAnchor() {
        super(ToolPro.CATEGORY, "pearl-anchor", "Pearls you into the deepest nearby hole for maximum protection.");
    }

    @Override
    public void onActivate() {
        timer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        if (timer > 0) {
            timer--;
            return;
        }

        FindItemResult pearl = InvUtils.findInHotbar(Items.ENDER_PEARL);
        if (!pearl.found()) return;

        BlockPos hole = findDeepestHole();
        if (hole == null) return;

        Vec3d aim = new Vec3d(hole.getX() + 0.5, hole.getY() + 0.1, hole.getZ() + 0.5);
        throwPearl(pearl, aim);
        timer = delay.get();
    }

    private BlockPos findDeepestHole() {
        BlockPos feet = mc.player.getBlockPos();
        int r = (int) Math.ceil(radius.get());

        BlockPos best = null;
        int bestY = Integer.MAX_VALUE;
        double bestDistanceSq = Double.MAX_VALUE;

        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                double horizontalSq = dx * dx + dz * dz;
                if (horizontalSq > radius.get() * radius.get()) continue;

                for (int dy = -depth.get(); dy <= 1; dy++) {
                    pos.set(feet.getX() + dx, feet.getY() + dy, feet.getZ() + dz);

                    if (!isHoleFloor(pos)) continue;

                    if (pos.getY() < bestY || (pos.getY() == bestY && horizontalSq < bestDistanceSq)) {
                        bestY = pos.getY();
                        bestDistanceSq = horizontalSq;
                        best = pos.toImmutable();
                    }
                }
            }
        }

        return best;
    }

    /**
     * A hole floor is an air block that stands on a blast resistant block and, optionally, is
     * enclosed by blast resistant blocks on all four horizontal sides.
     */
    private boolean isHoleFloor(BlockPos pos) {
        if (!mc.world.getBlockState(pos).isAir()) return false;
        if (!isResistant(pos.down())) return false;

        if (requireWalls.get()) {
            for (Direction direction : Direction.Type.HORIZONTAL) {
                if (!isResistant(pos.offset(direction))) return false;
            }
        }

        return true;
    }

    private boolean isResistant(BlockPos pos) {
        return mc.world.getBlockState(pos).getBlock().getBlastResistance() >= MIN_BLAST_RESISTANCE;
    }

    private void throwPearl(FindItemResult pearl, Vec3d aim) {
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
    }
}
