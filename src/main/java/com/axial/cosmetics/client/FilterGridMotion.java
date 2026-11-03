package com.axial.cosmetics.client;

import com.axial.cosmetics.client.ModMenuLayout.Rect;
import java.util.*;
import java.util.function.IntFunction;
import java.util.function.LongSupplier;

/** Keyed cards retain their position and visibility while search results change. */
final class FilterGridMotion<K> {
    record Tile<K>(K key,Rect bounds,float opacity,boolean present) {}
    private final class Card {
        final K key;
        final UiMotion x,y,alpha;
        Rect target;
        boolean present;
        Card(K key,Rect target,boolean animate){
            this.key=key;this.target=target;
            x=new UiMotion(target.x(),24,clock);y=new UiMotion(target.y()+(animate?5:0),24,clock);
            alpha=new UiMotion(animate?0:1,24,clock);
        }
    }
    private final Map<K,Card> cards=new LinkedHashMap<>();
    private final LongSupplier clock;
    private boolean initialized;
    FilterGridMotion(){this(System::nanoTime);}
    FilterGridMotion(LongSupplier clock){this.clock=clock;}
    void update(List<K> keys,IntFunction<Rect> bounds){
        cards.values().forEach(card->card.present=false);
        for(int i=0;i<keys.size();i++){
            K key=keys.get(i);Rect target=bounds.apply(i);
            Card card=cards.get(key);
            if(card==null){card=new Card(key,target,initialized);cards.put(key,card);}
            card.target=target;card.present=true;
        }
        initialized=true;
    }
    void offsetY(int amount){
        if(amount==0)return;
        for(var card:cards.values()){
            card.y.shift(amount);
            var r=card.target;card.target=new Rect(r.x(),r.y()+amount,r.width(),r.height());
        }
    }
    List<Tile<K>> frame(){
        var result=new ArrayList<Tile<K>>();
        var iterator=cards.values().iterator();
        while(iterator.hasNext()){
            var card=iterator.next();float alpha=(float)card.alpha.toward(card.present?1:0);
            int x=(int)Math.round(card.x.toward(card.target.x()));
            int y=(int)Math.round(card.y.toward(card.target.y()));
            if(!card.present&&alpha<.01f){iterator.remove();continue;}
            result.add(new Tile<>(card.key,new Rect(x,y,card.target.width(),card.target.height()),alpha,card.present));
        }
        // Departing cards draw behind incoming results and never receive input.
        result.sort(Comparator.comparing(Tile::present));
        return result;
    }
}
