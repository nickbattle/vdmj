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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.tc.annotations;

import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpressionList;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.modules.TCModule;
import com.fujitsu.vdmj.tc.statements.TCForAllStatement;
import com.fujitsu.vdmj.tc.statements.TCForIndexStatement;
import com.fujitsu.vdmj.tc.statements.TCForPatternBindStatement;
import com.fujitsu.vdmj.tc.statements.TCStatement;
import com.fujitsu.vdmj.tc.statements.TCWhileStatement;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCSetType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCLoopInvariantAnnotation extends TCAnnotation
{
	private static final long serialVersionUID = 1L;

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
		super.tcBefore(stmt, env, scope);
	}

	@Override
	public void tcAfter(TCStatement stmt, TCType type, Environment env, NameScope scope)
	{
		if (!isLoop(stmt))
		{
			name.report(6006, "@LoopInvariant only applies to loop statements");
		}
		else if (args.size() != 1)
		{
			name.report(6007, "@LoopInvariant is one boolean condition");
		}
		else
		{
			TCExpression inv = args.get(0);
			Environment local = loopEnvironment(stmt, env);
			TCType itype = inv.typeCheck(local, null, scope, null);	// Just checks scope
			
			if (!(itype instanceof TCBooleanType))
			{
				inv.report(6007, "Invariant must be a boolean expression");
			}
			
			TCNameSet reasonsAbout = inv.getVariableNames();
			TCNameSet updates = stmt.updatesState(false);
			
			if (!reasonsAbout.containsAll(updates))
			{
				// Invariant doesn't reason about some variable updated
				TCNameList missing = new TCNameList();
				missing.addAll(updates);
				missing.removeAll(reasonsAbout);
				name.warning(6007, "@LoopInvariant does not reason about " + missing);
			}
		}
	}
	
	private boolean isLoop(TCStatement stmt)
	{
		return
			(stmt instanceof TCWhileStatement) ||
			(stmt instanceof TCForIndexStatement) ||
			(stmt instanceof TCForAllStatement) ||
			(stmt instanceof TCForPatternBindStatement);
	}
	
	/**
	 * The for-loops create local variables, while loops don't.
	 */
	private Environment loopEnvironment(TCStatement stmt, Environment env)
	{
		if (stmt instanceof TCWhileStatement)
		{
			return env;		// No loop variable
		}
		else if (stmt instanceof TCForIndexStatement)
		{
			TCForIndexStatement fstmt = (TCForIndexStatement)stmt;
			return new FlatEnvironment(fstmt.vardef, env);
		}
		else if (stmt instanceof TCForAllStatement)
		{
			TCForAllStatement fstmt = (TCForAllStatement)stmt;
			
			if (fstmt.setType.isSet(fstmt.location))
			{
				TCSetType st = fstmt.setType.getSet();
				TCDefinitionList defs = fstmt.pattern.getDefinitions(st.setof, NameScope.LOCAL);
				return new FlatEnvironment(defs, env);
			}

			return env;		// TC error reported elsewhere?
		}
		else if (stmt instanceof TCForPatternBindStatement)
		{
			TCForPatternBindStatement fstmt = (TCForPatternBindStatement)stmt;
			TCDefinitionList defs = fstmt.patternBind.getDefinitions();
			return new FlatEnvironment(defs, env);
		}
		else
		{
			return env;		// TC error reported elsewhere?
		}
	}
}
