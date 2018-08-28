package ags.utils.dataStructures;

/**
 * Stores a distance and value to output
 */
public class Entry<T>
{
	private double distance;
	private T value;

	public Entry(double distance, T value)
	{
		this.distance = distance;
		this.value = value;
	}

	/**
	 * @return the distance
	 */
	public double getDistance()
	{
		return this.distance;
	}

	/**
	 * @param distance
	 *            the distance to set
	 */
	public void setDistance(double distance)
	{
		this.distance = distance;
	}

	/**
	 * @return the value
	 */
	public T getValue()
	{
		return this.value;
	}

	/**
	 * @param value
	 *            the value to set
	 */
	public void setValue(T value)
	{
		this.value = value;
	}
}