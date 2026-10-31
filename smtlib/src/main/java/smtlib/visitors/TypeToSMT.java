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

import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCStateDefinition;
import com.fujitsu.vdmj.tc.definitions.TCTypeDefinition;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCCharacterType;
import com.fujitsu.vdmj.tc.types.TCField;
import com.fujitsu.vdmj.tc.types.TCIntegerType;
import com.fujitsu.vdmj.tc.types.TCMapType;
import com.fujitsu.vdmj.tc.types.TCNamedType;
import com.fujitsu.vdmj.tc.types.TCNaturalOneType;
import com.fujitsu.vdmj.tc.types.TCNaturalType;
import com.fujitsu.vdmj.tc.types.TCRationalType;
import com.fujitsu.vdmj.tc.types.TCRealType;
import com.fujitsu.vdmj.tc.types.TCRecordType;
import com.fujitsu.vdmj.tc.types.TCSeq1Type;
import com.fujitsu.vdmj.tc.types.TCSeqType;
import com.fujitsu.vdmj.tc.types.TCSet1Type;
import com.fujitsu.vdmj.tc.types.TCSetType;
import com.fujitsu.vdmj.tc.types.TCTokenType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.visitors.TCTypeVisitor;
import com.fujitsu.vdmj.typechecker.Environment;
import smtlib.ast.And;
import smtlib.ast.Bracketed;
import smtlib.ast.Expression;
import smtlib.ast.ForAll;
import smtlib.ast.GE;
import smtlib.ast.GT;
import smtlib.ast.Implies;
import smtlib.ast.LT;
import smtlib.ast.Let;
import smtlib.ast.Sort;
import smtlib.ast.Source;
import smtlib.ast.Text;
import smtlib.solvers.Solver;

public class TypeToSMT extends TCTypeVisitor<QualifiedSort, Environment>
{
	private final Solver solver;
	private final Source varname;

	public TypeToSMT(String varname)
	{
		this.varname = new Text(varname);		// The "name" of the variable being qualified
		this.solver = Solver.getInstance();
	}

	public TypeToSMT(Source source)
	{
		this.varname = source;					// The "name" of the variable being qualified
		this.solver = Solver.getInstance();
	}

	@Override
	public QualifiedSort caseType(TCType node, Environment env)
	{
		throw new UnsupportedOperationException("Cannot convert type: " + node);
	}

	@Override
	public QualifiedSort caseNaturalType(TCNaturalType node, Environment env)
	{
		return new QualifiedSort(new Sort("Int"), new GE(varname, new Text("0")));
	}

