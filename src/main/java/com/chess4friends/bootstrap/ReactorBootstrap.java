package com.chess4friends.bootstrap;

import com.chess4friends.jersey.JerseyHelper;
import io.netty.handler.codec.http.*;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import reactor.core.Reactor;
import reactor.core.spec.Reactors;
import reactor.event.Event;
import reactor.event.dispatch.ThreadPoolExecutorDispatcher;
import reactor.net.NetChannel;
import reactor.net.NetServer;
import reactor.net.config.ServerSocketOptions;
import reactor.net.netty.NettyServerSocketOptions;
import reactor.net.netty.tcp.NettyTcpServer;
import reactor.net.tcp.spec.TcpServerSpec;
import reactor.spring.context.config.EnableReactor;
import reactor.tuple.Tuple;

import java.util.concurrent.CountDownLatch;

import static reactor.event.selector.Selectors.$;

/**
 * Created by florealeonard on 11/9/15.
 */

@EnableAutoConfiguration
@Configuration
@ComponentScan
@EnableReactor
public class ReactorBootstrap {

    @Autowired
    private ApplicationHandler applicationHandler;

    @Bean
    public Reactor buildReactor(reactor.core.Environment env){
        env.addDispatcher("asynk",new ThreadPoolExecutorDispatcher(1024,16));
        Reactor reactor = Reactors.reactor(env);

        reactor.receive($("resources"),(Event<Tuple> event) ->{
            JerseyHelper
                    .handleRequest(applicationHandler
                            ,(FullHttpRequest)event.getData().get(0)
                            ,(NetChannel<FullHttpRequest, FullHttpResponse>)event.getData().get(1));

            return event;
        });

        return reactor;
    }

    @Bean
    public ServerSocketOptions serverSocketOptions() {
        return new NettyServerSocketOptions()
                .pipelineConfigurer(pipeline -> pipeline.addLast(new HttpServerCodec())
                        .addLast(new HttpObjectAggregator(16 * 1024 * 1024)));
    }

    @Bean
    public ApplicationHandler buildApplicationHandler(){
       return new ApplicationHandler(new ResourceConfig()
               .packages("com.chess4friends")
               .register(JacksonFeature.class));
    }

    @Bean
    public NetServer<FullHttpRequest,FullHttpResponse> buildServer(reactor.core.Environment env,
                                                                   ServerSocketOptions opts,
                                                                   Reactor reactor) throws Exception{

        NetServer<FullHttpRequest, FullHttpResponse> server = new TcpServerSpec<FullHttpRequest, FullHttpResponse>(
                NettyTcpServer.class)
                .env(env)
                .dispatcher("asynk")
                .options(opts)
                .consume(ch -> {
                    System.out.println("on consume "+Thread.currentThread().getName()+"," +System.currentTimeMillis());
                    ch.consume(req->reactor.sendAndReceive("resources",Event.wrap(new Tuple(req,ch)),res->{
                        System.out.println("on receive "+Thread.currentThread().getName()+", " +System.currentTimeMillis());
                    }));
                }).get();

        server.start().await();

        return server;
    }

//    public static void main(String[] args) throws InterruptedException {
//        SpringApplication.run(ReactorBootstrap.class, args);
//        new CountDownLatch(1).await();
//    }
}
