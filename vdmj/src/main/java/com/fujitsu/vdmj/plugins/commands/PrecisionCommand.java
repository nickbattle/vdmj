/*******************************************************************************
 *
 *	Copyright (c) 2023 Nick Battle.
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
 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.plugins.commands;

import static com.fujitsu.vdmj.plugins.PluginConsole.println;

import java.math.MathContext;
import java.math.RoundingMode;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.plugins.AnalysisCommand;

public class PrecisionCommand extends AnalysisCommand
{
	private final static String USAGE = "Usage: precision [<n>]";
	
	public PrecisionCommand(String line)
	{
		super(line);
		
		if (!argv[0].equals("precision"))
		{
			throw new IllegalArgumentException(USAGE);
		}
	}

	@Override
	public String run(String line)
	{
		if (argv.length > 2)
		{
			return USAGE;
		}
		else if (argv.length == 1)
		{
			return "Decimal precision = " + Settings.precision.getPrecision();
		}

		try
		{
			int precision = Integer.parseInt(argv[1]);
			
			if (precision < 10)
			{
				return "Precision argument must be >= 10";
			}
			else
			{
				Settings.precision = new MathContext(precision, RoundingMode.HALF_UP);
				return "Decimal precision = " + Settings.precision.getPrecision();
			}
		}
		catch (NumberFormatException e)
		{
			return "Precision argument must be numeric";
		}
	}
	
	public static void help()
	{
		println("precision [<#places>] - set/show decimal precision");
	}
}