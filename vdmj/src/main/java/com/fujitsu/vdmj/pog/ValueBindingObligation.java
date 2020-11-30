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

import com.fujitsu.vdmj.ast.expressions.ASTExpression;
import com.fujitsu.vdmj.ast.patterns.ASTPattern;
import com.fujitsu.vdmj.ast.types.ASTType;
import com.fujitsu.vdmj.po.definitions.POEqualsDefinition;
import com.fujitsu.vdmj.po.definitions.POValueDefinition;

public class ValueBindingObligation extends ProofObligation
{
	public ValueBindingObligation(POValueDefinition poValueDefinition, POContextStack ctxt)
	{
		super(poValueDefinition.location, POType.VALUE_BINDING, ctxt);
		StringBuilder sb = new StringBuilder();

		sb.append("exists ");
		sb.append(poValueDefinition.pattern);
		sb.append(":");
		sb.append(poValueDefinition.type);
		sb.append(" & ");
		sb.append(poValueDefinition.pattern);
		sb.append(" = ");
		sb.append(poValueDefinition.exp);

		value = ctxt.getObligation(sb.toString());
	}

	public ValueBindingObligation(POEqualsDefinition def, POContextStack ctxt)
	{
		super(def.location, POType.VALUE_BINDING, ctxt);
		StringBuilder sb = new StringBuilder();

		sb.append("exists ");
		sb.append(def.pattern);
		sb.append(":");
		sb.append(def.expType);
		sb.append(" & ");
		sb.append(def.pattern);
		sb.append(" = ");
		sb.append(def.test);

		value = ctxt.getObligation(sb.toString());
	}

	public ValueBindingObligation(
		ASTPattern p, ASTType t, ASTExpression e, POContextStack ctxt)
	{
		super(p.location, POType.VALUE_BINDING, ctxt);
		StringBuilder sb = new StringBuilder();

		sb.append("exists ");
		sb.append(p);
		sb.append(":");
		sb.append(t);
		sb.append(" & ");
		sb.append(p);
		sb.append(" = ");
		sb.append(e);

		value = ctxt.getObligation(sb.toString());
	}
}
