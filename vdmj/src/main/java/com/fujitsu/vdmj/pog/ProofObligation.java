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

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.po.annotations.POAnnotationList;
import com.fujitsu.vdmj.po.patterns.visitors.POGetMatchingExpressionVisitor;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;

abstract public class ProofObligation implements Comparable<ProofObligation>
{
	public final LexLocation location;
	public final POType kind;
	public final String name;

	public int number;
	public String value;
	public POStatus status;
	public boolean isCheckable;
	public TCTypeList typeParams;
	public POAnnotationList annotations;
	public Context counterexample;
	public String countermessage;
	public String provedBy;

	private int var = 1;
	private TCExpression checkedExpression = null;

	public ProofObligation(LexLocation location, POType kind, POContextStack ctxt)
	{
		this.location = location;
		this.kind = kind;
		this.name = ctxt.getName();
		this.status = POStatus.UNPROVED;
		this.number = 0;
		this.isCheckable = ctxt.isCheckable();	// Set false for operation POs
		this.typeParams = ctxt.getTypeParams();
		this.annotations = ctxt.getAnnotations();
		this.counterexample = new Context(location, "Counterexample", null);
		this.countermessage = null;
		this.provedBy = null;
		
		if (!isCheckable)
		{
			this.status = POStatus.UNCHECKED;	// Implies unproved
		}

		POGetMatchingExpressionVisitor.init();	// Reset the "any" count, before PO creation
	}

	public String getValue()
	{
		return value;
	}
	
	public void setStatus(POStatus status)
	{
		this.status = status;
	}
	
	public void setProvedBy(String provedBy)
	{
		this.provedBy = provedBy;
	}

	public void setCounterexample(Context path)
	{
		counterexample.clear();
		Context ctxt = path;
		
		while (ctxt != null && ctxt.outer != null)
		{
			counterexample.putAll(ctxt);
			ctxt = ctxt.outer;
		}
	}
	
	public void setCounterMessage(String message)
	{
		this.countermessage = message;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append(name);
		sb.append(": ");
		sb.append(kind);
		sb.append(" obligation ");
		sb.append(location);
		sb.append("\n");
		sb.append(value);
		
		return sb.toString();
	}
	
	@Override
	public boolean equals(Object other)
	{
		if (other instanceof ProofObligation)
		{
			ProofObligation opo = (ProofObligation)other;
			return kind == opo.kind && location == opo.location;
		}
		
		return false; 
	}

	protected String getVar(String root)
	{
		return root + var++;
	}

	@Override
	public int compareTo(ProofObligation other)
	{
		return number - other.number;
	}

	public void setCheckedExpression(TCExpression checkedExpression)
	{
		this.checkedExpression = checkedExpression;
	}
	
	public TCExpression getCheckedExpression()
	{
		return checkedExpression;
	}
	
	/**
	 * Fully qualify a type name, if it is not declared local to the PO.
	 */
	protected String explicitType(TCType type, LexLocation poLoc)
	{
		return type.toExplicitString(poLoc);
	}
}
