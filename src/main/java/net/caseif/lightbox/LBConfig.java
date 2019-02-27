/*
 * This file is a part of LightBox.
 * Copyright (c) 2019, Max Roncace <mproncace@gmail.com>
 *
 * The MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.caseif.lightbox;

import static com.google.common.base.Preconditions.checkNotNull;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.plugin.PluginContainer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.annotation.Nonnull;

public class LBConfig {

    private static final String FILE_EXT = "conf";

    private static final Logger LOGGER = LoggerFactory.getLogger("LightBox");

    /**
     * Constructs and initializes a configuration loader for the given plugin
     * and config path. If the path does not include a file extension, the
     * default extension {@code .conf} will be appended to the last element.
     * <p></p>
     * <p>The config loader first attempts to load the configuration externally
     * from disk, creating it if necessary. Then, it attempts to load an
     * internal configuration from the classpath, merging into the external
     * (disk) configuration any values not present in the latter. An internal
     * configuration is not strictly required, and any failure to locate it will
     * be quietly ignored.</p>
     * <p></p>
     * <p>To avoid name clashes, the internal configuration's location must be
     * namespaced with the plugin's ID. For example, plugin {@code foo}'s
     * configuration with the path {@code {bar, quux}} should be located at
     * {@code /foo/bar/quux} on the classpath.</p>
     *
     * @param plugin The plugin to construct a configuration loader for
     * @param path The non-namespaced path of the configuration file
     * @throws IOException If an {@link IOException} is encountered while
     *         loading the internal or external configurations, or while
     *         re-saving the updated external configuration
     * @throws NullPointerException If either {@code plugin} or {@code path} are
     *         {@code null}
     */
    public static LBConfig provide(@Nonnull PluginContainer plugin, @Nonnull String... path)
            throws IOException, NullPointerException {
        return new LBConfig(checkNotNull(plugin, "plugin"), checkNotNull(path, "path"));
    }

    private final PluginContainer plugin;
    private final String[] path;

    private ConfigurationLoader<CommentedConfigurationNode> loader;
    private ConfigurationNode config;

    private LBConfig(PluginContainer plugin, String... path) throws IOException {
        if (path.length == 0) {
            throw new IllegalArgumentException("Config path must include at least one element");
        }

        LOGGER.debug("Initalizing configuration " + plugin.getId() + ":" + String.join("/", path));

        this.plugin = plugin;
        this.path = new String[path.length];
        System.arraycopy(path, 0, this.path, 0, path.length);

        if (!this.path[this.path.length - 1].contains(".")) {
            LOGGER.debug("Appending file extension to config " + String.join("/", this.path) + " for plugin "
                    + plugin.getId());
            this.path[this.path.length - 1] += "." + FILE_EXT;
        }

        loadDiskConfig();

        loadInternalConfig();

        save();
    }

    public void save() throws IOException {
        String pathStr = plugin.getId() + String.join("/", this.path);
        LOGGER.debug("Save requested for configuration " + pathStr);
        loader.save(config);
        LOGGER.debug("Saved configuration " + pathStr + " to disk");
    }

    public ConfigurationNode getRootNode() {
        return config;
    }

    public ConfigurationNode getNode(Object... path) {
        return config.getNode(path);
    }

    private void loadDiskConfig() throws IOException {
        Path filePath = Sponge.getConfigManager().getPluginConfig(this.plugin).getDirectory();

        for (String el : this.path) {
            filePath = filePath.resolve(el);
        }

        String pathStr = plugin.getId() + ":" + String.join("/", this.path);

        if (!Files.exists(filePath)) {
            LOGGER.debug("External configuration " + pathStr + " does not exist, creating");
            if (!Files.exists(filePath.getParent())) {
                LOGGER.debug("Creating parent directories for configuration " + pathStr);
                Files.createDirectories(filePath.getParent());
            }

            Files.createFile(filePath);
        }

        this.loader = HoconConfigurationLoader.builder().setPath(filePath).build();

        this.config = loader.load();
        LOGGER.debug("Finished loading external configuration " + pathStr);
    }

    private void loadInternalConfig() throws IOException {
        String pathStr = "/" + this.plugin.getId() + "/" + String.join("/", this.path);
        try (InputStream is = LBConfig.class.getResourceAsStream(pathStr)) {
            if (is == null) {
                LOGGER.debug("No internal configuration found for " + pathStr);
                return;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            LOGGER.debug("Got handle to internal configuration for " + pathStr);

            HoconConfigurationLoader loader = HoconConfigurationLoader.builder().setSource(() -> reader).build();
            ConfigurationNode internal = loader.load();
            LOGGER.debug("Loaded internal configuration for " + pathStr);

            this.config.mergeValuesFrom(internal);
            LOGGER.debug("Merged internal values into " + pathStr);
        }
    }
}
