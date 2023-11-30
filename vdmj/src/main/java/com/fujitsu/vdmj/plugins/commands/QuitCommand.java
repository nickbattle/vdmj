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

import com.fujitsu.vdmj.ExitStatus;

public class QuitCommand extends ControlAnalysisCommand
{
	private final static String CMD = "quit";
	private final static String USAGE = "Usage: " + CMD;
	public  final static String HELP = CMD + " - close the session";

	public QuitCommand(String line)
	{
		super(line);
		
		if (!argv[0].equals("quit") && !argv[0].equals("q"))
		{
			throw new IllegalArgumentException(USAGE);
		}
	}

	@Override
	public String run(String line)
	{
		if (argv.length == 1)
		{
			exitStatus = ExitStatus.EXIT_OK;
			carryOn = false;
			return null;
		}
		else
		{
			return USAGE;
		}
	}
}
