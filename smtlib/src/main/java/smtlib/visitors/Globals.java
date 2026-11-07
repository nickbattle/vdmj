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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionSet;
import com.fujitsu.vdmj.tc.definitions.TCExplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.definitions.TCImplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.definitions.TCLocalDefinition;
import com.fujitsu.vdmj.tc.expressions.TCApplyExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCVariableExpression;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.modules.TCModule;
import com.fujitsu.vdmj.tc.modules.TCModuleList;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
import com.fujitsu.vdmj.tc.types.TCIntegerType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatEnvironment;
import smtlib.ast.AssertCommand;
import smtlib.ast.Bracketed;
import smtlib.ast.Comment;
import smtlib.ast.DeclareDataType;
import smtlib.ast.DefineFun;
import smtlib.ast.DefineFunsRec;
import smtlib.ast.DefineSort;
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
	public static final String INTERNAL_MODULE = "$SMT-LIB";

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
	private List<DeclareDataType> datatypes = new Vector<DeclareDataType>();
	private List<DefineSort> sorts = new Vector<DefineSort>();
	private List<DefineFun> functions = new Vector<DefineFun>();
	private List<AssertCommand> assertions = new Vector<AssertCommand>();

	private TCDefinitionList environment = new TCDefinitionList();
	private Map<DefineFunsRec, TCDefinitionSet> recursiveGroups = null;

	public static void reset()
	{
		INSTANCE = null;
	}

	public void init()		// Start of each PO
	{
		if (recursiveGroups != null)
		{
			for (DefineFunsRec group : recursiveGroups.keySet())
			{
				group.init();
			}
		}
	
		constants.clear();
		datatypes.clear();
		sorts.clear();
		functions.clear();
		assertions.clear();
	}

	public boolean containsType(String name)
	{
		return contains(name, datatypes);
	}

	private <T extends SExp> boolean contains(String name, List<T> list)
	{
		for (T known : list)
		{
			if (known.get(1).toString().equals(name))
			{
				return true;
			}
		}

		return false;
	}

	public void createFunction(TCNameToken name)
	{
		DefineFun define = new DefineFun(name); // Placeholder
		DefineFunsRec group = findGroup(name);

		if (group != null)
		{
			if (!group.added)
			{
				functions.add(group);
				group.added = true;
			}

			group.addMember(define);
		}
		else
		{
			if (!contains(name.getName(), functions))
			{
				functions.add(0, define);
			}
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
		for (AssertCommand cmd : assertions)
		{
			if (cmd.equals(assertCommand))
			{
				return;
			}
		}

		assertions.add(assertCommand);
	}

	public void addDataType(DeclareDataType type)
	{
		if (!contains(type.getName(), datatypes))
		{
			datatypes.add(0, type);
		}
	}

	public void addSort(DefineSort sort)
	{
		if (!contains(sort.getName(), sorts))
		{
			sorts.add(0, sort);
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
		TCFunctionType ftype = new TCFunctionType(LexLocation.ANY, new TCTypeList(new TCIntegerType(LexLocation.ANY)), false, new TCBooleanType(LexLocation.ANY));
		environment.add(new TCLocalDefinition(LexLocation.ANY, getInternalName("inv_nat"), ftype));
		environment.add(new TCLocalDefinition(LexLocation.ANY, getInternalName("inv_nat1"), ftype));
	}

	public TCNameToken getInternalName(String name)
	{
		return new TCNameToken(LexLocation.ANY, INTERNAL_MODULE, name);
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
					getInternalName("inv_nat"),
					new Bracketed(new Expression("i", "Int")),
					new Sort("Bool"),
					new GE("i", "0")));

		script.add(new DefineFun(
					getInternalName("inv_nat1"),
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

		for (DeclareDataType type: datatypes)
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

	public DefineFun findFunction(TCNameToken fname)
	{
		DefineFunsRec group = findGroup(fname);

		if (group != null)
		{
			for (DefineFun member : group.members)
			{
				if (member.getName().equals(fname.getName()))
				{
					return member;
				}
			}
		}
		else
		{
			for (DefineFun known : functions)
			{
				if (known.getName().equals(fname.getName()))
				{
					return known;
				}
			}
		}

		return null;
	}

	private DefineFunsRec findGroup(TCNameToken name)
	{
		if (recursiveGroups != null)
		{
			for (DefineFunsRec group : recursiveGroups.keySet())
			{
				for (TCDefinition tcdef : recursiveGroups.get(group))
				{
					if (tcdef.name.equals(name))
					{
						return group;
					}
				}
			}
		}

		return null;
	}

	public void findrecursiveGroups(TCModuleList moduleList)
	{
		HashSet<TCDefinitionSet> groups = new HashSet<TCDefinitionSet>();

		for (TCModule module : moduleList)
		{
			for (TCDefinition def : module.defs)
			{
				if (def.isFunction())
				{
					TCExpression body = null;

					if (def instanceof TCExplicitFunctionDefinition)
					{
						TCExplicitFunctionDefinition exfn = (TCExplicitFunctionDefinition)def;
						body = exfn.body;
					}
					else if (def instanceof TCImplicitFunctionDefinition)
					{
						TCImplicitFunctionDefinition imfn = (TCImplicitFunctionDefinition)def;
						body = imfn.body;
					}

					if (body != null)
					{
						List<TCApplyExpression> applies = body.apply(new ApplyFinder(), null);
						TCDefinitionSet set = new TCDefinitionSet();

						for (TCApplyExpression apply : applies)
						{
							if (apply.recursiveCycles != null)
							{
								for (TCDefinitionList cycle : apply.recursiveCycles)
								{
									set.addAll(cycle);
								}
							}
						}

						if (!set.isEmpty())
						{
							// merge if there are overlaps
							boolean merged = false;

							for (TCDefinitionSet group : groups)
							{
								for (TCDefinition func : set)
								{
									if (group.contains(func))
									{
										group.addAll(set);
										merged = true;
										break;
									}
								}
							}

							// else we have a new set
							if (!merged)
							{
								groups.add(set);
							}
						}

						recursiveGroups = new HashMap<DefineFunsRec, TCDefinitionSet>();
						int id = 1;

						for (TCDefinitionSet group : groups)
						{
							recursiveGroups.put(new DefineFunsRec(id), group);
						}
					}
				}
			}
		}

		// printrecursiveGroups();
	}

	// private void printrecursiveGroups()
	// {
	// 	if (recursiveGroups != null && !recursiveGroups.isEmpty())
	// 	{
	// 		Console.out.println("Recursive function sets:");

	// 		for (TCDefinitionSet group : recursiveGroups.values())
	// 		{
	// 			Console.out.print("[");
	// 			String sep = "";

	// 			for (TCDefinition def : group)
	// 			{
	// 				Console.out.printf("%s%s", sep, def.name.toString());
	// 				sep = ", ";
	// 			}

	// 			Console.out.print("]\n");
	// 		}
	// 	}
	// }
}
