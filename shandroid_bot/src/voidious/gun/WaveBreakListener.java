package voidious.gun;

import java.util.List;

import voidious.utils.RobotState;
import voidious.utils.Wave;

public interface WaveBreakListener
{
	void onWaveBreak(Wave w, List<RobotState> waveBreakStates);
}