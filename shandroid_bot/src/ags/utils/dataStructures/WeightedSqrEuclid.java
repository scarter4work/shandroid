package ags.utils.dataStructures;

import java.util.Arrays;

import ags.utils.KdTree;

/**
 * Class for tree with Weighted Squared Euclidean distancing
 */
public class WeightedSqrEuclid<T> extends KdTree<T>
{
	private double[] weights;

	public WeightedSqrEuclid(int dimensions, Integer sizeLimit)
	{
		super(dimensions, sizeLimit);
		this.weights = new double[dimensions];
		Arrays.fill(this.weights, 1.0);
	}

	public void setWeights(double[] weights)
	{
		this.weights = weights;
	}

	@Override
	protected double getAxisWeightHint(int i)
	{
		return this.weights[i];
	}

	@Override
	protected double pointDist(double[] p1, double[] p2)
	{
		double d = 0;

		for (int i = 0; i < p1.length; i++)
		{
			double diff = (p1[i] - p2[i]) * this.weights[i];
			if (!Double.isNaN(diff))
			{
				d += diff * diff;
			}
		}

		return d;
	}

	@Override
	protected double pointRegionDist(double[] point, double[] min, double[] max)
	{
		double d = 0;

		for (int i = 0; i < point.length; i++)
		{
			double diff = 0;
			if (point[i] > max[i])
			{
				diff = (point[i] - max[i]) * this.weights[i];
			}
			else if (point[i] < min[i])
			{
				diff = (point[i] - min[i]) * this.weights[i];
			}

			if (!Double.isNaN(diff))
			{
				d += diff * diff;
			}
		}

		return d;
	}

	@Override
	protected double pointRegionMaxDist(double[] point, double[] min, double[] max)
	{
		double d = 0;

		for (int i = 0; i < point.length; i++)
		{
			double diff = Math.max(Math.abs(point[i] - min[i]), Math.abs(max[i] - point[i])) * this.weights[i];

			if (!Double.isNaN(diff))
			{
				d += diff * diff;
			}
		}

		return d;
	}
}