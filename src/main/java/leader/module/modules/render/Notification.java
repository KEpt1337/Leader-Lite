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
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Notification extends Module {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final List<NotificationEntry> entries = new ArrayList<>();

    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"RIGHT", "LEFT"});
    public final IntProperty duration = new IntProperty("duration", 1500, 500, 5000);
    public final IntProperty maxAlerts = new IntProperty("max-alerts", 5, 1, 10);
    public final FloatProperty scale = new FloatProperty("scale", 1.0F, 0.5F, 1.5F);
    public final FloatProperty fontScale = new FloatProperty("font-scale", 1.0F, 0.7F, 1.5F);
    public final IntProperty offsetX = new IntProperty("offset-x", 2, 0, 255);
    public final IntProperty offsetY = new IntProperty("offset-y", 20, 0, 255);
    public final BooleanProperty blur = new BooleanProperty("blur", false);
    public final IntProperty blurIterations = new IntProperty("blur-iterations", 2, 1, 8, blur::getValue);
    public final IntProperty blurOffset = new IntProperty("blur-offset", 3, 1, 10, blur::getValue);
    private Framebuffer stencilBlur;

    public Notification() {
        super("Notification", false);
    }

    public static void addNotification(String moduleName, boolean enabled) {
        entries.add(new NotificationEntry(moduleName, enabled, System.currentTimeMillis()));
        Notification notification = (Notification) Leader.moduleManager.modules.get(Notification.class);
        if (notification != null) {
            int max = notification.maxAlerts.getValue();
            while (entries.size() > max) {
                entries.remove(0);
            }
        }
    }

    private float getAlpha(long now, long start, long dur) {
        float elapsed = now - start;
        float fadeIn = Math.min(dur * 0.15F, 200.0F);
        float fadeOut = Math.min(dur * 0.20F, 300.0F);
        if (elapsed < fadeIn) {
            return elapsed / fadeIn;
        }
        if (elapsed > dur - fadeOut) {
            return (dur - elapsed) / fadeOut;
        }
        return 1.0F;
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (!this.isEnabled()) return;
        ScaledResolution sr = new ScaledResolution(mc);
        float screenWidth = sr.getScaledWidth();
        float screenHeight = sr.getScaledHeight();
        long now = System.currentTimeMillis();
        long dur = this.duration.getValue();
        entries.removeIf(entry -> now - entry.startTime > dur);
        if (entries.isEmpty()) return;

        float cardWidth = 100.0F;
        float cardHeight = 20.0F;
        float gap = 3.0F;
        float textScale = this.fontScale.getValue();
        float textHeight = FontManager.getFontHeight() * textScale;
        float textY = (cardHeight - textHeight) / 2.0F;

        float offX = this.offsetX.getValue() + 4.0F;
        float offY = this.offsetY.getValue() + 4.0F;
        boolean isRight = this.mode.getValue() == 0;
        boolean doBlur = this.blur.getValue();
        float invScale = 1.0F / this.scale.getValue();
        int max = Math.min(entries.size(), this.maxAlerts.getValue());
        float step = cardHeight + gap;

        float baseX = isRight ? screenWidth - cardWidth - offX : offX;
        float baseY = screenHeight - offY - cardHeight;

        GlStateManager.pushMatrix();
        GlStateManager.scale(this.scale.getValue(), this.scale.getValue(), 0.0F);

        for (int i = 0; i < max; i++) {
            NotificationEntry entry = entries.get(i);
            float progress = Math.min((float) (now - entry.startTime) / (float) dur, 1.0F);
            float alpha = getAlpha(now, entry.startTime, dur);
            int idx = max - 1 - i;
            float y = (baseY - idx * step) * invScale;
            float x = baseX * invScale;
            Color themeColor = entry.enabled ? new Color(0x00FF00) : new Color(0xFF4444);

            if (doBlur) {
                final float bx = x;
                final float by = y;
                ShaderElement.addBlurTask(() -> {
                    RenderUtil.enableRenderState();
                    RenderUtil.drawRect(bx, by, bx + cardWidth, by + cardHeight, -1);
                    RenderUtil.disableRenderState();
                });
            }

            float bgAlpha = 0.4F * alpha;
            float fillWidth = cardWidth * progress;
            float fillAlpha = Math.min(0.3F * alpha, 1.0F);
            float borderAlpha = 0.25F * alpha;

            RenderUtil.enableRenderState();
            RenderUtil.drawRect(x, y, x + cardWidth, y + cardHeight, new Color(0.0F, 0.0F, 0.0F, bgAlpha).getRGB());

            int fillColor = new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), (int) (fillAlpha * 255.0F)).getRGB();
            RenderUtil.drawRect(x, y, x + fillWidth, y + cardHeight, fillColor);

            int borderColor = new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), (int) (borderAlpha * 255.0F)).getRGB();
            RenderUtil.drawLine(x, y, x + cardWidth, y, 1.0F, borderColor);
            RenderUtil.drawLine(x, y + cardHeight, x + cardWidth, y + cardHeight, 1.0F, borderColor);
            RenderUtil.drawLine(x, y, x, y + cardHeight, 1.0F, borderColor);
            RenderUtil.drawLine(x + cardWidth, y, x + cardWidth, y + cardHeight, 1.0F, borderColor);
            RenderUtil.disableRenderState();

            GlStateManager.disableDepth();
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            int nameColor = new Color(1.0F, 1.0F, 1.0F, alpha).getRGB();
            int iconColor = new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), (int) (alpha * 255.0F)).getRGB();

            GlStateManager.pushMatrix();
            GlStateManager.translate(x + 4.0F, y + textY, 0.0F);
            GlStateManager.scale(textScale, textScale, 1.0F);
            FontManager.drawString(entry.moduleName, 0.0F, 0.0F, nameColor, false);
            GlStateManager.popMatrix();

            float iconSize = 8.0F;
            float iconX = x + cardWidth - 4.0F - iconSize;
            float iconY = y + (cardHeight - iconSize) / 2.0F;
            GlStateManager.pushMatrix();
            GlStateManager.translate(iconX, iconY, 0.0F);
            GlStateManager.disableTexture2D();
            GL11.glLineWidth(2.0F);
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            GL11.glBegin(GL11.GL_LINES);
            if (entry.enabled) {
                GL11.glColor4f(themeColor.getRed() / 255f, themeColor.getGreen() / 255f, themeColor.getBlue() / 255f, alpha);
                GL11.glVertex2f(1.0F, iconSize * 0.55F);
                GL11.glVertex2f(iconSize * 0.45F, iconSize - 1.0F);
                GL11.glVertex2f(iconSize * 0.45F, iconSize - 1.0F);
                GL11.glVertex2f(iconSize - 1.0F, 1.0F);
            } else {
                GL11.glColor4f(themeColor.getRed() / 255f, themeColor.getGreen() / 255f, themeColor.getBlue() / 255f, alpha);
                GL11.glVertex2f(1.0F, 1.0F);
                GL11.glVertex2f(iconSize - 1.0F, iconSize - 1.0F);
                GL11.glVertex2f(iconSize - 1.0F, 1.0F);
                GL11.glVertex2f(1.0F, iconSize - 1.0F);
            }
            GL11.glEnd();
            GL11.glDisable(GL11.GL_LINE_SMOOTH);
            GL11.glLineWidth(2.0F);
            GlStateManager.enableTexture2D();
            GlStateManager.popMatrix();

            GlStateManager.enableDepth();
            GlStateManager.disableBlend();
        }

        GlStateManager.popMatrix();
    }

    public void drawBlur() {
        HUD hud = (HUD) Leader.moduleManager.modules.get(HUD.class);
        if (hud != null && hud.blur.getValue()) return;
        if (!this.blur.getValue()) return;
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

    private static class NotificationEntry {
        final String moduleName;
        final boolean enabled;
        final long startTime;

        NotificationEntry(String moduleName, boolean enabled, long startTime) {
            this.moduleName = moduleName;
            this.enabled = enabled;
            this.startTime = startTime;
        }
    }
}
