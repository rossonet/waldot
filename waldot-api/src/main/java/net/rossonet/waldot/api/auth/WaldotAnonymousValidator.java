package net.rossonet.waldot.api.auth;

import org.eclipse.milo.opcua.sdk.server.Session;
import org.eclipse.milo.opcua.sdk.server.identity.AbstractIdentityValidator;
import org.eclipse.milo.opcua.sdk.server.identity.Identity.AnonymousIdentity;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.enumerated.UserTokenType;
import org.eclipse.milo.opcua.stack.core.types.structured.AnonymousIdentityToken;
import org.eclipse.milo.opcua.stack.core.types.structured.SignatureData;
import org.eclipse.milo.opcua.stack.core.types.structured.UserTokenPolicy;
import org.jspecify.annotations.Nullable;

import net.rossonet.waldot.api.configuration.WaldotConfiguration;

/**
 * Abstract class for Waldot anonymous token validation. This class extends the
 * AbstractIdentityValidator to provide a specific implementation for validating
 * anonymous tokens in the Waldot context.
 * 
 * @author Andrea Ambrosini - Zona Autonoma srl
 */
public abstract class WaldotAnonymousValidator extends AbstractIdentityValidator {

	protected WaldotConfiguration configuration;

	/**
	 * Tells whether the configuration allows anonymous sessions. A missing
	 * configuration keeps the historical default (anonymous allowed).
	 *
	 * @param configuration the WaldOT configuration, may be null
	 * @return true if anonymous sessions are accepted
	 */
	public static boolean isAnonymousAccessAllowed(final WaldotConfiguration configuration) {
		return configuration == null || configuration.getAnonymousAccessAllowed();
	}

	public WaldotAnonymousValidator(final WaldotConfiguration configuration) {
		this.configuration = configuration;
	}

	/**
	 * Rejects the anonymous token when {@code --anonymous-access false} is
	 * configured. Every subclass overriding
	 * {@link #validateAnonymousToken(Session, AnonymousIdentityToken, UserTokenPolicy, SignatureData)}
	 * must call it first.
	 *
	 * @throws UaException with {@code Bad_IdentityTokenRejected} when anonymous
	 *                     access is disabled
	 */
	protected void checkAnonymousAccessAllowed() throws UaException {
		if (!isAnonymousAccessAllowed(configuration)) {
			throw new UaException(StatusCodes.Bad_IdentityTokenRejected, "anonymous access is disabled");
		}
	}

	@Override
	public AnonymousIdentity validateAnonymousToken(final Session session, final AnonymousIdentityToken token,
			final UserTokenPolicy tokenPolicy, final SignatureData tokenSignature) throws UaException {
		checkAnonymousAccessAllowed();
		return new AnonymousIdentity() {

			private Object data;

			@Override
			public Object getUserData() {
				return data;
			}

			@Override
			public UserTokenType getUserTokenType() {
				return UserTokenType.Anonymous;
			}

			@Override
			public void setUserData(@Nullable Object userData) {
				this.data = userData;
			}
		};
	}

}
