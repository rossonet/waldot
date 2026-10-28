package net.rossonet.waldot.client.api.subscription;

import org.eclipse.milo.opcua.stack.core.types.enumerated.DataChangeTrigger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.DeadbandType;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode;

/**
 * Parameters of a monitored value.
 *
 * @param samplingInterval requested sampling interval (ms, -1 = publishing
 *                         interval of the subscription)
 * @param queueSize        size of the server queue
 * @param discardOldest    discard the oldest value when the queue is full
 * @param trigger          what is a change (status, value, timestamp)
 * @param deadbandType     deadband (none, absolute, percent)
 * @param deadbandValue    deadband value
 * @param monitoringMode   reporting, sampling or disabled
 */
public record DataItemOptions(double samplingInterval, int queueSize, boolean discardOldest,
		DataChangeTrigger trigger, DeadbandType deadbandType, double deadbandValue, MonitoringMode monitoringMode) {

	/** Default options: given sampling interval, queue 10, value changes. */
	public static DataItemOptions sampling(final double samplingInterval) {
		return new DataItemOptions(samplingInterval, 10, true, DataChangeTrigger.StatusValue, DeadbandType.None, 0.0,
				MonitoringMode.Reporting);
	}

	public DataItemOptions {
		if (queueSize < 1) {
			throw new IllegalArgumentException("queueSize must be >= 1");
		}
		if (deadbandValue < 0) {
			throw new IllegalArgumentException("deadbandValue must be >= 0");
		}
		trigger = trigger == null ? DataChangeTrigger.StatusValue : trigger;
		deadbandType = deadbandType == null ? DeadbandType.None : deadbandType;
		monitoringMode = monitoringMode == null ? MonitoringMode.Reporting : monitoringMode;
	}

	/** Same options with an absolute deadband. */
	public DataItemOptions absoluteDeadband(final double value) {
		return new DataItemOptions(samplingInterval, queueSize, discardOldest, trigger, DeadbandType.Absolute, value,
				monitoringMode);
	}

	/** Same options with a percent deadband (analog items only). */
	public DataItemOptions percentDeadband(final double value) {
		return new DataItemOptions(samplingInterval, queueSize, discardOldest, trigger, DeadbandType.Percent, value,
				monitoringMode);
	}

	/** Same options with another queue. */
	public DataItemOptions queue(final int size, final boolean discardOldestValue) {
		return new DataItemOptions(samplingInterval, size, discardOldestValue, trigger, deadbandType, deadbandValue,
				monitoringMode);
	}

	/** Same options with another trigger. */
	public DataItemOptions trigger(final DataChangeTrigger newTrigger) {
		return new DataItemOptions(samplingInterval, queueSize, discardOldest, newTrigger, deadbandType,
				deadbandValue, monitoringMode);
	}

	/** Same options with another monitoring mode. */
	public DataItemOptions monitoringMode(final MonitoringMode mode) {
		return new DataItemOptions(samplingInterval, queueSize, discardOldest, trigger, deadbandType, deadbandValue,
				mode);
	}

	/** @return true if a data change filter is needed */
	public boolean needsFilter() {
		return deadbandType != DeadbandType.None || trigger != DataChangeTrigger.StatusValue;
	}
}
