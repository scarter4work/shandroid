package voidious.move;

class WallHitDamage
{
	private double min;
	private double max;

	WallHitDamage(double min, double max)
	{
		this.min = min;
		this.max = max;
	}

	/**
	 * @return the min
	 */
	public double getMin()
	{
		return this.min;
	}

	/**
	 * @param min
	 *            the min to set
	 */
	public void setMin(double min)
	{
		this.min = min;
	}

	/**
	 * @return the max
	 */
	public double getMax()
	{
		return this.max;
	}

	/**
	 * @param max
	 *            the max to set
	 */
	public void setMax(double max)
	{
		this.max = max;
	}
}