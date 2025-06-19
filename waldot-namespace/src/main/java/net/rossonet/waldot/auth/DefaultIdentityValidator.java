package net.rossonet.waldot.auth;

import net.rossonet.waldot.api.auth.FactoryPasswordValidator;
import net.rossonet.waldot.api.auth.WaldotIdentityValidator;
import net.rossonet.waldot.api.configuration.WaldotConfiguration;

public class DefaultIdentityValidator extends WaldotIdentityValidator {
//TODO: implementare la validazione dell'utente
	public DefaultIdentityValidator(final WaldotConfiguration configuration) {
		super(configuration, new FactoryPasswordValidator());
	}

}