	@Override
	public QualifiedSort caseNaturalOneType(TCNaturalOneType node, Environment env)
	{
		return new QualifiedSort(new Sort("Int"), new GT(varname, new Text("0")));
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
	public QualifiedSort caseCharacterType(TCCharacterType node, Environment arg)
	{
		return new QualifiedSort(new Sort("Int"), null);
	}

	@Override
	public QualifiedSort caseTokenType(TCTokenType node, Environment arg)
	{
		return new QualifiedSort(new Sort("Int"), null);
	}

	@Override
	public QualifiedSort caseSeqType(TCSeqType node, Environment env)
	{
		Source evar = LocalNames.get("e");
		QualifiedSort esort = node.seqof.apply(new TypeToSMT(evar.toSource()), env);
		Expression defined = null;

		if (esort.qualifier != null)
		{
			// for all elements of the sequence, the element qualifier holds
			Source idx = LocalNames.get("i");
			defined = new ForAll(new Bracketed(new Expression(idx, new Text("Int"))),
				new Implies(
						new And(new GE(idx, new Text("0")),
								new LT(idx, new Expression("seq.len", varname))),
						new Let(new Bracketed(new Expression(evar, new Expression("seq.nth", varname, idx))),
							esort.qualifier)
				));
		}

		return new QualifiedSort(new Sort("Seq", esort.sort), defined);
	}

	@Override
	public QualifiedSort caseSeq1Type(TCSeq1Type node, Environment arg)
	{
		QualifiedSort qsort = caseSeqType(node, arg);

		if (qsort.qualifier != null)
		{
			return new QualifiedSort(qsort.sort,
				new And(
					qsort.qualifier,
					new GT(
						new Expression("seq.len", varname),
						new Text("0")))
				);
		}
		else
		{
			return new QualifiedSort(qsort.sort,
				new GT(
					new Expression("seq.len", varname),
					new Text("0")));
		}
	}

	@Override
	public QualifiedSort caseSetType(TCSetType node, Environment env)
	{
		Source evar = LocalNames.get("e");
		QualifiedSort esort = node.setof.apply(new TypeToSMT(evar.toSource()), env);
		Expression defined = null;

		if (esort.qualifier != null)
		{
			// for all elements of the set, the element qualifier holds
			Source elem = LocalNames.get("i");
			defined = new ForAll(new Bracketed(new Expression(elem, esort.sort)),
				new Implies(
						solver.setMember(elem, varname),
						new Let(new Bracketed(new Expression(evar, elem)), esort.qualifier)
				));
		}

		return new QualifiedSort(solver.setType(esort.sort), defined);
	}

	@Override
	public QualifiedSort caseSet1Type(TCSet1Type node, Environment arg)
	{
		QualifiedSort qsort = caseSetType(node, arg);

		return new QualifiedSort(qsort.sort,
			new GT(
				solver.setCard(varname),
				new Text("0")));
	}

	@Override
	public QualifiedSort caseMapType(TCMapType node, Environment env)
	{
		QualifiedSort keysort = node.from.apply(new TypeToSMT("key"), env);
		QualifiedSort valsort = node.to.apply(new TypeToSMT("value"), env);

		Expression rhs = new GE(
			new Expression("select", "m", "key"),
			new Text("0"));

		if (keysort.qualifier != null)
		{
			rhs = new And(keysort.qualifier, rhs);
		}

		return new QualifiedSort(
			new Sort(new Text("Array"), keysort.sort, valsort.sort),
			// forall ((key Sort)) (=> (set.member key (vdmj.dom map) )(<rhs>))
			new ForAll(
				new Bracketed(new Expression(new Text("key"), keysort.sort)),
					new Implies(
						solver.setMember(
							new Text("key"),
							new Expression("vdm.dom", varname.toSource())),
						rhs)));
	}

	@Override
	public QualifiedSort caseRecordType(TCRecordType node, Environment env)
	{
		TCDefinition rec = env.findType(node.name, Interpreter.getInstance().getDefaultName());
		TCType type = null;

		if (rec instanceof TCStateDefinition)
		{
			TCStateDefinition sdef = (TCStateDefinition)rec;
			type = sdef.getType();
		}
		else if (rec instanceof TCTypeDefinition)
		{
			TCTypeDefinition tdef = (TCTypeDefinition)rec;
			type = tdef.type;
		}

		if (type instanceof TCRecordType)
		{
			TCRecordType rtype = (TCRecordType)type;
			String typename = rtype.name.getName();
			Expression qualifier = null;

			if (rtype.invdef != null && !node.isMaximal())
			{
				qualifier = new Expression(rtype.invdef.name.getName(), varname);	// inv_R
			}

			for (TCField field: rtype.fields)
			{
				QualifiedSort fsort = field.type.apply(
					new TypeToSMT(
						new Expression(field.tag, varname.toString())), env);

				if (fsort.qualifier != null)
				{
					if (qualifier == null)
					{
						qualifier = fsort.qualifier;
					}
					else
					{
						qualifier = new And(qualifier, fsort.qualifier);
					}
				}
			}

			return new QualifiedSort(new Sort(typename), qualifier);
		}

		throw new UnsupportedOperationException("Cannot find record type: " + node.name);
	}

	@Override
	public QualifiedSort caseNamedType(TCNamedType node, Environment env)
	{
		TCDefinition rec = env.findType(node.typename, Interpreter.getInstance().getDefaultName());

		if (rec instanceof TCTypeDefinition)
		{
			TCTypeDefinition tdef = (TCTypeDefinition)rec;

			if (tdef.type instanceof TCNamedType)
			{
				TCNamedType ntype = (TCNamedType)tdef.type;
				QualifiedSort subsort = ntype.type.apply(this, env);

				if (ntype.invdef != null && !node.isMaximal())
				{
					if (subsort.qualifier != null)
					{
						subsort = new QualifiedSort(
							subsort.sort,
							new And(
								new Expression(ntype.invdef.name.getName(), varname),
								subsort.qualifier));
					}
					else
					{
						subsort = new QualifiedSort(
							subsort.sort,
							new Expression(ntype.invdef.name.getName(), varname));
					}
				}

				return subsort;
			}
		}

		throw new UnsupportedOperationException("Cannot find type: " + node.typename);
	}
}
