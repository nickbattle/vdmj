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
 *
 ******************************************************************************/

package com.fujitsu.vdmj.po.annotations;

import com.fujitsu.vdmj.po.POMappedList;
import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.annotations.TCAnnotation;
import com.fujitsu.vdmj.tc.annotations.TCAnnotationList;

public class POAnnotationList extends POMappedList<TCAnnotation, POAnnotation>
{
	private static final long serialVersionUID = 1L;
	
	public POAnnotationList()
	{
		super();
	}
	
	public POAnnotationList(TCAnnotationList from) throws Exception
	{
		super(from);
	}

	public ProofObligationList before(POContextStack ctxt, PODefinition def)
	{
		ProofObligationList list = new ProofObligationList();
		
		for (POAnnotation annotation: this)
		{
			list.addAll(annotation.before(ctxt, def));
		}
		
		return list;
	}

	public void after(POContextStack ctxt, PODefinition def, ProofObligationList obligations)
	{
		for (POAnnotation annotation: this)
		{
			annotation.after(ctxt, def, obligations);
		}
	}
}
