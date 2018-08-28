/**
 * Copyright 2009 Rednaxela
 * 
 * This software is provided 'as-is', without any express or implied
 * warranty. In no event will the authors be held liable for any damages
 * arising from the use of this software.
 * 
 * Permission is granted to anyone to use this software for any purpose,
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 * 
 *    1. The origin of this software must not be misrepresented; you must not
 *    claim that you wrote the original software. If you use this software
 *    in a product, an acknowledgment in the product documentation would be
 *    appreciated but is not required.
 * 
 *    2. This notice may not be removed or altered from any source
 *    distribution.
 */

package ags.utils;

import ags.utils.dataStructures.Entry;
import ags.utils.dataStructures.ResultHeap;
import ags.utils.dataStructures.ReverseResultHeap;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * An efficient well-optimized kd-tree
 * 
 * @author Rednaxela
 */

// MODIFIED by Voidious, 2011:
// - add find farthest neighbor search
// - also check cursor != null in removeOld
public abstract class KdTree<T>
{
	/**
	 * Enumeration representing the status of a node during the running
	 */
	private enum Status
	{
		NONE, LEFTVISITED, RIGHTVISITED, ALLVISITED
	}

	// Static variables
	private static final int BUCKET_SIZE = 24;

	// All types
	private int dimensions;
	private KdTree<T> parent;

	// Root only
	private LinkedList<double[]> locationStack;
	private Integer sizeLimit;

	// Leaf only
	private double[][] locations;
	private Object[] data;
	private int locationCount;

	// Stem only
	private KdTree<T> left, right;
	private int splitDimension;
	private double splitValue;

	// Bounds
	private double[] minLimit, maxLimit;
	private boolean singularity;

	// Temporary
	private Status status;

	/**
	 * Construct a KdTree with a given number of dimensions and a limit on
	 * maxiumum size (after which it throws away old points)
	 */
	public KdTree(int dimensions, Integer sizeLimit)
	{
		this.dimensions = dimensions;

		// Init as leaf
		this.locations = new double[BUCKET_SIZE][];
		this.data = new Object[BUCKET_SIZE];
		this.locationCount = 0;
		this.singularity = true;

		// Init as root
		this.parent = null;
		this.sizeLimit = sizeLimit;
		if (sizeLimit != null)
		{
			this.locationStack = new LinkedList<>();
		}
		else
		{
			this.locationStack = null;
		}
	}

	/**
	 * Constructor for child nodes. Internal use only.
	 */
	private KdTree(KdTree<T> parent)
	{
		this.dimensions = parent.dimensions;

		// Init as leaf
		this.locations = new double[Math.max(BUCKET_SIZE, parent.locationCount)][];
		this.data = new Object[Math.max(BUCKET_SIZE, parent.locationCount)];
		this.locationCount = 0;
		this.singularity = true;

		// Init as non-root
		this.parent = parent;
		this.locationStack = null;
		this.sizeLimit = null;
	}

	// Override in subclasses
	protected abstract double pointDist(double[] p1, double[] p2);

	protected abstract double pointRegionDist(double[] point, double[] min, double[] max);

	protected abstract double pointRegionMaxDist(double[] point, double[] min, double[] max);

	protected double getAxisWeightHint(int i)
	{
		return 1.0;
	}

	/**
	 * Get the number of points in the tree
	 */
	public int size()
	{
		return this.locationCount;
	}

