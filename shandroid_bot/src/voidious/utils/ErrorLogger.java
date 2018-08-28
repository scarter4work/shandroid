package voidious.utils;

import robocode.AdvancedRobot;
import robocode.RobocodeFileWriter;

import java.io.*;
import java.text.DateFormat;
import java.util.Calendar;

/**
 * Copyright (c) 2012 - Voidious
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

public class ErrorLogger
{
	private static final String ERROR_LOG = "error.log";
	private static final int MAX_ERROR_SIZE = 5000;
	private static final int CLEAR_SIZE = 50000;
	public static boolean enabled = false;

	private static ErrorLogger errorLogger;

	private AdvancedRobot robot;
	private File outFile = null;

	private ErrorLogger(AdvancedRobot robot)
	{
		this.robot = robot;
		this.outFile = robot.getDataFile(ERROR_LOG);
	}

	public static void init(AdvancedRobot robot)
	{
		if (errorLogger == null)
		{
			errorLogger = new ErrorLogger(robot);
		}
		else
		{
			errorLogger.setRobot(robot);
		}
	}

	private void setRobot(AdvancedRobot robot)
	{
		this.robot = robot;
	}

	public void logException(Exception e, String moreInfo)
	{
		try
		{
			this.clearQuota();

			try (RobocodeFileWriter rfw = this.getFileWriter(true); PrintWriter pw = new PrintWriter(rfw))
			{
				String timestamp = DateFormat.getInstance().format(Calendar.getInstance().getTime());
				pw.append(timestamp).append("\n");
				e.printStackTrace(pw);

				if (moreInfo != null && !moreInfo.equals(""))
				{
					pw.append("\n");
					pw.append(moreInfo);
					pw.append("\n");
				}

				pw.append("----");
				pw.append("\n");
			}
		}
		catch (IOException ioe)
		{
			System.out.println("Could not open file.");
		}
	}

	private void clearQuota() throws IOException
	{
		if (this.outFile.exists() && this.robot.getDataQuotaAvailable() < CLEAR_SIZE)
		{
			String errorFile = this.readErrorFile();
			errorFile = errorFile.substring(CLEAR_SIZE, errorFile.length() - 1);
			try (RobocodeFileWriter rfw = this.getFileWriter(false))
			{
				rfw.write(errorFile);
			}
		}
	}

	public void logError(String output)
	{
		if (enabled && output.length() < MAX_ERROR_SIZE)
		{
			try
			{
				this.clearQuota();
				System.out.println("ERROR:");
				System.out.println("  " + output.replaceAll("[\r\n]+", "\n  "));

				try (RobocodeFileWriter rfw = this.getFileWriter(true))
				{
					String timestamp = DateFormat.getInstance().format(Calendar.getInstance().getTime());
					rfw.append(timestamp).append("\n");
					rfw.append(output);
					rfw.append("\n\n");
				}
			}
			catch (IOException ioe)
			{
				ioe.printStackTrace();
			}
		}
	}

	private RobocodeFileWriter getFileWriter(boolean append) throws IOException
	{
		if (this.outFile.exists())
		{
			return new RobocodeFileWriter(this.outFile.getAbsolutePath(), append);
		}
		return new RobocodeFileWriter(this.outFile);
	}

	// mostly ganked from:
	// http://stackoverflow.com/questions/326390/how-to-create-a-java-string-from-the-contents-of-a-file
	private String readErrorFile() throws IOException
	{
		StringBuilder sb = new StringBuilder();

		try (BufferedReader reader = new BufferedReader(new FileReader(this.outFile)))
		{
			String line;
			String newLine = System.getProperty("line.separator");
			while ((line = reader.readLine()) != null)
			{
				sb.append(line);
				sb.append(newLine);
			}
		}
		return sb.toString();
	}

	public static ErrorLogger getInstance()
	{
		if (errorLogger == null)
		{
			throw new NullPointerException("You must initialize ErrorLogger before using it.");
		}
		return errorLogger;
	}
}
