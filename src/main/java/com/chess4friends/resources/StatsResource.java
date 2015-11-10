package com.chess4friends.resources;

import org.glassfish.jersey.server.monitoring.MonitoringStatistics;
import org.glassfish.jersey.server.spi.internal.ResourceMethodDispatcher;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

/**
 * Created by florealeonard on 11/10/15.
 */
@Path("/stats")
public class StatsResource {

    @Inject
    Provider<MonitoringStatistics> statistics;

    @GET
    public String getTotalExceptionMappings() throws InterruptedException {
        final MonitoringStatistics monitoringStatistics = statistics.get();

        return monitoringStatistics.toString();
    }
}
