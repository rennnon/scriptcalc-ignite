package com.devexperts.common;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.IgniteLogger;
import org.apache.ignite.IgniteSpringBean;
import org.apache.ignite.configuration.DataPageEvictionMode;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.events.EventType;
import org.apache.ignite.logger.log4j2.Log4J2Logger;
import org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.multicast.TcpDiscoveryMulticastIpFinder;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.*;

import java.util.Collections;

@Configuration
public class IgniteConfig {

    public static final String DATA_REGION_WITH_EVICTION_NAME = "40MB_Region_Eviction";
    private static final int DATA_REGION_WITH_EVICTION_INITIAL_SIZE = 20 * 1024 * 1024;
    private static final int DATA_REGION_WITH_EVICTION_MAX_SIZE = 40 * 1024 * 1024;

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
        // general setting
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setClientMode(properties.isClientMode());
        cfg.setPeerClassLoadingEnabled(properties.isPeerClassLoadingEnabled());
        cfg.setGridLogger(igniteLogger(properties));

        // discovery
        TcpDiscoveryMulticastIpFinder ipFinder = new TcpDiscoveryMulticastIpFinder();
        ipFinder.setAddresses(Collections.singletonList(properties.getAddresses()));
        cfg.setDiscoverySpi(new TcpDiscoverySpi().setIpFinder(ipFinder));

        // communication
        TcpCommunicationSpi communicationSpi = new TcpCommunicationSpi();
        communicationSpi.setLocalPort(properties.getCommunicationPort());
        communicationSpi.setLocalPortRange(properties.getCommunicationPortRange());
        cfg.setCommunicationSpi(communicationSpi);

        // data regions
        DataStorageConfiguration storageCfg = new DataStorageConfiguration();
        // 40MB memory region with eviction enabled.
        DataRegionConfiguration regionWithEviction = new DataRegionConfiguration();
        regionWithEviction.setName(DATA_REGION_WITH_EVICTION_NAME);
        regionWithEviction.setInitialSize(DATA_REGION_WITH_EVICTION_INITIAL_SIZE);
        regionWithEviction.setMaxSize(DATA_REGION_WITH_EVICTION_MAX_SIZE);
        regionWithEviction.setPageEvictionMode(DataPageEvictionMode.RANDOM_2_LRU);
        storageCfg.setDataRegionConfigurations(regionWithEviction);
        cfg.setDataStorageConfiguration(storageCfg);

        // events
        cfg.setIncludeEventTypes(EventType.EVT_CACHE_OBJECT_PUT, EventType.EVT_CACHE_OBJECT_REMOVED,
                EventType.EVT_NODE_JOINED, EventType.EVT_NODE_LEFT,
                EventType.EVT_CACHE_REBALANCE_PART_SUPPLIED, EventType.EVT_CACHE_REBALANCE_PART_UNLOADED,
                EventType.EVT_CACHE_REBALANCE_PART_LOADED);


        return cfg;
    }

    @Bean
    public IgniteLogger igniteLogger(NodeProperties properties) {
        try {
            return new Log4J2Logger(properties.getLog4j2ConfigPath());
        } catch (IgniteCheckedException e) {
            throw new BeanCreationException("Failed to create Ignite logger", e);
        }
    }

    @Configuration
    @Profile("server")
    @PropertySources({
            @PropertySource("classpath:server.properties"),
            @PropertySource("classpath:common.properties")
    })
    static class LocalServerConfig {
    }

    @Configuration
    @Profile("client")
    @PropertySources({
            @PropertySource("classpath:client.properties"),
            @PropertySource("classpath:common.properties")
    })
    static class LocalClientConfig {
    }


}
