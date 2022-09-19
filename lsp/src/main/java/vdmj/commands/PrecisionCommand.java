/*******************************************************************************
 *
 *	Copyright (c) 2020 Nick Battle.
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
 *
 ******************************************************************************/

package vdmj.commands;

import java.math.MathContext;
import java.math.RoundingMode;

import com.fujitsu.vdmj.Settings;

import dap.DAPMessageList;
import dap.DAPRequest;
import json.JSONObject;
import workspace.Diag;

public class PrecisionCommand extends Command
{
	public static final String USAGE = "Usage: precision [decimal digits]";
	public static final String[] HELP =	{ "precision", "precision [decimal digits] - set real variable precision" };
	
	private Integer precision = null;

	public PrecisionCommand(String line)
	{
		String[] parts = line.split("\\s+");
		
		if (parts.length == 2)
		{
			try
			{
				this.precision = Integer.parseInt(parts[1]);
			}
			catch (NumberFormatException e)
			{
				throw new IllegalArgumentException(USAGE);
			}
		}
	}
	
	@Override
	public DAPMessageList run(DAPRequest request)
	{
		try
		{
			if (precision == null)
			{
				return new DAPMessageList(request,
					new JSONObject("result", "Decimal precision = " + Settings.precision.getPrecision()));
			}
			else
			{
				if (precision < 10)
				{
					return new DAPMessageList(request,
							new JSONObject("result", "Precision must be >10"));
				}
				
				Settings.precision = new MathContext(precision, RoundingMode.HALF_UP);
				return new DAPMessageList(request,
					new JSONObject("result", "Decimal precision changed to " + Settings.precision.getPrecision()));
			}
		}
		catch (Exception e)
		{
			Diag.error(e);
			return new DAPMessageList(request, e);
		}
	}

	@Override
	public boolean notWhenRunning()
	{
		return true;
	}
}
