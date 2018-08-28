package ags.utils.dataStructures;

/**
 * Class for tracking up to 'size' closest values
 */
public class ResultHeap
{
	private Object[] data;
	private double[] distance;
	private int size;
	private int values;
	private Object removedData;
	private double removedDist;

	public ResultHeap(int size)
	{
		this.data = new Object[size];
		this.distance = new double[size];
		this.size = size;
		this.values = 0;
	}

	public void addValue(double dist, Object value)
	{
		// If there is still room in the heap
		if (this.values < this.size)
		{
			// Insert new value at the end
			this.data[this.values] = value;
			this.distance[this.values] = dist;
			this.upHeapify(this.values);
			this.values++;
		}
		// If there is no room left in the heap, and the new entry is lower
		// than the max entry
		else if (dist < this.distance[0])
		{
			// Replace the max entry with the new entry
			this.data[0] = value;
			this.distance[0] = dist;
			this.downHeapify(0);
		}
	}

	public void removeLargest()
	{
		if (this.values == 0)
		{
			throw new IllegalStateException();
		}

		this.removedData = this.data[0];
		this.removedDist = this.distance[0];
		this.values--;
		this.data[0] = this.data[this.values];
		this.distance[0] = this.distance[this.values];
		this.downHeapify(0);
	}

	protected void upHeapify(int c)
	{
		for (int p = (c - 1) / 2; c != 0 && this.distance[c] > this.distance[p]; c = p, p = (c - 1) / 2)
		{
			Object pData = this.data[p];
			double pDist = this.distance[p];
			this.data[p] = this.data[c];
			this.distance[p] = this.distance[c];
			this.data[c] = pData;
			this.distance[c] = pDist;
		}
	}

	protected void downHeapify(int p)
	{
		for (int c = p * 2 + 1; c < this.values; p = c, c = p * 2 + 1)
		{
			if (c + 1 < this.values && this.distance[c] < this.distance[c + 1])
			{
				c++;
			}
			if (this.distance[p] < this.distance[c])
			{
				// Swap the points
				Object pData = this.data[p];
				double pDist = this.distance[p];
				this.data[p] = this.data[c];
				this.distance[p] = this.distance[c];
				this.data[c] = pData;
				this.distance[c] = pDist;
			}
			else
			{
				break;
			}
		}
	}

	public double getMaxDist()
	{
		if (this.values < this.size)
		{
			return Double.POSITIVE_INFINITY;
		}
		return this.distance[0];
	}

	/**
	 * @return the values
	 */
	public int getValues()
	{
		return this.values;
	}

	/**
	 * @param values
	 *            the values to set
	 */
	public void setValues(int values)
	{
		this.values = values;
	}

	/**
	 * @return the removedData
	 */
	public Object getRemovedData()
	{
		return this.removedData;
	}

	/**
	 * @param removedData
	 *            the removedData to set
	 */
	public void setRemovedData(Object removedData)
	{
		this.removedData = removedData;
	}

	/**
	 * @return the removedDist
	 */
	public double getRemovedDist()
	{
		return this.removedDist;
	}

	/**
	 * @param removedDist
	 *            the removedDist to set
	 */
	public void setRemovedDist(double removedDist)
	{
		this.removedDist = removedDist;
	}

	/**
	 * @return the data
	 */
	public Object[] getData()
	{
		return this.data;
	}

	/**
	 * @return the distance
	 */
	public double[] getDistance()
	{
		return this.distance;
	}

	/**
	 * @return the size
	 */
	public int getSize()
	{
		return this.size;
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
	 * @param distance
	 *            the distance to set
	 */
	public void setDistance(double[] distance)
	{
		this.distance = distance;
	}

	/**
	 * @param size
	 *            the size to set
	 */
	public void setSize(int size)
	{
		this.size = size;
	}
}