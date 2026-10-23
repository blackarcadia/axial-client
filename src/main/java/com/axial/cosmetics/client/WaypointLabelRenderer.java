package com.axial.cosmetics.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;

public final class WaypointLabelRenderer {
    private WaypointLabelRenderer() { }

    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || client.options.hudHidden || client.currentScreen != null || client.gameRenderer.getCamera() == null || !WaypointConfig.enabled()) return;

        Camera camera = client.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getCameraPos();
        Quaternionf rotation = new Quaternionf(camera.getRotation()).conjugate();
        Matrix4f projection = client.gameRenderer.getBasicProjectionMatrix(client.options.getFov().getValue());
        String dimension = client.world.getRegistryKey().getValue().toString();
        int framebufferWidth = client.getWindow().getFramebufferWidth();
        int framebufferHeight = client.getWindow().getFramebufferHeight();
        float xScale = context.getScaledWindowWidth() / (float) framebufferWidth;
        float yScale = context.getScaledWindowHeight() / (float) framebufferHeight;

        java.util.List<com.axial.cosmetics.client.ModMenuLayout.Rect> labels=new java.util.ArrayList<>();
        var waypoints=WaypointConfig.visibleFor(client).stream().sorted(java.util.Comparator.comparingDouble(e->Math.hypot(e.x()-client.player.getX(),e.z()-client.player.getZ()))).toList();
        for (WaypointConfig.Entry waypoint : waypoints) {
            if(labels.size()>=24)break;
            if (!dimension.equals(waypoint.dimension())) continue;
            Vector4f projected = new Vector4f(
                    (float) (waypoint.x() + 0.5 - cameraPos.x),
                    // Use the waypoint's real position, rather than the player's
                    // current height, so the tag stays attached to the beam.
                    (float) (waypoint.y() + 1.0 - cameraPos.y),
                    (float) (waypoint.z() + 0.5 - cameraPos.z),
                    1.0f
            ).rotate(rotation).mul(projection);
            if (projected.w <= 0.0f || Math.abs(projected.x)>projected.w || Math.abs(projected.y)>projected.w) continue;
            int screenX = Math.round((projected.x / projected.w * 0.5f + 0.5f) * framebufferWidth * xScale);
            int screenY = Math.round((-projected.y / projected.w * 0.5f + 0.5f) * framebufferHeight * yScale);
            double distance = Math.hypot(waypoint.x() + 0.5 - client.player.getX(), waypoint.z() + 0.5 - client.player.getZ());
            String distanceText=formatDistance(distance);
            String name=client.textRenderer.trimToWidth(NavigationScreen.copy(waypoint.name(),.85f,true),130).getString();
            int nameWidth=client.textRenderer.getWidth(NavigationScreen.copy(name,.85f,true));
            int distanceWidth=client.textRenderer.getWidth(NavigationScreen.copy(distanceText,.75f,false));
            int labelWidth=nameWidth+distanceWidth+26;
            int labelX=screenX-labelWidth/2,labelY=screenY-18;
            var bounds=new ModMenuLayout.Rect(labelX,labelY,labelWidth,18);
            if(labelX<2||bounds.right()>context.getScaledWindowWidth()-2||labelY<2||bounds.bottom()>context.getScaledWindowHeight()-2)continue;
            if(labels.stream().anyMatch(r->r.x()<bounds.right()+2&&r.right()+2>bounds.x()&&r.y()<bounds.bottom()+2&&r.bottom()+2>bounds.y()))continue;
            labels.add(bounds);
            ModMenuUi.frame(context,bounds,ModMenuUi.LINE,0xDC14171B);
            ModMenuUi.circle(context,labelX+5,labelY+6,6,waypoint.color());
            NavigationScreen.line(context,client.textRenderer,name,labelX+15,labelY+5,ModMenuUi.INK,.85f,nameWidth,true);
            NavigationScreen.line(context,client.textRenderer,distanceText,labelX+nameWidth+21,labelY+6,ModMenuUi.MUTED,.75f,distanceWidth,false);

        }
    }

    private static String formatDistance(double distance) {
        if (distance >= 1_000.0) {
            return String.format(java.util.Locale.ROOT, "%.1fkm", distance / 1_000.0);
        }
        return String.format(java.util.Locale.ROOT, "%dm", Math.round(distance));
    }
}
