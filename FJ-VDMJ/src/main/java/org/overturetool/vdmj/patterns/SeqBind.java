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

package org.overturetool.vdmj.patterns;

import java.util.List;
import java.util.Vector;

import org.overturetool.vdmj.expressions.Expression;
import org.overturetool.vdmj.lex.LexNameList;
import org.overturetool.vdmj.pog.POContextStack;
import org.overturetool.vdmj.pog.ProofObligationList;
import org.overturetool.vdmj.runtime.Context;
import org.overturetool.vdmj.runtime.ValueException;
import org.overturetool.vdmj.values.ValueList;


public class SeqBind extends Bind
{
	private static final long serialVersionUID = 1L;
	public final Expression sequence;

	public SeqBind(Pattern pattern, Expression sequence)
	{
		super(pattern.location, pattern);
		this.sequence = sequence;
	}

	@Override
	public List<MultipleBind> getMultipleBindList()
	{
		PatternList plist = new PatternList();
		plist.add(pattern);
		List<MultipleBind> mblist = new Vector<MultipleBind>();
		mblist.add(new MultipleSeqBind(plist, sequence));
		return mblist;
	}

	@Override
	public String toString()
	{
		return pattern + " in seq " + sequence;
	}

	@Override
	public ValueList getBindValues(Context ctxt, boolean permuted) throws ValueException
	{
		return sequence.eval(ctxt).seqValue(ctxt);
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt)
	{
		return sequence.getProofObligations(ctxt);
	}

	@Override
	public ValueList getValues(Context ctxt)
	{
		return sequence.getValues(ctxt);
	}

	@Override
	public LexNameList getOldNames()
	{
		return sequence.getOldNames();
	}
}
