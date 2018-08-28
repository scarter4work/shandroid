package voidious.utils;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Copyright (c) 2009-2012 - Voidious
 * 
 * This software is provided 'as-is', without any express or implied warranty.
 * In no event will the authors be held liable for any damages arising from the
 * use of this software.
 * 
 * Permission is granted to anyone to use this software for any purpose,
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 * 
 * 1. The origin of this software must not be misrepresented; you must not claim
 * that you wrote the original software.
 * 
 * 2. Altered source versions must be plainly marked as such, and must not be
 * misrepresented as being the original software.
 * 
 * 3. This notice may not be removed or altered from any source distribution.
 */

public class RobotState
{
	private static final double BOT_HALF_WIDTH = 18;

	private final Point2D.Double location;
	private final double heading;
	private final double velocity;
	private final long time;
	private final boolean interpolated;
	private List<Point2D.Double> botCorners;
	private Rectangle2D.Double botRectangle;
	private List<Line2D.Double> botSides;

	public RobotState(Point2D.Double location, double heading, double velocity, long time, boolean interpolated)
	{
		this.location = location;
		this.heading = heading;
		this.velocity = velocity;
		this.time = time;
		this.interpolated = interpolated;
		this.botCorners = null;
		this.botRectangle = null;
		this.botSides = null;
	}

	List<Point2D.Double> botCorners()
	{
		if (this.botCorners == null)
		{
			this.botCorners = new ArrayList<>();
			this.botCorners.add(new Point2D.Double(this.location.x - BOT_HALF_WIDTH, this.location.y - BOT_HALF_WIDTH));
			this.botCorners.add(new Point2D.Double(this.location.x - BOT_HALF_WIDTH, this.location.y + BOT_HALF_WIDTH));
			this.botCorners.add(new Point2D.Double(this.location.x + BOT_HALF_WIDTH, this.location.y - BOT_HALF_WIDTH));
			this.botCorners.add(new Point2D.Double(this.location.x + BOT_HALF_WIDTH, this.location.y + BOT_HALF_WIDTH));
		}
		return this.botCorners;
	}

	public List<Line2D.Double> botSides()
	{
		if (this.botSides == null)
		{
			this.botSides = new ArrayList<>();
			this.botSides.add(new Line2D.Double(this.location.x - 18, this.location.y - 18, this.location.x + 18,
					this.location.y - 18));
			this.botSides.add(new Line2D.Double(this.location.x + 18, this.location.y - 18, this.location.x + 18,
					this.location.y + 18));
			this.botSides.add(new Line2D.Double(this.location.x + 18, this.location.y + 18, this.location.x - 18,
					this.location.y + 18));
			this.botSides.add(new Line2D.Double(this.location.x - 18, this.location.y + 18, this.location.x - 18,
					this.location.y - 18));
		}
		return this.botSides;
	}

	/**
	 * @return the botCorners
	 */
	public List<Point2D.Double> getBotCorners()
	{
		return this.botCorners;
	}

	/**
	 * @param botCorners
	 *            the botCorners to set
	 */
	public void setBotCorners(List<Point2D.Double> botCorners)
	{
		this.botCorners = botCorners;
	}

	/**
	 * @return the botRectangle
	 */
	public Rectangle2D.Double getBotRectangle()
	{
		return this.botRectangle;
	}

	/**
	 * @param botRectangle
	 *            the botRectangle to set
	 */
	public void setBotRectangle(Rectangle2D.Double botRectangle)
	{
		this.botRectangle = botRectangle;
	}

	/**
	 * @return the botSides
	 */
	public List<Line2D.Double> getBotSides()
	{
		return this.botSides;
	}

	/**
	 * @param botSides
	 *            the botSides to set
	 */
	public void setBotSides(List<Line2D.Double> botSides)
	{
		this.botSides = botSides;
	}

	/**
	 * @return the location
	 */
	public Point2D.Double getLocation()
	{
		return this.location;
	}

	/**
	 * @return the heading
	 */
	public double getHeading()
	{
		return this.heading;
	}

	/**
	 * @return the velocity
	 */
	public double getVelocity()
	{
		return this.velocity;
	}

	/**
	 * @return the time
	 */
	public long getTime()
	{
		return this.time;
	}

	/**
	 * @return the interpolated
	 */
	public boolean isInterpolated()
	{
		return this.interpolated;
	}
}