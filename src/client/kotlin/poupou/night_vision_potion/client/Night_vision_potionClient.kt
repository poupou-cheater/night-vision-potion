package poupou.night_vision_potion.client

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.entity.effect.StatusEffectCategory
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket

class Night_vision_potionClient : ClientModInitializer {

    private var checkCooldownTicks = 0

    companion object {
        private const val CHECK_INTERVAL_TICKS = 20
        private const val EFFECT_DURATION_TICKS = 440
        private const val REAPPLY_THRESHOLD_TICKS = 420
    }

    override fun onInitializeClient() {
        NightVisionClientConfig.load()

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            val player = client.player ?: return@register
            if (client.isPaused) return@register

            if (NightVisionClientConfig.shouldNoFallDamage()) {
                applyNoFallDamage(player)
            }

            if (NightVisionClientConfig.shouldAntiHunger()) {
                applyAntiHunger(player)
            }

            if (checkCooldownTicks > 0) {
                checkCooldownTicks--
                return@register
            }
            checkCooldownTicks = CHECK_INTERVAL_TICKS

            if (NightVisionClientConfig.shouldClearNegativeEffects()) {
                clearNegativeEffects(player)
            }

            ManagedEffect.entries.forEach { effect ->
                val config = NightVisionClientConfig.getEffectConfig(effect)
                if (!config.enabled) {
                    clearManagedEffect(player, effect, config.level)
                    return@forEach
                }

                maintainEffect(player, effect, config.level)
            }
        }
    }

    private fun maintainEffect(player: PlayerEntity, effect: ManagedEffect, level: Int) {
        val desiredAmplifier = (level - 1).coerceAtLeast(0)
        val current = player.getStatusEffect(effect.effect)
        if (!shouldApplyEffect(current, desiredAmplifier)) return

        player.addStatusEffect(
            StatusEffectInstance(effect.effect, EFFECT_DURATION_TICKS, desiredAmplifier, false, false, false)
        )
    }

    private fun shouldApplyEffect(current: StatusEffectInstance?, desiredAmplifier: Int): Boolean {
        if (current == null) return true
        if (current.amplifier < desiredAmplifier) return true
        if (current.amplifier > desiredAmplifier) return false
        return isManagedInstance(current, desiredAmplifier) && current.duration < REAPPLY_THRESHOLD_TICKS
    }

    private fun clearManagedEffects(player: PlayerEntity) {
        ManagedEffect.entries.forEach { effect ->
            clearManagedEffect(player, effect, NightVisionClientConfig.getEffectConfig(effect).level)
        }
    }

    private fun clearManagedEffect(player: PlayerEntity, effect: ManagedEffect, level: Int) {
        val current = player.getStatusEffect(effect.effect) ?: return
        val desiredAmplifier = (level - 1).coerceAtLeast(0)
        if (isManagedInstance(current, desiredAmplifier)) {
            player.removeStatusEffect(effect.effect)
        }
    }

    private fun clearNegativeEffects(player: PlayerEntity) {
        player.activeStatusEffects.entries.toList().forEach { (effect, instance) ->
            if (instance.effectType.value().category == StatusEffectCategory.HARMFUL) {
                player.removeStatusEffect(effect)
            }
        }
    }

    private fun applyNoFallDamage(player: ClientPlayerEntity) {
        if (player.isOnGround) return
        if (player.hasVehicle()) return
        if (player.isTouchingWater || player.isSubmergedInWater) return
        if (player.velocity.y >= 0.0) return

        player.fallDistance = 0.0
        player.networkHandler.sendPacket(PlayerMoveC2SPacket.OnGroundOnly(true, player.horizontalCollision))
    }

    private fun applyAntiHunger(player: ClientPlayerEntity) {
        player.hungerManager.add(20, 1.0f)
    }

    private fun isManagedInstance(current: StatusEffectInstance, desiredAmplifier: Int): Boolean {
        return current.amplifier == desiredAmplifier
            && current.duration <= EFFECT_DURATION_TICKS
            && !current.isAmbient
            && !current.shouldShowParticles()
            && !current.shouldShowIcon()
    }
}
