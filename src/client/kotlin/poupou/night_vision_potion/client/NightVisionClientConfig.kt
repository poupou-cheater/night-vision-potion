package poupou.night_vision_potion.client

import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import net.fabricmc.loader.api.FabricLoader
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

object NightVisionClientConfig {
    private const val FILE_NAME = "night_vision_potion.json"
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val configPath: Path = FabricLoader.getInstance().configDir.resolve(FILE_NAME)

    @Volatile
    private var state = State()

    fun load() {
        if (!Files.exists(configPath)) {
            save()
            return
        }

        try {
            Files.newBufferedReader(configPath).use { reader ->
                state = (gson.fromJson(reader, SavedState::class.java) ?: SavedState()).toState()
            }
        } catch (_: IOException) {
            state = State()
        } catch (_: JsonParseException) {
            state = State()
        }
    }

    fun save() {
        try {
            Files.createDirectories(configPath.parent)
            Files.newBufferedWriter(configPath).use { writer ->
                gson.toJson(SavedState.fromState(state), writer)
            }
        } catch (_: IOException) {
            // Keep running with in-memory config if disk write fails.
        }
    }

    fun shouldClearNegativeEffects(): Boolean = state.clearNegativeEffects

    fun setClearNegativeEffects(enabled: Boolean) {
        if (state.clearNegativeEffects == enabled) return
        state = state.copy(clearNegativeEffects = enabled)
        save()
    }

    fun shouldRemoveNetherColor(): Boolean = state.removeNetherColor

    fun setRemoveNetherColor(enabled: Boolean) {
        if (state.removeNetherColor == enabled) return
        state = state.copy(removeNetherColor = enabled)
        save()
    }

    fun getEffectConfig(effect: ManagedEffect): EffectConfig {
        return state.effects[effect] ?: EffectConfig(enabled = effect.defaultEnabled)
    }

    fun setEffectEnabled(effect: ManagedEffect, enabled: Boolean) {
        val current = getEffectConfig(effect)
        if (current.enabled == enabled) return
        state = state.withEffect(effect, current.copy(enabled = enabled))
        save()
    }

    fun cycleEffectLevel(effect: ManagedEffect) {
        if (!effect.supportsLevel) return

        val current = getEffectConfig(effect)
        val nextLevel = if (current.level >= 5) 1 else current.level + 1
        state = state.withEffect(effect, current.copy(level = nextLevel))
        save()
    }

    data class State(
        val clearNegativeEffects: Boolean = false,
        val removeNetherColor: Boolean = true,
        val effects: Map<ManagedEffect, EffectConfig> = ManagedEffect.entries.associateWith {
            EffectConfig(enabled = it.defaultEnabled)
        }
    ) {
        fun withEffect(effect: ManagedEffect, config: EffectConfig): State {
            val updatedEffects = effects.toMutableMap()
            updatedEffects[effect] = config.normalized(effect)
            return copy(effects = updatedEffects)
        }
    }

    data class EffectConfig(
        val enabled: Boolean = false,
        val level: Int = 1
    ) {
        fun normalized(effect: ManagedEffect): EffectConfig {
            return copy(
                enabled = enabled,
                level = if (effect.supportsLevel) level.coerceIn(1, 5) else 1
            )
        }
    }

