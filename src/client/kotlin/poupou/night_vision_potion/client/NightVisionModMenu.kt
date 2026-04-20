package poupou.night_vision_potion.client

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.screen.ScreenTexts
import net.minecraft.text.Text

class NightVisionModMenu : ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> {
        return ConfigScreenFactory { parent ->
            NightVisionConfigScreen(parent)
        }
    }
}

private class NightVisionConfigScreen(parent: Screen?) : Screen(Text.literal("Night Vision Potion")) {
    private val parentScreen = parent

    override fun init() {
        super.init()

        val buttonWidth = 220
        val buttonX = (width - buttonWidth) / 2
        val toggleY = height / 2 - 10
        val doneY = height / 2 + 24

        addDrawableChild(
            ButtonWidget.builder(toggleText()) {
                NightVisionClientConfig.setEnabled(!NightVisionClientConfig.isEnabled())
                it.message = toggleText()
            }
                .dimensions(buttonX, toggleY, buttonWidth, 20)
                .build()
        )

        addDrawableChild(
            ButtonWidget.builder(ScreenTexts.DONE) {
                close()
            }
                .dimensions(buttonX, doneY, buttonWidth, 20)
                .build()
        )
    }

    override fun close() {
        client?.setScreen(parentScreen)
    }

    private fun toggleText(): Text {
        val enabledLabel = if (NightVisionClientConfig.isEnabled()) "ON" else "OFF"
        return Text.literal("Night Vision: $enabledLabel")
    }
}
