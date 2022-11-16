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
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package dap;

import com.fujitsu.vdmj.messages.ConsoleWriter;

public class DAPErrConsoleWriter implements ConsoleWriter
{
	private final DAPServer server;
	
	public DAPErrConsoleWriter(DAPServer server)
	{
		this.server = server;
	}
	
	@Override
	public void println(String message)
	{
		server.stderr(message + "\n");
	}
	
	@Override
	public void print(String message)
	{
		server.stderr(message);
	}
	
	@Override
	public void printf(String format, Object... args)
	{
		server.stderr(String.format(format, args));
	}

	@Override
	public void println()
	{
		server.stderr("\n");
	}

	@Override
	public void close()
	{
		// ignore
	}
}
