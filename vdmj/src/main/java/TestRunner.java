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
 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.in.definitions.INClassDefinition;
import com.fujitsu.vdmj.runtime.ClassInterpreter;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.StateContext;
import com.fujitsu.vdmj.runtime.VDMOperation;
import com.fujitsu.vdmj.values.ObjectValue;
import com.fujitsu.vdmj.values.SetValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueSet;

public class TestRunner
{
	@VDMOperation
	public static Value collectTests(Value obj)
	{
		List<String> tests = new Vector<String>();
		ObjectValue instance = (ObjectValue) obj;

		if (ClassInterpreter.getInstance() instanceof ClassInterpreter)
		{
			for (INClassDefinition def : ((ClassInterpreter) ClassInterpreter.getInstance()).getClasses())
			{
				if (def.isAbstract || !isTestClass(def))
				{
					continue;
				}
				tests.add(def.name.getName());
			}
		}

		Context mainContext = new StateContext(instance.type.location, "reflection scope");

		mainContext.putAll(ClassInterpreter.getInstance().getInitialContext());
		mainContext.setThreadState(ClassInterpreter.getInstance().getInitialContext().threadState.CPU);

		ValueSet vals = new ValueSet();
		for (String value : tests)
		{
			try
			{
				vals.add(ClassInterpreter.getInstance().evaluate("new " + value
						+ "()", mainContext));
			} catch (Exception e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return new SetValue(vals);
	}

	private static boolean isTestClass(INClassDefinition def)
	{
		if (def.isAbstract || def.name.getName().equals("Test")
				|| def.name.getName().equals("TestCase")
				|| def.name.getName().equals("TestSuite"))
		{
			return false;
		}

		if (checkForSuper(def, "TestSuite"))
		{
			// the implementation must be upgrade before this work.
			// The upgrade should handle the static method for creatint the suire
			return false;
		}

		return checkForSuper(def, "Test");
	}

	private static boolean checkForSuper(INClassDefinition def, String superName)
	{
		for (INClassDefinition superDef : def.superdefs)
		{
			if (superDef.name.getName().equals(superName)
					|| checkForSuper(superDef, superName))
			{
				return true;
			}
		}
		return false;
	}
}
