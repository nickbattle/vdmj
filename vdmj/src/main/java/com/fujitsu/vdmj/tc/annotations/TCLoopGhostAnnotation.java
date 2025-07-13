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
import com.fujitsu.vdmj.tc.modules.TCModule;
import com.fujitsu.vdmj.tc.statements.TCForAllStatement;
import com.fujitsu.vdmj.tc.statements.TCForPatternBindStatement;
import com.fujitsu.vdmj.tc.statements.TCStatement;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCLoopGhostAnnotation extends TCAnnotation
{
	private static final long serialVersionUID = 1L;

	public String ghostName = "GHOST$";		// A default

	public TCLoopGhostAnnotation(TCIdentifierToken name, TCExpressionList args)
	{
		super(name, args);
	}

	@Override
	public void tcBefore(TCDefinition def, Environment env, NameScope scope)
	{
		name.report(6006, "@LoopGhost only applies to loop statements");
	}

	@Override
	public void tcBefore(TCModule module)
	{
		name.report(6006, "@LoopGhost only applies to loop statements");
	}

	@Override
	public void tcBefore(TCClassDefinition clazz)
	{
		name.report(6006, "@LoopGhost only applies to loop statements");
	}

	@Override
	public void tcBefore(TCExpression exp, Environment env, NameScope scope)
	{
		name.report(6006, "@LoopGhost only applies to loop statements");
	}

	@Override
	public void tcBefore(TCStatement stmt, Environment env, NameScope scope)
	{
		if (!isLoop(stmt))
		{
			name.report(6006, "@LoopGhost only applies to for-all and for-pattern statements");
		}
		else if (args.size() != 1)
		{
			name.report(6007, "@LoopGhost expects one argument");
		}
		else if (!(args.firstElement() instanceof TCVariableExpression))
		{
			name.report(6007, "@LoopGhost expects a variable name");
		}
		else
		{
			TCVariableExpression ghostExp = (TCVariableExpression)args.firstElement();

			if (env.findName(ghostExp.name, NameScope.NAMESANDSTATE) != null)
			{
				name.report(6007, "@LoopGhost variable already exists");
			}
			else
			{
				ghostName = ghostExp.name.getName();
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
			// (stmt instanceof TCWhileStatement) ||
			// (stmt instanceof TCForIndexStatement) ||
			(stmt instanceof TCForAllStatement) ||
			(stmt instanceof TCForPatternBindStatement);
	}
}
