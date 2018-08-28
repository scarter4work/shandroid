package voidious.utils;

import static voidious.utils.DiaUtils.normalizeAngle;

class BulletShadow
{
	private double minAngle;
	private double maxAngle;

	BulletShadow(double minAngle, double maxAngle)
	{
		this.minAngle = minAngle;
		this.maxAngle = maxAngle;
	}

	boolean overlaps(BulletShadow that)
	{
		double thatMinAngle = normalizeAngle(that.minAngle, this.minAngle);
		double thatMaxAngle = normalizeAngle(that.maxAngle, thatMinAngle);
		return this.overlaps(that.minAngle) || this.overlaps(that.maxAngle)
				|| (thatMinAngle <= this.minAngle && thatMaxAngle >= this.maxAngle);
	}

	private boolean overlaps(double angle)
	{
		angle = normalizeAngle(angle, this.minAngle);
		return this.minAngle <= angle && this.maxAngle >= angle;
	}

	/**
	 * @return the minAngle
	 */
	double getMinAngle()
	{
		return this.minAngle;
	}

	/**
	 * @param minAngle
	 *            the minAngle to set
	 */
	void setMinAngle(double minAngle)
	{
		this.minAngle = minAngle;
	}

	/**
	 * @return the maxAngle
	 */
	double getMaxAngle()
	{
		return this.maxAngle;
	}

	/**
	 * @param maxAngle
	 *            the maxAngle to set
	 */
	void setMaxAngle(double maxAngle)
	{
		this.maxAngle = maxAngle;
	}
}