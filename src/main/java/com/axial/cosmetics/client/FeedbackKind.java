package com.axial.cosmetics.client;

enum FeedbackKind {
    BUG("bug","BUG"),SUGGESTION("suggestion","SUGGESTION");
    final String id,label;
    FeedbackKind(String id,String label){this.id=id;this.label=label;}
}
