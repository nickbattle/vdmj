/*******************************************************************************
 *
 *	Copyright (c) 2016 Fujitsu Services Ltd.
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

package com.fujitsu.vdmj.pog;

import java.util.Collection;
import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.po.definitions.POStateDefinition;
import com.fujitsu.vdmj.po.expressions.POExists1Expression;
import com.fujitsu.vdmj.po.expressions.POExistsExpression;
import com.fujitsu.vdmj.po.expressions.POForAllExpression;
import com.fujitsu.vdmj.po.expressions.POIotaExpression;
import com.fujitsu.vdmj.po.expressions.POLambdaExpression;
import com.fujitsu.vdmj.po.expressions.POLetBeStExpression;
import com.fujitsu.vdmj.po.expressions.POMapCompExpression;
import com.fujitsu.vdmj.po.expressions.POSeqCompExpression;
import com.fujitsu.vdmj.po.expressions.POSetCompExpression;
import com.fujitsu.vdmj.po.patterns.POIdentifierPattern;
import com.fujitsu.vdmj.po.patterns.POMultipleBind;
import com.fujitsu.vdmj.po.patterns.POMultipleTypeBind;
import com.fujitsu.vdmj.po.patterns.POPatternList;
import com.fujitsu.vdmj.po.patterns.POTypeBind;
import com.fujitsu.vdmj.po.statements.POLetBeStStatement;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class POForAllContext extends POContext
{
	public final List<POMultipleBind> bindings;
	private String qualifier;

	public POForAllContext(POMapCompExpression exp)
	{
		this.bindings = exp.bindings;
	}

	public POForAllContext(POSetCompExpression exp)
	{
		this.bindings = exp.bindings;
	}

	public POForAllContext(POSeqCompExpression exp)
	{
		this.bindings = exp.bind.getMultipleBindList();
	}

	public POForAllContext(POForAllExpression exp)
	{
		this.bindings = exp.bindList;
	}

	public POForAllContext(POExistsExpression exp)
	{
		this.bindings = exp.bindList;
	}

	public POForAllContext(POExists1Expression exp)
	{
		this.bindings = exp.bind.getMultipleBindList();
	}

	public POForAllContext(POIotaExpression exp)
	{
		this.bindings = exp.bind.getMultipleBindList();
	}

	public POForAllContext(POLambdaExpression exp)
	{
		this.bindings = new Vector<POMultipleBind>();

		for (POTypeBind tb: exp.bindList)
		{
			POPatternList pl = new POPatternList();
			pl.add(tb.pattern);
			POMultipleTypeBind mtb = new POMultipleTypeBind(pl, tb.type);
			bindings.add(mtb);
		}
	}

	public POForAllContext(POLetBeStExpression exp)
	{
		this.bindings = exp.bind.getMultipleBindList();
	}

	public POForAllContext(POLetBeStStatement stmt)
	{
		this.bindings = stmt.bind.getMultipleBindList();
	}

	public POForAllContext(Collection<? extends TCNameToken> updates, Environment env)
	{
		this.bindings = new Vector<POMultipleBind>();
		
		for (TCNameToken var: updates)		// Can be empty
		{
			TCDefinition def = env.findName(var, NameScope.NAMESANDSTATE);
			
			if (def != null)
			{
				POPatternList plist = new POPatternList();
				plist.add(new POIdentifierPattern(var));
				bindings.add(new POMultipleTypeBind(plist, def.getType()));
			}
		}
	}

	public POForAllContext(POStateDefinition state, LexLocation from)
	{
		this.bindings = new Vector<POMultipleBind>();
		POPatternList plist = new POPatternList();
		plist.add(state.getPattern(from));
		bindings.add(new POMultipleTypeBind(plist, state.getType()));
	}

	public POForAllContext(Collection<? extends TCNameToken> updates, String qualifier, Environment env)
	{
		this(updates, env);
		this.qualifier = qualifier;
	}

	@Override
	public boolean isScopeBoundary()
	{
		return true;
	}

	@Override
	public String getSource()
	{
		StringBuilder sb = new StringBuilder();

		if (!bindings.isEmpty())	// eg. when a loop makes no updates
		{
			sb.append("forall ");
			String prefix = "";

			for (POMultipleBind mb: bindings)
			{
				sb.append(prefix);
				sb.append(mb);
				prefix = ", ";
			}

			sb.append(" &");
		}

		if (qualifier != null)
		{
			if (sb.length() > 0) sb.append(" ");
			sb.append(qualifier);
			sb.append(" =>");
		}

		return sb.toString();
	}
	
	@Override
	public TCNameSet reasonsAbout()
	{
		TCNameSet result = new TCNameSet();
		
		for (POMultipleBind mbind: bindings)
		{
			result.addAll(mbind.getVariableNames());
		}
		
		return result;
	}
}
