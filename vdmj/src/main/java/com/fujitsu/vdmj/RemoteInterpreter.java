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

package com.fujitsu.vdmj;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import com.fujitsu.vdmj.in.definitions.INClassDefinition;
import com.fujitsu.vdmj.in.modules.INModule;
import com.fujitsu.vdmj.runtime.ClassInterpreter;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.runtime.ModuleInterpreter;
import com.fujitsu.vdmj.runtime.SourceFile;
import com.fujitsu.vdmj.values.Value;

public class RemoteInterpreter
{
	private final Interpreter interpreter;

	public RemoteInterpreter(Interpreter interpreter)
	{
		this.interpreter = interpreter;
	}

	public Interpreter getInterpreter()
	{
		return interpreter;
	}

	public String execute(String line) throws Exception
	{
		return interpreter.execute(line).toString();
	}

	public Value valueExecute(String line) throws Exception
	{
		return interpreter.execute(line);
	}

	public void init()
	{
		interpreter.init();
	}

	public void create(String var, String exp) throws Exception
	{
		if (interpreter instanceof ClassInterpreter)
		{
			ClassInterpreter ci = (ClassInterpreter)interpreter;
			ci.create(var, exp);
		}
		else
		{
			throw new Exception("Only available for VDM++ and VDM-RT");
		}
	}

	public Set<File> getSourceFiles()
	{
		return interpreter.getSourceFiles();
	}

	public SourceFile getSourceFile(File file) throws IOException
	{
		return interpreter.getSourceFile(file);
	}

	public List<String> getModules() throws Exception
	{
		List<String> names = new Vector<String>();

		if (interpreter instanceof ClassInterpreter)
		{
			throw new Exception("Only available for VDM-SL");
		}
		else
		{
			for (INModule m: ((ModuleInterpreter)interpreter).getModules())
			{
				names.add(m.name.getName());
			}
		}

		return names;
	}

	public List<String> getClasses() throws Exception
	{
		List<String> names = new Vector<String>();

		if (interpreter instanceof ClassInterpreter)
		{
			for (INClassDefinition def: ((ClassInterpreter)interpreter).getClasses())
			{
				names.add(def.name.getName());
			}
		}
		else
		{
			throw new Exception("Only available for VDM++ and VDM-RT");
		}

		return names;
	}
}
