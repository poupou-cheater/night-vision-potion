package poupou.night_vision_potion.client

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.screen.ScreenTexts
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import kotlin.math.roundToInt

class NightVisionModMenu : ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> {
        return ConfigScreenFactory { parent ->
            NightVisionConfigScreen(parent)
        }
    }
}

private enum class ConfigCategory(val label: String) {
    POTIONS("Potions"),
    OTHER("Autre")
}

private class NightVisionConfigScreen(parent: Screen?) : Screen(Text.literal("Night Vision Potion")) {
    private val parentScreen = parent
    private var selectedCategory = ConfigCategory.POTIONS
    private var potionScrollOffset = 0

    private data class Layout(
        val panelX: Int,
        val panelY: Int,
        val panelWidth: Int,
        val panelHeight: Int,
        val tabsY: Int,
        val listY: Int,
        val rowHeight: Int,
        val labelX: Int,
        val toggleX: Int,
        val valueX: Int,
        val scrollX: Int,
        val contentWidth: Int,
        val toggleWidth: Int,
        val valueWidth: Int,
        val scrollHeight: Int,
        val doneY: Int
    )

    override fun init() {
        super.init()

        val layout = layout()

        addCategoryButtons(layout)

        when (selectedCategory) {
            ConfigCategory.POTIONS -> addPotionRows(layout)
            ConfigCategory.OTHER -> {
                addToggleRow(
                    rowY = layout.listY + layout.rowHeight,
                    toggleX = layout.toggleX,
                    toggleWidth = layout.toggleWidth,
                    valueX = layout.valueX,
                    valueWidth = layout.valueWidth,
                    checked = { NightVisionClientConfig.shouldClearNegativeEffects() },
                    onToggle = {
                        NightVisionClientConfig.setClearNegativeEffects(!NightVisionClientConfig.shouldClearNegativeEffects())
                    },
                    valueText = { Text.empty() }
                )

                addToggleRow(
                    rowY = layout.listY + layout.rowHeight * 2,
                    toggleX = layout.toggleX,
                    toggleWidth = layout.toggleWidth,
                    valueX = layout.valueX,
                    valueWidth = layout.valueWidth,
                    checked = { NightVisionClientConfig.shouldRemoveNetherColor() },
                    onToggle = {
                        NightVisionClientConfig.setRemoveNetherColor(!NightVisionClientConfig.shouldRemoveNetherColor())
                    },
                    valueText = { Text.empty() }
                )

                addToggleRow(
                    rowY = layout.listY + layout.rowHeight * 3,
                    toggleX = layout.toggleX,
                    toggleWidth = layout.toggleWidth,
                    valueX = layout.valueX,
                    valueWidth = layout.valueWidth,
                    checked = { NightVisionClientConfig.shouldNoFallDamage() },
                    onToggle = {
                        NightVisionClientConfig.setNoFallDamage(!NightVisionClientConfig.shouldNoFallDamage())
                    },
                    valueText = { Text.empty() }
                )

                addToggleRow(
                    rowY = layout.listY + layout.rowHeight * 4,
                    toggleX = layout.toggleX,
                    toggleWidth = layout.toggleWidth,
                    valueX = layout.valueX,
                    valueWidth = layout.valueWidth,
                    checked = { NightVisionClientConfig.shouldAntiHunger() },
                    onToggle = {
                        NightVisionClientConfig.setAntiHunger(!NightVisionClientConfig.shouldAntiHunger())
                    },
                    valueText = { Text.empty() }
                )
            }
        }

        addDrawableChild(
            ButtonWidget.builder(ScreenTexts.DONE) { close() }
                .dimensions(layout.panelX + 16, layout.doneY, layout.panelWidth - 32, 20)
                .build()
        )
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val layout = layout()

        drawPanel(context, layout)
        super.render(context, mouseX, mouseY, delta)
        drawTexts(context, layout)
        if (selectedCategory == ConfigCategory.POTIONS) {
            drawPotionScrollBar(context, layout)
        }
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        if (selectedCategory != ConfigCategory.POTIONS) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
        }

