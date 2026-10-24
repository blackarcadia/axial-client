package com.axial.cosmetics.client;

import java.util.ArrayDeque;

final class CrosshairHistory {
    private final ArrayDeque<CrosshairConfig> undo=new ArrayDeque<>();
    void checkpoint(CrosshairConfig config){if(undo.size()==32)undo.removeFirst();undo.addLast(config.copy());}
    boolean available(){return !undo.isEmpty();}
    void undo(CrosshairConfig config){if(!undo.isEmpty())config.copyFrom(undo.removeLast());}
}
