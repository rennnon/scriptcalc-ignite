package com.devexperts.client;

import com.devexperts.common.IgniteConfig;
import com.devexperts.common.Utils;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class ClientConsumerSpring {

    public static void main(String[] args) throws IgniteException {
        System.setProperty("spring.profiles.active", "client");
        AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext(IgniteConfig.class);
        Ignite ignite = context.getBean(Ignite.class);
        Utils.printNodeStats(ignite, System.out);

        ignite.compute(ignite.cluster().forServers()).broadcast(new CalculationTask());
        System.out.println("Calculation task has been sent");

        ignite.close();
    }

}
