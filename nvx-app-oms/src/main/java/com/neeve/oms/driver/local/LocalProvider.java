package com.neeve.oms.driver.local;

import java.util.Properties;

import com.neeve.event.*;
import com.neeve.sma.*;
import com.neeve.sma.impl.*;

final public class LocalProvider extends MessagingProviderBase {
    private LocalProvider(final String name, final Properties props) {
        super(null, name, props);
    }

    final public MessageBusBinding doCreateBinding(final String userName,
                                                   final MessageBusDescriptor descriptor,
                                                   final IEventHandler eventHandler) throws SmaException {
        return new LocalMessageBusBinding(userName, descriptor, eventHandler);
    }

    final public static MessagingProvider create(final String name, final Properties props) throws SmaException {
        return new LocalProvider(name, props);
    }
}
