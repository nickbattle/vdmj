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

import static com.fujitsu.vdmj.plugins.PluginConsole.*;

import com.fujitsu.vdmj.ExitStatus;
import com.fujitsu.vdmj.plugins.AnalysisCommand;

public class MessageCommand extends AnalysisCommand implements ReaderControl
{
	private final String message;
	private final ExitStatus exitStatus;
	private final boolean carryOn;
	
	public MessageCommand(String message)
	{
		super(null);
		this.message = message;
		this.exitStatus = ExitStatus.EXIT_OK;
		this.carryOn = true;
	}
	
	public MessageCommand(String message, ExitStatus exitStatus, boolean carryOn)
	{
		super(null);
		this.message = message;
		this.exitStatus = exitStatus;
		this.carryOn = carryOn;
	}
	
	@Override
	public void run()
	{
		println(message);
	}

	@Override
	public ExitStatus exitStatus() throws Throwable
	{
		return exitStatus;
	}

	@Override
	public boolean carryOn()
	{
		return carryOn;
	}
}
