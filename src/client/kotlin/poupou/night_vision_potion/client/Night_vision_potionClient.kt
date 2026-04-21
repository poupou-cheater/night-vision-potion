package poupou.night_vision_potion.client

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects

class Night_vision_potionClient : ClientModInitializer {

    private var checkCooldownTicks = 0

    companion object {
        private const val CHECK_INTERVAL_TICKS = 20
        private const val EFFECT_DURATION_TICKS = 420
        private const val REAPPLY_THRESHOLD_TICKS = 400
    }

    override fun onInitializeClient() {
        NightVisionClientConfig.load()

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            val player = client.player ?: return@register
            if (client.isPaused) return@register
            if (!NightVisionClientConfig.isEnabled()) {
                if (player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
                    player.removeStatusEffect(StatusEffects.NIGHT_VISION)
                }
                return@register
            }

            if (checkCooldownTicks > 0) {
                checkCooldownTicks--
                return@register
            }
            checkCooldownTicks = CHECK_INTERVAL_TICKS

            // Reapply before expiry to keep vision bright continuously.
            val current = player.getStatusEffect(StatusEffects.NIGHT_VISION)
            if (current == null || current.duration < REAPPLY_THRESHOLD_TICKS) {
                player.addStatusEffect(
                    StatusEffectInstance(StatusEffects.NIGHT_VISION, EFFECT_DURATION_TICKS, 0, false, false, false)
                )
            }
        }
    }
}
