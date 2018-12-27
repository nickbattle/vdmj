/*******************************************************************************
 *
 *	Copyright (c) 2018 Nick Battle.
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
 *
 ******************************************************************************/

package annotations.tc;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.tc.annotations.TCAnnotation;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpressionList;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.tc.modules.TCModule;
import com.fujitsu.vdmj.tc.statements.TCStatement;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCOverrideAnnotation extends TCAnnotation
{
	public TCOverrideAnnotation(TCIdentifierToken name, TCExpressionList args)
	{
		super(name, args);
	}

	@Override
	public void before(TCStatement stmt, Environment env, NameScope scope)
	{
		name.report(3359, "@Override only applies to expressions and statements");
	}

	@Override
	public void before(TCExpression exp, Environment env, NameScope scope)
	{
		name.report(3359, "@Override only applies to expressions and statements");
	}

	@Override
	public void before(TCModule module)
	{
		name.report(3359, "@Override only applies to expressions and statements");
	}

	@Override
	public void before(TCDefinition def, Environment env, NameScope scope)
	{
		if (Settings.dialect == Dialect.VDM_SL)
		{
			name.report(3360, "@Override not available in VDM-SL");
		}
		
		if (!args.isEmpty())
		{
			name.report(3361, "@Override has no arguments");
		}
		
		if (!def.isFunctionOrOperation())
		{
			name.report(3362, "@Override only applies to functions or operations");
		}
		else if (def.classDefinition != null)
		{
			boolean found = false;
			
			for (TCDefinition indef: def.classDefinition.localInheritedDefinitions)
			{
				if (indef.name.equals(def.name))
				{
					found = true;
					break;
				}
			}
			
			if (!found)
			{
				name.report(3363, "Definition does not @Override superclass");
			}
		}
	}
}
