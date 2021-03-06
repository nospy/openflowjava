/*
 * Copyright (c) 2013 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.openflowjava.protocol.impl.deserialization;

import com.google.common.collect.ImmutableMap;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.openflowjava.protocol.api.extensibility.DeserializerRegistry;
import org.opendaylight.openflowjava.protocol.api.extensibility.OFDeserializer;
import org.opendaylight.openflowjava.protocol.api.keys.MessageCodeKey;
import org.opendaylight.openflowjava.protocol.api.util.EncodeConstants;
import org.opendaylight.openflowjava.protocol.impl.util.TypeToClassKey;
import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * @author michal.polkorab
 * @author timotej.kubas
 */
public class DeserializationFactory {

    private final Map<TypeToClassKey, Class<?>> messageClassMap;
    private DeserializerRegistry registry;

    /**
     * Constructor
     */
    public DeserializationFactory() {
        final Map<TypeToClassKey, Class<?>> temp = new HashMap<>();
        TypeToClassMapInitializer.initializeTypeToClassMap(temp);
        messageClassMap = ImmutableMap.copyOf(temp);
    }

    /**
     * Transforms ByteBuf into correct POJO message
     * @param rawMessage
     * @param version version decoded from OpenFlow protocol message
     * @return correct POJO as DataObject
     */
    public DataObject deserialize(final ByteBuf rawMessage, final short version) {
        DataObject dataObject = null;
        int type = rawMessage.readUnsignedByte();
        Class<?> clazz = messageClassMap.get(new TypeToClassKey(version, type));
        rawMessage.skipBytes(EncodeConstants.SIZE_OF_SHORT_IN_BYTES);
        OFDeserializer<DataObject> deserializer = registry.getDeserializer(
                new MessageCodeKey(version, type, clazz));
        dataObject = deserializer.deserialize(rawMessage);
        return dataObject;
    }

    /**
     * @param registry
     */
    public void setRegistry(final DeserializerRegistry registry) {
        this.registry = registry;
    }

}
