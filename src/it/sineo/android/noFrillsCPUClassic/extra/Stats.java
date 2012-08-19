package it.sineo.android.noFrillsCPUClassic.extra;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;

import android.graphics.Color;

public class Stats implements Serializable, Comparator<Frequency> {

	private final static long serialVersionUID = 1L;

	private List<Frequency> frequencies;
	private Hashtable<String, Long> times;
	private Long totalTime;
	/**
	 * Could be null throughout the life-cycle of this object.
	 */
	private Stats previousStats;
	/**
	 * Auxiliary.
	 */
	private List<Frequency> sortedByFrequency;

	public Stats(List<Frequency> frequencies, Hashtable<String, Long> times, Long totalTime) {
		this.frequencies = frequencies;
		this.sortedByFrequency = new ArrayList<Frequency>(frequencies);
		Collections.copy(sortedByFrequency, frequencies);
		sortMethod = SortMethod.Frequency;
		Collections.sort(sortedByFrequency, this);
		this.times = times;
		this.totalTime = totalTime;
	}

	public List<Frequency> getFrequencies() {
		return frequencies;
	}

	public Double getPercentage(Frequency freq) {
		return times.get(freq.getValue()) / (double) totalTime;
	}

	public void setPreviousStats(Stats previousStats) {
		this.previousStats = previousStats;
	}

	public Double getPartialPercentage(Frequency freq) {
		if (previousStats == null) {
			throw new IllegalStateException("setPreviousStats() hasn't been called yet");
		}
		long time = times.get(freq.getValue()) - previousStats.times.get(freq.getValue());
		long total = totalTime - previousStats.totalTime;
		return time / (double) total;
	}

	public int getFrequencyIndex(Frequency freq) {
		return sortedByFrequency.indexOf(freq);
	}

	/*
	 * I'm not 100% sure this is really needed.
	 */
	public void clear() {
		frequencies.clear();
		frequencies = null;
		sortedByFrequency.clear();
		sortedByFrequency = null;
		times.clear();
		times = null;
		if (previousStats != null) {
			previousStats.clear();
			previousStats = null;
		}
	}

	public enum SortMethod {
		Frequency, FrequencyDesc, Percentage, PercentageDesc, PartialPercentage, PartialPercentageDesc;
	};

	private SortMethod sortMethod;

	@Override
	public int compare(Frequency lhf, Frequency rhf) {
		if (sortMethod == SortMethod.Frequency) {
			return lhf.getMHz().compareTo(rhf.getMHz());
		} else if (sortMethod == SortMethod.FrequencyDesc) {
			return -(lhf.getMHz().compareTo(rhf.getMHz()));
		} else if (sortMethod == SortMethod.Percentage) {
			return getPercentage(lhf).compareTo(getPercentage(rhf));
		} else if (sortMethod == SortMethod.PercentageDesc) {
			return -(getPercentage(lhf).compareTo(getPercentage(rhf)));
		} else if (sortMethod == SortMethod.PartialPercentage) {
			return getPartialPercentage(lhf).compareTo(getPartialPercentage(rhf));
		} else if (sortMethod == SortMethod.PartialPercentageDesc) {
			return -(getPartialPercentage(lhf).compareTo(getPartialPercentage(rhf)));
		} else {
			/* Impossible to reach */
			return 0;
		}
	}

	public void sort(SortMethod method) {
		this.sortMethod = method;
		Collections.sort(frequencies, this);
	}

	public static int colorFromPercentage(float percentage) {
		float[] hsv = {
				(1 - percentage) * 120f, 0.9f, 0.9f,
		};
		int color = Color.HSVToColor(hsv);
		color &= 0x00FFFFFF;
		color |= 0x90000000;
		return color;
	}

}
