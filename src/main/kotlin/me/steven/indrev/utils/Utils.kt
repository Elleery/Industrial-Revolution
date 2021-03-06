package me.steven.indrev.utils

import com.google.gson.JsonObject
import com.mojang.blaze3d.systems.RenderSystem
import me.steven.indrev.IndustrialRevolution
import me.steven.indrev.config.CableConfig
import me.steven.indrev.config.GeneratorConfig
import me.steven.indrev.config.HeatMachineConfig
import me.steven.indrev.config.IConfig
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry
import net.fabricmc.fabric.impl.screenhandler.ExtendedScreenHandlerType
import net.minecraft.block.Block
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.render.BufferRenderer
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.VertexFormats
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.fluid.Fluid
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.text.LiteralText
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.ChunkPos
import net.minecraft.util.math.Vec3d
import net.minecraft.util.registry.Registry
import team.reborn.energy.Energy
import team.reborn.energy.EnergySide

val EMPTY_INT_ARRAY = intArrayOf()

fun identifier(id: String) = Identifier(IndustrialRevolution.MOD_ID, id)

fun Identifier.block(block: Block): Identifier {
    Registry.register(Registry.BLOCK, this, block)
    return this
}

fun Identifier.fluid(fluid: Fluid): Identifier {
    Registry.register(Registry.FLUID, this, fluid)
    return this
}

fun Identifier.item(item: Item): Identifier {
    Registry.register(Registry.ITEM, this, item)
    return this
}

fun Identifier.blockEntityType(entityType: BlockEntityType<*>): Identifier {
    Registry.register(Registry.BLOCK_ENTITY_TYPE, this, entityType)
    return this
}

fun Identifier.tierBasedItem(vararg tiers: Tier = Tier.VALUES, itemSupplier: (Tier) -> Item) {
    tiers.forEach { tier ->
        val item = itemSupplier(tier)
        identifier("${this.path}_${tier.toString().toLowerCase()}").item(item)
    }
}

fun <T : ScreenHandler> Identifier.registerScreenHandler(
    f: (Int, PlayerInventory, ScreenHandlerContext) -> T
): ExtendedScreenHandlerType<T> =
    ScreenHandlerRegistry.registerExtended(this) { syncId, inv, buf ->
        f(syncId, inv, ScreenHandlerContext.create(inv.player.world, buf.readBlockPos()))
    } as ExtendedScreenHandlerType<T>

fun Box.isSide(vec3d: Vec3d) =
    vec3d.x == minX || vec3d.x == maxX - 1 || vec3d.y == minY || vec3d.y == maxY - 1 || vec3d.z == minZ || vec3d.z == maxZ - 1

fun itemSettings(): Item.Settings = Item.Settings().group(IndustrialRevolution.MOD_GROUP)

fun IntRange.toIntArray(): IntArray = this.map { it }.toIntArray()

fun BlockPos.toVec3d() = Vec3d(x.toDouble(), y.toDouble(), z.toDouble())

fun ChunkPos.asString() = "$x,$z"

infix fun <T> Boolean.then(param: () -> T): T? = if (this) param() else null

fun getChunkPos(s: String): ChunkPos? {
    val split = s.split(",")
    val x = split[0].toIntOrNull() ?: return null
    val z = split[1].toIntOrNull() ?: return null
    return ChunkPos(x, z)
}

fun EnergySide.opposite(): EnergySide =
    when (this) {
        EnergySide.DOWN -> EnergySide.UP
        EnergySide.UP -> EnergySide.DOWN
        EnergySide.NORTH -> EnergySide.SOUTH
        EnergySide.SOUTH -> EnergySide.NORTH
        EnergySide.WEST -> EnergySide.EAST
        EnergySide.EAST -> EnergySide.WEST
        EnergySide.UNKNOWN -> EnergySide.UNKNOWN
    }

fun getShortEnergyDisplay(energy: Double): String =
    when {
        energy > 1000000 -> "${"%.1f".format(energy / 1000000)}M"
        energy > 1000 -> "${"%.1f".format(energy / 1000)}k"
        else -> "%.1f".format(energy)
    }

