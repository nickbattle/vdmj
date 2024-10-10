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
 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.pog;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.definitions.PODefinitionList;
import com.fujitsu.vdmj.po.definitions.POExplicitFunctionDefinition;
import com.fujitsu.vdmj.po.definitions.POExplicitOperationDefinition;
import com.fujitsu.vdmj.po.definitions.POImplicitFunctionDefinition;
import com.fujitsu.vdmj.po.definitions.POImplicitOperationDefinition;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.patterns.POPattern;
import com.fujitsu.vdmj.po.patterns.POPatternList;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;

public class ParameterPatternObligation extends ProofObligation
{
	private final PODefinition predef;

	public ParameterPatternObligation(
		POExplicitFunctionDefinition def, POContextStack ctxt)
	{
		super(def.location, POType.FUNC_PATTERNS, ctxt);
		this.predef = def.predef;
		source = ctxt.getObligation(
			generate(def.paramPatternList, def.type.parameters, def.type.result));
	}

	public ParameterPatternObligation(
		POImplicitFunctionDefinition def, POContextStack ctxt)
	{
		super(def.location, POType.FUNC_PATTERNS, ctxt);
		this.predef = def.predef;
		source = ctxt.getObligation(
			generate(def.getParamPatternList(), def.type.parameters, def.type.result));
	}

	public ParameterPatternObligation(
		POExplicitOperationDefinition def, POContextStack ctxt)
	{
		super(def.location, POType.OPERATION_PATTERNS, ctxt);
		this.predef = def.predef;
		source = ctxt.getObligation(
			generate(def.getParamPatternList(), def.type.parameters, def.type.result));
	}

	public ParameterPatternObligation(
		POImplicitOperationDefinition def, POContextStack ctxt)
	{
		super(def.location, POType.OPERATION_PATTERNS, ctxt);
		this.predef = def.predef;
		source = ctxt.getObligation(
			generate(def.getListParamPatternList(), def.type.parameters, def.type.result));
	}

	private String generate(List<POPatternList> plist, TCTypeList params, TCType result)
	{
		StringBuilder foralls = new StringBuilder();
		StringBuilder argnames = new StringBuilder();
		StringBuilder exists = new StringBuilder();

		String INDENT = "  ";
		String fprefix = "";
		String lprefix = "";
		int argn = 1;

		for (POPatternList pl: plist)
		{
			StringBuilder ebindings = new StringBuilder();
			StringBuilder epredicates = new StringBuilder();
			Iterator<TCType> titer = params.iterator();
			String eprefix = "";
			String aprefix = "";
			int bindn = 1;
			
			if (!pl.isEmpty())
			{
				argnames.append("(");
				
				if (predef != null)
				{
					exists.append(INDENT);
					exists.append(INDENT);
					exists.append(lprefix);
					exists.append("(exists ");
				}
				else
				{
					exists.append(INDENT);
					exists.append(lprefix);
					exists.append("(exists ");
				}
				
				Set<String> existingBindings = new HashSet<String>();
	
				for (POPattern p: pl)
				{
					String aname = "arg" + argn++;
					String bname = "bind" + bindn++;
					TCType atype = titer.next();
					POExpression pmatch = p.getMatchingExpression();
					PODefinitionList dlist = p.getDefinitions(atype);
					
					foralls.append(fprefix);
					foralls.append(aname);
					foralls.append(":");
					foralls.append(atype.toExplicitString(location));
	
					argnames.append(aprefix);
					argnames.append(aname);
	
					ebindings.append(aprefix);
					aprefix = ", ";
					ebindings.append(bname);
					ebindings.append(":");
					ebindings.append(atype.toExplicitString(location));
	
					for (PODefinition def: dlist)
					{
						if (def.name != null && !existingBindings.contains(def.name.getName()))
						{
							ebindings.append(aprefix);
							ebindings.append(def.name.getName());
							ebindings.append(":");
							ebindings.append(def.getType().toExplicitString(location));
							existingBindings.add(def.name.getName());
						}
					}
					
					for (String any: getAnyBindings(pmatch))
					{
						if (!existingBindings.contains(any))
						{
							ebindings.append(aprefix);
							ebindings.append(any);
							ebindings.append(":?");
							existingBindings.add(any);
						}
					}
	
					epredicates.append(eprefix);
					eprefix = " and ";
					epredicates.append("(");
					epredicates.append(aname);
					epredicates.append(" = ");
					epredicates.append(bname);
					epredicates.append(") and (");
					epredicates.append(pmatch);
					epredicates.append(" = ");
					epredicates.append(bname);
					epredicates.append(")");
	
					fprefix = ", ";
				}
				
				argnames.append(")");
				exists.append(ebindings.toString());
				exists.append(" & ");
				exists.append(epredicates.toString());
				exists.append(")\n");
				lprefix = "and ";
			}

			if (result instanceof TCFunctionType)
			{
				TCFunctionType ft = (TCFunctionType)result;
				result = ft.result;
				params = ft.parameters;
			}
			else
			{
				break;
			}
		}

		foralls.append(" &\n");

		if (predef != null)
		{
			foralls.append(INDENT);
			foralls.append(predef.name.getName());
			foralls.append(argnames);
			foralls.append(" =>\n");
		}

		return "forall " + foralls.toString() + exists.toString();
	}

	/**
	 * Pick out the ignore patterns, which produce "any" variables.
	 * Currently this is done by looking for the name pattern, but it would
	 * be better to have a visitor to do this properly, and get the type.
	 */
	private List<String> getAnyBindings(POExpression pmatch)
	{
		Pattern p = Pattern.compile("\\$any\\d+");
		Matcher m = p.matcher(pmatch.toString());
		List<String> anys = new Vector<String>();
		
		while (m.find())
		{
			anys.add(m.group());
		}
		
		return anys;
	}
}
