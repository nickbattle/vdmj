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
 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.dbgp;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;

import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.messages.ConsoleWriter;

@Deprecated
abstract public class Redirector implements ConsoleWriter
{
	protected final PrintWriter out;
	protected DBGPRedirect type;
	protected DBGPReader dbgp;

	public Redirector(PrintWriter out)
	{
		this.out = out;
		this.type = DBGPRedirect.DISABLE;
		this.dbgp = null;
	}

	public static void initRedirectors()	// Note: dbgp doesn't use charset
	{
		Console.init(Charset.defaultCharset(),
			new StdoutRedirector(new PrintWriter(new OutputStreamWriter(System.out))),
			new StderrRedirector(new PrintWriter(new OutputStreamWriter(System.err))));
	}

	public void redirect(DBGPRedirect t, DBGPReader d)
	{
		this.type = t;
		this.dbgp = d;
	}
	
	@Override
	abstract public void print(String line);

	@Override
	public void println(String line)
	{
		print(line + "\n");
	}

	@Override
	public void printf(String format, Object ... args)
	{
		print(String.format(format, args));
	}

	@Override
	public void println()
	{
		print("\n");
	}

	@Override
	public void close()
	{
		out.close();
	}
}
