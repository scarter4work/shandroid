package voidious.utils;

import ags.utils.dataStructures.Entry;
import ags.utils.dataStructures.WeightedSqrEuclid;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Copyright (c) 2009-2012 - Voidious
 * 
 * This software is provided 'as-is', without any express or implied warranty.
 * In no event will the authors be held liable for any damages arising from the
 * use of this software.
 * 
 * Permission is granted to anyone to use this software for any purpose,
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 * 
 * 1. The origin of this software must not be misrepresented; you must not claim
 * that you wrote the original software.
 * 
 * 2. Altered source versions must be plainly marked as such, and must not be
 * misrepresented as being the original software.
 * 
 * 3. This notice may not be removed or altered from any source distribution.
 */

public class KnnView<T>
{
	private static int NAME_INDEX = 0;

	public double weight;
	private DistanceFormula formula;
	private int kSize;
	public int kDivisor;
	private int maxDataPoints;
	public boolean logBulletHits;
	public boolean logVisits;
	public boolean logVirtual;
	public boolean logMelee;
	private double hitThreshold;
	private double paddedHitThreshold;
	private double decayRate;
	public String name;
	private WeightedSqrEuclid<T> tree;
	public Map<Integer, List<Entry<T>>> cachedNeighbors;

	private static final double NO_DECAY = 0;

	public KnnView(DistanceFormula formula)
	{
		this.formula = formula;
		this.weight = 1;
		this.kSize = 1;
		this.kDivisor = 1;
		this.logBulletHits = false;
		this.logVisits = false;
		this.logVirtual = false;
		this.logMelee = false;
		this.hitThreshold = 0;
		this.paddedHitThreshold = 0;
		this.maxDataPoints = 0;
		this.decayRate = NO_DECAY;
		this.name = (new Long(Math.round(Math.random() * 10000000))).toString() + "-" + NAME_INDEX++;
		this.initTree();
		this.cachedNeighbors = new HashMap<>();
	}

	private void initTree()
	{
		this.tree = new WeightedSqrEuclid<>(this.formula.weights.length, this.maxDataPoints == 0 ? null : this.maxDataPoints);
		this.tree.setWeights(this.formula.weights);
	}

	public KnnView<T> setWeight(double weight)
	{
		this.weight = weight;
		return this;
	}

	public KnnView<T> setK(int kSize)
	{
		this.kSize = kSize;
		return this;
	}

	public KnnView<T> setKDivisor(int kDivisor)
	{
		this.kDivisor = kDivisor;
		return this;
	}

	public KnnView<T> bulletHitsOn()
	{
		this.logBulletHits = true;
		return this;
	}

	public KnnView<T> visitsOn()
	{
		this.logVisits = true;
		return this;
	}

	public KnnView<T> virtualWavesOn()
	{
		this.logVirtual = true;
		return this;
	}

	public KnnView<T> meleeOn()
	{
		this.logMelee = true;
		return this;
	}

	public KnnView<T> setHitThreshold(double hitThreshold)
	{
		this.hitThreshold = hitThreshold;
		return this;
	}

	public KnnView<T> setPaddedHitThreshold(double paddedHitThreshold)
	{
		this.paddedHitThreshold = paddedHitThreshold;
		return this;
	}

	public KnnView<T> setMaxDataPoints(int maxDataPoints)
	{
		this.maxDataPoints = maxDataPoints;
		this.initTree();
		return this;
	}

	public KnnView<T> setDecayRate(double decayRate)
	{
		this.decayRate = decayRate;
		return this;
	}

	public KnnView<T> setName(String name)
	{
		this.name = name;
		return this;
	}

	public double[] logWave(Wave w, T value)
	{
		double[] dataPoint = this.formula.dataPointFromWave(w);
		return this.logDataPoint(dataPoint, value);
	}

	private double[] logDataPoint(double[] dataPoint, T value)
	{
		this.tree.addPoint(dataPoint, value);
		return dataPoint;
	}

	public void clearCache()
	{
		this.cachedNeighbors.clear();
	}

	public boolean enabled(double hitPercentage, double marginOfError)
	{
		return (this.size() > 0 && (hitPercentage >= this.hitThreshold) && (Math.max(0, hitPercentage - marginOfError) >= this.paddedHitThreshold));
	}

	public int size()
	{
		return this.tree.size();
	}

	public List<Entry<T>> nearestNeighbors(Wave w, boolean aiming)
	{
		return this.nearestNeighbors(w, aiming, DiaUtils.limit(1, this.size() / this.kDivisor, this.kSize));
	}

	public List<Entry<T>> nearestNeighbors(Wave w, boolean aiming, int k)
	{
		double[] wavePoint = this.formula.dataPointFromWave(w, aiming);
		return this.tree.nearestNeighbor(wavePoint, k, false);
	}

	public void setWeights(double[] weights)
	{
		this.formula.weights = weights;
		this.tree.setWeights(weights);
	}

	public Map<Timestamped, Double> getDecayWeights(List<? extends Entry<? extends Timestamped>> timestampedEntries)
	{
		Map<Timestamped, Double> weightMap = new HashMap<>();
		int numScans = timestampedEntries.size();
		if (this.decayRate == KnnView.NO_DECAY)
		{
			for (Entry<? extends Timestamped> entry : timestampedEntries)
			{
				weightMap.put(entry.getValue(), 1.0);
			}
		}
		else
		{
			Timestamped[] sorted = new Timestamped[numScans];
			for (int x = 0; x < numScans; x++)
			{
				sorted[x] = timestampedEntries.get(x).getValue();
			}
			Arrays.sort(sorted);
			for (int x = 0; x < numScans; x++)
			{
				weightMap.put(sorted[x], 1.0 / DiaUtils.power(this.decayRate, numScans - x - 1));
			}
		}
		return weightMap;
	}
}
