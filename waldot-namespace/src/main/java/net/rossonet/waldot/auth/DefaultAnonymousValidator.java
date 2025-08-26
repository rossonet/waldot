package net.rossonet.waldot.auth;

import java.util.Collections;
import java.util.Set;

import org.eclipse.milo.opcua.stack.core.types.enumerated.UserTokenType;

import net.rossonet.waldot.api.auth.WaldotAnonymousValidator;
import net.rossonet.waldot.api.configuration.WaldotConfiguration;

public class DefaultAnonymousValidator extends WaldotAnonymousValidator {
	// TODO: implementare la validazione dell'utente anonimo
	public DefaultAnonymousValidator(WaldotConfiguration configuration) {
		super(configuration);
	}

	@Override
	public Set<UserTokenType> getSupportedTokenTypes() {
		return Collections.singleton(UserTokenType.Anonymous);
	}

}
