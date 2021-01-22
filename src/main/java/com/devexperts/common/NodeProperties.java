package com.devexperts.common;

import org.springframework.beans.factory.annotation.Value;

public class NodeProperties {
    @Value("${PEER_CLASS_LOADING}")
    private boolean peerClassLoadingEnabled;
    @Value("${CLIENT_MODE}")
    private boolean clientMode;
    @Value("${ADDRESSES}")
    private String addresses;


    public boolean isPeerClassLoadingEnabled() {
        return peerClassLoadingEnabled;
    }

    public boolean isClientMode() {
        return clientMode;
    }

    public String getAddresses() {
        return addresses;
    }
}
