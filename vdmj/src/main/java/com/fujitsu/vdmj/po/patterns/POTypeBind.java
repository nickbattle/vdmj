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

package com.fujitsu.vdmj.po.patterns;

import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.po.patterns.visitors.POBindVisitor;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POGState;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;

public class POTypeBind extends POBind
{
	private static final long serialVersionUID = 1L;
	public final TCType type;

	public POTypeBind(POPattern pattern, TCType type)
	{
		super(pattern.location, pattern);
		this.type = type;
	}

	@Override
	public List<POMultipleBind> getMultipleBindList()
	{
		POPatternList plist = new POPatternList();
		plist.add(pattern);
		List<POMultipleBind> mblist = new Vector<POMultipleBind>();
		mblist.add(new POMultipleTypeBind(plist, type));
		return mblist;
	}

	@Override
	public String toString()
	{
		return pattern + ":" + type;
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt, POGState pogState, Environment env)
	{
		return new ProofObligationList();
	}
	
	@Override
	public TCNameSet getVariableNames()
	{
		TCNameSet names = new TCNameSet();
		names.addAll(pattern.getVariableNames());
		return names;
	}

	@Override
	public <R, S> R apply(POBindVisitor<R, S> visitor, S arg)
	{
		return visitor.caseTypeBind(this, arg);
	}
}
