package voidious.utils;

public class Intersection
{
	private double angle;
	private double bandwidth;

	Intersection(double angle, double bandwidth)
	{
		this.angle = angle;
		this.bandwidth = bandwidth;
	}

	/**
	 * @return the angle
	 */
	public double getAngle()
	{
		return this.angle;
	}

	/**
	 * @param angle
	 *            the angle to set
	 */
	public void setAngle(double angle)
	{
		this.angle = angle;
	}

	/**
	 * @return the bandwidth
	 */
	public double getBandwidth()
	{
		return this.bandwidth;
	}

	/**
	 * @param bandwidth
	 *            the bandwidth to set
	 */
	public void setBandwidth(double bandwidth)
	{
		this.bandwidth = bandwidth;
	}
}