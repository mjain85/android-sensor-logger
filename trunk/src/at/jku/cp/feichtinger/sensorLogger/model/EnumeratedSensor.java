package at.jku.cp.feichtinger.sensorLogger.model;

import java.io.Serializable;

import android.hardware.Sensor;

public enum EnumeratedSensor implements Serializable {
	GRAVITY(Sensor.TYPE_GRAVITY, "gravity"), LINEAR_ACCELERATION(Sensor.TYPE_LINEAR_ACCELERATION, "linear_acceleration"), GYROSCOPE(
			Sensor.TYPE_GYROSCOPE, "gyroscope");

	private final int sensorId;
	private final String key;

	private EnumeratedSensor(final int sensorId, final String key) {
		this.sensorId = sensorId;
		this.key = key;
	}

	public int getSensorId() {
		return sensorId;
	}

	public String getKey() {
		return key;
	}

	/**
	 * Returns the sensor for a certain key if it is supported by this
	 * application.
	 */
	public static EnumeratedSensor fromKey(final String key) {
		final EnumeratedSensor[] values = EnumeratedSensor.values();
		for (final EnumeratedSensor s : values) {
			if (s.getKey().equals(key)) {
				return s;
			}
		}
		throw new RuntimeException("A sensor with key: '" + key + "' is not supported.");
	}

	/**
	 * Returns the sensor for a certain id if it is supported by this
	 * application.
	 */
	public static EnumeratedSensor fromId(final int id) {
		final EnumeratedSensor[] values = EnumeratedSensor.values();
		for (final EnumeratedSensor s : values) {
			if (s.getSensorId() == id) {
				return s;
			}
		}
		throw new RuntimeException("A sensor with id: '" + id + "' is not supported.");
	}
}
