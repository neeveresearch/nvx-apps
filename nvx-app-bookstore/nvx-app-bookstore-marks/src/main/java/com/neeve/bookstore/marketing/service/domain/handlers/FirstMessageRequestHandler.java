package com.neeve.bookstore.marketing.service.domain.handlers;

import java.util.Date;

import com.google.inject.*;

import com.neeve.ci.XRuntime;
import com.neeve.sma.MessageView;
import com.neeve.service.MessageHandler;
import com.neeve.service.IdentityInformationProvider;
import com.neeve.service.messages.FirstRequest;
import com.neeve.service.messages.AgentInfo;
import com.neeve.service.messages.AgentStartedEvent;

final public class FirstMessageRequestHandler implements MessageHandler<FirstRequest, AgentStartedEvent, com.neeve.bookstore.marketing.service.repository.Repository> {
    @Inject private IdentityInformationProvider identityInfoProvider;

    /**
     * Implementation of {@link MessageHandler#getType}
     */
    final public Type getType() {
        return Type.Local;
    }

    /**
     * Implementation of {@link MessageHandler#handle}
     */
    final public MessageView handle(final String origin,
                                    final FirstRequest request, 
                                    final AgentStartedEvent event, 
                                    final com.neeve.bookstore.marketing.service.repository.Repository repository) throws Exception {
        // dispatch the agent started event
        final AgentInfo agentInfo = AgentInfo.create();
        agentInfo.setApplicationName(identityInfoProvider.getName());
        agentInfo.setApplicationPartition(identityInfoProvider.getPartition());
        agentInfo.setPid(XRuntime.getPid());
        agentInfo.setStartTime(new Date());
        event.setAgentInfo(agentInfo);
        return null;
    }
}
