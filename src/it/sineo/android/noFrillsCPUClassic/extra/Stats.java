package it.sineo.android.noFrillsCPUClassic.extra;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.graphics.Color;

public class Stats implements Serializable, Comparator<Frequency> {

	private final static long serialVersionUID = 2L;

	private List<Frequency> frequencies;
	private Map<String, Long> times;
	private Long totalTime;
	/**
	 * Could be null throughout the life-cycle of this object.
	 */
	private Stats previousStats;
	private Stats zeroPoint;

	/**
	 * Auxiliary.
	 */
	private List<Frequency> sortedByFrequency;
	/*
	 * Used to reduce CPU usage as the stats will not change throughout the
	 * lifetime of the object. They are set-up in the sort() function, as it has
	 * to be called after previousState or zeroPoint have been set.
	 */
	private transient Map<String, Double> percentages;
	private transient Map<String, Double> partialPercentages;

	public Stats(List<Frequency> frequencies, Map<String, Long> times, Long totalTime) {
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
		if (percentages == null) {
			throw new IllegalStateException("sort() hasn't been called yet!");
		}
		return percentages.get(freq.getValue());
	}

	public void setPreviousStats(Stats previousStats) {
		this.previousStats = previousStats;
	}

	public void setZero(Stats zeroPoint) {
		this.zeroPoint = zeroPoint;
	}

	public Double getPartialPercentage(Frequency freq) {
		if (previousStats == null) {
			throw new IllegalStateException("setPreviousStats() hasn't been called yet");
		}
		if (partialPercentages == null) {
			throw new IllegalStateException("sort() hasn't been called yet!");
		}
		return partialPercentages.get(freq.getValue());
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

		if (percentages != null) {
			percentages.clear();
			percentages = null;
		}
		if (previousStats != null) {
			previousStats.clear();
			previousStats = null;

			if (partialPercentages != null) {
				partialPercentages.clear();
				partialPercentages = null;
			}
		}
		if (zeroPoint != null) {
			// Don't clear it!
			zeroPoint = null;
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

		int size = frequencies.size() + 2; // To avoid load factor exceeding.
		this.percentages = new HashMap<String, Double>(size, 1.0f);
		this.partialPercentages = new HashMap<String, Double>(size, 1.0f);

		for (Frequency freq : frequencies) {
			if (zeroPoint == null || zeroPoint.totalTime >= totalTime) {
				percentages.put(freq.getValue(), times.get(freq.getValue()) / (double) totalTime);
			} else {
				long time = times.get(freq.getValue()) - zeroPoint.times.get(freq.getValue());
				long total = totalTime - zeroPoint.totalTime;
				percentages.put(freq.getValue(), time / (double) total);
			}
			if (previousStats != null) {
				long time = times.get(freq.getValue()) - previousStats.times.get(freq.getValue());
				long total = totalTime - previousStats.totalTime;
				partialPercentages.put(freq.getValue(), time / (double) total);
			}
		}

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
