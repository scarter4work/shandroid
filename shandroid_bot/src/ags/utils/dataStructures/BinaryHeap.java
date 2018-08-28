package ags.utils.dataStructures;

import java.util.Arrays;

/**
 * An implementation of an implicit binary heap. Min-heap and max-heap both
 * supported
 */
public abstract class BinaryHeap<T>
{
	static final int defaultCapacity = 64;
	private final int direction;
	private Object[] data;
	private double[] keys;
	private int capacity;
	private int size;

	BinaryHeap(int capacity, int direction)
	{
		this.direction = direction;
		this.data = new Object[capacity];
		this.keys = new double[capacity];
		this.capacity = capacity;
		this.size = 0;
	}

	public void offer(double key, T value)
	{
		// If move room is needed, double array size
		if (this.size >= this.capacity)
		{
			this.capacity *= 2;
			this.data = Arrays.copyOf(this.data, this.capacity);
			this.keys = Arrays.copyOf(this.keys, this.capacity);
		}

		// Insert new value at the end
		this.data[this.size] = value;
		this.keys[this.size] = key;
		this.siftUp(this.size);
		this.size++;
	}

	void removeTip()
	{
		if (this.size == 0)
		{
			throw new IllegalStateException();
		}

		this.size--;
		this.data[0] = this.data[this.size];
		this.keys[0] = this.keys[this.size];
		this.data[this.size] = null;
		this.siftDown(0);
	}

	void replaceTip(double key, T value)
	{
		if (this.size == 0)
		{
			throw new IllegalStateException();
		}

		this.data[0] = value;
		this.keys[0] = key;
		this.siftDown(0);
	}

	@SuppressWarnings("unchecked")
	T getTip()
	{
		if (this.size == 0)
		{
			throw new IllegalStateException();
		}

		return (T) this.data[0];
	}

	double getTipKey()
	{
		if (this.size == 0)
		{
			throw new IllegalStateException();
		}

		return this.keys[0];
	}

	private void siftUp(int c)
	{
		for (int p = (c - 1) / 2; c != 0 && this.direction * this.keys[c] > this.direction * this.keys[p]; c = p, p = (c - 1) / 2)
		{
			Object pData = this.data[p];
			double pDist = this.keys[p];
			this.data[p] = this.data[c];
			this.keys[p] = this.keys[c];
			this.data[c] = pData;
			this.keys[c] = pDist;
		}
	}

	private void siftDown(int p)
	{
		for (int c = p * 2 + 1; c < this.size; p = c, c = p * 2 + 1)
		{
			if (c + 1 < this.size && this.direction * this.keys[c] < this.direction * this.keys[c + 1])
			{
				c++;
			}
			if (this.direction * this.keys[p] < this.direction * this.keys[c])
			{
				// Swap the points
				Object pData = this.data[p];
				double pDist = this.keys[p];
				this.data[p] = this.data[c];
				this.keys[p] = this.keys[c];
				this.data[c] = pData;
				this.keys[c] = pDist;
			}
			else
			{
				break;
			}
		}
	}

	public int size()
	{
		return this.size;
	}

	public int capacity()
	{
		return this.capacity;
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
	 * @return the keys
	 */
	public double[] getKeys()
	{
		return this.keys;
	}

	/**
	 * @param keys
	 *            the keys to set
	 */
	public void setKeys(double[] keys)
	{
		this.keys = keys;
	}

	/**
	 * @return the capacity
	 */
	public int getCapacity()
	{
		return this.capacity;
	}

	/**
	 * @param capacity
	 *            the capacity to set
	 */
	public void setCapacity(int capacity)
	{
		this.capacity = capacity;
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
	 * @return the defaultcapacity
	 */
	public static int getDefaultcapacity()
	{
		return defaultCapacity;
	}

	/**
	 * @return the direction
	 */
	public int getDirection()
	{
		return this.direction;
	}
}