fun buildEnergyTooltip(stack: ItemStack?, tooltip: MutableList<Text>?) {
    val handler = Energy.of(stack)
    tooltip?.add(TranslatableText("gui.widget.energy").formatted(Formatting.BLUE))
    tooltip?.add(LiteralText("${getShortEnergyDisplay(handler.energy)} / ${getShortEnergyDisplay(handler.maxStored)} LF").formatted(Formatting.GOLD))
    tooltip?.add(TranslatableText("item.indrev.rechargeable.tooltip").formatted(Formatting.ITALIC, Formatting.GRAY))
}

fun buildMachineTooltip(config: Any, tooltip: MutableList<Text>?) {
    if (Screen.hasShiftDown()) {
        when (config) {
            is IConfig -> {
                tooltip?.add(
                    TranslatableText("gui.indrev.tooltip.maxInput").formatted(Formatting.BLUE)
                        .append(
                            TranslatableText(
                                "gui.indrev.tooltip.lftick",
                                config.maxInput
                            ).formatted(Formatting.GOLD)
                        )
                )
                tooltip?.add(
                    TranslatableText("gui.indrev.tooltip.maxEnergyStored").formatted(Formatting.BLUE)
                        .append(
                            TranslatableText(
                                "gui.indrev.tooltip.lf",
                                getShortEnergyDisplay(config.maxEnergyStored)
                            ).formatted(
                                Formatting.GOLD
                            )
                        )
                )
                tooltip?.add(
                    TranslatableText("gui.indrev.tooltip.energyCost").formatted(Formatting.BLUE)
                        .append(
                            TranslatableText(
                                "gui.indrev.tooltip.lf",
                                config.energyCost
                            ).formatted(Formatting.GOLD)
                        )
                )
                tooltip?.add(
                    TranslatableText("gui.indrev.tooltip.processSpeed").formatted(Formatting.BLUE)
                        .append(LiteralText(config.processSpeed.toString()).formatted(Formatting.GOLD))
                )
            }
            is HeatMachineConfig -> {
                tooltip?.add(
                    TranslatableText("gui.indrev.tooltip.maxInput").formatted(Formatting.BLUE)
                        .append(
                            TranslatableText(
                                "gui.indrev.tooltip.lftick",
                                config.maxInput
                            ).formatted(Formatting.GOLD)
                        )
                )
                tooltip?.add(
                    TranslatableText("gui.indrev.tooltip.maxEnergyStored").formatted(Formatting.BLUE)
                        .append(
                            TranslatableText(
                                "gui.indrev.tooltip.lf",
                                getShortEnergyDisplay(config.maxEnergyStored)
                            ).formatted(
                                Formatting.GOLD
                            )
                        )
                )
                tooltip?.add(
                    TranslatableText("gui.indrev.tooltip.energyCost").formatted(Formatting.BLUE)
                        .append(
                            TranslatableText(
                                "gui.indrev.tooltip.lf",
                                config.energyCost
                            ).formatted(Formatting.GOLD)
                        )
                )
                tooltip?.add(
                    TranslatableText("gui.indrev.tooltip.processSpeed").formatted(Formatting.BLUE)
                        .append(LiteralText(config.processSpeed.toString()).formatted(Formatting.GOLD))
                )
                tooltip?.add(
                    TranslatableText("gui.indrev.tooltip.processSpeed").formatted(Formatting.BLUE)
                        .append(
                            TranslatableText(
                                "gui.indrev.tooltip.lftick",
                                config.processTemperatureBoost
                            ).formatted(Formatting.GOLD)
                        )
                )
            }
            is GeneratorConfig -> {
                tooltip?.add(
                    TranslatableText("gui.indrev.tooltip.maxOutput").formatted(Formatting.BLUE)
                        .append(
                            TranslatableText(
                                "gui.indrev.tooltip.lftick",
                                config.maxOutput
                            ).formatted(Formatting.GOLD)
                        )
                )
                tooltip?.add(
                    TranslatableText("gui.indrev.tooltip.maxEnergyStored").formatted(Formatting.BLUE)
                        .append(
                            TranslatableText(
                                "gui.indrev.tooltip.lf",
                                getShortEnergyDisplay(config.maxEnergyStored)
                            ).formatted(
                                Formatting.GOLD
                            )
                        )
                )
                tooltip?.add(
                    TranslatableText("gui.indrev.tooltip.ratio").formatted(Formatting.BLUE)
                        .append(
                            TranslatableText(
                                "gui.indrev.tooltip.lftick",
                                config.ratio
                            ).formatted(Formatting.GOLD)
                        )
                )
                tooltip?.add(
                    TranslatableText("gui.indrev.tooltip.processSpeed").formatted(Formatting.BLUE)
                        .append(
                            TranslatableText("gui.indrev.tooltip.lftick", config.temperatureBoost).formatted(
                                Formatting.GOLD
                            )
                        )
                )
            }
            is CableConfig -> {
                tooltip?.add(
                    TranslatableText("gui.indrev.tooltip.maxInput").formatted(Formatting.BLUE)
                        .append(
                            TranslatableText(
                                "gui.indrev.tooltip.lftick",
                                config.maxInput
                            ).formatted(Formatting.GOLD)
                        )
                )
                tooltip?.add(
                    TranslatableText("gui.indrev.tooltip.maxOutput").formatted(Formatting.BLUE)
                        .append(
                            TranslatableText(
                                "gui.indrev.tooltip.lftick",
                                config.maxOutput
                            ).formatted(Formatting.GOLD)
                        )
                )
            }
        }
    } else {
        tooltip?.add(
            TranslatableText("gui.indrev.tooltip.press_shift").formatted(
                Formatting.BLUE,
                Formatting.ITALIC
            )
        )
    }
}

