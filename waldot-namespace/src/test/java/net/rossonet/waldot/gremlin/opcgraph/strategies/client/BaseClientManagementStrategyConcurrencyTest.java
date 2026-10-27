package net.rossonet.waldot.gremlin.opcgraph.strategies.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.milo.opcua.sdk.server.identity.DefaultAnonymousIdentity;
import org.eclipse.milo.opcua.sdk.server.identity.DefaultUsernameIdentity;
import org.eclipse.milo.opcua.sdk.server.identity.Identity;
import org.eclipse.milo.opcua.sdk.server.identity.Identity.AnonymousIdentity;
import org.eclipse.milo.opcua.sdk.server.identity.Identity.UsernameIdentity;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.junit.jupiter.api.Test;

/**
 * Step 1.4: the sessions map is accessed by concurrent session activations and
 * the identity conversion must not fail with a ClassCastException.
 */
class BaseClientManagementStrategyConcurrencyTest {

	@Test
	void identityOfTheExpectedTypeIsReturned() throws Exception {
		final Identity anonymous = new DefaultAnonymousIdentity();
		assertSame(anonymous, BaseClientManagementStrategy.castIdentity(anonymous, AnonymousIdentity.class));
	}

	@Test
	void identityOfAnotherTypeIsRejected() {
		final Identity username = new DefaultUsernameIdentity("user");
		final UaException e = assertThrows(UaException.class,
				() -> BaseClientManagementStrategy.castIdentity(username, AnonymousIdentity.class));
		assertEquals(StatusCodes.Bad_IdentityTokenRejected, e.getStatusCode().value());
	}

	@Test
	void nullIdentityIsKeptAsBefore() throws Exception {
		assertNull(BaseClientManagementStrategy.castIdentity(null, UsernameIdentity.class));
	}

	@Test
	void sessionsMapIsConcurrent() throws Exception {
		final Field f = BaseClientManagementStrategy.class.getDeclaredField("sessions");
		f.setAccessible(true);
		assertInstanceOf(ConcurrentMap.class, f.get(new BaseClientManagementStrategy()));
	}
}
