package net.rossonet.agent.zenoh;

import static io.zenoh.Config.loadDefault;

import java.io.IOException;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.zenoh.Config;
import io.zenoh.Session;
import io.zenoh.Zenoh;
import io.zenoh.bytes.Encoding;
import io.zenoh.bytes.IntoZBytes;
import io.zenoh.bytes.ZBytes;
import io.zenoh.exceptions.ZError;
import io.zenoh.keyexpr.KeyExpr;
import io.zenoh.pubsub.Publisher;
import io.zenoh.pubsub.PublisherOptions;
import io.zenoh.pubsub.PutOptions;
import io.zenoh.qos.CongestionControl;
import io.zenoh.qos.Reliability;
import io.zenoh.sample.Sample;
import io.zenoh.session.SessionInfo;

public class StudioZenohSolution {

	// docker run -it --rm -p 7447:7447/tcp -p 8000:8000/tcp
	// rossonet/linneo-zenoh:latest

	@Test
	public void baseCommunicationSubPub() throws InterruptedException, IOException, ZError {
		final Session s1 = createClient();
		final Session s2 = createClient();
		final Session s3 = createClient();
		final Session s4 = createClient();

		final String key = UUID.randomUUID().toString() + "/" + UUID.randomUUID().toString();
		final KeyExpr keyExpr = KeyExpr.tryFrom(key);
		System.out.println("Declaring publisher on '" + keyExpr + "'...");

		// A publisher config can optionally be provided.
		final PublisherOptions publisherOptions = new PublisherOptions();
		publisherOptions.setEncoding(Encoding.ZENOH_STRING);
		publisherOptions.setCongestionControl(CongestionControl.BLOCK);
		publisherOptions.setReliability(Reliability.RELIABLE);

		// Declare the publisher
		final Publisher publisher = s1.declarePublisher(keyExpr, publisherOptions);
		final IntoZBytes attachment = new IntoZBytes() {

			@Override
			public ZBytes into() {
				return new ZBytes("This is an attachment".getBytes());
			}

		};
		s2.declareSubscriber(keyExpr, this::handleSample);
		s3.declareSubscriber(keyExpr, this::handleSample);
		s4.declareSubscriber(keyExpr, this::handleSample);
		int idx = 0;
		while (idx < 100) {
			Thread.sleep(1000);
			final String payload = String.format("[%4d] %s", idx, UUID.randomUUID().toString());
			System.out.println("Putting Data ('" + keyExpr + "': '" + payload + "')...");

			if (attachment != null) {
				final PutOptions putOptions = new PutOptions();
				putOptions.setAttachment(attachment);
				publisher.put(payload, putOptions);
			} else {
				publisher.put(payload);
			}
			idx++;
		}
	}

	@Test
	public void checkClient() throws InterruptedException, ZError {
		createClient();
	}

	private Session createClient() throws ZError {
		Zenoh.initLogFromEnvOr("error");
		final Config config = loadDefault();
		System.out.println("Opening session...");
		final Session session = Zenoh.open(config);
		final SessionInfo info = session.info();
		System.out.println("zid: " + info.zid());
		System.out.println("routers zid: " + info.routersZid());
		System.out.println("peers zid: " + info.peersZid());
		return session;

	}

	private void handleSample(Sample sample) {
		final String attachment = sample.getAttachment() != null ? ", with attachment: " + sample.getAttachment() : "";
		System.out.println(">> [Subscriber] Received " + sample.getKind() + " ('" + sample.getKeyExpr() + "': '"
				+ sample.getPayload().toString() + "'" + attachment + ")");
	}

}
