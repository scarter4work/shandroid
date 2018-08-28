package voidious.move;

import java.awt.geom.Point2D;

class Destination
{
	private Point2D.Double location;
	private double risk;
	private double goAngle;

	Destination(Point2D.Double location, double risk, double goAngle)
	{
		this.location = location;
		this.risk = risk;
		this.goAngle = goAngle;
	}

	/**
	 * @return the location
	 */
	public Point2D.Double getLocation()
	{
		return this.location;
	}

	/**
	 * @param location
	 *            the location to set
	 */
	public void setLocation(Point2D.Double location)
	{
		this.location = location;
	}

	/**
	 * @return the risk
	 */
	public double getRisk()
	{
		return this.risk;
	}

	/**
	 * @param risk
	 *            the risk to set
	 */
	public void setRisk(double risk)
	{
		this.risk = risk;
	}

	/**
	 * @return the goAngle
	 */
	public double getGoAngle()
	{
		return this.goAngle;
	}

	/**
	 * @param goAngle
	 *            the goAngle to set
	 */
	public void setGoAngle(double goAngle)
	{
		this.goAngle = goAngle;
	}
}