        val previousOffset = potionScrollOffset
        val delta = if (verticalAmount < 0) 1 else -1
        potionScrollOffset = (potionScrollOffset + delta).coerceIn(0, maxPotionScrollOffset())
        if (previousOffset != potionScrollOffset) {
            clearAndInit()
            return true
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun close() {
        client?.setScreen(parentScreen)
    }

    private fun addCategoryButtons(layout: Layout) {
        val spacing = 8
        val categoryWidth = (layout.contentWidth - spacing) / 2

        addDrawableChild(
            ButtonWidget.builder(categoryText(ConfigCategory.POTIONS)) {
                selectedCategory = ConfigCategory.POTIONS
                clearAndInit()
            }
                .dimensions(layout.labelX, layout.tabsY, categoryWidth, 20)
                .build()
        )

        addDrawableChild(
            ButtonWidget.builder(categoryText(ConfigCategory.OTHER)) {
                selectedCategory = ConfigCategory.OTHER
                clearAndInit()
            }
                .dimensions(layout.labelX + categoryWidth + spacing, layout.tabsY, categoryWidth, 20)
                .build()
        )
    }

    private fun addPotionRows(layout: Layout) {
        visiblePotionEffects().forEachIndexed { index, effect ->
            val rowY = layout.listY + layout.rowHeight * (index + 1)
            addEffectRow(effect, rowY, layout.toggleX, layout.toggleWidth, layout.valueX, layout.valueWidth)
        }
    }

    private fun addEffectRow(effect: ManagedEffect, rowY: Int, toggleX: Int, toggleWidth: Int, valueX: Int, valueWidth: Int) {
        val levelLabel = if (effect.supportsLevel) {
            { effectLevelText(effect) }
        } else {
            { Text.literal("22 sec") }
        }

        addDrawableChild(
            ButtonWidget.builder(effectToggleText(effect)) { button ->
                val current = NightVisionClientConfig.getEffectConfig(effect)
                NightVisionClientConfig.setEffectEnabled(effect, !current.enabled)
                button.message = effectToggleText(effect)
            }
                .dimensions(toggleX, rowY, toggleWidth, 20)
                .build()
        )

        if (effect.supportsLevel) {
            addDrawableChild(
                ButtonWidget.builder(levelLabel()) { button ->
                    NightVisionClientConfig.cycleEffectLevel(effect)
                    button.message = effectLevelText(effect)
                }
                    .dimensions(valueX, rowY, valueWidth, 20)
                    .build()
            )
        }
    }

    private fun addToggleRow(
        rowY: Int,
        toggleX: Int,
        toggleWidth: Int,
        valueX: Int,
        valueWidth: Int,
        checked: () -> Boolean,
        onToggle: () -> Unit,
        valueText: () -> Text
    ) {
        addDrawableChild(
            ButtonWidget.builder(toggleText(checked())) { button ->
                onToggle()
                button.message = toggleText(checked())
            }
                .dimensions(toggleX, rowY, toggleWidth, 20)
                .build()
        )

        if (valueText().string.isNotEmpty()) {
            addDrawableChild(
                ButtonWidget.builder(valueText()) {}
                    .dimensions(valueX, rowY, valueWidth, 20)
                    .build()
                    .apply { active = false }
            )
        }
    }

    private fun drawPanel(context: DrawContext, layout: Layout) {
        context.fill(0, 0, width, height, 0xCC0B0D10.toInt())
        context.fill(layout.panelX, layout.panelY, layout.panelX + layout.panelWidth, layout.panelY + layout.panelHeight, 0xEE171A1F.toInt())
        val borderColor = 0xFF2B313A.toInt()
        context.fill(layout.panelX, layout.panelY, layout.panelX + layout.panelWidth, layout.panelY + 1, borderColor)
        context.fill(layout.panelX, layout.panelY + layout.panelHeight - 1, layout.panelX + layout.panelWidth, layout.panelY + layout.panelHeight, borderColor)
        context.fill(layout.panelX, layout.panelY, layout.panelX + 1, layout.panelY + layout.panelHeight, borderColor)
        context.fill(layout.panelX + layout.panelWidth - 1, layout.panelY, layout.panelX + layout.panelWidth, layout.panelY + layout.panelHeight, borderColor)
        context.fill(layout.panelX + 1, layout.panelY + 1, layout.panelX + layout.panelWidth - 1, layout.panelY + 34, 0xFF111419.toInt())
    }

    private fun drawTexts(context: DrawContext, layout: Layout) {
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, layout.panelY + 10, 0xFFF2F4F8.toInt())
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Potions auto et correction Nether"), width / 2, layout.panelY + 22, 0xFF9AA4B2.toInt())

        context.drawTextWithShadow(textRenderer, Text.literal("Actif"), layout.toggleX + 8, layout.listY + 6, 0xFFAAB3C2.toInt())
        context.drawTextWithShadow(textRenderer, Text.literal("Niveau"), layout.valueX + 8, layout.listY + 6, 0xFFAAB3C2.toInt())

        when (selectedCategory) {
            ConfigCategory.POTIONS -> drawPotionLabels(context, layout)
            ConfigCategory.OTHER -> drawOtherLabels(context, layout)
        }
    }

    private fun drawPotionLabels(context: DrawContext, layout: Layout) {
        visiblePotionEffects().forEachIndexed { index, effect ->
            val rowY = layout.listY + layout.rowHeight * (index + 1) + 6
            context.drawTextWithShadow(textRenderer, Text.literal(effect.label), layout.labelX, rowY, 0xFFE6EAF0.toInt())
            if (!effect.supportsLevel) {
                context.drawTextWithShadow(textRenderer, Text.literal("22 sec"), layout.valueX + 12, rowY, 0xFF9AA4B2.toInt())
            }
        }
    }

