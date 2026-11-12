package com.axial.cosmetics.client;

enum FeedbackCategory {
    PRISONS("prisons","PRISONS"),CLIENT("client","CLIENT");
    final String id,label;
    FeedbackCategory(String id,String label){this.id=id;this.label=label;}
}
