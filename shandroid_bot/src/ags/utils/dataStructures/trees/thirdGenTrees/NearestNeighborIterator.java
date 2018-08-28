package ags.utils.dataStructures.trees.thirdGenTrees;

import java.util.Arrays;
import java.util.Iterator;

import ags.utils.dataStructures.IntervalHeap;
import ags.utils.dataStructures.MinBinaryHeap;
import ags.utils.dataStructures.MinHeap;

public class NearestNeighborIterator<T> implements Iterator<T>, Iterable<T>
{
	private DistanceFunction distanceFunction;
	private double[] searchPoint;
	private MinHeap<KdNode<T>> pendingPaths;
	private IntervalHeap<T> evaluatedPoints;
	private int pointsRemaining;
	private double lastDistanceReturned;

	protected NearestNeighborIterator(KdNode<T> treeRoot, double[] searchPoint, int maxPointsReturned,
			DistanceFunction distanceFunction)
	{
		this.searchPoint = Arrays.copyOf(searchPoint, searchPoint.length);
		this.pointsRemaining = Math.min(maxPointsReturned, treeRoot.size());
		this.distanceFunction = distanceFunction;
		this.pendingPaths = new MinBinaryHeap<>();
		this.pendingPaths.offer(0, treeRoot);
		this.evaluatedPoints = new IntervalHeap<>();
	}

	/* -------- INTERFACE IMPLEMENTATION -------- */

	@Override
	public boolean hasNext()
	{
		return this.pointsRemaining > 0;
	}

	@Override
	public T next()
	{
		if (!this.hasNext())
		{
			throw new IllegalStateException("NearestNeighborIterator has reached end!");
		}

		while (this.pendingPaths.size() > 0
				&& (this.evaluatedPoints.size() == 0 || (this.pendingPaths.getMinKey() < this.evaluatedPoints.getMinKey())))
		{
			KdTree.nearestNeighborSearchStep(this.pendingPaths, this.evaluatedPoints, this.pointsRemaining,
					this.distanceFunction, this.searchPoint);
		}

		// Return the smallest distance point
		this.pointsRemaining--;
		this.lastDistanceReturned = this.evaluatedPoints.getMinKey();
		T value = this.evaluatedPoints.getMin();
		this.evaluatedPoints.removeMin();
		return value;
	}

	public double distance()
	{
		return this.lastDistanceReturned;
	}

	@Override
	public void remove()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterator<T> iterator()
	{
		return this;
	}

	/**
	 * @return the distanceFunction
	 */
	public DistanceFunction getDistanceFunction()
	{
		return this.distanceFunction;
	}

	/**
	 * @param distanceFunction
	 *            the distanceFunction to set
	 */
	public void setDistanceFunction(DistanceFunction distanceFunction)
	{
		this.distanceFunction = distanceFunction;
	}

	/**
	 * @return the searchPoint
	 */
	public double[] getSearchPoint()
	{
		return this.searchPoint;
	}

	/**
	 * @param searchPoint
	 *            the searchPoint to set
	 */
	public void setSearchPoint(double[] searchPoint)
	{
		this.searchPoint = searchPoint;
	}

	/**
	 * @return the pendingPaths
	 */
	public MinHeap<KdNode<T>> getPendingPaths()
	{
		return this.pendingPaths;
	}

	/**
	 * @param pendingPaths
	 *            the pendingPaths to set
	 */
	public void setPendingPaths(MinHeap<KdNode<T>> pendingPaths)
	{
		this.pendingPaths = pendingPaths;
	}

	/**
	 * @return the evaluatedPoints
	 */
	public IntervalHeap<T> getEvaluatedPoints()
	{
		return this.evaluatedPoints;
	}

	/**
	 * @param evaluatedPoints
	 *            the evaluatedPoints to set
	 */
	public void setEvaluatedPoints(IntervalHeap<T> evaluatedPoints)
	{
		this.evaluatedPoints = evaluatedPoints;
	}

	/**
	 * @return the pointsRemaining
	 */
	public int getPointsRemaining()
	{
		return this.pointsRemaining;
	}

	/**
	 * @param pointsRemaining
	 *            the pointsRemaining to set
	 */
	public void setPointsRemaining(int pointsRemaining)
	{
		this.pointsRemaining = pointsRemaining;
	}

	/**
	 * @return the lastDistanceReturned
	 */
	public double getLastDistanceReturned()
	{
		return this.lastDistanceReturned;
	}

	/**
	 * @param lastDistanceReturned
	 *            the lastDistanceReturned to set
	 */
	public void setLastDistanceReturned(double lastDistanceReturned)
	{
		this.lastDistanceReturned = lastDistanceReturned;
	}
}