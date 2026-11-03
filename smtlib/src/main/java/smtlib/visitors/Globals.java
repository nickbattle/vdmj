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

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.definitions.TCLocalDefinition;
import com.fujitsu.vdmj.tc.expressions.TCVariableExpression;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
import com.fujitsu.vdmj.tc.types.TCIntegerType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatEnvironment;
import smtlib.ast.AssertCommand;
import smtlib.ast.Bracketed;
import smtlib.ast.Comment;
import smtlib.ast.DeclareType;
import smtlib.ast.DefineFun;
import smtlib.ast.Expression;
import smtlib.ast.GE;
import smtlib.ast.GT;
import smtlib.ast.SExp;
import smtlib.ast.Script;
import smtlib.ast.Sort;
import smtlib.ast.Undefined;

public class Globals
{
	private static Globals INSTANCE = null;
	private static final String INTERNAL_MODULE = "$SMT-LIB";

	private Globals()
	{
		setInternalEnvironment();
	}
	
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

	private TCDefinitionList environment = new TCDefinitionList();

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

	private void setInternalEnvironment()
	{
		TCNameToken fname = new TCNameToken(LexLocation.ANY, "$SMT-LIB", "inv_nat");
		TCFunctionType ftype = new TCFunctionType(LexLocation.ANY, new TCTypeList(new TCIntegerType(LexLocation.ANY)), false, new TCBooleanType(LexLocation.ANY));
		environment.add(new TCLocalDefinition(LexLocation.ANY, fname, ftype));

		fname = new TCNameToken(LexLocation.ANY, "$SMT-LIB", "inv_nat1");
		environment.add(new TCLocalDefinition(LexLocation.ANY, fname, ftype));
	}

	public TCNameToken getInternalName(String name)
	{
		return new TCNameToken(LexLocation.ANY, INTERNAL_MODULE, "inv_nat");
	}

	public TCVariableExpression getInteralVariable(String name)
	{
		return new TCVariableExpression(LexLocation.ANY, getInternalName(name), name);
	}

	public Environment getEnvironment(Environment outer)
	{
		return new FlatEnvironment(environment, outer);
	}

	private Script getInternals()
	{
		Script script = new Script();

		script.add(new DefineFun(
					"inv_nat",
					new Bracketed(new Expression("i", "Int")),
					new Sort("Bool"),
					new GE("i", "0")));

		script.add(new DefineFun(
					"inv_nat1",
					new Bracketed(new Expression("i", "Int")),
					new Sort("Bool"),
					new GT("i", "0")));

		return script;
	}

	public Script globalList()
	{
		Script script = new Script();

		script.add(new Comment("\nGlobal function definitions"));
		script.addAll(getInternals());

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
