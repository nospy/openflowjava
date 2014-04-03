/*
 * Copyright (c) 2013 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.openflowjava.protocol.impl.connection;

import org.opendaylight.openflowjava.protocol.api.connection.ConnectionConfiguration;
import org.opendaylight.openflowjava.protocol.api.connection.ConnectionConfiguration.FEATURE_SUPPORT;
import org.opendaylight.openflowjava.protocol.api.connection.SwitchConnectionHandler;
import org.opendaylight.openflowjava.protocol.api.extensibility.MessageTypeKey;
import org.opendaylight.openflowjava.protocol.api.extensibility.OFSerializer;
import org.opendaylight.openflowjava.protocol.api.extensibility.SerializerRegistry;
import org.opendaylight.openflowjava.protocol.impl.core.TcpHandler;
import org.opendaylight.openflowjava.protocol.impl.serialization.SerializationFactory;
import org.opendaylight.openflowjava.protocol.impl.serialization.SerializerRegistryImpl;
import org.opendaylight.openflowjava.protocol.spi.connection.SwitchConnectionProvider;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

/**
 * Exposed class for server handling
 * @author mirehak
 * @author michal.polkorab
 */
public class SwitchConnectionProviderImpl implements SwitchConnectionProvider {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(SwitchConnectionProviderImpl.class);
    private SwitchConnectionHandler switchConnectionHandler;
    private ServerFacade serverFacade;
    private ConnectionConfiguration connConfig;
    private SerializationFactory serializationFactory;
    private SerializerRegistry serializerRegistry;

    /** Constructor */
    public SwitchConnectionProviderImpl() {
        serializerRegistry = new SerializerRegistryImpl();
        serializerRegistry.init();
        serializationFactory = new SerializationFactory();
        serializationFactory.setSerializerTable(serializerRegistry);
    }

    @Override
    public void setConfiguration(ConnectionConfiguration connConfig) {
        this.connConfig = connConfig;
    }

    @Override
    public void setSwitchConnectionHandler(SwitchConnectionHandler switchConnectionHandler) {
        LOGGER.debug("setSwitchConnectionHandler");
        this.switchConnectionHandler = switchConnectionHandler;
    }

    @Override
    public ListenableFuture<Boolean> shutdown() {
        LOGGER.debug("Shutdown summoned");
        //TODO: provide exception in case of: not started, not configured (already stopped)
        ListenableFuture<Boolean> result = serverFacade.shutdown();
        return result;
    }

    @Override
    public ListenableFuture<Boolean> startup() {
        LOGGER.debug("Startup summoned");
        serverFacade = createAndConfigureServer();
        
        LOGGER.debug("Starting ..");
        ListenableFuture<Boolean> result = null;
        try {
            if (serverFacade == null) {
                throw new IllegalStateException("No server configured");
            }
            if (serverFacade.getIsOnlineFuture().isDone()) {
                throw new IllegalStateException("Server already running");
            }
            if (switchConnectionHandler == null) {
                throw new IllegalStateException("switchConnectionHandler is not set");
            }
            new Thread(serverFacade).start();
            result = serverFacade.getIsOnlineFuture();
        } catch (Exception e) {
            SettableFuture<Boolean> exResult = SettableFuture.create();
            exResult.setException(e);
            result = exResult;
        }
        return result;
    }

    /**
     * @return
     */
    private TcpHandler createAndConfigureServer() {
        LOGGER.debug("Configuring ..");
        TcpHandler server = new TcpHandler(connConfig.getAddress(), connConfig.getPort());
        server.setSwitchConnectionHandler(switchConnectionHandler);
        server.setSwitchIdleTimeout(connConfig.getSwitchIdleTimeout());
        boolean tlsSupported = FEATURE_SUPPORT.REQUIRED.equals(connConfig.getTlsSupport());
        server.setEncryption(tlsSupported);
        server.setSerializationFactory(serializationFactory);
        return server;
    }

    /**
     * @return servers
     */
    public ServerFacade getServerFacade() {
        return serverFacade;
    }

    @Override
    public <E extends DataObject> void registerCustomSerializer(MessageTypeKey<E> key,
            OFSerializer<E> serializer) {
        serializerRegistry.registerSerializer(key, serializer);
    }

    @Override
    public void close() throws Exception {
        shutdown();
    }
}
