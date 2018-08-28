package voidious.utils.geom;

import java.awt.geom.Point2D;

/**
 * Copyright (c) 2009-2011 - Voidious
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

// y = mx + b
public class LineSeg
{
	private double m;
	private double b;
	private double xMin;
	private double xMax;
	private double yMin;
	private double yMax;
	private double x1;
	private double y1;
	private double x2;
	private double y2;

	public LineSeg(double x1, double y1, double x2, double y2)
	{
		if (x1 == x2)
		{
			this.m = Double.POSITIVE_INFINITY;
			this.b = Double.NaN;
			this.xMin = this.xMax = x1;
		}
		else
		{
			this.m = (y2 - y1) / (x2 - x1);
			this.b = y1 - (this.m * x1);
			this.xMin = Math.min(x1, x2);
			this.xMax = Math.max(x1, x2);
		}
		this.yMin = Math.min(y1, y2);
		this.yMax = Math.max(y1, y2);

		this.x1 = x1;
		this.y1 = y1;
		this.x2 = x2;
		this.y2 = y2;
	}

	public LineSeg(Point2D.Double p1, Point2D.Double p2)
	{
		this(p1.x, p1.y, p2.x, p2.y);
	}

	/**
	 * @return the m
	 */
	public double getM()
	{
		return this.m;
	}

	/**
	 * @param m
	 *            the m to set
	 */
	public void setM(double m)
	{
		this.m = m;
	}

	/**
	 * @return the b
	 */
	public double getB()
	{
		return this.b;
	}

	/**
	 * @param b
	 *            the b to set
	 */
	public void setB(double b)
	{
		this.b = b;
	}

	/**
	 * @return the xMin
	 */
	public double getxMin()
	{
		return this.xMin;
	}

	/**
	 * @param xMin
	 *            the xMin to set
	 */
	public void setxMin(double xMin)
	{
		this.xMin = xMin;
	}

	/**
	 * @return the xMax
	 */
	public double getxMax()
	{
		return this.xMax;
	}

	/**
	 * @param xMax
	 *            the xMax to set
	 */
	public void setxMax(double xMax)
	{
		this.xMax = xMax;
	}

	/**
	 * @return the yMin
	 */
	public double getyMin()
	{
		return this.yMin;
	}

	/**
	 * @param yMin
	 *            the yMin to set
	 */
	public void setyMin(double yMin)
	{
		this.yMin = yMin;
	}

	/**
	 * @return the yMax
	 */
	public double getyMax()
	{
		return this.yMax;
	}

	/**
	 * @param yMax
	 *            the yMax to set
	 */
	public void setyMax(double yMax)
	{
		this.yMax = yMax;
	}

	/**
	 * @return the x1
	 */
	public double getX1()
	{
		return this.x1;
	}

	/**
	 * @param x1
	 *            the x1 to set
	 */
	public void setX1(double x1)
	{
		this.x1 = x1;
	}

	/**
	 * @return the y1
	 */
	public double getY1()
	{
		return this.y1;
	}

	/**
	 * @param y1
	 *            the y1 to set
	 */
	public void setY1(double y1)
	{
		this.y1 = y1;
	}

	/**
	 * @return the x2
	 */
	public double getX2()
	{
		return this.x2;
	}

	/**
	 * @param x2
	 *            the x2 to set
	 */
	public void setX2(double x2)
	{
		this.x2 = x2;
	}

	/**
	 * @return the y2
	 */
	public double getY2()
	{
		return this.y2;
	}

	/**
	 * @param y2
	 *            the y2 to set
	 */
	public void setY2(double y2)
	{
		this.y2 = y2;
	}
}