    private fun drawOtherLabels(context: DrawContext, layout: Layout) {
        context.drawTextWithShadow(textRenderer, Text.literal("Suppr. effets négatifs"), layout.labelX, layout.listY + layout.rowHeight + 6, 0xFFE6EAF0.toInt())
        context.drawTextWithShadow(textRenderer, Text.literal("Suppr. couleur Nether"), layout.labelX, layout.listY + layout.rowHeight * 2 + 6, 0xFFE6EAF0.toInt())
        context.drawTextWithShadow(textRenderer, Text.literal("No fall damage"), layout.labelX, layout.listY + layout.rowHeight * 3 + 6, 0xFFE6EAF0.toInt())
        context.drawTextWithShadow(textRenderer, Text.literal("Anti hunger"), layout.labelX, layout.listY + layout.rowHeight * 4 + 6, 0xFFE6EAF0.toInt())
    }

    private fun drawPotionScrollBar(context: DrawContext, layout: Layout) {
        val maxOffset = maxPotionScrollOffset()
        if (maxOffset <= 0) return

        val trackX = layout.scrollX
        val trackY = layout.listY + layout.rowHeight
        val trackHeight = layout.scrollHeight - layout.rowHeight
        val thumbHeight = (trackHeight * visiblePotionRowCount().toFloat() / ManagedEffect.entries.size.toFloat()).roundToInt().coerceAtLeast(18)
        val travel = (trackHeight - thumbHeight).coerceAtLeast(1)
        val ratio = potionScrollOffset.toFloat() / maxOffset.toFloat()
        val thumbY = trackY + (travel * ratio).roundToInt()

        context.fill(trackX, trackY, trackX + 6, trackY + trackHeight, 0xFF242A33.toInt())
        context.fill(trackX, thumbY, trackX + 6, thumbY + thumbHeight, 0xFF6F7C8C.toInt())
    }

    private fun layout(): Layout {
        val panelWidth = (width * 0.78f).roundToInt().coerceIn(320, 430)
        val panelHeight = (height * 0.82f).roundToInt().coerceIn(260, 340)
        val panelX = (width - panelWidth) / 2
        val panelY = (height - panelHeight) / 2
        val rowHeight = 24
        val contentWidth = panelWidth - 32
        val labelWidth = (contentWidth * 0.42f).roundToInt()
        val toggleWidth = (contentWidth * 0.24f).roundToInt().coerceAtLeast(72)
        val valueWidth = (contentWidth * 0.24f).roundToInt().coerceAtLeast(82)
        val labelX = panelX + 16
        val toggleX = labelX + labelWidth + 8
        val valueX = toggleX + toggleWidth + 8
        val scrollX = panelX + panelWidth - 18
        val tabsY = panelY + 42
        val listY = tabsY + 28
        val doneY = panelY + panelHeight - 28
        val scrollHeight = doneY - listY - 8

        return Layout(
            panelX = panelX,
            panelY = panelY,
            panelWidth = panelWidth,
            panelHeight = panelHeight,
            tabsY = tabsY,
            listY = listY,
            rowHeight = rowHeight,
            labelX = labelX,
            toggleX = toggleX,
            valueX = valueX,
            scrollX = scrollX,
            contentWidth = contentWidth,
            toggleWidth = toggleWidth,
            valueWidth = valueWidth,
            scrollHeight = scrollHeight,
            doneY = doneY
        )
    }

    private fun visiblePotionEffects(): List<ManagedEffect> {
        val allEffects = ManagedEffect.entries
        return allEffects.drop(potionScrollOffset).take(visiblePotionRowCount())
    }

    private fun visiblePotionRowCount(): Int {
        val layout = layout()
        val maxBySpace = ((layout.scrollHeight - layout.rowHeight) / layout.rowHeight).coerceAtLeast(2)
        return minOf(maxBySpace, ManagedEffect.entries.size)
    }

    private fun maxPotionScrollOffset(): Int {
        return (ManagedEffect.entries.size - visiblePotionRowCount()).coerceAtLeast(0)
    }

    private fun toggleText(enabled: Boolean): Text {
        return Text.literal(if (enabled) "Oui" else "Non")
    }

    private fun effectToggleText(effect: ManagedEffect): Text {
        return toggleText(NightVisionClientConfig.getEffectConfig(effect).enabled)
    }

    private fun effectLevelText(effect: ManagedEffect): Text {
        return Text.literal("Niv. ${NightVisionClientConfig.getEffectConfig(effect).level}")
    }

    private fun categoryText(category: ConfigCategory): Text {
        return if (selectedCategory == category) {
            Text.literal("• ${category.label}").formatted(Formatting.WHITE)
        } else {
            Text.literal(category.label).formatted(Formatting.GRAY)
        }
    }
}
