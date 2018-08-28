package ags.utils.dataStructures.trees.thirdGenTrees;

import java.util.Arrays;

class KdNode<T>
{
	// All types
	private int dimensions;
	private int bucketCapacity;
	private int size;

	// Leaf only
	private double[][] points;
	private Object[] data;

	// Stem only
	private KdNode<T> left, right;
	private int splitDimension;
	private double splitValue;

	// Bounds
	private double[] minBound, maxBound;
	private boolean singlePoint;

	KdNode(int dimensions, int bucketCapacity)
	{
		// Init base
		this.dimensions = dimensions;
		this.bucketCapacity = bucketCapacity;
		this.size = 0;
		this.singlePoint = true;

		// Init leaf elements
		this.points = new double[bucketCapacity + 1][];
		this.data = new Object[bucketCapacity + 1];
	}

	/* -------- SIMPLE GETTERS -------- */

	public int size()
	{
		return this.size;
	}

	boolean isLeaf()
	{
		return this.points != null;
	}

	/* -------- OPERATIONS -------- */

	public void addPoint(double[] point, T value)
	{
		KdNode<T> cursor = this;
		while (!cursor.isLeaf())
		{
			cursor.extendBounds(point);
			cursor.size++;
			if (point[cursor.splitDimension] > cursor.splitValue)
			{
				cursor = cursor.right;
			}
			else
			{
				cursor = cursor.left;
			}
		}
		cursor.addLeafPoint(point, value);
	}

	/* -------- INTERNAL OPERATIONS -------- */

	private void addLeafPoint(double[] point, T value)
	{
		// Add the data point
		this.points[this.size] = point;
		this.data[this.size] = value;
		this.extendBounds(point);
		this.size++;

		if (this.size == this.points.length - 1)
		{
			// If the node is getting too large
			if (this.calculateSplit())
			{
				// If the node successfully had it's split value calculated,
				// split node
				this.splitLeafNode();
			}
			else
			{
				// If the node could not be split, enlarge node
				this.increaseLeafCapacity();
			}
		}
	}

	private void extendBounds(double[] point)
	{
		if (this.minBound == null)
		{
			this.minBound = Arrays.copyOf(point, this.dimensions);
			this.maxBound = Arrays.copyOf(point, this.dimensions);
			return;
		}

		for (int i = 0; i < this.dimensions; i++)
		{
			if (Double.isNaN(point[i]))
			{
				if (!Double.isNaN(this.minBound[i]) || !Double.isNaN(this.maxBound[i]))
				{
					this.singlePoint = false;
				}
				this.minBound[i] = Double.NaN;
				this.maxBound[i] = Double.NaN;
			}
			else if (this.minBound[i] > point[i])
			{
				this.minBound[i] = point[i];
				this.singlePoint = false;
			}
			else if (this.maxBound[i] < point[i])
			{
				this.maxBound[i] = point[i];
				this.singlePoint = false;
			}
		}
	}

	private void increaseLeafCapacity()
	{
		this.points = Arrays.copyOf(this.points, this.points.length * 2);
		this.data = Arrays.copyOf(this.data, this.data.length * 2);
	}

	private boolean calculateSplit()
	{
		if (this.singlePoint)
			return false;

		double width = 0;
		for (int i = 0; i < this.dimensions; i++)
		{
			double dwidth = (this.maxBound[i] - this.minBound[i]);
			if (Double.isNaN(dwidth))
				dwidth = 0;
			if (dwidth > width)
			{
				this.splitDimension = i;
				width = dwidth;
			}
		}

		if (width == 0)
		{
			return false;
		}

		// Start the split in the middle of the variance
		this.splitValue = (this.minBound[this.splitDimension] + this.maxBound[this.splitDimension]) * 0.5;

		// Never split on infinity or NaN
		if (this.splitValue == Double.POSITIVE_INFINITY)
		{
			this.splitValue = Double.MAX_VALUE;
		}
		else if (this.splitValue == Double.NEGATIVE_INFINITY)
		{
			this.splitValue = -Double.MAX_VALUE;
		}

		// Don't let the split value be the same as the upper value as
		// can happen due to rounding errors!
		if (this.splitValue == this.maxBound[this.splitDimension])
		{
			this.splitValue = this.minBound[this.splitDimension];
		}

		// Success
		return true;
	}

