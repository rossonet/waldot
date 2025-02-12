package net.rossonet.waldot.auth;

import net.rossonet.waldot.api.auth.WaldotX509IdentityValidator;
import net.rossonet.waldot.api.configuration.WaldotConfiguration;

public class DefaultX509IdentityValidator extends WaldotX509IdentityValidator {
//TODO: implementare la validazione dell'utente X509
	public DefaultX509IdentityValidator(WaldotConfiguration configuration) {
		super(configuration);
	}

}
