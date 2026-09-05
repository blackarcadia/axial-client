package com.axial.cosmetics.client;

import net.fabricmc.fabric.api.client.model.loading.v1.BlockStateResolver;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DaylightDetectorBlock;
import net.minecraft.block.NoteBlock;
import net.minecraft.block.enums.NoteBlockInstrument;
import net.minecraft.client.render.model.SimpleBlockStateModel;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.render.model.json.ModelVariant;
import net.minecraft.util.Identifier;

public final class WeatherDetectorModelRegistry {
    private static final Identifier NOTE_BLOCK_MODEL_ID = Identifier.of("minecraft", "block/note_block");
    private static final Identifier DAYLIGHT_DETECTOR_MODEL_ID = Identifier.of("minecraft", "block/daylight_detector");
    private static final Identifier WEATHER_DETECTOR_MODEL_ID = Identifier.of("minecraft", "block/custom/weatherdetector");
    private static final Identifier CUSTOM_MODEL_DATA_219_MODEL_ID = Identifier.of("minecraft", "block/custom/coin_converter");
    private static final Identifier CUSTOM_MODEL_DATA_220_MODEL_ID = Identifier.of("minecraft", "block/custom/custom_model_data_220");
    private static final Identifier CUSTOM_MODEL_DATA_250_MODEL_ID = Identifier.of("minecraft", "block/custom/custom_model_data_250");
    private static final Identifier CUSTOM_MODEL_DATA_273_MODEL_ID = Identifier.of("axial_cosmetics", "block/custom/custom_model_data_273");
    private static final Identifier CUSTOM_MODEL_DATA_274_MODEL_ID = Identifier.of("axial_cosmetics", "block/custom/custom_model_data_274");
    private static final Identifier CUSTOM_MODEL_DATA_275_MODEL_ID = Identifier.of("axial_cosmetics", "block/custom/custom_model_data_275");
    private static final Identifier CUSTOM_MODEL_DATA_276_MODEL_ID = Identifier.of("axial_cosmetics", "block/custom/custom_model_data_276");
    private static final net.minecraft.block.BlockState WEATHER_DETECTOR_STATE = Blocks.NOTE_BLOCK.getDefaultState()
            .with(NoteBlock.INSTRUMENT, NoteBlockInstrument.BELL)
            .with(NoteBlock.NOTE, 8)
            .with(NoteBlock.POWERED, false);
    private static final net.minecraft.block.BlockState CUSTOM_MODEL_DATA_219_STATE = Blocks.NOTE_BLOCK.getDefaultState()
            .with(NoteBlock.INSTRUMENT, NoteBlockInstrument.BELL)
            .with(NoteBlock.NOTE, 7)
            .with(NoteBlock.POWERED, false);
    private static final net.minecraft.block.BlockState CUSTOM_MODEL_DATA_273_STATE = Blocks.NOTE_BLOCK.getDefaultState()
            .with(NoteBlock.INSTRUMENT, NoteBlockInstrument.BELL)
            .with(NoteBlock.NOTE, 10)
            .with(NoteBlock.POWERED, false);
    private static final net.minecraft.block.BlockState CUSTOM_MODEL_DATA_274_STATE = Blocks.NOTE_BLOCK.getDefaultState()
            .with(NoteBlock.INSTRUMENT, NoteBlockInstrument.BELL)
            .with(NoteBlock.NOTE, 13)
            .with(NoteBlock.POWERED, false);
    private static final net.minecraft.block.BlockState CUSTOM_MODEL_DATA_220_STATE = Blocks.NOTE_BLOCK.getDefaultState()
            .with(NoteBlock.INSTRUMENT, NoteBlockInstrument.BELL)
            .with(NoteBlock.NOTE, 9)
            .with(NoteBlock.POWERED, false);
    private static final net.minecraft.block.BlockState CUSTOM_MODEL_DATA_275_STATE = Blocks.NOTE_BLOCK.getDefaultState()
            .with(NoteBlock.INSTRUMENT, NoteBlockInstrument.BELL)
            .with(NoteBlock.NOTE, 11)
            .with(NoteBlock.POWERED, false);
    private static final net.minecraft.block.BlockState CUSTOM_MODEL_DATA_276_STATE = Blocks.NOTE_BLOCK.getDefaultState()
            .with(NoteBlock.INSTRUMENT, NoteBlockInstrument.BELL)
            .with(NoteBlock.NOTE, 12)
            .with(NoteBlock.POWERED, false);

    private WeatherDetectorModelRegistry() {
    }

    public static void register() {
        ModelLoadingPlugin.register(context ->
                context.registerBlockStateResolver(Blocks.NOTE_BLOCK, WeatherDetectorModelRegistry::resolveNoteBlock));
        ModelLoadingPlugin.register(context ->
                context.registerBlockStateResolver(Blocks.DAYLIGHT_DETECTOR, WeatherDetectorModelRegistry::resolveDaylightDetector));
        ModelLoadingPlugin.register(context ->
                context.registerBlockStateResolver(Blocks.BARRIER, WeatherDetectorModelRegistry::resolveBarrier));
    }

    private static void resolveNoteBlock(BlockStateResolver.Context context) {
        if (context.block() != Blocks.NOTE_BLOCK) {
            return;
        }
        for (BlockState state : Blocks.NOTE_BLOCK.getStateManager().getStates()) {
            context.setModel(state, model(NOTE_BLOCK_MODEL_ID));
        }
        context.setModel(WEATHER_DETECTOR_STATE, new SimpleBlockStateModel.Unbaked(new ModelVariant(WEATHER_DETECTOR_MODEL_ID)).cached());
        context.setModel(CUSTOM_MODEL_DATA_219_STATE, model(CUSTOM_MODEL_DATA_219_MODEL_ID));
        context.setModel(CUSTOM_MODEL_DATA_220_STATE, model(CUSTOM_MODEL_DATA_220_MODEL_ID));
        context.setModel(CUSTOM_MODEL_DATA_273_STATE, model(CUSTOM_MODEL_DATA_273_MODEL_ID));
        context.setModel(CUSTOM_MODEL_DATA_274_STATE, model(CUSTOM_MODEL_DATA_274_MODEL_ID));
        context.setModel(CUSTOM_MODEL_DATA_275_STATE, model(CUSTOM_MODEL_DATA_275_MODEL_ID));
        context.setModel(CUSTOM_MODEL_DATA_276_STATE, model(CUSTOM_MODEL_DATA_276_MODEL_ID));
    }

    private static void resolveDaylightDetector(BlockStateResolver.Context context) {
        if (context.block() != Blocks.DAYLIGHT_DETECTOR) {
            return;
        }
        for (BlockState state : Blocks.DAYLIGHT_DETECTOR.getStateManager().getStates()) {
            context.setModel(state, model(DAYLIGHT_DETECTOR_MODEL_ID));
            if (!state.get(DaylightDetectorBlock.INVERTED)) {
                context.setModel(state, model(CUSTOM_MODEL_DATA_273_MODEL_ID));
            }
        }
    }

    private static void resolveBarrier(BlockStateResolver.Context context) {
        if (context.block() != Blocks.BARRIER) {
            return;
        }
        for (BlockState state : Blocks.BARRIER.getStateManager().getStates()) {
            context.setModel(state, model(CUSTOM_MODEL_DATA_250_MODEL_ID));
        }
    }

    private static BlockStateModel.UnbakedGrouped model(Identifier id) {
        return new SimpleBlockStateModel.Unbaked(new ModelVariant(id)).cached();
    }
}
