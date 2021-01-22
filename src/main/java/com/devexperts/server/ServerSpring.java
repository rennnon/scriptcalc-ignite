package com.devexperts.server;

import com.devexperts.common.IgniteConfig;
import com.devexperts.common.Utils;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class ServerSpring {

    public static void main(String[] args) throws IgniteException {
        System.setProperty("spring.profiles.active", "server");
        AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext(IgniteConfig.class);
        Ignite ignite = context.getBean(Ignite.class);
        Utils.printNodeStats(ignite, System.out);
    }
}