fun draw2Colors(matrices: MatrixStack, x1: Int, y1: Int, x2: Int, y2: Int, color1: Long, color2: Long) {
    val matrix = matrices.peek().model

    var j: Int
    var xx1 = x1.toFloat()
    var xx2 = x2.toFloat()
    var yy1 = x1.toFloat()
    var yy2 = x2.toFloat()

    if (x1 < x2) {
        j = x1
        xx1 = x2.toFloat()
        xx2 = j.toFloat()
    }

    if (y1 < y2) {
        j = y1
        yy1 = y2.toFloat()
        yy2 = j.toFloat()
    }

    val f1 = (color1 shr 24 and 255) / 255.0f
    val g1 = (color1 shr 16 and 255) / 255.0f
    val h1 = (color1 shr 8 and 255) / 255.0f
    val k1 = (color1 and 255) / 255.0f

    val f2 = (color2 shr 24 and 255) / 255.0f
    val g2 = (color2 shr 16 and 255) / 255.0f
    val h2 = (color2 shr 8 and 255) / 255.0f
    val k2 = (color2 and 255) / 255.0f

    RenderSystem.enableBlend()
    RenderSystem.disableTexture()
    RenderSystem.defaultBlendFunc()
    Tessellator.getInstance().buffer.run {
        begin(7, VertexFormats.POSITION_COLOR)
        vertex(matrix, xx1, yy1, 0.0f).color(g1, h1, k1, f1).next()
        vertex(matrix, xx1, yy2, 0.0f).color(g1, h1, k1, f1).next()
        vertex(matrix, xx2, yy2, 0.0f).color(g1, h1, k1, f1).next()
        vertex(matrix, xx1, yy1, 0.0f).color(g1, h1, k1, f1).next()
        end()
        BufferRenderer.draw(this)
        begin(7, VertexFormats.POSITION_COLOR)
        vertex(matrix, xx1, yy1, 0.0f).color(g2, h2, k2, f2).next()
        vertex(matrix, xx2, yy2, 0.0f).color(g2, h2, k2, f2).next()
        vertex(matrix, xx2, yy1, 0.0f).color(g2, h2, k2, f2).next()
        vertex(matrix, xx1, yy1, 0.0f).color(g2, h2, k2, f2).next()
        end()
        BufferRenderer.draw(this)
    }
    RenderSystem.enableTexture()
    RenderSystem.disableBlend()
}

fun parsePossibleOutputs(obj: JsonObject): Array<Identifier> =
    Array(obj.size()) { i -> Identifier(obj.get(i.toString()).asString) }

fun <T> getFirstMatch(identifier: Array<Identifier>, registry: Registry<T>): T =
    registry[identifier.first { registry.getOrEmpty(it).isPresent }]!!