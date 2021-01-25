package com.devexperts.common;

import org.springframework.beans.factory.annotation.Value;

public class NodeProperties {
    @Value("${PEER_CLASS_LOADING}")
    private boolean peerClassLoadingEnabled;
    @Value("${CLIENT_MODE}")
    private boolean clientMode;
    @Value("${ADDRESSES}")
    private String addresses;
    @Value("${COMMUNICATION_PORT}")
    private int communicationPort;
    @Value("${COMMUNICATION_PORT_RANGE}")
    private int communicationPortRange;

    public boolean isPeerClassLoadingEnabled() {
        return peerClassLoadingEnabled;
    }

    public boolean isClientMode() {
        return clientMode;
    }

    public String getAddresses() {
        return addresses;
    }

    public int getCommunicationPort() {
        return communicationPort;
    }

    public int getCommunicationPortRange() {
        return communicationPortRange;
    }
}
