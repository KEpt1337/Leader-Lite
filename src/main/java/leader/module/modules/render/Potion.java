package leader.module.modules.render;

import leader.Leader;
import leader.event.EventTarget;
import leader.events.Render2DEvent;
import leader.module.Module;
import leader.property.properties.BooleanProperty;
import leader.property.properties.FloatProperty;
import leader.property.properties.IntProperty;
import leader.property.properties.ModeProperty;
import leader.util.RenderUtil;
import leader.util.shader.ShaderElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Potion extends Module {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private final Map<Integer, Integer> potionMaxDurations = new HashMap<>();
    private List<PotionEffect> currentEffects = new ArrayList<>();

    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"RIGHT", "LEFT"});
    public final IntProperty offsetX = new IntProperty("offset-x", 2, 0, 255);
    public final IntProperty offsetY = new IntProperty("offset-y", 2, 0, 255);
    public final FloatProperty scale = new FloatProperty("scale", 1.0F, 0.5F, 1.5F);
    public final FloatProperty fontScale = new FloatProperty("font-scale", 1.0F, 0.7F, 1.5F);
    public final BooleanProperty blur = new BooleanProperty("blur", false);
    public final IntProperty blurIterations = new IntProperty("blur-iterations", 2, 1, 8, blur::getValue);
    public final IntProperty blurOffset = new IntProperty("blur-offset", 3, 1, 10, blur::getValue);
    private Framebuffer stencilBlur;

    public Potion() {
        super("Potion", false);
    }

    private String getPotionName(PotionEffect effect) {
        net.minecraft.potion.Potion potion = net.minecraft.potion.Potion.potionTypes[effect.getPotionID()];
        return I18n.format(potion.getName()) + " " + intToRoman(effect.getAmplifier() + 1);
    }

    private static String intToRoman(int num) {
        int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] symbols = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            while (values[i] <= num) {
                num -= values[i];
                sb.append(symbols[i]);
            }
        }
        return sb.toString();
    }

    private void updateMaxDurations() {
        List<Integer> toRemove = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : potionMaxDurations.entrySet()) {
            if (mc.thePlayer.getActivePotionEffect(net.minecraft.potion.Potion.potionTypes[entry.getKey()]) == null) {
                toRemove.add(entry.getKey());
            }
        }
        for (int id : toRemove) potionMaxDurations.remove(id);
        for (PotionEffect effect : currentEffects) {
            int id = effect.getPotionID();
            if (!potionMaxDurations.containsKey(id) || potionMaxDurations.get(id) < effect.getDuration()) {
                potionMaxDurations.put(id, effect.getDuration());
            }
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (!this.isEnabled() || mc.thePlayer.getActivePotionEffects().isEmpty()) return;

        currentEffects = mc.thePlayer.getActivePotionEffects().stream()
                .sorted(Comparator.comparingInt(e -> -(
                        e.getDuration() + (potionMaxDurations.getOrDefault(e.getPotionID(), 0) / 2)
                )))
                .collect(Collectors.toList());
        updateMaxDurations();

        ScaledResolution sr = new ScaledResolution(mc);
        float screenWidth = sr.getScaledWidth();
        float screenHeight = sr.getScaledHeight();
        boolean isRight = this.mode.getValue() == 0;

        float cardWidth = 130.0F;
        float cardHeight = 28.0F;
        float gap = 2.0F;
        float textScale = this.fontScale.getValue();
        float textHeight = FontManager.getFontHeight() * textScale;
        float textY1 = 3.0F;
        float textY2 = textY1 + textHeight + 1.0F;
        float iconSize = cardHeight - 4.0F;
        float iconOffset = iconSize + 4.0F;

        float offX = this.offsetX.getValue() + 4.0F;
        float offY = this.offsetY.getValue() + 4.0F;
        float invScale = 1.0F / this.scale.getValue();
        boolean doBlur = this.blur.getValue();

        float baseX = isRight ? (screenWidth - cardWidth - offX) * invScale : offX * invScale;
        float baseY = offY * invScale;
        float step = (cardHeight + gap) * invScale;

        GlStateManager.pushMatrix();
        GlStateManager.scale(this.scale.getValue(), this.scale.getValue(), 0.0F);

        int index = 0;
        for (PotionEffect effect : currentEffects) {
            net.minecraft.potion.Potion potion = net.minecraft.potion.Potion.potionTypes[effect.getPotionID()];
            int id = effect.getPotionID();
            int maxDur = potionMaxDurations.getOrDefault(id, Math.max(effect.getDuration(), 1));
            float ratio = Math.min((float) effect.getDuration() / (float) maxDur, 1.0F);
            int potionColor = potion.getLiquidColor();
            Color themeColor = new Color(potionColor);
            String name = getPotionName(effect);
            String durationStr = net.minecraft.potion.Potion.getDurationString(effect);

            float x = baseX;
            float y = baseY + index * step;

            if (doBlur) {
                final float bx = x;
                final float by = y;
                ShaderElement.addBlurTask(() -> {
                    RenderUtil.enableRenderState();
                    RenderUtil.drawRect(bx, by, bx + cardWidth, by + cardHeight, -1);
                    RenderUtil.disableRenderState();
                });
            }

            RenderUtil.enableRenderState();
            RenderUtil.drawRect(x, y, x + cardWidth, y + cardHeight, new Color(0, 0, 0, 0.45F).getRGB());

            float fillWidth = cardWidth * ratio;
            int fillColor = new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 60).getRGB();
            RenderUtil.drawRect(x, y, x + fillWidth, y + cardHeight, fillColor);

            int borderColor = new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 50).getRGB();
            RenderUtil.drawLine(x, y, x + cardWidth, y, 1.0F, borderColor);
            RenderUtil.drawLine(x + cardWidth, y, x + cardWidth, y + cardHeight, 1.0F, borderColor);
            RenderUtil.drawLine(x + cardWidth, y + cardHeight, x, y + cardHeight, 1.0F, borderColor);
            RenderUtil.drawLine(x, y + cardHeight, x, y, 1.0F, borderColor);
            RenderUtil.disableRenderState();

            if (potion.hasStatusIcon()) {
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                mc.getTextureManager().bindTexture(new ResourceLocation("textures/gui/container/inventory.png"));
                int iconIndex = potion.getStatusIconIndex();
                float u = iconIndex % 8 * 18;
                float v = 198 + iconIndex / 8 * 18;
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                Gui.drawScaledCustomSizeModalRect((int) (x + 2.0F), (int) (y + 2.0F), u, v, 18, 18, (int) iconSize, (int) iconSize, 256.0F, 256.0F);
                GlStateManager.disableBlend();
            }

            GlStateManager.disableDepth();
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            GlStateManager.pushMatrix();
            GlStateManager.translate(x + iconOffset, y + textY1, 0.0F);
            GlStateManager.scale(textScale, textScale, 1.0F);
            FontManager.drawString(name, 0.0F, 0.0F, -1, false);
            GlStateManager.popMatrix();

            GlStateManager.pushMatrix();
            GlStateManager.translate(x + iconOffset, y + textY2, 0.0F);
            GlStateManager.scale(textScale, textScale, 1.0F);
            FontManager.drawString(durationStr, 0.0F, 0.0F, themeColor.getRGB(), false);
            GlStateManager.popMatrix();

            GlStateManager.enableDepth();
            GlStateManager.disableBlend();

            index++;
        }

        GlStateManager.popMatrix();
    }

    public void drawBlur() {
        if (!this.blur.getValue()) return;
        HUD hud = (HUD) Leader.moduleManager.modules.get(HUD.class);
        if (hud != null && hud.blur.getValue()) return;
        Notification notification = (Notification) Leader.moduleManager.modules.get(Notification.class);
        if (notification != null && notification.blur.getValue()) return;
        if (stencilBlur == null) {
            stencilBlur = ShaderElement.createFrameBuffer(null);
        }
        stencilBlur.framebufferClear();
        stencilBlur.bindFramebuffer(false);
        for (Runnable runnable : ShaderElement.getTasks()) {
            runnable.run();
        }
        ShaderElement.getTasks().clear();
        stencilBlur.unbindFramebuffer();
        leader.util.shader.KawaseBlur.renderBlur(stencilBlur.framebufferTexture, blurIterations.getValue(), blurOffset.getValue());
    }
}
