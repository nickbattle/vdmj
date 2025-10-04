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

import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.mapper.Mappable;
import com.fujitsu.vdmj.tc.definitions.TCAccessSpecifier;
import com.fujitsu.vdmj.tc.definitions.TCAssignmentDefinition;
import com.fujitsu.vdmj.tc.expressions.TCExpressionList;
import com.fujitsu.vdmj.tc.expressions.TCSeqEnumExpression;
import com.fujitsu.vdmj.tc.expressions.TCSetEnumExpression;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.statements.TCForAllStatement;
import com.fujitsu.vdmj.tc.statements.TCForIndexStatement;
import com.fujitsu.vdmj.tc.statements.TCForPatternBindStatement;
import com.fujitsu.vdmj.tc.statements.TCStatement;
import com.fujitsu.vdmj.tc.statements.TCWhileStatement;
import com.fujitsu.vdmj.tc.types.TCSeq1Type;
import com.fujitsu.vdmj.tc.types.TCSeqType;
import com.fujitsu.vdmj.tc.types.TCSet1Type;
import com.fujitsu.vdmj.tc.types.TCSetType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCLoopInvariantList extends Vector<TCLoopInvariantAnnotation> implements Mappable
{
	private final TCStatement stmt;
	private final List<TCLoopGhostAnnotation> ghosts;
	private TCAssignmentDefinition ghostDef = null;

	public TCLoopInvariantList(TCStatement stmt)
	{
		List<TCLoopInvariantAnnotation> invariants =
			stmt.getAnnotations().getInstances(TCLoopInvariantAnnotation.class);

		super.addAll(invariants);
		this.ghosts = stmt.getAnnotations().getInstances(TCLoopGhostAnnotation.class);
		this.stmt = stmt;
	}

	public List<TCLoopGhostAnnotation> getGhostNames()
	{
		return ghosts;
	}

	private TCNameToken getGhostName()
	{
		if (!ghosts.isEmpty())
		{
			String name = ghosts.get(0).ghostName;
			return new TCNameToken(stmt.location, stmt.location.module, name);
		}
		else
		{
			return new TCNameToken(stmt.location, stmt.location.module, "DONE_" + stmt.location.startLine + "$");
		}
	}

	public TCAssignmentDefinition getGhostDefinition(TCStatement stmt, Environment env)
	{
		if (ghostDef == null)
		{
			if (stmt instanceof TCForAllStatement)
			{
				TCForAllStatement fstmt = (TCForAllStatement)stmt;
				TCSetType st = fstmt.setType.getSet();

				if (st instanceof TCSet1Type)
				{
					st = new TCSetType(st.location, st.setof);
				}

				ghostDef = new TCAssignmentDefinition(
					TCAccessSpecifier.DEFAULT, getGhostName(), st,
					new TCSetEnumExpression(stmt.location, new TCExpressionList()));

				ghostDef.typeCheck(env, NameScope.LOCAL);
			}
			else if (stmt instanceof TCForPatternBindStatement)
			{
				TCForPatternBindStatement fstmt = (TCForPatternBindStatement)stmt;
				TCSeqType st = fstmt.expType.getSeq();

				if (st instanceof TCSeq1Type)
				{
					st = new TCSeqType(st.location, st.seqof);
				}

				ghostDef = new TCAssignmentDefinition(
					TCAccessSpecifier.DEFAULT, getGhostName(), st,
					new TCSeqEnumExpression(stmt.location, new TCExpressionList()));

				ghostDef.typeCheck(env, NameScope.LOCAL);
			}
		}

		return ghostDef;
	}

	/**
	 * A Ghost variable is required for some loop types. 
	 */
	public Environment getGhostEnvironment(TCStatement stmt, Environment env)
	{
		if (stmt instanceof TCWhileStatement)
		{
			return env;		// No ghost variable
		}
		else if (stmt instanceof TCForIndexStatement)
		{
			return env;		// No ghost variable
		}
		else if (stmt instanceof TCForAllStatement)
		{
			return new FlatEnvironment(getGhostDefinition(stmt, env), env);
		}
		else if (stmt instanceof TCForPatternBindStatement)
		{
			return new FlatEnvironment(getGhostDefinition(stmt, env), env);
		}
		else
		{
			return env;		// Can't happen?
		}
	}
}
