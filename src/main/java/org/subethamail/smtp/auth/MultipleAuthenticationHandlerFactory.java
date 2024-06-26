package org.subethamail.smtp.auth;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.StringTokenizer;

import org.subethamail.smtp.AuthenticationHandler;
import org.subethamail.smtp.AuthenticationHandlerFactory;
import org.subethamail.smtp.MessageContext;
import org.subethamail.smtp.RejectException;

/**
 * This handler combines the behavior of several other authentication handler factories.
 *
 * @author Jeff Schnitzer
 */
public class MultipleAuthenticationHandlerFactory implements AuthenticationHandlerFactory
{
	/**
	 * Maps the auth type (eg "PLAIN") to a handler. The mechanism name (key) is in upper case. 
	 */
	final Map<String, AuthenticationHandlerFactory> plugins = new HashMap<>();

	/** 
	 * A more orderly list of the supported mechanisms. Mechanism names are in upper case.
	 */
	List<String> mechanisms = new ArrayList<>();

	public MultipleAuthenticationHandlerFactory()
	{
		// Starting with an empty list is ok, let the user add them all
	}

	public MultipleAuthenticationHandlerFactory(Collection<AuthenticationHandlerFactory> factories)
	{
		for (AuthenticationHandlerFactory fact: factories)
		{
			this.addFactory(fact);
		}
	}

	public void addFactory(AuthenticationHandlerFactory fact)
	{
		List<String> partialMechanisms = fact.getAuthenticationMechanisms();
		for (String mechanism: partialMechanisms)
		{
			if (!this.mechanisms.contains(mechanism))
			{
				this.mechanisms.add(mechanism);
				this.plugins.put(mechanism, fact);
			}
		}
	}

	@Override
    public List<String> getAuthenticationMechanisms()
	{
		return this.mechanisms;
	}

	@Override
    public AuthenticationHandler create()
	{
		return new Handler();
	}

	/**
	 */
	final class Handler implements AuthenticationHandler
	{
		AuthenticationHandler active;

		/* */
		@Override
        public Optional<String> auth(String clientInput, MessageContext context) throws RejectException
		{
			if (this.active == null)
			{
				StringTokenizer stk = new StringTokenizer(clientInput);
				String auth = stk.nextToken();
				if (!"AUTH".equalsIgnoreCase(auth))
					throw new IllegalArgumentException("Not an AUTH command: " + clientInput);

				String method = stk.nextToken();
				AuthenticationHandlerFactory fact = MultipleAuthenticationHandlerFactory.this.plugins
						.get(method.toUpperCase(Locale.ENGLISH));

				if (fact == null)
					throw new RejectException(504, "Method not supported");

				this.active = fact.create();
			}

			return this.active.auth(clientInput, context);
		}

		/* */
		@Override
        public Object getIdentity()
		{
			return this.active.getIdentity();
		}
	}
}
