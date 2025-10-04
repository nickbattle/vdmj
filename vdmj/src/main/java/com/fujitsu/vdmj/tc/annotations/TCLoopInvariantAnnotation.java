/*******************************************************************************
 *
 *	Copyright (c) 2025 Nick Battle.
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

package com.fujitsu.vdmj.tc.annotations;

import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpressionList;
import com.fujitsu.vdmj.tc.expressions.TCVariableExpression;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.modules.TCModule;
import com.fujitsu.vdmj.tc.statements.TCForAllStatement;
import com.fujitsu.vdmj.tc.statements.TCForIndexStatement;
import com.fujitsu.vdmj.tc.statements.TCForPatternBindStatement;
import com.fujitsu.vdmj.tc.statements.TCStatement;
import com.fujitsu.vdmj.tc.statements.TCWhileStatement;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCLoopInvariantAnnotation extends TCAnnotation
{
	private static final long serialVersionUID = 1L;
	private boolean hasLoopVars = false;
	public TCNameToken ghostName = null;

	public TCLoopInvariantAnnotation(TCIdentifierToken name, TCExpressionList args)
	{
		super(name, args);
	}

	@Override
	public void tcBefore(TCDefinition def, Environment env, NameScope scope)
	{
		name.report(6006, "@LoopInvariant only applies to loop statements");
	}

	@Override
	public void tcBefore(TCModule module)
	{
		name.report(6006, "@LoopInvariant only applies to loop statements");
	}

	@Override
	public void tcBefore(TCClassDefinition clazz)
	{
		name.report(6006, "@LoopInvariant only applies to loop statements");
	}

	@Override
	public void tcBefore(TCExpression exp, Environment env, NameScope scope)
	{
		name.report(6006, "@LoopInvariant only applies to loop statements");
	}

	@Override
	public void tcBefore(TCStatement stmt, Environment env, NameScope scope)
	{
		if (!isLoop(stmt))
		{
			name.report(6006, "@LoopInvariant only applies to loop statements");
		}
		else if (args.size() != 1 && args.size() != 2)
		{
			name.report(6007, "@LoopInvariant args: <boolean expression> [, <ghost>]");
		}
		else if (args.size() == 2)
		{
			if (stmt instanceof TCWhileStatement || stmt instanceof TCForIndexStatement)
			{
				args.get(1).warning(6007, "@LoopInvariant ghost not used");
			}
			else if (args.get(1) instanceof TCVariableExpression)
			{
				TCVariableExpression variable = (TCVariableExpression)args.get(1);

				if (env.findName(variable.name, scope) != null)
				{
					args.get(1).report(6007, "@LoopInvariant ghost already in scope");
				}
				
				ghostName = variable.name;
			}
			else
			{
				args.get(1).report(6007, "@LoopInvariant ghost must be a name");
			}
		}

		// Further checks are performed across the collection of all LoopInvariants on
		// a particular loop statement. See TCLoopAnnotations.typeCheck().
	}

	/**
	 * Check for the various loop types that are allowed for this annotation.
	 */
	private boolean isLoop(TCStatement stmt)
	{
		return
			(stmt instanceof TCWhileStatement) ||
			(stmt instanceof TCForIndexStatement) ||
			(stmt instanceof TCForAllStatement) ||
			(stmt instanceof TCForPatternBindStatement);
	}

	/**
	 * Mark this invariant, if it reasons about the loop variable(s) that are bound
	 * by the for-loop statements. These cannot be used outside the loop body, because
	 * the variables are not in scope.
	 */
	public void setHasLoopVars()
	{
		hasLoopVars = true;
	}

	public boolean hasLoopVars()
	{
		return hasLoopVars;
	}
}
