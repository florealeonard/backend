package com.chess4friends.resources;

import com.chess4friends.domain.Echo;

import javax.ws.rs.*;

/**
 * Created by florealeonard on 11/9/15.
 */
@Path("/echo/{hello}")
public class EchoResource {

    @GET
    @Produces("application/json")
    public Echo echo(@PathParam("hello") String hello){
        Echo echo = new Echo();

        try {
            System.out.println(Thread.currentThread().getName()+" , sleeping for "+(Integer.valueOf(hello)*1000)+"ms");
            Thread.sleep(Integer.valueOf(hello)*1000);;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return echo;
    }
}
