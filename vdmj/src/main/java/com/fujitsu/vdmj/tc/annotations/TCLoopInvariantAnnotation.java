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

import java.util.List;

import com.fujitsu.vdmj.tc.definitions.TCAccessSpecifier;
import com.fujitsu.vdmj.tc.definitions.TCAssignmentDefinition;
import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpressionList;
import com.fujitsu.vdmj.tc.expressions.TCSeqEnumExpression;
import com.fujitsu.vdmj.tc.expressions.TCSetEnumExpression;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.modules.TCModule;
import com.fujitsu.vdmj.tc.statements.TCForAllStatement;
import com.fujitsu.vdmj.tc.statements.TCForIndexStatement;
import com.fujitsu.vdmj.tc.statements.TCForPatternBindStatement;
import com.fujitsu.vdmj.tc.statements.TCStatement;
import com.fujitsu.vdmj.tc.statements.TCWhileStatement;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCSeq1Type;
import com.fujitsu.vdmj.tc.types.TCSeqType;
import com.fujitsu.vdmj.tc.types.TCSet1Type;
import com.fujitsu.vdmj.tc.types.TCSetType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCLoopInvariantAnnotation extends TCAnnotation
{
	private static final long serialVersionUID = 1L;
	private TCAssignmentDefinition ghost = null;

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
			setGhost(env, stmt, inv);
			Environment local = loopEnvironment(stmt, env, inv);

			TCType itype = inv.typeCheck(local, null, scope, null);	// Just checks scope
			
			if (!(itype instanceof TCBooleanType))
			{
				inv.report(6007, "Invariant must be a boolean expression");
			}
		}
	}

	/**
	 * Look at the loop invariant and create a definition if it contains a GHOST$ variable.
	 * We assume there is only for for now.
	 */
	private void setGhost(Environment env, TCStatement stmt, TCExpression inv)
	{
		TCNameToken ghostName = new TCNameToken(inv.location, inv.location.module, "GHOST$");

		for (TCNameToken var: inv.getVariableNames())
		{
			if (var.getName().endsWith("$"))
			{
				ghostName = var;	// Override default name
				break;
			}
		}

		if (stmt instanceof TCForAllStatement)
		{
			TCForAllStatement fstmt = (TCForAllStatement)stmt;
			TCSetType st = fstmt.setType.getSet();

			if (st instanceof TCSet1Type)
			{
				st = new TCSetType(st.location, st.setof);
			}

			ghost = new TCAssignmentDefinition(
				TCAccessSpecifier.DEFAULT, ghostName, st,
				new TCSetEnumExpression(inv.location, new TCExpressionList()));

			ghost.typeCheck(env, NameScope.LOCAL);
		}
		else if (stmt instanceof TCForPatternBindStatement)
		{
			TCForPatternBindStatement fstmt = (TCForPatternBindStatement)stmt;
			TCSeqType st = fstmt.expType.getSeq();

			if (st instanceof TCSeq1Type)
			{
				st = new TCSeqType(st.location, st.seqof);
			}

			ghost = new TCAssignmentDefinition(
				TCAccessSpecifier.DEFAULT, ghostName, st,
				new TCSeqEnumExpression(inv.location, new TCExpressionList()));

			ghost.typeCheck(env, NameScope.LOCAL);
		}
	}

	/**
	 * Some checks have to be performed across all of the @LoopInvariants declared. So this is
	 * called from the typeCheck of each loop type.
	 */
	public static void typeCheck(TCStatement stmt, TCNameList loopVars, TCAnnotationList annotations)
	{
		List<TCLoopInvariantAnnotation> loopInvs = annotations.getInstances(TCLoopInvariantAnnotation.class);

		if (!loopInvs.isEmpty())
		{
			TCNameSet updates = stmt.updatesState(false);
			TCNameSet reasonsAbout = new TCNameSet();
			
			for (TCLoopInvariantAnnotation inv: loopInvs)
			{
				reasonsAbout.addAll(inv.args.get(0).getVariableNames());
			}
				
			if (!reasonsAbout.containsAll(updates))
			{
				// Invariant doesn't reason about some variable updated in the loop
				TCNameList missing = new TCNameList();
				missing.addAll(updates);
				missing.removeAll(reasonsAbout);
				stmt.warning(6007, "@LoopInvariants do not reason about " + missing);
			}

			if (!loopVars.isEmpty())	// So we must have at least one inv without these
			{
				boolean oneOkay = false;

				for (TCLoopInvariantAnnotation loopInv: loopInvs)
				{
					boolean hasLoopVars = false;

					for (TCNameToken loopVar: loopVars)
					{
						TCNameSet vars = loopInv.args.get(0).getVariableNames();

						if (vars.contains(loopVar))
						{
							hasLoopVars = true;
							break;
						}
					}

					if (!hasLoopVars)
					{
						oneOkay = true;
						break;
					}
				}

				if (!oneOkay)
				{
					stmt.report(6007, "At least one @LoopInvariant must be independent of " + loopVars);
				}
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
	private Environment loopEnvironment(TCStatement stmt, Environment env, TCExpression inv)
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
			TCSetType st = fstmt.setType.getSet();
			TCDefinitionList defs = fstmt.pattern.getDefinitions(st.setof, NameScope.LOCAL);

			if (ghost != null)
			{
				defs.add(ghost);
			}

			return new FlatEnvironment(defs, env);
		}
		else if (stmt instanceof TCForPatternBindStatement)
		{
			TCForPatternBindStatement fstmt = (TCForPatternBindStatement)stmt;
			TCSeqType st = fstmt.expType.getSeq();
			TCDefinitionList defs = fstmt.getPattern().getDefinitions(st.seqof, NameScope.LOCAL);

			if (ghost != null)
			{
				defs.add(ghost);
			}

			return new FlatEnvironment(defs, env);
		}
		else
		{
			return env;		// TC error reported elsewhere?
		}
	}
}
