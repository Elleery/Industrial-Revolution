package me.steven.indrev.blockentities.generators

import me.steven.indrev.blocks.nuclear.NuclearReactorCore
import me.steven.indrev.inventories.DefaultSidedInventory
import me.steven.indrev.items.CoolerItem
import me.steven.indrev.items.UraniumRodItem
import me.steven.indrev.items.rechargeable.RechargeableItem
import me.steven.indrev.registry.MachineRegistry
import me.steven.indrev.utils.Tier
import net.minecraft.item.ItemStack

class NuclearReactorBlockEntity : GeneratorBlockEntity(Tier.MK4, MachineRegistry.NUCLEAR_GENERATOR_REGISTRY) {

    private var loadedFuel = 0
    private var modifier = 0

    override fun shouldGenerate(): Boolean {
        val block = this.cachedState.block
        if (block is NuclearReactorCore && !block.isFormed(this.cachedState)) return false
        else if (loadedFuel <= 0) {
            modifier = 0
            for (slot in getInventory().inputSlots) {
                val itemStack = getInventory().getInvStack(slot)
                val item = itemStack.item
                if (item is UraniumRodItem) {
                    itemStack.damage++
                    if (itemStack.damage <= 0) getInventory().setInvStack(slot, ItemStack.EMPTY)
                    modifier++
                    loadedFuel += 10
                }
            }
        }
        loadedFuel--
        return loadedFuel > 0
    }

    override fun getGenerationRatio(): Double = if (temperature.toInt() in getOptimalRange()) 300.0 else 10.0

    override fun getOptimalRange(): IntRange = 2000..3000

    override fun getBaseHeatingEfficiency(): Double = 2.3

    override fun getLimitTemperature(): Double = 4000.0

    override fun createInventory(): DefaultSidedInventory =
        DefaultSidedInventory(11, intArrayOf(2, 3, 4, 5, 6, 7, 8, 9, 10), intArrayOf()) { slot, stack ->
            val item = stack?.item
            when {
                item is RechargeableItem && item.canOutput -> slot == 0
                item is CoolerItem -> slot == 1
                item is UraniumRodItem -> slot != 1 && slot != 0
                else -> false
            }
        }
}