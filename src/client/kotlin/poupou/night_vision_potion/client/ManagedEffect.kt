package poupou.night_vision_potion.client

import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.registry.entry.RegistryEntry

enum class ManagedEffect(
    val label: String,
    val effect: RegistryEntry<StatusEffect>,
    val defaultEnabled: Boolean = false,
    val supportsLevel: Boolean = true
) {
    NIGHT_VISION("Night Vision", StatusEffects.NIGHT_VISION, defaultEnabled = true, supportsLevel = false),
    SPEED("Speed", StatusEffects.SPEED),
    HASTE("Haste", StatusEffects.HASTE),
    STRENGTH("Strength", StatusEffects.STRENGTH),
    JUMP_BOOST("Jump", StatusEffects.JUMP_BOOST),
    LUCK("Luck", StatusEffects.LUCK),
    WATER_BREATHING("Water Breathing", StatusEffects.WATER_BREATHING, supportsLevel = false),
    DOLPHINS_GRACE("Dolphin's Grace", StatusEffects.DOLPHINS_GRACE, supportsLevel = false)
}