package com.toolpro.util;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * Small collection of shared helpers used by the crystal PvP modules.
 * Everything here is intentionally packet based so that the actions stay in sync with what
 * Meteor's own combat modules do and to avoid relying on client side interaction cooldowns.
 */
public final class CombatUtils {
    private CombatUtils() {
    }

    /**
     * @return {@code true} if the block can act as a base for an end crystal (obsidian or bedrock).
     */
    public static boolean isCrystalBase(BlockPos pos) {
        if (mc.world == null) return false;
        return mc.world.getBlockState(pos).isOf(Blocks.OBSIDIAN) || mc.world.getBlockState(pos).isOf(Blocks.BEDROCK);
    }

    /**
     * Sends an attack (left click) on the given entity followed by a hand swing.
     */
    public static void attackEntity(Entity entity, Hand hand, boolean swing) {
        if (mc.getNetworkHandler() == null || mc.player == null) return;

        mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));
        if (swing) swing(hand);
    }

    /**
     * Places an end crystal on top of the given base block by sending a block interaction packet.
     * The crystal must already be selected in the provided hand.
     */
    public static void placeCrystal(BlockPos base, Hand hand, boolean swing) {
        if (mc.getNetworkHandler() == null || mc.player == null) return;

        Vec3d hitVec = new Vec3d(base.getX() + 0.5, base.getY() + 1.0, base.getZ() + 0.5);
        BlockHitResult hitResult = new BlockHitResult(hitVec, Direction.UP, base, false);

        mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(hand, hitResult, 0));
        if (swing) swing(hand);
    }

    /**
     * Uses (throws) the item currently held in the given hand. Used for ender pearls.
     */
    public static ActionResult useItem(Hand hand) {
        if (mc.interactionManager == null || mc.player == null) return ActionResult.PASS;
        return mc.interactionManager.interactItem(mc.player, hand);
    }

    public static void swing(Hand hand) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        mc.player.swingHand(hand);
        mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
    }
}
