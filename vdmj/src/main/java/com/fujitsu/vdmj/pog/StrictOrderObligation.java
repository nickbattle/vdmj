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
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.pog;

import com.fujitsu.vdmj.po.definitions.POTypeDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameToken;

public class StrictOrderObligation extends ProofObligation
{
	public StrictOrderObligation(POTypeDefinition def, POContextStack ctxt)
	{
		super(def.ordPattern1.location, POType.STRICT_ORDER, ctxt);
		TCNameToken ordT = def.name.getOrdName(location);
		String po = "(forall x:%T & not %N(x, x)) and\n"
			+ "(forall x:%T, y:%T, z:%T & %N(x, y) and %N(y, z) => %N(x, z))";
		po = po.replaceAll("%N", ordT.getName());
		po = po.replaceAll("%T", def.name.getName());
		source = ctxt.getSource(po);
	}
}
