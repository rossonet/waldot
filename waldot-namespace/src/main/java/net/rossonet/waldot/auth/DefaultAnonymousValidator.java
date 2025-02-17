package net.rossonet.waldot.auth;

import net.rossonet.waldot.api.auth.WaldotAnonymousValidator;
import net.rossonet.waldot.api.configuration.WaldotConfiguration;

public class DefaultAnonymousValidator extends WaldotAnonymousValidator {
	// TODO: implementare la validazione dell'utente anonimo
	public DefaultAnonymousValidator(WaldotConfiguration configuration) {
		super(configuration);
	}

}
