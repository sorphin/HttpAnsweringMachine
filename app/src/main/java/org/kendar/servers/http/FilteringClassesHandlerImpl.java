package org.kendar.servers.http;

import org.apache.http.conn.HttpClientConnectionManager;
import org.kendar.http.*;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class FilteringClassesHandlerImpl implements FilteringClassesHandler {
    class FiltersConfiguration{
        public HashMap<HttpFilterType,List<FilterDescriptor>> filters = new HashMap<>();
        public HashMap<String,FilterDescriptor> filtersById = new HashMap<>();
    }
    private final List<CustomFilters> customFilterLoaders;
    private final Environment environment;
    private final AtomicReference<FiltersConfiguration> filtersConfiguration;

    static class PrioritySorter implements Comparator<FilterDescriptor>
    {
        @Override
        public int compare(FilterDescriptor o1, FilterDescriptor o2) {
            return Integer.compare(o1.getPriority(),o2.getPriority());
        }
    }

    public FilteringClassesHandlerImpl(List<CustomFilters> customFilterLoaders,Environment environment){
        this.customFilterLoaders = customFilterLoaders;
        this.environment = environment;
        var config = new FiltersConfiguration();
        filtersConfiguration =  new AtomicReference<>(config);
    }



    @PostConstruct
    public void init(){
        var config = new FiltersConfiguration();
        filtersConfiguration.set(config);
        config.filters.put(HttpFilterType.NONE,new ArrayList<>());
        config.filters.put(HttpFilterType.PRE_RENDER,new ArrayList<>());
        config.filters.put(HttpFilterType.API,new ArrayList<>());
        config.filters.put(HttpFilterType.STATIC,new ArrayList<>());
        config.filters.put(HttpFilterType.PRE_CALL,new ArrayList<>());
        config.filters.put(HttpFilterType.POST_CALL,new ArrayList<>());
        config.filters.put(HttpFilterType.POST_RENDER,new ArrayList<>());

        for (var filterLoader : customFilterLoaders) {
            for (var ds : filterLoader.loadFilters()) {
                config.filters.get(ds.getPhase()).add(ds);
                config.filtersById.put(ds.getId(), ds);
            }
        }
    }

    @Override
    public boolean handle(HttpFilterType filterType, Request request, Response response, HttpClientConnectionManager connectionManager) throws InvocationTargetException, IllegalAccessException {
        var config = filtersConfiguration.get();
        if(!config.filters.containsKey(filterType)) return false;
        for(var filterEntry: config.filters.get(filterType)){
            if(!filterEntry.isEnabled())continue;
            if(!methodMatches(filterEntry,request))continue;
            if(!filterMathches(filterEntry,request))continue;
            var isBlocking = filterEntry.execute(request,response,connectionManager);

            //If is blocking "by result"
            if(isBlocking == true){
                return true;
            }
            if(filterEntry.isBlocking()){
                return true;
            }
        }
        return false;
    }

    private boolean methodMatches(FilterDescriptor filterEntry, Request request) {
        if(filterEntry.getMethod().equalsIgnoreCase("*"))return true;
        return filterEntry.getMethod().equalsIgnoreCase(request.getMethod());
    }

    private boolean filterMathches(FilterDescriptor filterEntry, Request request) {
        if(!filterEntry.matchesHost(request.getHost(),environment))return false;
        return filterEntry.matchesPath(request.getPath(),environment,request);
    }
}
