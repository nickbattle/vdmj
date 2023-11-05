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
 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.pog;

import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.po.definitions.POExplicitFunctionDefinition;
import com.fujitsu.vdmj.po.patterns.POPattern;
import com.fujitsu.vdmj.po.patterns.POPatternList;
import com.fujitsu.vdmj.po.patterns.visitors.PORemoveIgnoresVisitor;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
import com.fujitsu.vdmj.tc.types.TCType;

public class TotalFunctionObligation extends ProofObligation
{
	public TotalFunctionObligation(POExplicitFunctionDefinition def, POContextStack ctxt)
	{
		super(def.location, POType.TOTAL, ctxt);
		value = ctxt.getObligation(getContext(def.name.getName(), def));
	}

	private String getContext(String name, POExplicitFunctionDefinition def)
	{
		boolean curried = (def.paramPatternList.size() > 1);
		List<String> defined = new Vector<String>();
		TCFunctionType ftype = def.type;

		StringBuilder fapply = new StringBuilder();
		fapply.append(name);
		String sep = "";

		if (def.typeParams != null)
		{
			fapply.append("[");
			
			for (TCType ptype: def.typeParams)
			{
				fapply.append(sep);
				fapply.append(ptype.toString());
				sep = ", ";
			}
			
			fapply.append("]");
		}
		
		fapply.append("(");
		
		if (!ftype.parameters.isEmpty())
		{
			sep = "";
			PORemoveIgnoresVisitor.init();
			
    		for (POPatternList pl: def.paramPatternList)
    		{
    			int param = 0;
    			
    			for (POPattern p: pl)
    			{
    				fapply.append(sep);
    				POPattern p2 = p.removeIgnorePatterns();
   					p2.setMaximal(ftype.parameters.get(param).isMaximal());
    				fapply.append(p2);
					sep = ", ";
					param++;
    			}

    			if (ftype.result instanceof TCFunctionType)
    			{
    				if (ftype.partial)		// Needs to be defined too
    				{
    					defined.add(fapply.toString() + ")");
    				}

    				TCFunctionType rtype = (TCFunctionType)ftype.result;
    				
    				if (curried && rtype.hasTotal())
    				{
    					ftype = rtype;
    				}
    				else
    				{
    					break;
    				}

    				if (curried)
	    			{
	    				sep = ")(";
	    			}
    			}
    		}
		}

		fapply.append(")");

		StringBuilder sb = new StringBuilder();
		sep = "";
		
		for (String tbdef: defined)
		{
			sb.append(sep);
			sb.append("defined_(");
			sb.append(tbdef);
			sb.append(")");
			sep = " => ";
		}
		
		sb.append(sep);
		sb.append("is_(");
		sb.append(fapply);
		sb.append(", ");
		sb.append(explicitType(ftype.result, def.location));
		sb.append(")");

		return sb.toString();
	}
}
