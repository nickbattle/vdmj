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

import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.definitions.TCLocalDefinition;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpressionList;
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
import com.fujitsu.vdmj.tc.types.TCSeqType;
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
			Environment local = loopEnvironment(stmt, env, inv);

			TCType itype = inv.typeCheck(local, null, scope, null);	// Just checks scope
			
			if (!(itype instanceof TCBooleanType))
			{
				inv.report(6007, "Invariant must be a boolean expression");
			}
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
			
			if (fstmt.setType.isSet(fstmt.location))
			{
				TCSetType st = fstmt.setType.getSet();
				TCDefinitionList defs = fstmt.pattern.getDefinitions(st.setof, NameScope.LOCAL);

				for (TCNameToken var: inv.getVariableNames())
				{
					if (var.getName().endsWith("$"))
					{
						defs.add(new TCLocalDefinition(inv.location, var, fstmt.setType));
					}
				}

				return new FlatEnvironment(defs, env);
			}

			return env;		// TC error reported elsewhere?
		}
		else if (stmt instanceof TCForPatternBindStatement)
		{
			TCForPatternBindStatement fstmt = (TCForPatternBindStatement)stmt;

			if (fstmt.expType.isSeq(fstmt.location))
			{
				TCSeqType st = fstmt.expType.getSeq();
				TCDefinitionList defs = fstmt.getPattern().getDefinitions(st.seqof, NameScope.LOCAL);

				for (TCNameToken var: inv.getVariableNames())
				{
					if (var.getName().endsWith("$"))
					{
						defs.add(new TCLocalDefinition(inv.location, var, fstmt.expType));
					}
				}

				return new FlatEnvironment(defs, env);
			}

			return env;		// TC error reported elsewhere?
		}
		else
		{
			return env;		// TC error reported elsewhere?
		}
	}
}
