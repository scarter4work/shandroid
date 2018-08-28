package voidious.radar;

import java.awt.geom.Point2D;

class RadarScan
{
	private long lastScanTime;
	private Point2D.Double lastLocation;

	RadarScan(long lastScanTime, Point2D.Double lastLocation)
	{
		this.lastScanTime = lastScanTime;
		this.lastLocation = lastLocation;
	}

	/**
	 * @return the lastScanTime
	 */
	long getLastScanTime()
	{
		return this.lastScanTime;
	}

	/**
	 * @param lastScanTime
	 *            the lastScanTime to set
	 */
	public void setLastScanTime(long lastScanTime)
	{
		this.lastScanTime = lastScanTime;
	}

	/**
	 * @return the lastLocation
	 */
	Point2D.Double getLastLocation()
	{
		return this.lastLocation;
	}

	/**
	 * @param lastLocation
	 *            the lastLocation to set
	 */
	public void setLastLocation(Point2D.Double lastLocation)
	{
		this.lastLocation = lastLocation;
	}
}