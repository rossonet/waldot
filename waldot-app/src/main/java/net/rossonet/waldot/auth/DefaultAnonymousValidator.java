package net.rossonet.waldot.auth;

import net.rossonet.waldot.api.auth.WaldotAnonymousValidator;
import net.rossonet.waldot.api.configuration.WaldotConfiguration;

public class DefaultAnonymousValidator extends WaldotAnonymousValidator {

	public DefaultAnonymousValidator(WaldotConfiguration configuration) {
		super(configuration);
	}

}
