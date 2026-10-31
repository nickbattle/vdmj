/*******************************************************************************
 *
 *	Copyright (c) 2026 Nick Battle.
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

package smtlib.visitors;

import java.util.List;
import java.util.Vector;

import smtlib.ast.AssertCommand;
import smtlib.ast.Comment;
import smtlib.ast.DeclareType;
import smtlib.ast.DefineFun;
import smtlib.ast.SExp;
import smtlib.ast.Script;
import smtlib.ast.Sort;
import smtlib.ast.Undefined;

public class Globals
{
	private static Globals INSTANCE = null;
	
	public static Globals getInstance()
	{
		if (INSTANCE == null)
		{
			INSTANCE = new Globals();
		}
		
		return INSTANCE;
	}

	private List<DefineFun> constants = new Vector<DefineFun>();
	private List<DeclareType> datatypes = new Vector<DeclareType>();
	private List<DefineFun> functions = new Vector<DefineFun>();
	private List<AssertCommand> assertions = new Vector<AssertCommand>();

	public void reset()
	{
		INSTANCE = null;
	}

	public boolean containsType(String name)
	{
		return contains(name, datatypes);
	}

	private <T extends SExp> boolean contains(String name, List<T> list)
	{
		for (T known: list)
		{
			if (known.get(1).toString().equals(name))
			{
				return true;
			}
		}

		return false;
	}

	public void addFunction(DefineFun func)
	{
		if (!contains(func.getName(), functions))
		{
			functions.add(0, func);
		}
	}

	public void addUndefined(Sort sort)
	{
		DefineFun undef = new Undefined(sort);

		if (!contains(undef.getName(), constants))
		{
			constants.add(0, undef);
		}
	}

	public void addAssert(AssertCommand assertCommand)
	{
		for (AssertCommand cmd: assertions)
		{
			if (cmd.equals(assertCommand))
			{
				return;
			}
		}

		assertions.add(assertCommand);
	}

	public void addType(DeclareType type)
	{
		if (!contains(type.getName(), datatypes))
		{
			datatypes.add(0, type);
		}
	}

	public void addConstant(DefineFun constant)
	{
		if (!contains(constant.getName(), constants))
		{
			constants.add(0, constant);
		}
	}

	public Script globalList()
	{
		Script script = new Script();
		script.add(new Comment("\nGlobal function definitions"));

		for (DefineFun constant: constants)
		{
			script.add(constant);
		}

		for (DeclareType type: datatypes)
		{
			script.add(type);
		}

		for (DefineFun function: functions)
		{
			script.add(function);
		}

		for (AssertCommand cmd: assertions)
		{
			script.add(cmd);
		}

		return script;
	}

	public DefineFun getFunction(String fname)
	{
		for (DefineFun known: functions)
		{
			if (known.getName().equals(fname))
			{
				return known;
			}
		}

		return null;
	}
}
