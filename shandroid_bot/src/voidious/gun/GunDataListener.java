package voidious.gun;

import voidious.utils.Wave;

public interface GunDataListener
{
	void on1v1FiringWaveBreak(Wave w, double hitAngle, double tolerance);

	void onMarkFiringWave(Wave w);
}