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

package com.fujitsu.vdmj.tc.expressions;

import java.util.Iterator;

import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.expressions.visitors.TCExpressionVisitor;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCField;
import com.fujitsu.vdmj.tc.types.TCRecordType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.TypeComparator;
import com.fujitsu.vdmj.util.Utils;

public class TCMkTypeExpression extends TCExpression
{
	private static final long serialVersionUID = 1L;
	public final TCNameToken typename;
	public final TCExpressionList args;
	public final boolean maximal;

	private TCRecordType recordType = null;
	public TCTypeList argTypes = null;

	public TCMkTypeExpression(TCNameToken typename, TCExpressionList args, boolean maximal)
	{
		super(typename.getLocation());
		this.typename = typename;
		this.args = args;
		this.maximal = maximal;
	}

	@Override
	public String toString()
	{
		return "mk_" + typename + (maximal ? "!" : "") + "(" + Utils.listToString(args) + ")";
	}

	@Override
	public TCType typeCheck(Environment env, TCTypeList qualifiers, NameScope scope, TCType constraint)
	{
		TCDefinition typeDef = env.findType(typename, location.module);

		if (typeDef == null)
		{
			report(3126, "Unknown type '" + typename + "' in constructor");
			return setType(new TCUnknownType(location));
		}

		TCType rec = typeDef.getType();

		if (!(rec instanceof TCRecordType))
		{
			report(3127, "Type '" + typename + "' is not a record type");
			return setType(rec);
		}

		recordType = (TCRecordType)rec;

		if (recordType.opaque && !location.module.equals(recordType.location.module))
		{
			report(3127, "Type '" + typename + "' has no struct export");
			return setType(rec);
		}
		
		if (maximal)
		{
			recordType = recordType.copy(true);
		}

		if (typename.isExplicit())
		{
			// If the type name is explicit, the TCType ought to have an explicit
			// name. This only really affects trace expansion.

			recordType = recordType.copy(recordType.isMaximal());
		}

		if (recordType.fields.size() != args.size())
		{
			report(3128, "Record and constructor do not have same number of fields");
			return setType(rec);
		}

		int i=0;
		Iterator<TCField> fiter = recordType.fields.iterator();
		argTypes = new TCTypeList();

		for (TCExpression arg: args)
		{
			TCType fieldType = fiter.next().type;
			TCType argType = arg.typeCheck(env, null, scope, fieldType);
			i++;

			if (!TypeComparator.compatible(fieldType, argType))
			{
				arg.report(3129, "Constructor field " + i + " is of wrong type");
				detail2("Expected", fieldType, "Actual", argType);
			}

			argTypes.add(argType);
		}

		return checkConstraint(constraint, recordType);
	}
	
	@Override
	public <R, S> R apply(TCExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseMkTypeExpression(this, arg);
	}
}
