package com.craftycorvid.improvedmaps.config;

import static com.craftycorvid.improvedmaps.ImprovedMaps.MOD_ID;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import net.fabricmc.loader.api.FabricLoader;

public class ModConfig {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().disableHtmlEscaping()
            .create();
    private static final File configFile = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + ".json").toFile();

    public enum MinimapCorner {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }

    // Config values. The server_/client_ prefix says who decides: server_ settings are read by
    // whoever runs the world and ignored on a client connected to someone else's server, client_
    // settings apply wherever their owner plays.
    //
    // The `alternate` names are the keys these fields used before the prefixes existed. Gson reads
    // them if present and loadConfig writes the file back, so an existing config carries over on
    // first launch instead of silently resetting to defaults.
    @SerializedName(value = "server_atlasMapCapacity", alternate = {"atlasMapCapacity"})
    public int server_atlasMapCapacity = 512;

    @SerializedName(value = "server_updateAtlasWhenNotInHand",
            alternate = {"updateAtlasWhenNotInHand"})
    public boolean server_updateAtlasWhenNotInHand = true;

    @SerializedName(value = "server_cacheBiomeMapColors", alternate = {"biomeMapColors"})
    public boolean server_cacheBiomeMapColors = true;

    @SerializedName(value = "client_showBiomeMapColors", alternate = {"showBiomeMapColors"})
    public boolean client_showBiomeMapColors = true;

    @SerializedName(value = "client_minimapEnabled", alternate = {"minimapEnabled"})
    public boolean client_minimapEnabled = true;

    @SerializedName(value = "client_minimapCorner", alternate = {"minimapCorner"})
    public MinimapCorner client_minimapCorner = MinimapCorner.TOP_RIGHT;

    @SerializedName(value = "client_minimapSize", alternate = {"minimapSize"})
    public int client_minimapSize = 96;

    // Reading and saving
    public static ModConfig loadConfig() {
        ModConfig config = null;

        if (configFile.exists()) {
            // An existing config is present, we should use its values
            try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(new FileInputStream(configFile),
                    StandardCharsets.UTF_8))) {
                // Parses the config file and puts the values into config object
                config = gson.fromJson(fileReader, ModConfig.class);
            } catch (IOException e) {
                throw new RuntimeException(
                        "[improved-maps] Problem occurred when trying to load config: ", e);
            }
        }
        // gson.fromJson() can return null if file is empty
        if (config == null) {
            config = new ModConfig();
        }

        // Saves the file in order to write new fields if they were added
        config.saveConfig();
        return config;
    }

    public void saveConfig() {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8)) {
            gson.toJson(this, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
