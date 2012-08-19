package it.sineo.android.noFrillsCPUClassic.extra;

import java.text.NumberFormat;

public class Frequency {

	/*
	 * We don't need synchronized access to it.
	 */
	private static NumberFormat nf;

	static {
		nf = NumberFormat.getNumberInstance();
		nf.setMinimumFractionDigits(1);
		nf.setMaximumFractionDigits(3);
	}

	private String value;
	private Integer mhz;

	public Frequency(String value) {
		setValue(value);
	}

	public Integer getMHz() {
		return mhz;
	}

	public String toString() {
		if (mhz < 1000) {
			return mhz + " kHz";
		} else if (mhz < (1000 * 1000)) {
			return (mhz / 1000) + " MHz";
		} else {
			/*
			 * "crude" GHz aren't pretty.
			 */
			return nf.format(mhz / (double) (1000 * 1000)) + " GHz";
		}
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
		this.mhz = Integer.parseInt(value);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Frequency other = (Frequency) obj;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}
}
