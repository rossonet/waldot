package net.rossonet.waldot.ice;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.logging.Logger;

import org.ice4j.Transport;
import org.ice4j.TransportAddress;
import org.ice4j.ice.Agent;
import org.ice4j.ice.Candidate;
import org.ice4j.ice.Component;
import org.ice4j.ice.IceMediaStream;
import org.ice4j.ice.IceProcessingState;
import org.ice4j.ice.LocalCandidate;
import org.ice4j.ice.NominationStrategy;
import org.ice4j.ice.RemoteCandidate;
import org.ice4j.ice.harvest.CandidateHarvester;
import org.ice4j.ice.harvest.StunCandidateHarvester;
import org.ice4j.ice.harvest.UPNPHarvester;

public class IceUtils {

	/**
	 * The listener that would end example execution once we enter the completed
	 * state.
	 */
	public static final class IceProcessingListener implements PropertyChangeListener {
		/**
		 * System.exit()s as soon as ICE processing enters a final state.
		 *
		 * @param evt the {@link PropertyChangeEvent} containing the old and new states
		 *            of ICE processing.
		 */
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			final long processingEndTime = System.currentTimeMillis();

			final Object iceProcessingState = evt.getNewValue();

			logger.info("Agent entered the " + iceProcessingState + " state.");
			if (iceProcessingState == IceProcessingState.COMPLETED) {
				logger.info("Total ICE processing time: " + (processingEndTime - startTime) + "ms");
				final Agent agent = (Agent) evt.getSource();
				final List<IceMediaStream> streams = agent.getStreams();

				for (final IceMediaStream stream : streams) {
					final String streamName = stream.getName();
					logger.info("Pairs selected for stream: " + streamName);
					final List<Component> components = stream.getComponents();

					for (final Component cmp : components) {
						final String cmpName = cmp.getName();
						logger.info(cmpName + ": " + cmp.getSelectedPair());
					}
				}

				logger.info("Printing the completed check lists:");
				for (final IceMediaStream stream : streams) {
					final String streamName = stream.getName();
					logger.info("Check list for  stream: " + streamName);
					// uncomment for a more verbose output
					logger.info(stream.getCheckList().toString());
				}

				logger.info("Total ICE processing time to completion: " + (System.currentTimeMillis() - startTime));
			} else if (iceProcessingState == IceProcessingState.TERMINATED
					|| iceProcessingState == IceProcessingState.FAILED) {
				/*
				 * Though the process will be instructed to die, demonstrate that Agent
				 * instances are to be explicitly prepared for garbage collection.
				 */
				((Agent) evt.getSource()).free();

				logger.info("Total ICE processing time: " + (System.currentTimeMillis() - startTime));
				System.exit(0);
			}
		}
	}

	private static final Logger logger = Logger.getLogger(IceUtils.class.getName());

	/**
	 * The indicator which determines whether the <tt>Ice</tt> application (i.e. the
	 * run-sample Ant target) is to start connectivity establishment of the remote
	 * peer (in addition to the connectivity establishment of the local agent which
	 * is always started, of course).
	 */
	private static final boolean START_CONNECTIVITY_ESTABLISHMENT_OF_REMOTE_PEER = false;

	/**
	 * Start time for debugging purposes.
	 */
	static long startTime;

	/**
	 * Creates a vanilla ICE <tt>Agent</tt> and adds to it an audio and a video
	 * stream with RTP and RTCP components.
	 *
	 * @param rtpPort the port that we should try to bind the RTP component on (the
	 *                RTCP one would automatically go to rtpPort + 1)
	 * @return an ICE <tt>Agent</tt> with an audio stream with RTP and RTCP
	 *         components.
	 *
	 * @throws Throwable if anything goes wrong.
	 */
	protected static Agent createAgent(int rtpPort) throws Throwable {
		return createAgent(rtpPort, false);
	}

	/**
	 * Creates an ICE <tt>Agent</tt> (vanilla or trickle, depending on the value of
	 * <tt>isTrickling</tt>) and adds to it an audio and a video stream with RTP and
	 * RTCP components.
	 *
	 * @param rtpPort the port that we should try to bind the RTP component on (the
	 *                RTCP one would automatically go to rtpPort + 1)
	 * @return an ICE <tt>Agent</tt> with an audio stream with RTP and RTCP
	 *         components.
	 * @param isTrickling indicates whether the newly created agent should be
	 *                    performing trickle ICE.
	 *
	 * @throws Throwable if anything goes wrong.
	 */
	protected static Agent createAgent(int rtpPort, boolean isTrickling) throws Throwable {
		return createAgent(rtpPort, isTrickling, null);
	}

	protected static Agent createAgent(int rtpPort, boolean isTrickling, List<CandidateHarvester> harvesters)
			throws Throwable {
		final long startTime = System.currentTimeMillis();
		final Agent agent = new Agent();
		agent.setTrickling(isTrickling);

		if (harvesters == null) {
// STUN
			final StunCandidateHarvester stunHarv = new StunCandidateHarvester(
					new TransportAddress("meet-jit-si-turnrelay.jitsi.net", 3478, Transport.UDP));

			agent.addCandidateHarvester(stunHarv);

//UPnP: adding an UPnP harvester because they are generally slow
//which makes it more convenient to test things like trickle.
			agent.addCandidateHarvester(new UPNPHarvester());
		} else {
			for (final CandidateHarvester harvester : harvesters) {
				agent.addCandidateHarvester(harvester);
			}
		}

//STREAMS
		createStream(rtpPort, "audio", agent);
		createStream(rtpPort + 2, "video", agent);

		final long endTime = System.currentTimeMillis();
		final long total = endTime - startTime;

		logger.info("Total harvesting time: " + total + "ms.");

		return agent;
	}

	/**
	 * Creates an <tt>IceMediaStream</tt> and adds to it an RTP and and RTCP
	 * component.
	 *
	 * @param rtpPort    the port that we should try to bind the RTP component on
	 *                   (the RTCP one would automatically go to rtpPort + 1)
	 * @param streamName the name of the stream to create
	 * @param agent      the <tt>Agent</tt> that should create the stream.
	 *
	 * @return the newly created <tt>IceMediaStream</tt>.
	 * @throws Throwable if anything goes wrong.
	 */
	private static IceMediaStream createStream(int rtpPort, String streamName, Agent agent) throws Throwable {
		final IceMediaStream stream = agent.createMediaStream(streamName);

		long startTime = System.currentTimeMillis();

		// TODO: component creation should probably be part of the library. it
		// should also be started after we've defined all components to be
		// created so that we could run the harvesting for everyone of them
		// simultaneously with the others.

		// rtp
		agent.createComponent(stream, rtpPort, rtpPort, rtpPort + 100);

		long endTime = System.currentTimeMillis();
		logger.info("RTP Component created in " + (endTime - startTime) + " ms");
		startTime = endTime;
		// rtcpComp
		agent.createComponent(stream, rtpPort + 1, rtpPort + 1, rtpPort + 101);

		endTime = System.currentTimeMillis();
		logger.info("RTCP Component created in " + (endTime - startTime) + " ms");

		return stream;
	}

	public static void main(String[] args) throws Throwable {
		startTime = System.currentTimeMillis();

		final Agent localAgent = createAgent(9090, false);
		localAgent.setNominationStrategy(NominationStrategy.NOMINATE_HIGHEST_PRIO);
		final Agent remotePeer = createAgent(6060, false);

		localAgent.addStateChangeListener(new IceProcessingListener());

		// let them fight ... fights forge character.
		localAgent.setControlling(true);
		remotePeer.setControlling(false);

		final long endTime = System.currentTimeMillis();

		transferRemoteCandidates(localAgent, remotePeer);
		for (final IceMediaStream stream : localAgent.getStreams()) {
			stream.setRemoteUfrag(remotePeer.getLocalUfrag());
			stream.setRemotePassword(remotePeer.getLocalPassword());
		}

		if (START_CONNECTIVITY_ESTABLISHMENT_OF_REMOTE_PEER) {
			transferRemoteCandidates(remotePeer, localAgent);
		}

		for (final IceMediaStream stream : remotePeer.getStreams()) {
			stream.setRemoteUfrag(localAgent.getLocalUfrag());
			stream.setRemotePassword(localAgent.getLocalPassword());
		}

		logger.info("Total candidate gathering time: " + (endTime - startTime) + "ms");
		logger.info("LocalAgent:\n" + localAgent);

		localAgent.startConnectivityEstablishment();

		if (START_CONNECTIVITY_ESTABLISHMENT_OF_REMOTE_PEER) {
			remotePeer.startConnectivityEstablishment();
		}

		logger.info("Local audio clist:\n" + localAgent.getStream("audio").getCheckList());

		final IceMediaStream videoStream = localAgent.getStream("video");

		if (videoStream != null) {
			logger.info("Local video clist:\n" + videoStream.getCheckList());
		}

		// Give processing enough time to finish. We'll System.exit() anyway
		// as soon as localAgent enters a final state.
		Thread.sleep(60000);
	}

	/**
	 * Installs remote candidates in <tt>localAgent</tt>..
	 *
	 * @param localAgent a reference to the agent that we will pretend to be the
	 *                   local
	 * @param remotePeer a reference to what we'll pretend to be a remote agent.
	 */
	static void transferRemoteCandidates(Agent localAgent, Agent remotePeer) {
		final List<IceMediaStream> streams = localAgent.getStreams();

		for (final IceMediaStream localStream : streams) {
			final String streamName = localStream.getName();

			// get a reference to the local stream
			final IceMediaStream remoteStream = remotePeer.getStream(streamName);

			if (remoteStream != null) {
				transferRemoteCandidates(localStream, remoteStream);
			} else {
				localAgent.removeStream(localStream);
			}
		}
	}

	/**
	 * Adds to <tt>localComponent</tt> a list of remote candidates that are actually
	 * the local candidates from <tt>remoteComponent</tt>.
	 *
	 * @param localComponent  the <tt>Component</tt> where that we should be adding
	 *                        <tt>remoteCandidate</tt>s to.
	 * @param remoteComponent the source of remote candidates.
	 */
	private static void transferRemoteCandidates(Component localComponent, Component remoteComponent) {
		final List<LocalCandidate> remoteCandidates = remoteComponent.getLocalCandidates();

		localComponent.setDefaultRemoteCandidate(remoteComponent.getDefaultCandidate());

		for (final Candidate<?> rCand : remoteCandidates) {
			localComponent.addRemoteCandidate(new RemoteCandidate(rCand.getTransportAddress(), localComponent,
					rCand.getType(), rCand.getFoundation(), rCand.getPriority(), null));
		}
	}

	/**
	 * Installs remote candidates in <tt>localStream</tt>..
	 *
	 * @param localStream  the stream where we will be adding remote candidates to.
	 * @param remoteStream the stream that we should extract remote candidates from.
	 */
	private static void transferRemoteCandidates(IceMediaStream localStream, IceMediaStream remoteStream) {
		final List<Component> localComponents = localStream.getComponents();

		for (final Component localComponent : localComponents) {
			final int id = localComponent.getComponentID();

			final Component remoteComponent = remoteStream.getComponent(id);

			if (remoteComponent != null) {
				transferRemoteCandidates(localComponent, remoteComponent);
			} else {
				localStream.removeComponent(localComponent);
			}
		}
	}
}