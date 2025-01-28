package net.rossonet.waldot.auth;

import net.rossonet.waldot.api.auth.WaldotIdentityValidator;
import net.rossonet.waldot.api.configuration.WaldotConfiguration;

public class DefaultIdentityValidator extends WaldotIdentityValidator {

	public DefaultIdentityValidator(WaldotConfiguration configuration) {
		super(configuration);
	}

}
