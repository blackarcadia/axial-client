package com.axial.cosmetics.client;

import net.fabricmc.fabric.api.client.model.loading.v1.BlockStateResolver;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.block.Blocks;
import net.minecraft.block.NoteBlock;
import net.minecraft.block.enums.NoteBlockInstrument;
import net.minecraft.client.render.model.SimpleBlockStateModel;
import net.minecraft.client.render.model.json.ModelVariant;
import net.minecraft.util.Identifier;

public final class WeatherDetectorModelRegistry {
    private static final Identifier WEATHER_DETECTOR_MODEL_ID = Identifier.of("minecraft", "block/custom/weatherdetector");
    private static final Identifier CUSTOM_MODEL_DATA_273_MODEL_ID = Identifier.of("minecraft", "block/custom/custom_model_data_273");
    private static final net.minecraft.block.BlockState WEATHER_DETECTOR_STATE = Blocks.NOTE_BLOCK.getDefaultState()
            .with(NoteBlock.INSTRUMENT, NoteBlockInstrument.BELL)
            .with(NoteBlock.NOTE, 8)
            .with(NoteBlock.POWERED, false);
    private static final net.minecraft.block.BlockState CUSTOM_MODEL_DATA_273_STATE = Blocks.NOTE_BLOCK.getDefaultState()
            .with(NoteBlock.INSTRUMENT, NoteBlockInstrument.BELL)
            .with(NoteBlock.NOTE, 9)
            .with(NoteBlock.POWERED, false);
    private static final net.minecraft.block.BlockState BARRIER_STATE = Blocks.BARRIER.getDefaultState();

    private WeatherDetectorModelRegistry() {
    }

    public static void register() {
        ModelLoadingPlugin.register(context ->
                context.registerBlockStateResolver(Blocks.NOTE_BLOCK, WeatherDetectorModelRegistry::resolveNoteBlock));
        ModelLoadingPlugin.register(context ->
                context.registerBlockStateResolver(Blocks.BARRIER, WeatherDetectorModelRegistry::resolveBarrier));
    }

    private static void resolveNoteBlock(BlockStateResolver.Context context) {
        if (context.block() != Blocks.NOTE_BLOCK) {
            return;
        }
        context.setModel(WEATHER_DETECTOR_STATE, new SimpleBlockStateModel.Unbaked(new ModelVariant(WEATHER_DETECTOR_MODEL_ID)).cached());
        context.setModel(CUSTOM_MODEL_DATA_273_STATE, new SimpleBlockStateModel.Unbaked(new ModelVariant(CUSTOM_MODEL_DATA_273_MODEL_ID)).cached());
    }

    private static void resolveBarrier(BlockStateResolver.Context context) {
        if (context.block() != Blocks.BARRIER) {
            return;
        }
        context.setModel(BARRIER_STATE, new SimpleBlockStateModel.Unbaked(new ModelVariant(WEATHER_DETECTOR_MODEL_ID)).cached());
    }
}
