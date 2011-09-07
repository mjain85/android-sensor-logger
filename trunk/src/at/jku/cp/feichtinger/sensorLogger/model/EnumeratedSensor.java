package at.jku.cp.feichtinger.sensorLogger.model;

import java.io.Serializable;

import android.hardware.Sensor;
import android.os.Parcel;
import android.os.Parcelable;

public enum EnumeratedSensor implements Serializable {
	GRAVITY(Sensor.TYPE_GRAVITY, "gravity"), LINEAR_ACCELERATION(Sensor.TYPE_LINEAR_ACCELERATION, "linear_acceleration");

	private final int sensorId;
	private final String key;

	private EnumeratedSensor(int sensorId, String key) {
		this.sensorId = sensorId;
		this.key = key;
	}

	public int getSensorId() {
		return sensorId;
	}

	public String getKey() {
		return key;
	}

	public static EnumeratedSensor fromKey(String key) {
		if (key.equals(GRAVITY.getKey()))
			return GRAVITY;
		else if (key.equals(LINEAR_ACCELERATION.getKey()))
			return LINEAR_ACCELERATION;

		return null;
	}

	public static EnumeratedSensor fromId(int id) {
		if (id == Sensor.TYPE_GRAVITY)
			return GRAVITY;
		else if (id == Sensor.TYPE_LINEAR_ACCELERATION)
			return LINEAR_ACCELERATION;

		return null;
	}
}
