/*******************************************************************************
 *
 *	Copyright (c) 2017 Fujitsu Services Ltd.
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

import com.fujitsu.vdmj.po.definitions.POTypeDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameToken;

public class EquivRelationObligation extends ProofObligation
{
	public EquivRelationObligation(POTypeDefinition def, POContextStack ctxt)
	{
		super(def.location, POType.EQUIV_RELATION, ctxt);
		TCNameToken eqT = def.name.getEqName(location);
		String po = "(forall x:%T & %N(x, x)) and\n"
			+ "(forall x, y:%T & %N(x, y) => %N(y, x)) and\n"
			+ "(forall x, y, z:%T & %N(x, y) and %N(y, z) => %N(x, z))";
		po = po.replaceAll("%N", eqT.getName());
		po = po.replaceAll("%T", def.name.getName());
		value = ctxt.getObligation(po);
	}
}
