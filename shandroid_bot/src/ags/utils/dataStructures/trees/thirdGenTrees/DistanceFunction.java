package ags.utils.dataStructures.trees.thirdGenTrees;

public interface DistanceFunction
{
	double distance(double[] p1, double[] p2);

	double distanceToRect(double[] point, double[] min, double[] max);
}