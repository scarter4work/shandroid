package ags.utils.dataStructures;

import ags.utils.KdTree;

/**
 * Class for tree with Unweighted Squared Euclidean distancing
 */
public class SqrEuclid<T> extends KdTree<T>
{
	public SqrEuclid(int dimensions, Integer sizeLimit)
	{
		super(dimensions, sizeLimit);
	}

	@Override
	protected double pointDist(double[] p1, double[] p2)
	{
		double d = 0;

		for (int i = 0; i < p1.length; i++)
		{
			double diff = (p1[i] - p2[i]);
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
				diff = (point[i] - max[i]);
			}
			else if (point[i] < min[i])
			{
				diff = (point[i] - min[i]);
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
		throw new IllegalStateException();
	}
}