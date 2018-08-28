package ags.utils.dataStructures;

import java.util.Arrays;

/**
 * An implementation of an implicit binary interval heap.
 */
public class IntervalHeap<T> implements MinHeap<T>, MaxHeap<T>
{
	private static final int defaultCapacity = 64;
	private Object[] data;
	private double[] keys;
	private int capacity;
	private int size;

	public IntervalHeap()
	{
		this(defaultCapacity);
	}

	public IntervalHeap(int capacity)
	{
		this.data = new Object[capacity];
		this.keys = new double[capacity];
		this.capacity = capacity;
		this.size = 0;
	}

	@Override
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
		this.size++;
		this.data[this.size - 1] = value;
		this.keys[this.size - 1] = key;
		this.siftInsertedValueUp();
	}

	@Override
	public void removeMin()
	{
		if (this.size == 0)
		{
			throw new IllegalStateException();
		}

		this.size--;
		this.data[0] = this.data[this.size];
		this.keys[0] = this.keys[this.size];
		this.data[this.size] = null;
		this.siftDownMin(0);
	}

	@Override
	public void replaceMin(double key, T value)
	{
		if (this.size == 0)
		{
			throw new IllegalStateException();
		}

		this.data[0] = value;
		this.keys[0] = key;
		if (this.size > 1)
		{
			// Swap with pair if necessary
			if (this.keys[1] < key)
			{
				this.swap(0, 1);
			}
			this.siftDownMin(0);
		}
	}

	@Override
	public void replaceMax(double key, T value)
	{
		if (this.size == 0)
		{
			throw new IllegalStateException();
		}
		else if (this.size == 1)
		{
			this.replaceMin(key, value);
			return;
		}

		this.data[1] = value;
		this.keys[1] = key;
		// Swap with pair if necessary
		if (key < this.keys[0])
		{
			this.swap(0, 1);
		}
		this.siftDownMax(1);
	}

	@Override
	@SuppressWarnings("unchecked")
	public T getMin()
	{
		if (this.size == 0)
		{
			throw new IllegalStateException();
		}

		return (T) this.data[0];
	}

	@Override
	@SuppressWarnings("unchecked")
	public T getMax()
	{
		if (this.size == 0)
		{
			throw new IllegalStateException();
		}
		else if (this.size == 1)
		{
			return (T) this.data[0];
		}

		return (T) this.data[1];
	}

	@Override
	public double getMinKey()
	{
		if (this.size == 0)
		{
			throw new IllegalStateException();
		}

		return this.keys[0];
	}

	@Override
	public double getMaxKey()
	{
		if (this.size == 0)
		{
			throw new IllegalStateException();
		}
		else if (this.size == 1)
		{
			return this.keys[0];
		}

		return this.keys[1];
	}

	private int swap(int x, int y)
	{
		Object yData = this.data[y];
		double yDist = this.keys[y];
		this.data[y] = this.data[x];
		this.keys[y] = this.keys[x];
		this.data[x] = yData;
		this.keys[x] = yDist;
		return y;
	}

	/**
	 * Min-side (u % 2 == 0): - leftchild: 2u + 2 - rightchild: 2u + 4 - parent:
	 * (x/2-1)&~1
	 * 
	 * Max-side (u % 2 == 1): - leftchild: 2u + 1 - rightchild: 2u + 3 - parent:
	 * (x/2-1)|1
	 */
	private void siftInsertedValueUp()
	{
		int u = this.size - 1;
		if (u == 0)
		{
			// Do nothing if it's the only element!
		}
		else if (u == 1)
		{
			// If it is the second element, just sort it with it's pair
			if (this.keys[u] < this.keys[u - 1])
			{ // If less than it's pair
				this.swap(u, u - 1); // Swap with it's pair
			}
		}
		else if (u % 2 == 1)
		{
			// Already paired. Ensure pair is ordered right
			int p = (u / 2 - 1) | 1; // The larger value of the parent pair
			if (this.keys[u] < this.keys[u - 1])
			{ // If less than it's pair
				u = this.swap(u, u - 1); // Swap with it's pair
				if (this.keys[u] < this.keys[p - 1])
				{ // If smaller than smaller parent pair
					// Swap into min-heap side
					u = this.swap(u, p - 1);
					this.siftUpMin(u);
				}
			}
			else
			{
				if (this.keys[u] > this.keys[p])
				{ // If larger that larger parent pair
					// Swap into max-heap side
					u = this.swap(u, p);
					this.siftUpMax(u);
				}
			}
		}
		else
		{
			// Inserted in the lower-value slot without a partner
			int p = (u / 2 - 1) | 1; // The larger value of the parent pair
			if (this.keys[u] > this.keys[p])
			{ // If larger that larger parent pair
				// Swap into max-heap side
				u = this.swap(u, p);
				this.siftUpMax(u);
			}
			else if (this.keys[u] < this.keys[p - 1])
			{ // If smaller than smaller parent pair
				// Swap into min-heap side
				u = this.swap(u, p - 1);
				this.siftUpMin(u);
			}
		}
	}

	private void siftUpMin(int c)
	{
		// Min-side parent: (x/2-1)&~1
		for (int p = (c / 2 - 1) & ~1; p >= 0 && this.keys[c] < this.keys[p]; c = p, p = (c / 2 - 1) & ~1)
		{
			this.swap(c, p);
		}
	}

	private void siftUpMax(int c)
	{
		// Max-side parent: (x/2-1)|1
		for (int p = (c / 2 - 1) | 1; p >= 0 && this.keys[c] > this.keys[p]; c = p, p = (c / 2 - 1) | 1)
		{
			this.swap(c, p);
		}
	}

	private void siftDownMin(int p)
	{
		for (int c = p * 2 + 2; c < this.size; p = c, c = p * 2 + 2)
		{
			if (c + 2 < this.size && this.keys[c + 2] < this.keys[c])
			{
				c += 2;
			}
			if (this.keys[c] < this.keys[p])
			{
				this.swap(p, c);
				// Swap with pair if necessary
				if (c + 1 < this.size && this.keys[c + 1] < this.keys[c])
				{
					this.swap(c, c + 1);
				}
			}
			else
			{
				break;
			}
		}
	}

	private void siftDownMax(int p)
	{
		for (int c = p * 2 + 1; c <= this.size; p = c, c = p * 2 + 1)
		{
			if (c == this.size)
			{
				// If the left child only has half a pair
				if (this.keys[c - 1] > this.keys[p])
				{
					this.swap(p, c - 1);
				}
				break;
			}
			else if (c + 2 == this.size)
			{
				// If there is only room for a right child lower pair
				if (this.keys[c + 1] > this.keys[c])
				{
					if (this.keys[c + 1] > this.keys[p])
					{
						this.swap(p, c + 1);
					}
					break;
				}
			}
			else if (c + 2 < this.size)
			{
				// If there is room for a right child upper pair
				if (this.keys[c + 2] > this.keys[c])
				{
					c += 2;
				}
			}
			if (this.keys[c] > this.keys[p])
			{
				this.swap(p, c);
				// Swap with pair if necessary
				if (this.keys[c - 1] > this.keys[c])
				{
					this.swap(c, c - 1);
				}
			}
			else
			{
				break;
			}
		}
	}

	@Override
	public int size()
	{
		return this.size;
	}

	public int capacity()
	{
		return this.capacity;
	}

	@Override
	public String toString()
	{
		java.text.DecimalFormat twoPlaces = new java.text.DecimalFormat("0.00");
		StringBuffer str = new StringBuffer(IntervalHeap.class.getCanonicalName());
		str.append(", size: ").append(this.size()).append(" capacity: ").append(this.capacity());
		int i = 0, p = 2;
		while (i < this.size())
		{
			int x = 0;
			str.append("\t");
			while ((i + x) < this.size() && x < p)
			{
				str.append(twoPlaces.format(this.keys[i + x])).append(", ");
				x++;
			}
			str.append("\n");
			i += x;
			p *= 2;
		}
		return str.toString();
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
}