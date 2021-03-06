package me.steven.indrev.blockentities.generators

import me.steven.indrev.IndustrialRevolution
import me.steven.indrev.components.InventoryComponent
import me.steven.indrev.components.Property
import me.steven.indrev.components.TemperatureComponent
import me.steven.indrev.config.GeneratorConfig
import me.steven.indrev.inventories.IRInventory
import me.steven.indrev.items.misc.IRCoolerItem
import me.steven.indrev.registry.MachineRegistry
import me.steven.indrev.utils.EMPTY_INT_ARRAY
import me.steven.indrev.utils.Tier
import net.minecraft.block.BlockState
import net.minecraft.block.entity.AbstractFurnaceBlockEntity
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.screen.ArrayPropertyDelegate
import team.reborn.energy.Energy

class CoalGeneratorBlockEntity :
    GeneratorBlockEntity(Tier.MK1, MachineRegistry.COAL_GENERATOR_REGISTRY) {

    init {
        this.propertyDelegate = ArrayPropertyDelegate(5)
        this.inventoryComponent = InventoryComponent {
            IRInventory(3, intArrayOf(2), EMPTY_INT_ARRAY) { slot, stack ->
                val item = stack?.item
                when {
                    Energy.valid(stack) && Energy.of(stack).maxOutput > 0 -> slot == 0
                    item is IRCoolerItem -> slot == 1
                    slot == 2 -> BURN_TIME_MAP.containsKey(stack?.item)
                    else -> false
                }
            }
        }
        this.temperatureComponent = TemperatureComponent({ this }, 0.08, 900..2000, 2500.0)
    }

    private var burnTime: Int by Property(3, 0)
    private var maxBurnTime: Int by Property(4, 0)

    override fun shouldGenerate(): Boolean {
        if (burnTime > 0) burnTime--
        else if (maxStoredPower > energy) {
            val inventory = inventoryComponent?.inventory ?: return false
            val invStack = inventory.getStack(2)
            val item = invStack.item
            if (!invStack.isEmpty && BURN_TIME_MAP.containsKey(invStack.item)) {
                burnTime = BURN_TIME_MAP[invStack.item] ?: return false
                maxBurnTime = burnTime
                invStack.count--
                if (!invStack.isEmpty)
                    inventory.setStack(2, invStack)
                else if (item.hasRecipeRemainder())
                    inventory.setStack(2, ItemStack(item.recipeRemainder))
                else
                    inventory.setStack(2, ItemStack.EMPTY)
            }
        }
        markDirty()
        return burnTime > 0 && energy < maxStoredPower
    }

    override fun fromTag(state: BlockState?, tag: CompoundTag?) {
        super.fromTag(state, tag)
        burnTime = tag?.getInt("BurnTime") ?: 0
        maxBurnTime = tag?.getInt("MaxBurnTime") ?: 0
    }

    override fun toTag(tag: CompoundTag?): CompoundTag {
        tag?.putInt("BurnTime", burnTime)
        tag?.putInt("MaxBurnTime", maxBurnTime)
        return super.toTag(tag)
    }

    override fun fromClientTag(tag: CompoundTag?) {
        super.fromClientTag(tag)
        burnTime = tag?.getInt("BurnTime") ?: 0
        maxBurnTime = tag?.getInt("MaxBurnTime") ?: 0
    }

    override fun toClientTag(tag: CompoundTag?): CompoundTag {
        tag?.putInt("BurnTime", burnTime)
        tag?.putInt("MaxBurnTime", maxBurnTime)
        return super.toClientTag(tag)
    }

    override fun getConfig(): GeneratorConfig = IndustrialRevolution.CONFIG.generators.coalGenerator

    companion object {
        private val BURN_TIME_MAP = AbstractFurnaceBlockEntity.createFuelTimeMap()
    }
}