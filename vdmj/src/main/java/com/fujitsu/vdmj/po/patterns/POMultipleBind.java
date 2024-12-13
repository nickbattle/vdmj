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

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.po.PONode;
import com.fujitsu.vdmj.po.patterns.visitors.POMultipleBindVisitor;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POGState;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.typechecker.Environment;

/**
 * The parent class of {@link POMultipleSetBind} and {@link POMultipleTypeBind}.
 */
public abstract class POMultipleBind extends PONode
{
	private static final long serialVersionUID = 1L;

	/** The textual location of the bind. */
	public final LexLocation location;
	/** The list of patterns for this bind. */
	public final POPatternList plist;

	/**
	 * Create a multiple bind with the given pattern list. The location is
	 * taken from the location of the first pattern in the list.
	 */

	public POMultipleBind(POPatternList plist)
	{
		this.plist = plist;
		this.location = plist.get(0).location;
	}

	/** Return this one bind as a list of {@link POMultipleBind}. */
	public List<POMultipleBind> getMultipleBindList()
	{
		List<POMultipleBind> list = new Vector<POMultipleBind>();
		list.add(this);
		return list;
	}

	/** Get a list of POs. */
	abstract public ProofObligationList getProofObligations(POContextStack ctxt, POGState pogState, Environment env);

	/** Get variables used by the bind */
	abstract public TCNameSet getVariableNames();
	
	/**
	 * Implemented by all multiple binds to allow visitor processing.
	 */
	abstract public <R, S> R apply(POMultipleBindVisitor<R, S> visitor, S arg);
}
