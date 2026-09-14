package com.axial.cosmetics.client;

import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.ProfileResult;
import net.minecraft.client.session.Session;

public interface AccountSessionAccess {
    void axial$setAccount(Session session, UserApiService service,
                          UserApiService.UserProperties properties, ProfileResult profile);
}
