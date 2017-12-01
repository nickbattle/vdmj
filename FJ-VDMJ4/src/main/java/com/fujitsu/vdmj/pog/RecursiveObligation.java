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
 *
 ******************************************************************************/

package com.fujitsu.vdmj.pog;

import com.fujitsu.vdmj.po.definitions.POExplicitFunctionDefinition;
import com.fujitsu.vdmj.po.definitions.POImplicitFunctionDefinition;
import com.fujitsu.vdmj.po.expressions.POApplyExpression;
import com.fujitsu.vdmj.po.patterns.POPatternList;
import com.fujitsu.vdmj.po.types.POPatternListTypePair;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.util.Utils;

public class RecursiveObligation extends ProofObligation
{
	public RecursiveObligation(
		POExplicitFunctionDefinition def, POApplyExpression apply, POContextStack ctxt)
	{
		super(apply.location, POType.RECURSIVE, ctxt);
		StringBuilder sb = new StringBuilder();

		sb.append(def.measureName.getName());
		
		if (def.typeParams != null)
		{
			sb.append("[");
			
			for (TCNameToken type: def.typeParams)
			{
				sb.append("@");
				sb.append(type);
			}
			
			sb.append("]");
		}
		
		String sep = "";
		sb.append("(");
		
		for (POPatternList plist: def.paramPatternList)
		{
			 sb.append(sep);
			 sb.append(Utils.listToString(plist));
			 sep = ", ";
		}

		sb.append(")");
		sb.append(def.measureLexical > 0 ? " LEX" + def.measureLexical + "> " : " > ");
		sb.append(apply.getMeasureApply(def.measureName));

		value = ctxt.getObligation(sb.toString());
	}

	public RecursiveObligation(
		POImplicitFunctionDefinition def, POApplyExpression apply, POContextStack ctxt)
	{
		super(def.location, POType.RECURSIVE, ctxt);
		StringBuilder sb = new StringBuilder();

		sb.append(def.measureName);
		sb.append("(");

		for (POPatternListTypePair pltp: def.parameterPatterns)
		{
			sb.append(pltp.patterns);
		}

		sb.append(")");
		sb.append(def.measureLexical > 0 ? " LEX" + def.measureLexical + "> " : " > ");
		sb.append(def.measureName);
		sb.append("(");
		sb.append(apply.args);
		sb.append(")");

		value = ctxt.getObligation(sb.toString());
	}
}
