package ags.utils.dataStructures;

import ags.utils.KdTree;

/**
 * Class for tree with Manhattan distancing
 */
public class Manhattan<T> extends KdTree<T>
{
	public Manhattan(int dimensions, Integer sizeLimit)
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
				d += (diff < 0) ? -diff : diff;
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
				diff = (min[i] - point[i]);
			}

			if (!Double.isNaN(diff))
			{
				d += diff;
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