package org.apereo.cas.web.flow;

import org.apache.commons.lang3.tuple.Pair;
import org.apereo.cas.authentication.Credential;
import org.apereo.cas.authentication.principal.Principal;
import org.apereo.cas.ticket.registry.TicketRegistrySupport;
import org.apereo.cas.web.support.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.binding.message.MessageContext;
import org.springframework.webflow.action.AbstractAction;
import org.springframework.webflow.action.EventFactorySupport;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

/**
 * Webflow action to receive and record the AUP response.
 *
 * @author Misagh Moayyed
 * @since 4.1
 */
public class AcceptableUsagePolicyFormAction extends AbstractAction {
    private static final Logger LOGGER = LoggerFactory.getLogger(AcceptableUsagePolicyFormAction.class);

    /**
     * Event id to signal the policy needs to be accepted.
     **/
    private static final String EVENT_ID_MUST_ACCEPT = "mustAccept";

    private final AcceptableUsagePolicyRepository repository;

    private final TicketRegistrySupport ticketRegistrySupport;

    public AcceptableUsagePolicyFormAction(final AcceptableUsagePolicyRepository repository,
                                           final TicketRegistrySupport ticketRegistrySupport) {
        this.repository = repository;
        this.ticketRegistrySupport = ticketRegistrySupport;
    }

    /**
     * Verify whether the policy is accepted.
     *
     * @param context        the context
     * @param credential     the credential
     * @param messageContext the message context
     * @return success if policy is accepted. {@link #EVENT_ID_MUST_ACCEPT} otherwise.
     */
    public Event verify(final RequestContext context,
                        final Credential credential,
                        final MessageContext messageContext) {
        final Pair<Boolean, Principal> res = repository.verify(context, credential);
        context.getFlowScope().put("principal", res.getValue());
        if (res.getKey()) {
            return success();
        }
        return accept();
    }

    /**
     * Record the fact that the policy is accepted.
     *
     * @param context        the context
     * @param credential     the credential
     * @param messageContext the message context
     * @return success if policy acceptance is recorded successfully.
     */
    public Event submit(final RequestContext context, final Credential credential, final MessageContext messageContext) {
        if (repository.submit(context, credential)) {
            return success();
        }

        return error();
    }

    @Override
    protected Event doExecute(final RequestContext requestContext) throws Exception {
        return verify(requestContext, WebUtils.getCredential(requestContext), requestContext.getMessageContext());
    }

    /**
     * Accept event signaled by id {@link #EVENT_ID_MUST_ACCEPT}.
     *
     * @return the event
     */
    protected Event accept() {
        return new EventFactorySupport().event(this, EVENT_ID_MUST_ACCEPT);
    }
}
