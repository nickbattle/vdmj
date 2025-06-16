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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.messages;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.io.PrintWriter;

public class ConsolePrintWriter implements ConsoleWriter
{
	private final PrintWriter out;
	
	public ConsolePrintWriter(PrintWriter out)
	{
		this.out = out;
	}
	
	public ConsolePrintWriter(OutputStream out)
	{
		this.out = new PrintWriter(out);
	}

	public ConsolePrintWriter(File file) throws FileNotFoundException
	{
		this.out = new PrintWriter(file);
	}

	@Override
	public void print(String line)
	{
		out.print(line);
		out.flush();
	}

	@Override
	public void println(String line)
	{
		out.println(line);
		out.flush();
	}

	@Override
	public void printf(String format, Object... args)
	{
		out.printf(format, args);
		out.flush();
	}

	@Override
	public void println()
	{
		out.println();
		out.flush();
	}

	@Override
	public void close()
	{
		out.close();
	}
}