    private data class SavedState(
        val clearNegativeEffects: Boolean? = null,
        val removeNetherColor: Boolean? = null,
        val nightVision: SavedEffectConfig? = null,
        val speed: SavedEffectConfig? = null,
        val haste: SavedEffectConfig? = null,
        val strength: SavedEffectConfig? = null,
        val jumpBoost: SavedEffectConfig? = null,
        val luck: SavedEffectConfig? = null,
        val waterBreathing: SavedEffectConfig? = null,
        val dolphinsGrace: SavedEffectConfig? = null
    ) {
        fun toState(): State {
            val resolvedEffects = mutableMapOf<ManagedEffect, EffectConfig>()
            resolvedEffects[ManagedEffect.NIGHT_VISION] = (nightVision?.toEffectConfig()
                ?: EffectConfig(enabled = ManagedEffect.NIGHT_VISION.defaultEnabled)).normalized(ManagedEffect.NIGHT_VISION)
            resolvedEffects[ManagedEffect.SPEED] = (speed?.toEffectConfig()
                ?: EffectConfig(enabled = ManagedEffect.SPEED.defaultEnabled)).normalized(ManagedEffect.SPEED)
            resolvedEffects[ManagedEffect.HASTE] = (haste?.toEffectConfig()
                ?: EffectConfig(enabled = ManagedEffect.HASTE.defaultEnabled)).normalized(ManagedEffect.HASTE)
            resolvedEffects[ManagedEffect.STRENGTH] = (strength?.toEffectConfig()
                ?: EffectConfig(enabled = ManagedEffect.STRENGTH.defaultEnabled)).normalized(ManagedEffect.STRENGTH)
            resolvedEffects[ManagedEffect.JUMP_BOOST] = (jumpBoost?.toEffectConfig()
                ?: EffectConfig(enabled = ManagedEffect.JUMP_BOOST.defaultEnabled)).normalized(ManagedEffect.JUMP_BOOST)
            resolvedEffects[ManagedEffect.LUCK] = (luck?.toEffectConfig()
                ?: EffectConfig(enabled = ManagedEffect.LUCK.defaultEnabled)).normalized(ManagedEffect.LUCK)
            resolvedEffects[ManagedEffect.WATER_BREATHING] = (waterBreathing?.toEffectConfig()
                ?: EffectConfig(enabled = ManagedEffect.WATER_BREATHING.defaultEnabled)).normalized(ManagedEffect.WATER_BREATHING)
            resolvedEffects[ManagedEffect.DOLPHINS_GRACE] = (dolphinsGrace?.toEffectConfig()
                ?: EffectConfig(enabled = ManagedEffect.DOLPHINS_GRACE.defaultEnabled)).normalized(ManagedEffect.DOLPHINS_GRACE)

            return State(
                clearNegativeEffects = clearNegativeEffects ?: false,
                removeNetherColor = removeNetherColor ?: true,
                effects = resolvedEffects
            )
        }

        companion object {
            fun fromState(state: State): SavedState {
                return SavedState(
                    clearNegativeEffects = state.clearNegativeEffects,
                    removeNetherColor = state.removeNetherColor,
                    nightVision = SavedEffectConfig.fromEffectConfig(state.effects.getValue(ManagedEffect.NIGHT_VISION)),
                    speed = SavedEffectConfig.fromEffectConfig(state.effects.getValue(ManagedEffect.SPEED)),
                    haste = SavedEffectConfig.fromEffectConfig(state.effects.getValue(ManagedEffect.HASTE)),
                    strength = SavedEffectConfig.fromEffectConfig(state.effects.getValue(ManagedEffect.STRENGTH)),
                    jumpBoost = SavedEffectConfig.fromEffectConfig(state.effects.getValue(ManagedEffect.JUMP_BOOST)),
                    luck = SavedEffectConfig.fromEffectConfig(state.effects.getValue(ManagedEffect.LUCK)),
                    waterBreathing = SavedEffectConfig.fromEffectConfig(state.effects.getValue(ManagedEffect.WATER_BREATHING)),
                    dolphinsGrace = SavedEffectConfig.fromEffectConfig(state.effects.getValue(ManagedEffect.DOLPHINS_GRACE))
                )
            }
        }
    }

    private data class SavedEffectConfig(
        val enabled: Boolean? = null,
        val level: Int? = null
    ) {
        fun toEffectConfig(): EffectConfig {
            return EffectConfig(
                enabled = enabled ?: false,
                level = level ?: 1
            )
        }

        companion object {
            fun fromEffectConfig(config: EffectConfig): SavedEffectConfig {
                return SavedEffectConfig(
                    enabled = config.enabled,
                    level = config.level
                )
            }
        }
    }
}
