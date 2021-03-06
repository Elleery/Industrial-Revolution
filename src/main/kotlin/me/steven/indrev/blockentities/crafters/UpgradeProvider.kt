package me.steven.indrev.blockentities.crafters

import me.steven.indrev.items.upgrade.Upgrade

interface UpgradeProvider {

    fun getUpgradeSlots(): IntArray

    fun getAvailableUpgrades(): Array<Upgrade>

    fun getBaseValue(upgrade: Upgrade): Double
}