package com.axial.cosmetics.client;

import java.util.Locale;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

/** Display casing for client menus only; stored names, identifiers and input values are untouched. */
public final class ClientMenuText {
    private ClientMenuText(){}
    public static String uppercase(String value){return value.toUpperCase(Locale.ROOT);}
    public static MutableText literal(String value){return Text.literal(uppercase(value));}
}
