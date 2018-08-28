package voidious.gun;

import voidious.utils.Wave;

class MeleeFiringAngle
{
	private double angle;
	private double distance;
	private double bandwidth;
	private double scanWeight;
	private final Wave wave;

	MeleeFiringAngle(double angle, double distance, double bandwidth, double scanWeight, Wave wave)
	{
		this.angle = angle;
		this.distance = distance;
		this.bandwidth = bandwidth;
		this.scanWeight = scanWeight;
		this.wave = wave;
	}

	/**
	 * @return the angle
	 */
	double getAngle()
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

	/**
	 * @return the scanWeight
	 */
	public double getScanWeight()
	{
		return this.scanWeight;
	}

	/**
	 * @param scanWeight
	 *            the scanWeight to set
	 */
	public void setScanWeight(double scanWeight)
	{
		this.scanWeight = scanWeight;
	}

	/**
	 * @return the wave
	 */
	public Wave getWave()
	{
		return this.wave;
	}
}