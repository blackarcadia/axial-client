package com.axial.cosmetics.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/** Brief Island XP gain notification, fed by AxialSky's private system marker. */
public final class IslandXpPopup {
    private static final IslandXpAnimation ANIMATION=new IslandXpAnimation();
    private IslandXpPopup() { }
    private static long now() {return System.nanoTime()/1_000_000;}

    public static void show(String gained) {ANIMATION.show(parseAmount(gained),now());}
    public static void clear() {ANIMATION.clear();}

    public static void render(DrawContext context) {
        MinecraftClient client=MinecraftClient.getInstance();
        if(client.world==null) {ANIMATION.clear();return;}
        if(client.currentScreen instanceof HudEditorAccess editor&&editor.axial$isEditingHud())return;
        var frame=ANIMATION.frame(now());
        if(!frame.active()||frame.alpha()<.004f)return;
        Text text=Text.literal("+"+formatAmount(frame.amount())+" XP");
        var bounds=bounds(client);
        float center=bounds.x()+bounds.width()/2f;
        float y=bounds.y()+6+frame.lift();
        // Fractional translation avoids the old one-pixel stepping during the rise.
        float halfWidth=client.textRenderer.getWidth(text)*frame.scale()/2f;
        center=Math.clamp(center,halfWidth,Math.max(halfWidth,client.getWindow().getScaledWidth()-halfWidth));
        y=Math.clamp(y,0f,Math.max(0,client.getWindow().getScaledHeight()-client.textRenderer.fontHeight*frame.scale()));
        int alpha=Math.clamp(Math.round(frame.alpha()*255),1,255);
        // Minecraft treats near-zero text alpha specially; skip rather than flash opaque.
        if(alpha<4)return;
        context.getMatrices().pushMatrix();
        try {
            context.getMatrices().translate(center,y);
            context.getMatrices().scale(frame.scale(),frame.scale());
            context.getMatrices().translate(-client.textRenderer.getWidth(text)/2f,0);
            context.drawTextWithShadow(client.textRenderer,text,0,0,
                    alpha<<24|colorFor(frame.amount()));
        } finally {context.getMatrices().popMatrix();}
    }

    public static HudSnap.Box bounds(MinecraftClient client) {
        String amount=formatAmount(ANIMATION.frame(now()).amount());
        int width=Math.min(client.getWindow().getScaledWidth(),Math.max(84,client.textRenderer.getWidth(Text.literal("+"+amount+" XP"))+8));
        int height=client.textRenderer.fontHeight+13;
        return new HudSnap.Box(IslandXpHudConfig.x(client.getWindow().getScaledWidth(),width),
                IslandXpHudConfig.y(client.getWindow().getScaledHeight(),height),width,height);
    }
    public static void preview(DrawContext context,MinecraftClient client) {
        var bounds=bounds(client);Text text=Text.literal("+1,250 XP");
        context.drawTextWithShadow(client.textRenderer,text,bounds.x()+(bounds.width()-client.textRenderer.getWidth(text))/2,bounds.y()+6,0xFFFFAD5C);
    }
    private static int colorFor(double value) {
        if(value<100)return 0x00D5D9E2;
        if(value<500)return 0x008DD7FF;
        if(value<800)return 0x00FFE36C;
        if(value<=1500)return 0x00FFAD5C;
        return 0x00FF9AA5;
    }
    static String formatAmount(double value) {
        if(value<1_000_000)return String.format(java.util.Locale.ROOT,"%,d",Math.round(value));
        if(value<1_000_000_000)return String.format(java.util.Locale.ROOT,"%.2fM",value/1_000_000);
        return String.format(java.util.Locale.ROOT,"%.2fB",value/1_000_000_000);
    }
    static double parseAmount(String amount) {
        if(amount==null)return 0;
        String normalized=amount.replace(",","").trim().toUpperCase(java.util.Locale.ROOT);
        try {
            double multiplier=1;
            if(normalized.endsWith("K")){multiplier=1_000;normalized=normalized.substring(0,normalized.length()-1);}
            else if(normalized.endsWith("M")){multiplier=1_000_000;normalized=normalized.substring(0,normalized.length()-1);}
            double value=Double.parseDouble(normalized)*multiplier;
            return Double.isFinite(value)&&value>0?value:0;
        } catch(NumberFormatException ignored) {return 0;}
    }
}
