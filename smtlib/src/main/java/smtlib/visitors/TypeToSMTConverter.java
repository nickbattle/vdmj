/*******************************************************************************
 *
 *	Copyright (c) 2026 Nick Battle.
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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package smtlib.visitors;

import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCIntegerType;
import com.fujitsu.vdmj.tc.types.TCMapType;
import com.fujitsu.vdmj.tc.types.TCNaturalOneType;
import com.fujitsu.vdmj.tc.types.TCNaturalType;
import com.fujitsu.vdmj.tc.types.TCRationalType;
import com.fujitsu.vdmj.tc.types.TCRealType;
import com.fujitsu.vdmj.tc.types.TCSeq1Type;
import com.fujitsu.vdmj.tc.types.TCSeqType;
import com.fujitsu.vdmj.tc.types.TCSetType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.visitors.TCTypeVisitor;
import com.fujitsu.vdmj.typechecker.Environment;

import smtlib.ast.Bracketed;
import smtlib.ast.Expression;
import smtlib.ast.Sort;
import smtlib.ast.Text;

public class TypeToSMTConverter extends TCTypeVisitor<QualifiedSort, Environment>
{
	private final Text varname;

	public TypeToSMTConverter(String varname)
	{
		this.varname = new Text(varname);		// The "name" of the variable being qualified
	}

	@Override
	public QualifiedSort caseType(TCType node, Environment env)
	{
		throw new UnsupportedOperationException("Unsupported type: " + node);
	}

	@Override
	public QualifiedSort caseNaturalType(TCNaturalType node, Environment env)
	{
		return new QualifiedSort(new Sort("Int"), new Expression(">=", varname, new Text("0")));
	}

	@Override
	public QualifiedSort caseNaturalOneType(TCNaturalOneType node, Environment env)
	{
		return new QualifiedSort(new Sort("Int"), new Expression(">", varname, new Text("0")));
	}

	@Override
	public QualifiedSort caseIntegerType(TCIntegerType node, Environment env)
	{
		return new QualifiedSort(new Sort("Int"), null);
	}

	@Override
	public QualifiedSort caseRationalType(TCRationalType node, Environment env)
	{
		return new QualifiedSort(new Sort("Real"), null);
	}

	@Override
	public QualifiedSort caseRealType(TCRealType node, Environment env)
	{
		return new QualifiedSort(new Sort("Real"), null);
	}

	@Override
	public QualifiedSort caseBooleanType(TCBooleanType node, Environment env)
	{
		return new QualifiedSort(new Sort("Bool"), null);
	}

	@Override
	public QualifiedSort caseSeqType(TCSeqType node, Environment env)
	{
		QualifiedSort esort = node.seqof.apply(new TypeToSMTConverter("e"), env);

		if (esort.qualifier == null)
		{
			return new QualifiedSort(new Sort(new Text("Seq"), esort.sort), null);
		}

		return new QualifiedSort(
			new Sort(new Text("Seq"), esort.sort),
			// (forall ((i Int)) (=> (and (>= i 0) (< i (seq.len s))) (>= (seq.nth s i) 0))))
			new Expression(new Text("forall"),
				new Bracketed(new Expression(new Text("i"), esort.sort)),
				new Expression("=>",
					new Expression("and",
						new Expression(">=", "i", "0"),
						new Expression(new Text("<"),
							new Text("i"),
							new Expression(new Text("seq.len"), varname))),
					new Expression("let",
						new Bracketed(
							new Expression(new Text("e"),
							new Expression(new Text("seq.nth"), varname, new Text("i")))),
						esort.qualifier))));
	}

	@Override
	public QualifiedSort caseSeq1Type(TCSeq1Type node, Environment arg)
	{
		QualifiedSort qsort = caseSeqType(node, arg);

		if (qsort.qualifier == null)
		{
			return qsort;
		}

		return new QualifiedSort(qsort.sort,
			new Expression("and",
				new Expression(new Text(">"),
					new Expression(new Text("seq.len"), varname),
					new Text("0")), qsort.qualifier));
	}

	@Override
	public QualifiedSort caseSetType(TCSetType node, Environment env)
	{
		QualifiedSort esort = node.setof.apply(new TypeToSMTConverter("e"), env);

		return new QualifiedSort(
			new Sort(new Text("Set"), esort.sort),
			// (forall ((x Int)) (=> (set.member x S) <qualifier>)));
			new Expression(new Text("forall"),
				new Bracketed(new Expression(new Text("e"), esort.sort)),
				new Expression("=>",
					new Expression(new Text("set.member"), new Text("e"), varname),
					esort.qualifier)));
	}

	@Override
	public QualifiedSort caseMapType(TCMapType node, Environment env)
	{
		QualifiedSort keysort = node.from.apply(new TypeToSMTConverter("key"), env);
		QualifiedSort valsort = node.to.apply(new TypeToSMTConverter("value"), env);

		return new QualifiedSort(
			new Sort(new Text("Array"), keysort.sort, valsort.sort),
			new Expression(new Text("forall"),
				new Bracketed(new Expression(new Text("key"), keysort.sort)),
					new Expression("=>",
						new Expression(new Text("set.member"), new Text("key"), new Text("dom_" + varname.toSource())),
						new Expression("and",
							keysort.qualifier,
							new Expression(new Text(">="), new Expression("select", "m", "key"), new Text("0"))))));
	}
}
