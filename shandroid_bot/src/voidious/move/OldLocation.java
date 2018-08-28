package voidious.move;

import java.awt.geom.Point2D;

class OldLocation
{
	private Point2D.Double location;
	private long time;

	OldLocation(Point2D.Double l, long t)
	{
		this.location = l;
		this.time = t;
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
	 * @return the time
	 */
	public long getTime()
	{
		return this.time;
	}

	/**
	 * @param time
	 *            the time to set
	 */
	public void setTime(long time)
	{
		this.time = time;
	}
}