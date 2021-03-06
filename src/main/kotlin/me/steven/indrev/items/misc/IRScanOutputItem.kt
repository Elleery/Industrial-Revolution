package me.steven.indrev.items.misc

import me.steven.indrev.utils.getChunkPos
import net.minecraft.client.item.TooltipContext
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.text.LiteralText
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import net.minecraft.world.World

class IRScanOutputItem(settings: Settings) : Item(settings) {
    override fun appendTooltip(stack: ItemStack?, world: World?, tooltip: MutableList<Text>?, context: TooltipContext?) {
        val tag = stack?.tag ?: return
        val type = Identifier(tag.getString("VeinIdentifier"))
        val pos = getChunkPos(tag.getString("ChunkPos")) ?: return
        tooltip?.add(TranslatableText("item.indrev.chunk_scanner.tooltip2",
            TranslatableText("vein.${type.namespace}.${type.path}").formatted(Formatting.WHITE)).formatted(Formatting.BLUE))
        tooltip?.add(TranslatableText("item.indrev.chunk_scanner.tooltip3",
            LiteralText("X: ${pos.startX} Z: ${pos.startZ}").formatted(Formatting.WHITE),
            LiteralText("X: ${pos.endX} Z: ${pos.endZ}").formatted(Formatting.WHITE)).formatted(Formatting.BLUE))
        val dim = tag.getString("Dimension")
        tooltip?.add(TranslatableText("item.indrev.chunk_scanner.tooltip4", TranslatableText(dim).formatted(Formatting.WHITE)).formatted(Formatting.BLUE))
    }
}