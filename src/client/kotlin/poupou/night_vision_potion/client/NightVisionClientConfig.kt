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
    private var state = State(enabled = true)

    fun load() {
        if (!Files.exists(configPath)) {
            save()
            return
        }

        try {
            Files.newBufferedReader(configPath).use { reader ->
                state = gson.fromJson(reader, State::class.java) ?: State(enabled = true)
            }
        } catch (_: IOException) {
            state = State(enabled = true)
        } catch (_: JsonParseException) {
            state = State(enabled = true)
        }
    }

    fun save() {
        try {
            Files.createDirectories(configPath.parent)
            Files.newBufferedWriter(configPath).use { writer ->
                gson.toJson(state, writer)
            }
        } catch (_: IOException) {
            // Keep running with in-memory config if disk write fails.
        }
    }

    fun isEnabled(): Boolean = state.enabled

    fun setEnabled(enabled: Boolean) {
        if (state.enabled == enabled) return
        state = state.copy(enabled = enabled)
        save()
    }

    data class State(
        val enabled: Boolean = true
    )
}