	@SuppressWarnings("unchecked")
	private void splitLeafNode()
	{
		this.right = new KdNode<>(this.dimensions, this.bucketCapacity);
		this.left = new KdNode<>(this.dimensions, this.bucketCapacity);

		// Move locations into children
		for (int i = 0; i < this.size; i++)
		{
			double[] oldLocation = this.points[i];
			Object oldData = this.data[i];
			if (oldLocation[this.splitDimension] > this.splitValue)
			{
				this.right.addLeafPoint(oldLocation, (T) oldData);
			}
			else
			{
				this.left.addLeafPoint(oldLocation, (T) oldData);
			}
		}

		this.points = null;
		this.data = null;
	}

	/**
	 * @return the dimensions
	 */
	public int getDimensions()
	{
		return this.dimensions;
	}

	/**
	 * @param dimensions
	 *            the dimensions to set
	 */
	public void setDimensions(int dimensions)
	{
		this.dimensions = dimensions;
	}

	/**
	 * @return the bucketCapacity
	 */
	public int getBucketCapacity()
	{
		return this.bucketCapacity;
	}

	/**
	 * @param bucketCapacity
	 *            the bucketCapacity to set
	 */
	public void setBucketCapacity(int bucketCapacity)
	{
		this.bucketCapacity = bucketCapacity;
	}

	/**
	 * @return the size
	 */
	public int getSize()
	{
		return this.size;
	}

	/**
	 * @param size
	 *            the size to set
	 */
	public void setSize(int size)
	{
		this.size = size;
	}

	/**
	 * @return the points
	 */
	public double[][] getPoints()
	{
		return this.points;
	}

	/**
	 * @param points
	 *            the points to set
	 */
	public void setPoints(double[][] points)
	{
		this.points = points;
	}

	/**
	 * @return the data
	 */
	public Object[] getData()
	{
		return this.data;
	}

	/**
	 * @param data
	 *            the data to set
	 */
	public void setData(Object[] data)
	{
		this.data = data;
	}

	/**
	 * @return the left
	 */
	public KdNode<T> getLeft()
	{
		return this.left;
	}

	/**
	 * @param left
	 *            the left to set
	 */
	public void setLeft(KdNode<T> left)
	{
		this.left = left;
	}

	/**
	 * @return the right
	 */
	public KdNode<T> getRight()
	{
		return this.right;
	}

	/**
	 * @param right
	 *            the right to set
	 */
	public void setRight(KdNode<T> right)
	{
		this.right = right;
	}

	/**
	 * @return the splitDimension
	 */
	public int getSplitDimension()
	{
		return this.splitDimension;
	}

	/**
	 * @param splitDimension
	 *            the splitDimension to set
	 */
	public void setSplitDimension(int splitDimension)
	{
		this.splitDimension = splitDimension;
	}

	/**
	 * @return the splitValue
	 */
	public double getSplitValue()
	{
		return this.splitValue;
	}

	/**
	 * @param splitValue
	 *            the splitValue to set
	 */
	public void setSplitValue(double splitValue)
	{
		this.splitValue = splitValue;
	}

	/**
	 * @return the minBound
	 */
	public double[] getMinBound()
	{
		return this.minBound;
	}

	/**
	 * @param minBound
	 *            the minBound to set
	 */
	public void setMinBound(double[] minBound)
	{
		this.minBound = minBound;
	}

	/**
	 * @return the maxBound
	 */
	public double[] getMaxBound()
	{
		return this.maxBound;
	}

	/**
	 * @param maxBound
	 *            the maxBound to set
	 */
	public void setMaxBound(double[] maxBound)
	{
		this.maxBound = maxBound;
	}

	/**
	 * @return the singlePoint
	 */
	public boolean isSinglePoint()
	{
		return this.singlePoint;
	}

	/**
	 * @param singlePoint
	 *            the singlePoint to set
	 */
	public void setSinglePoint(boolean singlePoint)
	{
		this.singlePoint = singlePoint;
	}
}