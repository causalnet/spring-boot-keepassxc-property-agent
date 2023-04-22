package au.net.causal.springboot.keepassxcpropertyagent.logging;

import org.slf4j.helpers.NOP_FallbackServiceProvider;

/**
 * The basic SLF4J provider that is just enough to avoid it printing the missing provider message when the agent starts up
 * but still does no-op.
 */
public class AgentLoggingProvider extends NOP_FallbackServiceProvider
{
}
