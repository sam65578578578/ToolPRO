package com.toolpro.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

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
        if (mc.level == null) return false;
        var block = mc.level.getBlockState(pos).getBlock();
        return block == Blocks.OBSIDIAN || block == Blocks.BEDROCK;
    }

    /**
     * Sends an attack (left click) on the given entity followed by a hand swing.
     */
    public static void attackEntity(Entity entity, InteractionHand hand, boolean swing) {
        if (mc.getConnection() == null || mc.player == null) return;

        mc.getConnection().send(new ServerboundAttackPacket(entity.getId()));
        if (swing) swing(hand);
    }

    /**
     * Places an end crystal on top of the given base block by interacting with its upper face.
     * The crystal must already be selected in the provided hand.
     */
    public static void placeCrystal(BlockPos base, InteractionHand hand, boolean swing) {
        if (mc.gameMode == null || mc.player == null) return;

        Vec3 hitVec = new Vec3(base.getX() + 0.5, base.getY() + 1.0, base.getZ() + 0.5);
        BlockHitResult hitResult = new BlockHitResult(hitVec, Direction.UP, base, false);

        mc.gameMode.useItemOn(mc.player, hand, hitResult);
        if (swing) swing(hand);
    }

    /**
     * Uses (throws) the item currently held in the given hand. Used for ender pearls.
     */
    public static void useItem(InteractionHand hand) {
        if (mc.gameMode == null || mc.player == null) return;
        mc.gameMode.useItem(mc.player, hand);
    }

    public static void swing(InteractionHand hand) {
        if (mc.player == null || mc.getConnection() == null) return;
        mc.player.swing(hand);
        mc.getConnection().send(new ServerboundSwingPacket(hand));
    }
}
