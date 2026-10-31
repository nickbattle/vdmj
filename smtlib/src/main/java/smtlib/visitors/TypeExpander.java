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

import com.fujitsu.vdmj.tc.expressions.TCAndExpression;
import com.fujitsu.vdmj.tc.expressions.TCApplyExpression;
import com.fujitsu.vdmj.tc.expressions.TCCardinalityExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpressionList;
import com.fujitsu.vdmj.tc.expressions.TCExpressionSet;
import com.fujitsu.vdmj.tc.expressions.TCForAllExpression;
import com.fujitsu.vdmj.tc.expressions.TCGreaterEqualExpression;
import com.fujitsu.vdmj.tc.expressions.TCGreaterExpression;
import com.fujitsu.vdmj.tc.expressions.TCImpliesExpression;
import com.fujitsu.vdmj.tc.expressions.TCInSetExpression;
import com.fujitsu.vdmj.tc.expressions.TCLenExpression;
import com.fujitsu.vdmj.tc.expressions.TCLessExpression;
import com.fujitsu.vdmj.tc.expressions.TCMapDomainExpression;
import com.fujitsu.vdmj.tc.expressions.TCVariableExpression;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.patterns.TCIdentifierPattern;
import com.fujitsu.vdmj.tc.patterns.TCMultipleBindList;
import com.fujitsu.vdmj.tc.patterns.TCMultipleSetBind;
import com.fujitsu.vdmj.tc.patterns.TCMultipleTypeBind;
import com.fujitsu.vdmj.tc.patterns.TCPatternList;
import com.fujitsu.vdmj.tc.types.TCField;
import com.fujitsu.vdmj.tc.types.TCMapType;
import com.fujitsu.vdmj.tc.types.TCNamedType;
import com.fujitsu.vdmj.tc.types.TCNaturalOneType;
import com.fujitsu.vdmj.tc.types.TCNaturalType;
import com.fujitsu.vdmj.tc.types.TCRecordType;
import com.fujitsu.vdmj.tc.types.TCSeq1Type;
import com.fujitsu.vdmj.tc.types.TCSeqType;
import com.fujitsu.vdmj.tc.types.TCSet1Type;
import com.fujitsu.vdmj.tc.types.TCSetType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.visitors.TCTypeVisitor;
import com.fujitsu.vdmj.typechecker.Environment;
import smtlib.ast.DeclareType;
import smtlib.ast.Expression;
import smtlib.ast.Text;

public class TypeExpander extends TCTypeVisitor<QualifiedType, Environment> implements ASTHelper
{
	private final TCExpression VAR;
	private final Globals globals;

	public TypeExpander(TCNameToken var)
	{
		this.VAR = new TCVariableExpression(ANYWHERE, var, var.toString());
		this.globals = Globals.getInstance();
	}

	public TypeExpander(TCExpression var)
	{
		this.VAR = var;		// eg. s(i) rather than just a variable
		this.globals = Globals.getInstance();
	}

	@Override
	public QualifiedType caseType(TCType node, Environment env)
	{
		return new QualifiedType(node);
	}

	@Override
	public QualifiedType caseNaturalType(TCNaturalType node, Environment env)
	{
		return new QualifiedType(
			INT,
			new TCGreaterEqualExpression(VAR, GE, ZERO));
	}

	@Override
	public QualifiedType caseNaturalOneType(TCNaturalOneType node, Environment env)
	{
		return new QualifiedType(
			INT,
			new TCGreaterExpression(VAR, GT, ZERO));
	}

	@Override
	public QualifiedType caseSeqType(TCSeqType node, Environment env)
	{
		TCVariableExpression loopvar = LocalNames.variable("i");
		TCApplyExpression apply = new TCApplyExpression(VAR, new TCExpressionList(loopvar));	// s(i)

		QualifiedType seqOf = node.seqof.apply(new TypeExpander(apply), env);

		if (seqOf.qualifier != null)
		{
			TCPatternList plist = new TCPatternList();
			plist.add(new TCIdentifierPattern(loopvar.name));
			TCMultipleBindList bindlist = new TCMultipleBindList();
			bindlist.add(new TCMultipleTypeBind(plist, INT));

			return new QualifiedType(
				new TCSeqType(ANYWHERE, seqOf.type),

				// forall $i:int & (0 < $i and $i <= len s) => s($i) >= 0
				new TCForAllExpression(ANYWHERE,
					bindlist,
					new TCImpliesExpression(
						new TCAndExpression(
							new TCLessExpression(ZERO, LT, loopvar),
							AND,
							new TCLessExpression(loopvar, LE, new TCLenExpression(ANYWHERE, VAR))),
						IMPLIES,
						seqOf.qualifier
						)
					)
			);
		}
		else
		{
			return new QualifiedType(new TCSeqType(ANYWHERE, seqOf.type));	// No qualification
		}
	}

	@Override
	public QualifiedType caseSeq1Type(TCSeq1Type node, Environment arg)
	{
		QualifiedType qtype = caseSeqType(node, arg);

		if (qtype.qualifier == null)
		{
			return new QualifiedType(qtype.type,
				new TCLessExpression(
					ZERO,
					LT,
					new TCLenExpression(ANYWHERE, VAR)));
		}
		else
		{
			return new QualifiedType(qtype.type,
				new TCAndExpression(
					qtype.qualifier,
					AND,
					new TCLessExpression(
						ZERO,
						LT,
						new TCLenExpression(ANYWHERE, VAR))));
		}
	}

