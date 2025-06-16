/*******************************************************************************
 *
 *	Copyright (c) 2016 Fujitsu Services Ltd.
 *
 *	Author: Nick Battle
 *
 *	This file is part of VDMJ.
 *
 *	VDMJ is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *
 *	VDMJ is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.scheduler;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.config.Properties;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.messages.RTLogger;

public class SystemClock
{
	private static long wallTime = 0;
	final static double PRECISION = 100000000.0d;

	public static synchronized long getWallTime()
	{
		return wallTime;
	}

	public static void init()
	{
		wallTime = 0;
	}

	public static synchronized void advance(long duration)
	{
		wallTime += duration;

		if (Settings.dialect == Dialect.VDM_RT &&
			Properties.rt_diags_timestep)
		{
			RTLogger.log(String.format("-- Moved time by %d", duration));
		}
	}

	/**
	 * Time unit enumeration used to specify units used in the VDM syntax.
	 * This change has come from Overture.
	 * @author kela
	 */
	public enum TimeUnit
	{
		seconds(1.0, "s"),
		decisecond(Math.pow(10, -1), "ds"),
		centisecond(Math.pow(10, -2), "cs"),
		millisecond(Math.pow(10, -3), "ms"),
		microsecond(Math.pow(10, -6), "\u03BCs"),	// mu s :-)
		nanosecond(Math.pow(10, -9), "ns");

		private final Double value;
		private final String symbol;

		private TimeUnit(Double value, String symbol)
		{
			this.value = value;
			this.symbol = symbol;
		}

		public Double getValue()
		{
			return value;
		}

		@Override
		public String toString()
		{
			return symbol + " factor: " + value;
		}
	}

	/**
	 * Utility method to convert a value in the given unit to the internal time
	 * 
	 * @param unit
	 *            The unit of the time parameter
	 * @param time
	 *            The time to convert
	 * @return The internal time representation of the parameter
	 */
	public static long timeToInternal(TimeUnit unit, Double time)
	{
		return Math.round(time * unit.getValue()
				/ TimeUnit.nanosecond.getValue());
	}
}
