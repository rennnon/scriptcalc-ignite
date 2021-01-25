package com.devexperts.common;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteSpringBean;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.multicast.TcpDiscoveryMulticastIpFinder;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.*;

import java.util.Collections;

@Configuration
public class IgniteConfig {
    @Bean
    public NodeProperties nodeProperties() {
        return new NodeProperties();
    }

    @Bean
    public Ignite igniteInstance(IgniteConfiguration igniteConfiguration) {
        IgniteSpringBean ignite = new IgniteSpringBean();
        ignite.setConfiguration(igniteConfiguration);
        return ignite;
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public IgniteConfiguration nodeConfiguration(NodeProperties properties) {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setClientMode(properties.isClientMode());
        cfg.setPeerClassLoadingEnabled(properties.isPeerClassLoadingEnabled());
        TcpDiscoveryMulticastIpFinder ipFinder = new TcpDiscoveryMulticastIpFinder();
        ipFinder.setAddresses(Collections.singletonList(properties.getAddresses()));
        cfg.setDiscoverySpi(new TcpDiscoverySpi().setIpFinder(ipFinder));
        TcpCommunicationSpi communicationSpi = new TcpCommunicationSpi();
        communicationSpi.setLocalPort(properties.getCommunicationPort());
        communicationSpi.setLocalPortRange(properties.getCommunicationPortRange());
        cfg.setCommunicationSpi(communicationSpi);
        return cfg;
    }

    @Configuration
    @Profile("server")
    @PropertySources({
            @PropertySource("classpath:server.properties")
    })
    static class LocalServerConfig {
    }

    @Configuration
    @Profile("client")
    @PropertySources({
            @PropertySource("classpath:client.properties")
    })
    static class LocalClientConfig {
    }


}
