package poupou.night_vision_potion.mixin.client

import net.minecraft.client.render.Camera
import net.minecraft.client.world.ClientWorld
import net.minecraft.world.World
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import poupou.night_vision_potion.client.NightVisionClientConfig

@Mixin(targets = ["net.minecraft.client.render.BackgroundRenderer"])
abstract class BackgroundRendererMixin {

    @Inject(method = ["render"], at = [At("TAIL")])
    private fun removeNetherTint(
        camera: Camera,
        tickDelta: Float,
        world: ClientWorld,
        viewDistance: Int,
        skyDarkness: Float,
        ci: CallbackInfo
    ) {
        if (!NightVisionClientConfig.shouldRemoveNetherColor()) return
        if (world.registryKey != World.NETHER) return

        val brightness = maxOf(red, green, blue)
        red = brightness
        green = brightness
        blue = brightness
    }

    companion object {
        @Shadow
        @JvmStatic
        var red: Float = 0.0f

        @Shadow
        @JvmStatic
        var green: Float = 0.0f

        @Shadow
        @JvmStatic
        var blue: Float = 0.0f
    }
}