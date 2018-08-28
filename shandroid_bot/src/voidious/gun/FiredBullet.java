package voidious.gun;

import java.awt.geom.Point2D;

public class FiredBullet
{
	private final long fireTime;
	private final Point2D.Double sourceLocation;
	private final double firingAngle;
	private final double bulletSpeed;
	private final double dx;
	private final double dy;
	private long deathTime;

	FiredBullet(long fireTime, Point2D.Double sourceLocation, double firingAngle, double bulletSpeed)
	{
		this.fireTime = fireTime;
		this.sourceLocation = sourceLocation;
		this.firingAngle = firingAngle;
		this.bulletSpeed = bulletSpeed;
		this.dx = Math.sin(firingAngle) * bulletSpeed;
		this.dy = Math.cos(firingAngle) * bulletSpeed;
		this.deathTime = Long.MAX_VALUE;
	}

	public Point2D.Double position(long currentTime)
	{
		return new Point2D.Double(this.sourceLocation.x + (this.dx * (currentTime - this.fireTime)), this.sourceLocation.y
				+ (this.dy * (currentTime - this.fireTime)));
	}

	public long getFireTime() {
		return fireTime;
	}

	public Point2D.Double getSourceLocation() {
		return sourceLocation;
	}

	public double getFiringAngle() {
		return firingAngle;
	}

	public double getBulletSpeed() {
		return bulletSpeed;
	}

	public double getDx() {
		return dx;
	}

	public double getDy() {
		return dy;
	}

	public long getDeathTime() {
		return deathTime;
	}

	public void setDeathTime(long deathTime) {
		this.deathTime = deathTime;
	}
}