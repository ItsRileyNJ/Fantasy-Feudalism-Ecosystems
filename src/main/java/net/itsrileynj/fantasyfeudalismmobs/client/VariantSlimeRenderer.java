package net.itsrileynj.fantasyfeudalismmobs.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import net.itsrileynj.fantasyfeudalismmobs.SlimeVariants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Slime;

public final class VariantSlimeRenderer extends SlimeRenderer {
    private static final ResourceLocation VANILLA = ResourceLocation.withDefaultNamespace("textures/entity/slime/slime.png");
    private static final ResourceLocation FLAME = SlimeVariants.id("textures/entity/slime/flame_slime.png");
    private static final ResourceLocation SNOW = SlimeVariants.id("textures/entity/slime/snow_slime.png");
    private final ResourceLocation texture;

    public VariantSlimeRenderer(EntityRendererProvider.Context context, boolean flame) {
        super(context);
        texture = flame ? FLAME : SNOW;
    }

    @Override
    public ResourceLocation getTextureLocation(Slime slime) {
        return texture;
    }

    static void registerTextures() {
        var textures = Minecraft.getInstance().getTextureManager();
        textures.register(FLAME, new SlimePalette(0x632004, 0xF69024, 0xFFEA50));
        textures.register(SNOW, new SlimePalette(0x0A245F, 0x73C3ED, 0x9BEAFF));
    }

    /** Reuse the active slime texture's light and dark pixels and its translucent outer layer. */
    private static final class SlimePalette extends SimpleTexture {
        private static final int SHADOW = 10;
        private static final int BASE = 194;
        private static final int HIGHLIGHT = 206;
        private final int dark;
        private final int base;
        private final int light;

        SlimePalette(int dark, int base, int light) {
            super(VANILLA);
            this.dark = dark;
            this.base = base;
            this.light = light;
        }

        @Override
        protected TextureImage getTextureImage(ResourceManager resources) {
            var texture = super.getTextureImage(resources);
            try {
                recolor(texture.getImage());
            } catch (IOException ignored) {
                // SimpleTexture reports its original resource error when it loads the image.
            }
            return texture;
        }

        private void recolor(NativeImage image) {
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int pixel = image.getPixelRGBA(x, y);
                    int alpha = FastColor.ABGR32.alpha(pixel);
                    if (alpha == 0) continue;
                    int green = FastColor.ABGR32.green(pixel);
                    int recolored = green <= BASE
                            ? interpolate(alpha, green, SHADOW, BASE, dark, base)
                            : interpolate(alpha, green, BASE, HIGHLIGHT, base, light);
                    image.setPixelRGBA(x, y, recolored);
                }
            }
        }

        private static int interpolate(int alpha, int green, int low, int high, int from, int to) {
            float shade = Mth.clamp((green - low) / (float)(high - low), 0F, 1F);
            int red = Mth.lerpInt(shade, FastColor.ARGB32.red(from), FastColor.ARGB32.red(to));
            int mid = Mth.lerpInt(shade, FastColor.ARGB32.green(from), FastColor.ARGB32.green(to));
            int blue = Mth.lerpInt(shade, FastColor.ARGB32.blue(from), FastColor.ARGB32.blue(to));
            return FastColor.ABGR32.color(alpha, blue, mid, red);
        }
    }
}