	@Override
	public QualifiedType caseSetType(TCSetType node, Environment env)
	{
		TCVariableExpression bindvar = LocalNames.variable("e");
		QualifiedType setOf = node.setof.apply(new TypeExpander(bindvar), env);

		if (setOf.qualifier != null)
		{
			TCPatternList plist = new TCPatternList();
			plist.add(new TCIdentifierPattern(bindvar.name));
			TCMultipleBindList bindlist = new TCMultipleBindList();
			bindlist.add(new TCMultipleTypeBind(plist, setOf.type));

			return new QualifiedType(
				new TCSetType(ANYWHERE, setOf.type),

				// forall $i:T & $i in set S & $i >= 0
				new TCForAllExpression(ANYWHERE,
					bindlist,
					new TCImpliesExpression(
						new TCInSetExpression(bindvar, INSET, VAR),
						IMPLIES,
						setOf.qualifier))
			);
		}
		else
		{
			return new QualifiedType(new TCSetType(ANYWHERE, setOf.type));	// No qualification
		}
	}

	@Override
	public QualifiedType caseSet1Type(TCSet1Type node, Environment arg)
	{
		QualifiedType qtype = caseSetType(node, arg);

		if (qtype.qualifier == null)
		{
			return new QualifiedType(qtype.type,
				new TCLessExpression(
					ZERO,
					LT,
					new TCCardinalityExpression(ANYWHERE, VAR)));
		}
		else
		{
			return new QualifiedType(qtype.type,
				new TCAndExpression(
					qtype.qualifier,
					AND,
					new TCLessExpression(
						ZERO,
						LT,
						new TCCardinalityExpression(ANYWHERE, VAR))));
		}
	}

	@Override
	public QualifiedType caseMapType(TCMapType node, Environment arg)
	{
		TCNameToken domvar = LocalNames.token("domvar");
		TCNameToken rngvar = LocalNames.token("rngvar");
		QualifiedType from = node.from.apply(new TypeExpander(domvar), arg);
		QualifiedType to   = node.to.apply(new TypeExpander(rngvar), arg);

		TCExpressionSet quals = new TCExpressionSet();

		if (from.qualifier != null)
		{
			TCPatternList plist = new TCPatternList();
			TCIdentifierPattern bindvar = LocalNames.pattern("domvar");
			plist.add(bindvar);
			TCMultipleSetBind mtbind = new TCMultipleSetBind(plist, new TCMapDomainExpression(ANYWHERE, VAR));
			TCMultipleBindList mblist = new TCMultipleBindList();
			mblist.add(mtbind);

			// forall domvar in set dom M & qualifier
			quals.add(new TCForAllExpression(ANYWHERE, mblist, from.qualifier));
		}

		if (to.qualifier != null)
		{
			TCPatternList plist = new TCPatternList();
			TCIdentifierPattern bindvar = LocalNames.pattern("rngvar");
			plist.add(bindvar);
			TCMultipleSetBind mtbind = new TCMultipleSetBind(plist, new TCMapDomainExpression(ANYWHERE, VAR));
			TCMultipleBindList mblist = new TCMultipleBindList();
			mblist.add(mtbind);

			// forall rngvar in set dom M & qualifier
			quals.add(new TCForAllExpression(ANYWHERE, mblist, to.qualifier));
		}

		return new QualifiedType(new TCMapType(ANYWHERE, from.type, to.type),
			quals.isEmpty() ? null : Utils.andOf(quals));
	}

	@Override
	public QualifiedType caseRecordType(TCRecordType node, Environment env)
	{
		TCExpression qualifier = null;

		if (node.invdef != null)
		{
			qualifier = new TCApplyExpression(
				new TCVariableExpression(ANYWHERE, node.invdef.name, node.invdef.name.getName()),
				new TCExpressionList(VAR));

			DefinitionExpander.getInstance().expandFunction(node.invdef);
		}

		String typename = node.name.getName();

		if (!globals.containsType(typename))
		{
			Expression ctor = new Expression(new Text("mk_" + typename));
			
			for (TCField field: node.fields)
			{
				QualifiedSort qsort = field.type.apply(new TypeToSMT(typename), env);
				ctor.add(new Expression(new Text(field.tag), qsort.sort));
				field.type.apply(this, env);
			}
				
			globals.addType(new DeclareType(typename, ctor));
		}

		return new QualifiedType(node, qualifier);
	}

	@Override
	public QualifiedType caseNamedType(TCNamedType node, Environment env)
	{
		TCExpression qualifier = null;

		if (node.invdef != null)
		{
			qualifier = new TCApplyExpression(
				new TCVariableExpression(ANYWHERE, node.invdef.name, node.invdef.name.getName()),
				new TCExpressionList(VAR));

			DefinitionExpander.getInstance().expandFunction(node.invdef);
		}

		if (!globals.containsType(node.typename.getName()))
		{
			globals.addType(new DeclareType(node.typename.getName()));
		}

		return new QualifiedType(node, qualifier);
	}
}
