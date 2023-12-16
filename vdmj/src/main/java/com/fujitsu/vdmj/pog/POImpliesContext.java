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

import com.fujitsu.vdmj.po.definitions.POExplicitOperationDefinition;
import com.fujitsu.vdmj.po.definitions.POImplicitOperationDefinition;
import com.fujitsu.vdmj.po.expressions.POExpression;

public class POImpliesContext extends POContext
{
	public final String exp;

	public POImpliesContext(POExpression precondition)
	{
		this.exp = precondition.toString();
	}

	public POImpliesContext(POExplicitOperationDefinition def)
	{
		this.exp = preconditionCall(def.name, null, def.predef.getParamPatternList(), def.precondition);
	}

	public POImpliesContext(POImplicitOperationDefinition def)
	{
		this.exp = preconditionCall(def.name, null, def.predef.getParamPatternList(), def.precondition);
	}

	@Override
	public String getContext()
	{
		StringBuilder sb = new StringBuilder();

		sb.append(exp);
		sb.append(" =>");

		return sb.toString();
	}
}