	/**
	 * Add a point and associated value to the tree
	 */
	public void addPoint(double[] location, T value)
	{
		KdTree<T> cursor = this;

		while (cursor.locations == null || cursor.locationCount >= cursor.locations.length)
		{
			if (cursor.locations != null)
			{
				cursor.splitDimension = cursor.findWidestAxis();
				cursor.splitValue = (cursor.minLimit[cursor.splitDimension] + cursor.maxLimit[cursor.splitDimension]) * 0.5;

				// Never split on infinity or NaN
				if (cursor.splitValue == Double.POSITIVE_INFINITY)
				{
					cursor.splitValue = Double.MAX_VALUE;
				}
				else if (cursor.splitValue == Double.NEGATIVE_INFINITY)
				{
					cursor.splitValue = -Double.MAX_VALUE;
				}
				else if (Double.isNaN(cursor.splitValue))
				{
					cursor.splitValue = 0;
				}

				// Don't split node if it has no width in any axis. Double the
				// bucket size instead
				if (cursor.minLimit[cursor.splitDimension] == cursor.maxLimit[cursor.splitDimension])
				{
					double[][] newLocations = new double[cursor.locations.length * 2][];
					System.arraycopy(cursor.locations, 0, newLocations, 0, cursor.locationCount);
					cursor.locations = newLocations;
					Object[] newData = new Object[newLocations.length];
					System.arraycopy(cursor.data, 0, newData, 0, cursor.locationCount);
					cursor.data = newData;
					break;
				}

				// Don't let the split value be the same as the upper value as
				// can happen due to rounding errors!
				if (cursor.splitValue == cursor.maxLimit[cursor.splitDimension])
				{
					cursor.splitValue = cursor.minLimit[cursor.splitDimension];
				}

				// Create child leaves
				KdTree<T> leftLeaf = new ChildNode(cursor, false);
				KdTree<T> rightLeaf = new ChildNode(cursor, true);

				// Move locations into children
				for (int i = 0; i < cursor.locationCount; i++)
				{
					double[] oldLocation = cursor.locations[i];
					Object oldData = cursor.data[i];
					if (oldLocation[cursor.splitDimension] > cursor.splitValue)
					{
						// Right
						rightLeaf.locations[rightLeaf.locationCount] = oldLocation;
						rightLeaf.data[rightLeaf.locationCount] = oldData;
						rightLeaf.locationCount++;
						rightLeaf.extendBounds(oldLocation);
					}
					else
					{
						// Left
						leftLeaf.locations[leftLeaf.locationCount] = oldLocation;
						leftLeaf.data[leftLeaf.locationCount] = oldData;
						leftLeaf.locationCount++;
						leftLeaf.extendBounds(oldLocation);
					}
				}

				// Make into stem
				cursor.left = leftLeaf;
				cursor.right = rightLeaf;
				cursor.locations = null;
				cursor.data = null;
			}

			cursor.locationCount++;
			cursor.extendBounds(location);

			if (location[cursor.splitDimension] > cursor.splitValue)
			{
				cursor = cursor.right;
			}
			else
			{
				cursor = cursor.left;
			}
		}

		cursor.locations[cursor.locationCount] = location;
		cursor.data[cursor.locationCount] = value;
		cursor.locationCount++;
		cursor.extendBounds(location);

		if (this.sizeLimit != null)
		{
			this.locationStack.add(location);
			if (this.locationCount > this.sizeLimit)
			{
				this.removeOld();
			}
		}
	}

	/**
	 * Extends the bounds of this node do include a new location
	 */
	private void extendBounds(double[] location)
	{
		if (this.minLimit == null)
		{
			this.minLimit = new double[this.dimensions];
			System.arraycopy(location, 0, this.minLimit, 0, this.dimensions);
			this.maxLimit = new double[this.dimensions];
			System.arraycopy(location, 0, this.maxLimit, 0, this.dimensions);
			return;
		}

		for (int i = 0; i < this.dimensions; i++)
		{
			if (Double.isNaN(location[i]))
			{
				this.minLimit[i] = Double.NaN;
				this.maxLimit[i] = Double.NaN;
				this.singularity = false;
			}
			else if (this.minLimit[i] > location[i])
			{
				this.minLimit[i] = location[i];
				this.singularity = false;
			}
			else if (this.maxLimit[i] < location[i])
			{
				this.maxLimit[i] = location[i];
				this.singularity = false;
			}
		}
	}

	/**
	 * Find the widest axis of the bounds of this node
	 */
	private int findWidestAxis()
	{
		int widest = 0;
		double width = (this.maxLimit[0] - this.minLimit[0]) * this.getAxisWeightHint(0);
		if (Double.isNaN(width))
			width = 0;
		for (int i = 1; i < this.dimensions; i++)
		{
			double nwidth = (this.maxLimit[i] - this.minLimit[i]) * this.getAxisWeightHint(i);
			if (Double.isNaN(nwidth))
				nwidth = 0;
			if (nwidth > width)
			{
				widest = i;
				width = nwidth;
			}
		}
		return widest;
	}

	/**
	 * Remove the oldest value from the tree. Note: This cannot trim the bounds
	 * of nodes, nor empty nodes, and thus you can't expect it to perfectly
	 * preserve the speed of the tree as you keep adding.
	 */
	private void removeOld()
	{
		double[] location = this.locationStack.removeFirst();
		KdTree<T> cursor = this;

		// Find the node where the point is
		while (cursor.locations == null)
		{
			if (location[cursor.splitDimension] > cursor.splitValue)
			{
				cursor = cursor.right;
			}
			else
			{
				cursor = cursor.left;
			}
		}

		for (int i = 0; i < cursor.locationCount; i++)
		{
			if (cursor.locations[i] == location)
			{
				System.arraycopy(cursor.locations, i + 1, cursor.locations, i, cursor.locationCount - i - 1);
				cursor.locations[cursor.locationCount - 1] = null;
				System.arraycopy(cursor.data, i + 1, cursor.data, i, cursor.locationCount - i - 1);
				cursor.data[cursor.locationCount - 1] = null;
				do
				{
					cursor.locationCount--;
					cursor = cursor.parent;
				}
				while (cursor != null && cursor.parent != null);
				return;
			}
		}
		// If we got here... we couldn't find the value to remove. Weird...
	}

