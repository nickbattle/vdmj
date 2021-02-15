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

import java.io.Serializable;
import java.util.List;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.po.PONode;
import com.fujitsu.vdmj.po.patterns.visitors.POBindVisitor;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.ProofObligationList;

/**
 * The parent class of {@link POSetBind} and {@link POTypeBind}.
 */
public abstract class POBind extends PONode implements Serializable
{
	private static final long serialVersionUID = 1L;

	/** The pattern of the bind. */
	public final POPattern pattern;

	/**
	 * Create a bind at the given location with the given pattern.
	 */
	public POBind(LexLocation location, POPattern pattern)
	{
		super(location);
		this.pattern = pattern;
	}

	/** Return this one bind as a list of {@link POMultipleBind}. */
	abstract public List<POMultipleBind> getMultipleBindList();

	/** Return a list of POs. */
	abstract public ProofObligationList getProofObligations(POContextStack ctxt);

	/**
	 * Implemented by all binds to allow visitor processing.
	 */
	abstract public <R, S> R apply(POBindVisitor<R, S> visitor, S arg);
}
