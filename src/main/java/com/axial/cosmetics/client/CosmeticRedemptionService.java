package com.axial.cosmetics.client;

import com.google.gson.JsonObject;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Code text is posted over HTTPS to the verified account API; no codes ship in client files. */
final class CosmeticRedemptionService {
    record Result(UUID account,WardrobeItem item,Set<String> owned,long revision) {}
    static CompletableFuture<Result> redeem(String code,UUID requestId) {
        var body=new JsonObject();body.addProperty("code",code);body.addProperty("requestId",requestId.toString());
        return FeedbackService.authenticatedRequest("redeem",body).thenApply(CosmeticRedemptionService::parse);
    }
    static Result parse(JsonObject response) {
        var account=UUID.fromString(response.get("uuid").getAsString());
        var item=WardrobeItem.fromId(response.get("cosmetic").getAsString());
        var owned=new java.util.HashSet<String>();
        for(var id:response.getAsJsonArray("cosmetics"))if(WardrobeItem.fromId(id.getAsString())!=null)owned.add(id.getAsString());
        if(item==null||!owned.contains(response.get("cosmetic").getAsString()))throw new IllegalArgumentException("Invalid unlock response");
        long revision=response.get("revision").getAsLong();
        if(revision<1)throw new IllegalArgumentException("Invalid unlock revision");
        return new Result(account,item,Set.copyOf(owned),revision);
    }
}
