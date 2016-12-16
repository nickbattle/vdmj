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

package com.fujitsu.vdmj.tc.patterns;

import java.io.Serializable;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.messages.InternalException;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.definitions.TCMultiBindListDefinition;
import com.fujitsu.vdmj.tc.types.TCSeqType;
import com.fujitsu.vdmj.tc.types.TCSetType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.TypeComparator;

public class TCPatternBind implements Serializable
{
	private static final long serialVersionUID = 1L;

	public final LexLocation location;
	public final TCPattern pattern;
	public final TCBind bind;

	private TCDefinitionList defs = null;

	public TCPatternBind(LexLocation location, TCPattern pattern, TCBind bind)
	{
		this.location = location;

		if (pattern!= null)
		{
			this.pattern = pattern;
			this.bind = null;
		}
		else if (bind != null)
		{
			this.pattern = null;
			this.bind = bind;
		}
		else
		{
			throw new InternalException(3, "PatternBind passed two nulls");
		}
	}

	@Override
	public String toString()
	{
		return (pattern == null ? bind : pattern).toString();
	}

	public TCDefinitionList getDefinitions()
	{
		assert (defs != null) :
			"PatternBind must be type checked before getDefinitions";

		return defs;
	}

	public void typeCheck(Environment base, NameScope scope, TCType type)
	{
		defs = null;

		if (bind != null)
		{
			if (bind instanceof TCTypeBind)
			{
				TCTypeBind typebind = (TCTypeBind)bind;
				typebind.typeResolve(base);
				
				TypeComparator.checkComposeTypes(typebind.type, base, false);

				if (!TypeComparator.compatible(typebind.type, type))
				{
					bind.report(3198, "Type bind not compatible with expression");
					bind.detail2("Bind", typebind.type, "Exp", type);
				}
			}
			else if (bind instanceof TCSetBind)
			{
				TCSetBind setbind = (TCSetBind)bind;
				TCType bindtype = setbind.set.typeCheck(base, null, scope, null);
				TCSetType settype = bindtype.getSet();

				if (!bindtype.isSet(location))
				{
					setbind.set.report(3199, "Set bind not compatible with expression");
				}
				else if (!TypeComparator.compatible(type, settype.setof))
				{
					setbind.set.report(3199, "Set bind not compatible with expression");
					setbind.set.detail2("Bind", settype.setof, "Exp", type);
				}
			}
			else if (bind instanceof TCSeqBind)
			{
				TCSeqBind seqbind = (TCSeqBind)bind;
				TCType bindtype = seqbind.sequence.typeCheck(base, null, scope, null);
				TCSeqType seqtype = bindtype.getSeq();

				if (!bindtype.isSeq(location))
				{
					seqbind.sequence.report(3199, "Seq bind not compatible with expression");
				}
				else if (!TypeComparator.compatible(type, seqtype.seqof))
				{
					seqbind.sequence.report(3199, "Seq bind not compatible with expression");
					seqbind.sequence.detail2("Bind", seqtype.seqof, "Exp", type);
				}
			}

			TCDefinition def =
				new TCMultiBindListDefinition(bind.location, bind.getMultipleBindList());

			def.typeCheck(base, scope);
			defs = new TCDefinitionList(def);
		}
		else
		{
			assert (type != null) :
					"Can't typecheck a pattern without a type";

			pattern.typeResolve(base);
			defs = pattern.getAllDefinitions(type, NameScope.LOCAL);
		}
	}
}
