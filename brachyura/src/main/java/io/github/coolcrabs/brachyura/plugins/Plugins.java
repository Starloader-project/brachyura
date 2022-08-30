package io.github.coolcrabs.brachyura.plugins;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;

import io.github.coolcrabs.brachyura.plugins.service.SlBrachyuraService;
import io.github.coolcrabs.brachyura.profiler.ProfilePlugin;
import io.github.coolcrabs.brachyura.project.EntryGlobals;
import io.github.coolcrabs.brachyura.util.NetUtil;

public class Plugins {
    private Plugins() { }

    private static class PluginLoader extends URLClassLoader {

        public PluginLoader() {
            super(new URL[0], PluginLoader.class.getClassLoader());
        }

        @Override
        protected void addURL(URL url) {
            super.addURL(url);
        }
    }

    private static List<@NotNull Plugin> plugins;
    private static final PluginLoader PLUGIN_LOADER = new PluginLoader();
    private static final Map<Class<? extends SlBrachyuraService>, Map<String, List<SlBrachyuraService>>> REGISTERED_SERVICES = new HashMap<>();

    @Nullable
    public static <T extends SlBrachyuraService> T getService(@NotNull Class<T> service, @Nullable String hint) {
        List<T> services = getServices(service, hint);
        if (services.isEmpty()) {
            return null;
        } else {
            return services.get(0);
        }
    }

    @SuppressWarnings("null")
    public static <T extends SlBrachyuraService> @NotNull List<T> getServices(@NotNull Class<T> service, @Nullable String hint) {
        if (hint != null) {
            hint = hint.toLowerCase(Locale.ROOT); // Hints are always in lowercase to avoid issues with typos
        }
        if (plugins == null) {
            getPlugins(); // Ensure that all plugins are loaded
        }
        Map<String, List<SlBrachyuraService>> services = REGISTERED_SERVICES.get(service);

        if (services == null) {
            services = new HashMap<>();
            Map<String, List<SlBrachyuraService>> var1001 = services;
            ServiceLoader.load(service, PLUGIN_LOADER).forEach(slbrachyuraService -> {
                var1001.compute(slbrachyuraService.getHint().toLowerCase(Locale.ROOT), (var1002, list) -> {
                    if (list == null) {
                        list = new ArrayList<>();
                    }
                    list.add(slbrachyuraService);
                    return list;
                });
            });
            REGISTERED_SERVICES.put(service, services);
        }

        if (hint == null) {
            List<T> found = new ArrayList<>();
            services.values().forEach(l -> l.forEach(var1003 -> {
                @SuppressWarnings("unchecked")
                T var1004 = (T) var1003;
                found.add(var1004);
            }));
            return Collections.unmodifiableList(found);
        } else {
            @SuppressWarnings("unchecked")
            List<T> found = (List<T>) services.get(hint);
            if (found == null) {
                return Collections.emptyList();
            } else {
                return Collections.unmodifiableList(found);
            }
        }
    }

    @NotNull
    public static List<@NotNull Plugin> getPlugins() {
        // Slbrachyura: better plugin loading
        List<@NotNull Plugin> plugins = Plugins.plugins;
        if (plugins == null) {
            synchronized (Plugins.class) {
                plugins = Plugins.plugins;
                if (plugins != null) { // Extremely rare race condition
                    return plugins;
                }
                plugins = new ArrayList<>();
                discoverPlugins();
                classloadPlugins(plugins);
                plugins.add(ProfilePlugin.INSTANCE);
                plugins = Collections.unmodifiableList(plugins);
                assert plugins != null;  // Got to fix that with EEA one day
                Plugins.plugins = plugins;
            }
        }
        return plugins;
    }

    @SuppressWarnings("null")
    private static void classloadPlugins(List<@NotNull Plugin> to) {
        ServiceLoader<Plugin> loader = ServiceLoader.load(Plugin.class, PLUGIN_LOADER);
        loader.forEach(to::add);
    }

    private static void discoverPlugins() {
        Path properties = EntryGlobals.getProjectDir().resolve("buildscript").resolve("buildscript.properties");
        if (Files.exists(properties)) {
            Properties props = new Properties();
            try (InputStream in = Files.newInputStream(properties)) {
                props.load(in);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            String[] plugins = props.getOrDefault("plugins", "").toString().split(";");
            if (plugins.length == 0) {
                return;
            }
            Path pluginsCacheDir = EntryGlobals.getProjectDir().resolve("buildscript").resolve(".brachyura").resolve("plugins");

            try {
                Files.createDirectories(pluginsCacheDir);
            } catch (IOException e) {
                e.printStackTrace();
            }

            for (String plugin : plugins) {
                if (plugin.startsWith("http://") || plugin.startsWith("https://")) {
                    try {
                        String[] destination = plugin.split(",");
                        if (destination.length != 2) {
                            // While yes, we could use the checksum of the jar to store the plugin as something, this makes no sense really
                            throw new IOException("Don't know what to store plugin file \"" + plugin + "\" as.");
                        }
                        String destinationFile = destination[1];
                        if (destinationFile == null) {
                            throw new AssertionError();
                        }
                        Path pluginFile = pluginsCacheDir.resolve(destinationFile);
                        if (Files.notExists(pluginFile)) {
                            Files.copy(NetUtil.inputStream(NetUtil.url(destination[0])), pluginFile);
                        }
                        PLUGIN_LOADER.addURL(pluginFile.toUri().toURL());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (plugin.startsWith("file://")) {
                    PLUGIN_LOADER.addURL(NetUtil.url(plugin));
                } else {
                    // While yes, we could support downloading maven artifacts, as of now I am not going to do it until we
                    // have better maven support.
                    try {
                        Path pluginFile = EntryGlobals.getProjectDir().resolve("buildscript").resolve(plugin);
                        if (Files.notExists(pluginFile)) {
                            Logger.error("Plugin file does not exist: " + pluginFile.toAbsolutePath().toString());
                        } else {
                            PLUGIN_LOADER.addURL(pluginFile.toUri().toURL());
                        }
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
