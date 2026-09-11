package com.axial.cosmetics.client;

import com.axial.cosmetics.AxialCosmetics;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.HuskEntityRenderer;
import net.minecraft.client.render.entity.state.ZombieEntityRenderState;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.Identifier;

public final class AlienHuskEntityRenderer extends HuskEntityRenderer {
    private static final String ALIEN_TAG = "prisonscore_alien";
    private static final Identifier ALIEN_TEXTURE = AxialCosmetics.id("textures/entity/zombie/prisonscore_alien.png");

    public AlienHuskEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public ZombieEntityRenderState createRenderState() {
        return new AlienHuskRenderState();
    }

    @Override
    public void updateRenderState(ZombieEntity entity, ZombieEntityRenderState state, float tickProgress) {
        super.updateRenderState(entity, state, tickProgress);
        Team team = entity.getScoreboardTeam();
        // Vanilla does not sync command tags; servers can mirror the tag to this team.
        ((AlienHuskRenderState) state).alien = entity.getCommandTags().contains(ALIEN_TAG)
                || (team != null && ALIEN_TAG.equals(team.getName()));
    }

    @Override
    public Identifier getTexture(ZombieEntityRenderState state) {
        return ((AlienHuskRenderState) state).alien ? ALIEN_TEXTURE : super.getTexture(state);
    }

    private static final class AlienHuskRenderState extends ZombieEntityRenderState {
        private boolean alien;
    }
}
