package com.axial.cosmetics.client;

import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.number.BlankNumberFormat;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ScoreboardNumbersTest {
    @Test void serverBlankFormatReturnsEmptyEvenForNonzeroScore() {
        var entry = new ScoreboardEntry("Player", 42, null, null);
        var formatted = entry.formatted(BlankNumberFormat.INSTANCE);
        assertEquals("", formatted.getString());
        assertEquals(42, entry.value());
    }
    @Test void switchingOnRestoresNumbersForBlankObjectiveAndPerEntryFormats() {
        for (var override : new net.minecraft.scoreboard.number.NumberFormat[]{null, BlankNumberFormat.INSTANCE}) {
            var entry = new ScoreboardEntry("Player", 42, null, override);
            var blank = entry.formatted(BlankNumberFormat.INSTANCE);
            assertEquals("", ScoreboardNumbers.resolve(blank, entry.value(), false).getString());
            assertEquals("42", ScoreboardNumbers.resolve(blank, entry.value(), true).getString());
            assertEquals("", ScoreboardNumbers.resolve(blank, entry.value(), false).getString());
            assertEquals("42", ScoreboardNumbers.resolve(blank, entry.value(), true).getString());
        }
    }
    @Test void visibleCustomFormatsArePreservedAndScoresAreNotChanged() {
        var custom = Text.literal("★ 42").formatted(Formatting.GOLD);
        assertSame(custom, ScoreboardNumbers.resolve(custom, 42, true));
        assertEquals("", ScoreboardNumbers.resolve(custom, 42, false).getString());
        assertEquals("★ 42", custom.getString());
        for (int score : new int[]{0, -7, 123456})
            assertEquals(Integer.toString(score), ScoreboardNumbers.resolve(Text.empty(), score, true).getString());
    }
}
