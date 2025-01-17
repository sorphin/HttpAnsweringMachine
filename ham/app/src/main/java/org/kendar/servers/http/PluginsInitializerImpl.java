package org.kendar.servers.http;

import org.kendar.utils.LoggerBuilder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PluginsInitializerImpl implements PluginsInitializer {

    private final LoggerBuilder loggerBuilder;
    private final HashMap<String, String> plugins = new HashMap<>();
    private final HashMap<String, String> specialLoggers = new HashMap<>();
    public PluginsInitializerImpl(LoggerBuilder loggerBuilder) {
        this.loggerBuilder = loggerBuilder;
    }

    @Override
    public void addPluginAddress(String address, String description) {
        if (null != address) {
            plugins.put(address, description);
        }
    }

    @Override
    public void addSpecialLogger(String path, String description) {
        if (null != path) {

            specialLoggers.put(path, description);
        }
    }

    @Override
    public Map<String, String> getPluginAddresses() {
        return new HashMap<>(plugins);
    }

    @Override
    public List<SpecialLoggerDescriptor> getSpecialLoggers() {
        return specialLoggers.entrySet().stream().map(m -> {
            var res = new SpecialLoggerDescriptor();
            res.setDescription(m.getValue());
            res.setPath(m.getKey());
            res.setLevel(loggerBuilder.getLevel(m.getKey()).levelStr);
            return res;
        }).collect(Collectors.toList());
    }
}
