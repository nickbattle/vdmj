/*******************************************************************************
 *
 *	Copyright (c) 2016 Aarhus University.
 *
 *	Author: Nick Battle and Kenneth Lausdahl
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

import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.in.definitions.INExplicitOperationDefinition;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.runtime.ClassInterpreter;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ObjectContext;
import com.fujitsu.vdmj.runtime.StateContext;
import com.fujitsu.vdmj.runtime.VDMOperation;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCOperationType;
import com.fujitsu.vdmj.values.NameValuePair;
import com.fujitsu.vdmj.values.ObjectValue;
import com.fujitsu.vdmj.values.OperationValue;
import com.fujitsu.vdmj.values.SeqValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;

public class TestSuite
{
	@VDMOperation
	public static Value getTestMethodNamed(Value test)
	{
		List<String> tests = new Vector<String>();
		ObjectValue instance = (ObjectValue) test;

		for (NameValuePair p : instance.members.asList())
		{
			if (p.value instanceof OperationValue)
			{
				if (p.name.getName().toLowerCase().startsWith("test"))
				{
					tests.add(p.name.getName());
				}
			}
		}

		ValueList vals = new ValueList();
		for (String value : tests)
		{
			vals.add(new SeqValue(value));
		}

		return new SeqValue(vals);
	}

	@VDMOperation
	public static Value createTests(Value test) throws Exception
	{
		List<String> tests = new Vector<String>();
		ValueList vals = new ValueList();
		ObjectValue instance = (ObjectValue) test;

		for (NameValuePair p : instance.members.asList())
		{
			if (p.value instanceof OperationValue)
			{
				OperationValue opVal = (OperationValue) p.value;
				if (!opVal.isConstructor
						&& p.name.getName().toLowerCase().startsWith("test"))
				{
					tests.add(p.name.getName());

					Context mainContext = new StateContext(p.name.getLocation(), "reflection scope");

					mainContext.putAll(ClassInterpreter.getInstance().getInitialContext());
					mainContext.setThreadState(ClassInterpreter.getInstance().getInitialContext().threadState.CPU);

					try
					{
						INExplicitOperationDefinition ctor = getTestConstructor(instance);
						if (ctor == null
								// || !ctor.name.getModule().equals(instance.type.name.getLocation().module)
								// && ctor.parameterDefinitions.isEmpty()
								|| ctor.accessSpecifier.access != Token.PUBLIC)
						{
							throw new Exception("Class "
									+ p.name.getModule()
									+ " has no public constructor TestCase(String name) or TestCase()");
						}

						String vdmTestExpression = "";
						if (((TCOperationType) ctor.getType()).parameters.size() == 1)
						{
							vdmTestExpression = "new " + p.name.getModule()
									+ "(\"" + p.name.getName() + "\")";
							vals.add(ClassInterpreter.getInstance().evaluate(vdmTestExpression, mainContext));
						} else
						{
							boolean foundSetName = false;
							// check that we have setName and that it is accesiable
							for (TCNameToken name : instance.members.keySet())
							{
								if (name.getName().equals("setName"))
								{
									foundSetName = true;
								}
							}
							if (!foundSetName)
							{
								throw new Exception("Cannot instantiate test case: "
										+ instance.type.name.getName());
							}

							vdmTestExpression = "new " + p.name.getModule()
									+ "()";
							Value testClassInstance = ClassInterpreter.getInstance().evaluate(vdmTestExpression, mainContext);
							ObjectValue tmp = (ObjectValue) testClassInstance;
							ObjectContext octxt = new ObjectContext(mainContext.location, "TestClassContext", mainContext, tmp);
							vdmTestExpression = "setName(\"" + p.name.getName()
									+ "\")";

							ClassInterpreter.getInstance().evaluate(vdmTestExpression, octxt);
							vals.add(testClassInstance);
						}

					} catch (Exception e)
					{
						throw e;
					}
				}
			}
		}
		return new SeqValue(vals);
	}

	private static INExplicitOperationDefinition getTestConstructor(
			ObjectValue instance)
	{
		INExplicitOperationDefinition ctor = getConstructor(instance, "seq of (char)");
		if (ctor != null)
		{
			return ctor;
		}
		return getConstructor(instance, "()");
	}

	private static INExplicitOperationDefinition getConstructor(
			ObjectValue instance, String typeName)
	{
		boolean searchForDefaultCtor = typeName.trim().equals("()");
		INExplicitOperationDefinition defaultSuperCtor = null;
		for (NameValuePair pair : instance.members.asList())
		{
			if (pair.value instanceof OperationValue)
			{
				OperationValue op = (OperationValue) pair.value;
				if (op.isConstructor)
				{
					if (searchForDefaultCtor
							&& ((TCOperationType) op.expldef.getType()).parameters.isEmpty())
					{
						if (op.expldef.name.getName().equals(instance.type.name.getName()))
						{
							return op.expldef;
						} else
						{
							defaultSuperCtor = op.expldef;
						}
					} else if (((TCOperationType) op.expldef.getType()).parameters.size() == 1
							&& ((TCOperationType) op.expldef.getType()).parameters.get(0).toString().equals(typeName)
							&& op.expldef.name.getName().equals(instance.type.name.getName()))
					{
						return op.expldef;
					}
				}
			}
		}

		if (searchForDefaultCtor)
		{
			return defaultSuperCtor;
		}
		return null;
	}
}
