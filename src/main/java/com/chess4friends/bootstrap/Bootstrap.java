package com.chess4friends.bootstrap;

import com.chess4friends.jersey.JerseyHandler;
import com.chess4friends.netty.NettyChannelInitializer;
import com.chess4friends.netty.NettyServer;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Created by florealeonard on 11/10/15.
 */

@EnableAutoConfiguration
@Configuration()
@ComponentScan
public class Bootstrap {

    @Autowired
    private ApplicationHandler applicationHandler;

    @Value("${server.host}")
    private String host;

    @Value("${server.port}")
    private String port;


    @Bean
    public ApplicationHandler buildApplicationHandler(){
        Map<String,Object> props = new HashMap<>();
        props.put("jersey.config.server.monitoring.statistics.enabled","true");

        return new ApplicationHandler(new ResourceConfig()
                .packages("com.chess4friends")
                .addProperties(props)
                .register(JacksonFeature.class));
    }

    @Bean
    public NettyServer buildServer(){
        NettyChannelInitializer channel =  new NettyChannelInitializer(new JerseyHandler(applicationHandler),new DefaultEventExecutorGroup(64), 1024*100);
        NettyServer server = new NettyServer(channel,new InetSocketAddress(host, Integer.valueOf(port)),1024);
        server.startServer();
        return server;
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Bootstrap.class, args);
        new CountDownLatch(1).await();
    }
}
