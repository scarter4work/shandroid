package ags.utils.dataStructures.trees.thirdGenTrees;

import ags.utils.dataStructures.MaxBinaryHeap;
import ags.utils.dataStructures.MaxHeap;
import ags.utils.dataStructures.MinBinaryHeap;
import ags.utils.dataStructures.MinHeap;

public class KdTree<T> extends KdNode<T>
{
	public KdTree(int dimensions)
	{
		this(dimensions, 24);
	}

	private KdTree(int dimensions, int bucketCapacity)
	{
		super(dimensions, bucketCapacity);
	}

	public MaxHeap<T> findNearestNeighbors(double[] searchPoint, int maxPointsReturned, DistanceFunction distanceFunction)
	{
		MinBinaryHeap<KdNode<T>> pendingPaths = new MinBinaryHeap<>();
		MaxBinaryHeap<T> evaluatedPoints = new MaxBinaryHeap<>();
		int pointsRemaining = Math.min(maxPointsReturned, this.size());
		pendingPaths.offer(0, this);

		while (pendingPaths.size() > 0
				&& (evaluatedPoints.size() < pointsRemaining || (pendingPaths.getMinKey() < evaluatedPoints.getMaxKey())))
		{
			nearestNeighborSearchStep(pendingPaths, evaluatedPoints, pointsRemaining, distanceFunction, searchPoint);
		}

		return evaluatedPoints;
	}

	static <T> void nearestNeighborSearchStep(MinHeap<KdNode<T>> pendingPaths, MaxHeap<T> evaluatedPoints,
			int desiredPoints, DistanceFunction distanceFunction, double[] searchPoint)
	{
		// If there are pending paths possibly closer than the nearest evaluated
		// point, check it out
		KdNode<T> cursor = pendingPaths.getMin();
		pendingPaths.removeMin();

		// Descend the tree, recording paths not taken
		while (!cursor.isLeaf())
		{
			KdNode<T> pathNotTaken;
			if (searchPoint[cursor.getSplitDimension()] > cursor.getSplitValue())
			{
				pathNotTaken = cursor.getLeft();
				cursor = cursor.getRight();
			}
			else
			{
				pathNotTaken = cursor.getRight();
				cursor = cursor.getLeft();
			}
			double otherDistance = distanceFunction.distanceToRect(searchPoint, pathNotTaken.getMinBound(),
					pathNotTaken.getMaxBound());
			// Only add a path if we either need more points or it's closer than
			// furthest point on list so far
			if (evaluatedPoints.size() < desiredPoints || otherDistance <= evaluatedPoints.getMaxKey())
			{
				pendingPaths.offer(otherDistance, pathNotTaken);
			}
		}

		if (cursor.isSinglePoint())
		{
			double nodeDistance = distanceFunction.distance(cursor.getPoints()[0], searchPoint);
			// Only add a point if either need more points or it's closer than
			// furthest on list so far
			if (evaluatedPoints.size() < desiredPoints || nodeDistance <= evaluatedPoints.getMaxKey())
			{
				for (int i = 0; i < cursor.size(); i++)
				{
					T value = (T) cursor.getData()[i];

					// If we don't need any more, replace max
					if (evaluatedPoints.size() == desiredPoints)
					{
						evaluatedPoints.replaceMax(nodeDistance, value);
					}
					else
					{
						evaluatedPoints.offer(nodeDistance, value);
					}
				}
			}
		}
		else
		{
			// Add the points at the cursor
			for (int i = 0; i < cursor.size(); i++)
			{
				double[] point = cursor.getPoints()[i];
				T value = (T) cursor.getData()[i];
				double distance = distanceFunction.distance(point, searchPoint);
				// Only add a point if either need more points or it's closer
				// than furthest on list so far
				if (evaluatedPoints.size() < desiredPoints)
				{
					evaluatedPoints.offer(distance, value);
				}
				else if (distance < evaluatedPoints.getMaxKey())
				{
					evaluatedPoints.replaceMax(distance, value);
				}
			}
		}
	}
}