	/**
	 * Calculates the nearest 'count' points to 'location'
	 */
	public List<Entry<T>> nearestNeighbor(double[] location, int count, boolean sequentialSorting)
	{
		KdTree<T> cursor = this;
		cursor.status = Status.NONE;
		double range = Double.POSITIVE_INFINITY;
		ResultHeap resultHeap = new ResultHeap(count);

		do
		{
			if (cursor.status == Status.ALLVISITED)
			{
				// At a fully visited part. Move up the tree
				cursor = cursor.parent;
				continue;
			}

			if (cursor.status == Status.NONE && cursor.locations != null)
			{
				// At a leaf. Use the data.
				if (cursor.locationCount > 0)
				{
					if (cursor.singularity)
					{
						double dist = this.pointDist(cursor.locations[0], location);
						if (dist <= range)
						{
							for (int i = 0; i < cursor.locationCount; i++)
							{
								resultHeap.addValue(dist, cursor.data[i]);
							}
						}
					}
					else
					{
						for (int i = 0; i < cursor.locationCount; i++)
						{
							double dist = this.pointDist(cursor.locations[i], location);
							resultHeap.addValue(dist, cursor.data[i]);
						}
					}
					range = resultHeap.getMaxDist();
				}

				if (cursor.parent == null)
				{
					break;
				}
				cursor = cursor.parent;
				continue;
			}

			// Going to descend
			KdTree<T> nextCursor = null;
			if (cursor.status == Status.NONE)
			{
				// At a fresh node, descend the most probably useful direction
				if (location[cursor.splitDimension] > cursor.splitValue)
				{
					// Descend right
					nextCursor = cursor.right;
					cursor.status = Status.RIGHTVISITED;
				}
				else
				{
					// Descend left;
					nextCursor = cursor.left;
					cursor.status = Status.LEFTVISITED;
				}
			}
			else if (cursor.status == Status.LEFTVISITED)
			{
				// Left node visited, descend right.
				nextCursor = cursor.right;
				cursor.status = Status.ALLVISITED;
			}
			else if (cursor.status == Status.RIGHTVISITED)
			{
				// Right node visited, descend left.
				nextCursor = cursor.left;
				cursor.status = Status.ALLVISITED;
			}

			// Check if it's worth descending. Assume it is if it's sibling has
			// not been visited yet.
			if (cursor.status == Status.ALLVISITED)
			{
				if (nextCursor != null && (nextCursor.locationCount == 0
						|| (!nextCursor.singularity && this.pointRegionDist(location, nextCursor.minLimit, nextCursor.maxLimit) > range))) {
					continue;
				}
			}

			// Descend down the tree
			cursor = nextCursor;
			if (cursor != null) {
				cursor.status = Status.NONE;
			}
		}
		while (cursor.parent != null || cursor.status != Status.ALLVISITED);

		ArrayList<Entry<T>> results = new ArrayList<>(resultHeap.getValues());
		this.addResult(results, resultHeap, sequentialSorting);


		return results;
	}

	private List<Entry<T>> addResult(List<Entry<T>> results, ResultHeap resultHeap, boolean sequentialSorting) {
		if (sequentialSorting)
		{
			while (resultHeap.getValues() > 0)
			{
				resultHeap.removeLargest();
				results.add(new Entry<>(resultHeap.getRemovedDist(), (T) resultHeap.getRemovedData()));
			}
		}
		else
		{
			for (int i = 0; i < resultHeap.getValues(); i++)
			{
				results.add(new Entry<>(resultHeap.getDistance()[i], (T) resultHeap.getData()[i]));
			}
		}

		return results;
	}

	/**
	 * Internal class for child nodes
	 */
	private class ChildNode extends KdTree<T>
	{
		ChildNode(KdTree<T> parent, boolean right)
		{
			super(parent);
		}

		// Distance measurements are always called from the root node
		@Override
		protected double pointDist(double[] p1, double[] p2)
		{
			throw new IllegalStateException();
		}

		@Override
		protected double pointRegionDist(double[] point, double[] min, double[] max)
		{
			throw new IllegalStateException();
		}

		@Override
		protected double pointRegionMaxDist(double[] point, double[] min, double[] max)
		{
			throw new IllegalStateException();
		}
	}
}