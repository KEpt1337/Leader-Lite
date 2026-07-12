package leader.mixin;

import leader.Leader;
import leader.module.modules.misc.AntiObfuscate;
import leader.module.modules.render.BetterFPS;
import leader.module.modules.render.NickHider;
import leader.util.FontRendererHook;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@SideOnly(Side.CLIENT)
@Mixin(value = {FontRenderer.class}, priority = 9999)
public abstract class MixinFontRenderer {
    @Shadow
    public abstract int getCharWidth(char character);

    @Unique
    private final FontRendererHook patcher$fontRendererHook = new FontRendererHook((FontRenderer) (Object) this);

    @Inject(method = "getStringWidth", at = @At("HEAD"), cancellable = true)
    public void getStringWidth(String text, CallbackInfoReturnable<Integer> cir) {
        if (BetterFPS.betterFont.getValue() && BetterFPS.using) {
            cir.setReturnValue(this.patcher$fontRendererHook.getStringWidth(text));
        } else {
            int i = 0;
            boolean flag = false;

            for (int j = 0; j < text.length(); ++j) {
                char c0 = text.charAt(j);
                int k = this.getCharWidth(c0);
                if (k < 0 && j < text.length() - 1) {
                    ++j;
                    c0 = text.charAt(j);
                    if (c0 != 'l' && c0 != 'L') {
                        if (c0 == 'r' || c0 == 'R') {
                            flag = false;
                        }
                    } else {
                        flag = true;
                    }

                    k = 0;
                }

                i += k;
                if (flag && k > 0) {
                    ++i;
                }
            }
            cir.setReturnValue(i);
        }
    }

    @Inject(method = "renderStringAtPos", at = @At("HEAD"), cancellable = true)
    private void patcher$useOptimizedRendering(String text, boolean shadow, CallbackInfo ci) {
        if (BetterFPS.betterFont.getValue() && BetterFPS.using) {
            if (this.patcher$fontRendererHook.renderStringAtPos(text, shadow)) {
                ci.cancel();
            }
        }
    }

    @ModifyVariable(
            method = {"renderString"},
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true
    )
    private String renderString(String string) {
        if (Leader.moduleManager == null) {
            return string;
        } else {
            AntiObfuscate antiObfuscate = (AntiObfuscate) Leader.moduleManager.modules.get(AntiObfuscate.class);
            if (antiObfuscate.isEnabled()) {
                string = antiObfuscate.stripObfuscated(string);
            }
            NickHider nickHider = (NickHider) Leader.moduleManager.modules.get(NickHider.class);
            return nickHider.isEnabled() ? nickHider.replaceNick(string) : string;
        }
    }
    @ModifyVariable(
            method = {"getStringWidth"},
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true
    )
    private String getStringWidth(String string) {
        if (Leader.moduleManager == null) {
            return string;
        } else {
            AntiObfuscate antiObfuscate = (AntiObfuscate) Leader.moduleManager.modules.get(AntiObfuscate.class);
            if (antiObfuscate.isEnabled()) {
                string = antiObfuscate.stripObfuscated(string);
            }
            NickHider nickHider = (NickHider) Leader.moduleManager.modules.get(NickHider.class);
            return nickHider.isEnabled() ? nickHider.replaceNick(string) : string;
        }
    }
    @Inject(method = "<init>*", at = @At("RETURN"))
    private void afterConstruct(GameSettings settings, ResourceLocation asciiTexture,
                                TextureManager manager, boolean unicode,
                                CallbackInfo ci) {
        if (!unicode) {
            try {
                Field pagesField = ReflectionHelper.findField(FontRenderer.class, "field_78263_ae", "unicodePages");
                ResourceLocation[] pages = (ResourceLocation[]) pagesField.get(this);
                if (pages != null) Arrays.fill(pages, null);
                Field glyphField = ReflectionHelper.findField(FontRenderer.class, "field_78279_b", "glyphWidth");
                byte[] glyphs = (byte[]) glyphField.get(this);
                if (glyphs != null) Arrays.fill(glyphs, (byte) 0);
            } catch (Exception ignored) {}
        }
    }
}
