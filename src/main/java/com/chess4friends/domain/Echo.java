package com.chess4friends.domain;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by florealeonard on 11/9/15.
 */

@XmlRootElement
public class Echo {
    private String echo;

    public String getEcho() {
        return echo;
    }

    public void setEcho(String echo) {
        this.echo = echo;
    }
}
