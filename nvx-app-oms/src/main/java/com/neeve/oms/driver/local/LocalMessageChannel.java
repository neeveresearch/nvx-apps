package com.neeve.oms.driver.local;

import com.neeve.sma.MessageBusBinding;
import com.neeve.sma.MessageChannelDescriptor;
import com.neeve.sma.MessageView;
import com.neeve.sma.SmaException;
import com.neeve.sma.impl.MessageChannelBase;

final public class LocalMessageChannel extends MessageChannelBase {
    final LocalMessageBusBinding _binding;

    LocalMessageChannel(final MessageChannelDescriptor descriptor, final LocalMessageBusBinding binding) throws SmaException {
        super(null, descriptor, binding);
        _binding = binding;
    }

    @Override
    final protected boolean doSend(final MessageView view,
                                   final MessageBusBinding.FlushContext flushContext,
                                   final int flags) throws SmaException {
        _binding.send(view);
        return false;
    }

    @Override
    final protected void doJoin(final String[] filters, final int flags) throws SmaException {}

    @Override
    final protected void doLeave(final int flags) throws SmaException {}

    @Override
    final protected void doClose() throws SmaException {}

    @Override
    final public String getType() {
        return "Local";
    }
}
