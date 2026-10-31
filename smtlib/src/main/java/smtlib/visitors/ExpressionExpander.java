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

import java.lang.reflect.Constructor;
import com.fujitsu.vdmj.ast.lex.LexIntegerToken;
import com.fujitsu.vdmj.ast.lex.LexToken;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.annotations.TCAnnotatedExpression;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.definitions.TCExplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.definitions.TCImplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.definitions.TCLocalDefinition;
import com.fujitsu.vdmj.tc.definitions.TCTypeDefinition;
import com.fujitsu.vdmj.tc.definitions.TCValueDefinition;
import com.fujitsu.vdmj.tc.expressions.*;
import com.fujitsu.vdmj.tc.expressions.visitors.TCExpressionVisitor;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.patterns.TCIdentifierPattern;
import com.fujitsu.vdmj.tc.patterns.TCMultipleBind;
import com.fujitsu.vdmj.tc.patterns.TCMultipleBindList;
import com.fujitsu.vdmj.tc.patterns.TCMultipleSeqBind;
import com.fujitsu.vdmj.tc.patterns.TCMultipleSetBind;
import com.fujitsu.vdmj.tc.patterns.TCMultipleTypeBind;
import com.fujitsu.vdmj.tc.patterns.TCPatternList;
import com.fujitsu.vdmj.tc.patterns.TCRecordPattern;
import com.fujitsu.vdmj.tc.patterns.TCSeqBind;
import com.fujitsu.vdmj.tc.patterns.TCSetBind;
import com.fujitsu.vdmj.tc.patterns.TCTuplePattern;
import com.fujitsu.vdmj.tc.patterns.TCTypeBind;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCSeqType;
import com.fujitsu.vdmj.tc.types.TCSetType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.util.Pair;

public class ExpressionExpander extends TCExpressionVisitor<TCExpression, TCExpressionSet> implements ASTHelper
{
	private final Environment env;
	private final Globals globals;

	public ExpressionExpander(Environment env)
	{
		this.env = env;
		this.globals = Globals.getInstance();
	}

	@Override
	public TCExpression caseExpression(TCExpression node, TCExpressionSet constraints)
	{
		throw new RuntimeException("No expansion for " + node.getClass().getSimpleName());
	}

	@Override
	public TCExpression caseForAllExpression(TCForAllExpression node, TCExpressionSet constraints)
	{
		Pair<TCMultipleBindList,TCExpression> pair = expandQuantifier(true, node.bindList, node.predicate, constraints);
		return new TCForAllExpression(node.location, pair.first, pair.second);
	}

	@Override
	public TCExpression caseExistsExpression(TCExistsExpression node, TCExpressionSet constraints)
	{
		Pair<TCMultipleBindList,TCExpression> pair = expandQuantifier(false, node.bindList, node.predicate, constraints);
		return new TCExistsExpression(node.location, pair.first, pair.second);
	}

	@Override
	public TCExpression caseExists1Expression(TCExists1Expression node, TCExpressionSet constraints)
	{
		Pair<TCMultipleBindList, TCExpression> pair = expandQuantifier(false, node.bind.getMultipleBindList(), node.predicate, constraints);
		TCMultipleBind bind = pair.first.firstElement();

		if (bind instanceof TCMultipleTypeBind)
		{
			TCMultipleTypeBind mtbind = (TCMultipleTypeBind)bind;
			TCTypeBind tbind = new TCTypeBind(mtbind.plist.firstElement(), mtbind.type);
			return new TCExists1Expression(node.location, tbind, pair.second);
		}
		else if (bind instanceof TCMultipleSeqBind)
		{
			TCMultipleSeqBind msbind = (TCMultipleSeqBind)bind;
			TCSeqBind sbind = new TCSeqBind(msbind.plist.firstElement(), msbind.sequence);
			return new TCExists1Expression(node.location, sbind, pair.second);
		}
		else if (bind instanceof TCMultipleSetBind)
		{
			TCMultipleSetBind msbind = (TCMultipleSetBind)bind;
			TCSetBind sbind = new TCSetBind(msbind.plist.firstElement(), msbind.set);
			return new TCExists1Expression(node.location, sbind, pair.second);
		}
		else
		{
			throw new UnsupportedOperationException("Cannot expand: " + bind);
		}
	}

	@Override
	public TCExpression caseLetDefExpression(TCLetDefExpression node, TCExpressionSet constraints)
	{
		Pair<TCDefinitionList,TCExpression> pair = expandLetDef(node.localDefs, node.expression, constraints);
		return new TCLetDefExpression(node.location, pair.first, pair.second);
	}

	private Pair<TCDefinitionList, TCExpression> expandLetDef(
		TCDefinitionList localDefs, TCExpression expression, TCExpressionSet constraints)
	{
		TCExpressionSet qualifiers = new TCExpressionSet();
		TCDefinitionList newdefs = new TCDefinitionList();
		TCDefinitionList locals = new TCDefinitionList();
		TCExpressionList letquals = new TCExpressionList();

		for (TCDefinition def: localDefs)
		{
			if (def instanceof TCValueDefinition)
			{
				TCValueDefinition vdef = (TCValueDefinition)def;

				if (vdef.pattern instanceof TCIdentifierPattern)
				{
					TCIdentifierPattern id = (TCIdentifierPattern)vdef.pattern;
					QualifiedType qtype = vdef.getType().apply(new TypeExpander(id.name), env);
					TCType updated = qtype.type;

					if (qtype.qualifier != null)
					{
						qualifiers.add(qtype.qualifier);
					}

					newdefs.add(new TCValueDefinition(
						vdef.nameScope,
						vdef.annotations,
						vdef.accessSpecifier,
						vdef.pattern,
						updated,
						vdef.exp.apply(this, constraints)));
				}
				else if (vdef.pattern instanceof TCRecordPattern)
				{
					TCRecordPattern rp = (TCRecordPattern)vdef.pattern;
					TCType ptype = vdef.getType();
					TCIdentifierPattern p2 = Utils.checkPattern(rp, ptype, letquals, env);

					TCExpressionList args = new TCExpressionList();
					TCLetDefExpression letdef = (TCLetDefExpression)letquals.firstElement();

					for (TCDefinition let: letdef.localDefs)
					{
						TCValueDefinition v = (TCValueDefinition)let;
						args.add(v.exp);
					}

					TCMkTypeExpression mkrec = new TCMkTypeExpression(rp.typename, args, false);

					QualifiedType qsort = ptype.apply(new TypeExpander(mkrec), env);

					if (qsort.qualifier != null)
					{
						qualifiers.add(qsort.qualifier);
					}

					newdefs.add(new TCValueDefinition(
						vdef.nameScope,
						vdef.annotations,
						vdef.accessSpecifier,
						p2,
						ptype,
						vdef.exp.apply(this, constraints)));

					locals.addAll(p2.getDefinitions(ptype, NameScope.LOCAL));
				}
				else if (vdef.pattern instanceof TCTuplePattern)
				{
					TCTuplePattern rp = (TCTuplePattern)vdef.pattern;
					TCType ptype = vdef.getType();
					TCIdentifierPattern p2 = Utils.checkPattern(rp, ptype, letquals, env);

					TCExpressionList args = new TCExpressionList();
					TCLetDefExpression letdef = (TCLetDefExpression)letquals.firstElement();

					for (TCDefinition let: letdef.localDefs)
					{
						TCValueDefinition v = (TCValueDefinition)let;
						args.add(v.exp);
					}

					TCTupleExpression tuple = new TCTupleExpression(p2.location, args);

					QualifiedType qsort = ptype.apply(new TypeExpander(tuple), env);

					if (qsort.qualifier != null)
					{
						qualifiers.add(qsort.qualifier);
					}

					newdefs.add(new TCValueDefinition(
						vdef.nameScope,
						vdef.annotations,
						vdef.accessSpecifier,
						p2,
						ptype,
						vdef.exp.apply(this, constraints)));

					locals.addAll(p2.getDefinitions(ptype, NameScope.LOCAL));
				}
				else
				{
					throw new UnsupportedOperationException("Cannot expand pattern: " + def);
				}
			}
			else
			{
				throw new UnsupportedOperationException("Cannot expand let: " + def);
			}
		}

		TCExpressionSet bodyConstraints = new TCExpressionSet();
		TCExpression body = expression.apply(this, bodyConstraints);

		// [<qualifiers> (=>/and) [<bodyConstraints> and]] <translated body>

		if (!bodyConstraints.isEmpty())
		{
			body = new TCAndExpression(Utils.andOf(bodyConstraints), AND, body);
		}

		if (!qualifiers.isEmpty())
		{
			body = new TCImpliesExpression(Utils.andOf(qualifiers), IMPLIES, body);
		}

		if (!letquals.isEmpty())
		{
			Environment env2 = new FlatEnvironment(locals, env);
			body = Utils.combineQualifiers(letquals, body);
			body.typeCheck(env2, null, NameScope.NAMES, null);
		}

		constraints.clear();	// For callers?

		return new Pair<TCDefinitionList, TCExpression>(newdefs, body);
	}

 	@Override
	public TCExpression caseLetBeStExpression(TCLetBeStExpression node, TCExpressionSet constraints)
	{
		TCMultipleBindList bindList = new TCMultipleBindList();
		bindList.add(node.bind);
		Pair<TCMultipleBindList,TCExpression> pair = expandQuantifier(false, bindList, node.value, constraints);

		if (node.suchThat != null)
		{
			TCExpression suchThat = node.suchThat.apply(this, constraints);
			constraints.add(suchThat);	// Moved into body exp
		}
		
		return new TCLetBeStExpression(node.location, pair.first.firstElement(), null, pair.second);
	}

	private Pair<TCMultipleBindList, TCExpression> expandQuantifier(boolean forall, TCMultipleBindList bindList, TCExpression predicate, TCExpressionSet constraints)
	{
		TCExpressionSet qualifiers = new TCExpressionSet();
		TCExpressionList letquals = new TCExpressionList();
		TCMultipleBindList newBindList = new TCMultipleBindList();
		
		for (TCMultipleBind bind: bindList)
		{
			if (bind instanceof TCMultipleTypeBind)
			{
				TCMultipleTypeBind mtbind = (TCMultipleTypeBind)bind;
				TCType updatedType = mtbind.type;
				TCPatternList updatedPlist = new TCPatternList();
				updatedPlist.addAll(mtbind.plist);

				for (int i=0; i < mtbind.plist.size(); i++)
				{
					TCIdentifierPattern p2 = Utils.checkPattern(mtbind.plist.get(i), mtbind.type, letquals, env);
					updatedPlist.set(i, p2);
					
					TCDefinition def = p2.getDefinitions(mtbind.type, NameScope.LOCAL).firstElement();
					TCNameToken var = def.name;
					QualifiedType qtype = def.getType().apply(new TypeExpander(var), env);
					updatedType = qtype.type;

					if (qtype.qualifier != null)
					{
						qualifiers.add(qtype.qualifier);
					}
				}

				newBindList.add(new TCMultipleTypeBind(updatedPlist, updatedType));
			}
			else if (bind instanceof TCMultipleSeqBind)
			{
				TCMultipleSeqBind msbind = (TCMultipleSeqBind)bind;
				TCSeqType stype = msbind.sequence.getType().getSeq();
				TCType updatedType = stype.seqof;
				TCPatternList updatedPlist = new TCPatternList();
				updatedPlist.addAll(msbind.plist);

				for (int i=0; i < msbind.plist.size(); i++)
				{
					TCIdentifierPattern p2 = Utils.checkPattern(msbind.plist.get(i), stype.seqof, letquals, env);
					updatedPlist.set(i, p2);

					TCDefinition def = p2.getDefinitions(stype.seqof, NameScope.LOCAL).firstElement();
					TCNameToken var = def.name;
					QualifiedType qtype = def.getType().apply(new TypeExpander(var), env);
					updatedType = qtype.type;

					if (qtype.qualifier != null)
					{
						qualifiers.add(qtype.qualifier);
					}

					// And p2 in set elems S
					TCVariableExpression pvar = new TCVariableExpression(p2.location, p2.name, p2.toString());
					TCElementsExpression elems = new TCElementsExpression(p2.location, msbind.sequence.apply(this, constraints));
					qualifiers.add(new TCInSetExpression(pvar, INSET, elems));
				}

				// Turn x in seq S into x:T, given qualifier above
				newBindList.add(new TCMultipleTypeBind(updatedPlist, updatedType));
			}
			else if (bind instanceof TCMultipleSetBind)
			{
				TCMultipleSetBind msbind = (TCMultipleSetBind)bind;
				TCSetType stype = msbind.set.getType().getSet();
				TCType updatedType = stype.setof;
				TCPatternList updatedPlist = new TCPatternList();
				updatedPlist.addAll(msbind.plist);

				for (int i=0; i < msbind.plist.size(); i++)
				{
					TCIdentifierPattern p2 = Utils.checkPattern(msbind.plist.get(i), stype.setof, letquals, env);
					updatedPlist.set(i, p2);

					TCDefinition def = p2.getDefinitions(stype.setof, NameScope.LOCAL).firstElement();
					TCNameToken var = def.name;
					QualifiedType qtype = def.getType().apply(new TypeExpander(var), env);
					updatedType = qtype.type;

					if (qtype.qualifier != null)
					{
						qualifiers.add(qtype.qualifier);
					}

					// And p2 in set S
					TCVariableExpression pvar = new TCVariableExpression(p2.location, p2.name, p2.toString());
					qualifiers.add(new TCInSetExpression(pvar, INSET, msbind.set.apply(this, constraints)));
				}

				// Turn x in set S into x:T, given qualifier above
				newBindList.add(new TCMultipleTypeBind(updatedPlist, updatedType));
			}
			else
			{
				throw new UnsupportedOperationException("Cannot expand: " + bind);
			}
		}

		TCExpressionSet bodyConstraints = new TCExpressionSet();
		TCExpression body = predicate.apply(this, bodyConstraints);

		// [<qualifiers> (=>/and) [<bodyConstraints> and]] <translated body>

		if (!bodyConstraints.isEmpty())
		{
			body = new TCAndExpression(Utils.andOf(bodyConstraints), AND, body);
		}

		if (!qualifiers.isEmpty())
		{
			if (forall)
			{
				body = new TCImpliesExpression(Utils.andOf(qualifiers), IMPLIES, body);
			}
			else
			{
				body = new TCAndExpression(Utils.andOf(qualifiers), AND, body);
			}
		}

		if (!letquals.isEmpty())
		{
			body = Utils.combineQualifiers(letquals, body);
		}
		
		constraints.clear();	// For callers?

		return new Pair<TCMultipleBindList, TCExpression>(newBindList, body);
	}

	@Override
	public TCExpression caseApplyExpression(TCApplyExpression node, TCExpressionSet constraints)
	{
		TCDefinition def = findApplyDefinition(node, env);

		if (def instanceof TCExplicitFunctionDefinition)
		{
			TCExplicitFunctionDefinition exfn = (TCExplicitFunctionDefinition)def;
			
			if (exfn.isCurried)
			{
				throw new UnsupportedOperationException("Cannot expand curried fn: " + node);
			}

			DefinitionExpander.getInstance().expandFunction(exfn);

			TCExpressionList expargs = expandAll(node.args, exfn.type.parameters, constraints);
			
			if (exfn.predef != null)
			{
				// f(a) only valid when pre_f(a) is true
				TCNameToken pre_f = exfn.predef.name;

				constraints.add(new TCApplyExpression(
					new TCVariableExpression(node.location, pre_f, pre_f.getName()),
					expargs));
			}

			if (exfn.postdef != null)
			{
				// f(a) only valid when post_f(a, f(a)) is true
				TCNameToken post_f = exfn.postdef.name;
				TCExpressionList postargs = new TCExpressionList();
				postargs.addAll(expargs);
				postargs.add(new TCApplyExpression(
					node.root.apply(this, constraints),
					expargs));

				constraints.add(new TCApplyExpression(
					new TCVariableExpression(node.location, post_f, post_f.getName()),
					postargs));
			}

			return new TCApplyExpression(
				node.root.apply(this, constraints),
				expargs);
		}
		else if (def instanceof TCImplicitFunctionDefinition)
		{
			TCImplicitFunctionDefinition imfn = (TCImplicitFunctionDefinition)def;

			DefinitionExpander.getInstance().expandFunction(imfn);

			TCExpressionList expargs = expandAll(node.args, imfn.type.parameters, constraints);

			if (imfn.predef != null)
			{
				// f(a) only valid when pre_f(a) is true
				TCNameToken pre_f = imfn.predef.name;

				constraints.add(new TCApplyExpression(
					new TCVariableExpression(node.location, pre_f, pre_f.getName()),
					expargs));
			}

			if (imfn.postdef != null)
			{
				// f(a) only valid when post_f(a, f(a)) is true
				TCNameToken post_f = imfn.postdef.name;
				TCExpressionList postargs = new TCExpressionList();
				postargs.addAll(expargs);
				postargs.add(new TCApplyExpression(
					node.root.apply(this, constraints),
					expargs));

				constraints.add(new TCApplyExpression(
					new TCVariableExpression(node.location, post_f, post_f.getName()),
					postargs));
			}

			return new TCApplyExpression(
				node.root.apply(this, constraints),
				expargs);
		}
		else if (node.root.getType().isSeq(node.location))
		{
			TCExpression root = node.root.apply(this, constraints);
			TCExpression index = node.args.firstElement().apply(this, constraints);

			// s(i) only defined when 0 < i <= len s
			TCExpression defined = new TCAndExpression(
					new TCLessExpression(ZERO, LT, index),
					AND,
					new TCLessExpression(index, LE, new TCLenExpression(node.location, node.root)));

			constraints.add(defined);
			return new TCApplyExpression(root, new TCExpressionList(index));
		}
		else if (node.root.getType().isMap(node.location))
		{
			TCExpression map = node.root.apply(this, constraints);
			TCExpression key = node.args.get(0).apply(this, constraints);

			// m(k) only defined when k in set dom m
			TCExpression defined = new TCInSetExpression(
				key,
				INSET,
				new TCMapDomainExpression(node.location, map));

			constraints.add(defined);
			return new TCApplyExpression(map, new TCExpressionList(key));
		}
		
		return node;
	}

	@Override
	public TCExpression caseVariableExpression(TCVariableExpression node, TCExpressionSet arg)
	{
		TCDefinition def = env.findName(node.name, NameScope.NAMES);

		if (def instanceof TCLocalDefinition && def.nameScope == NameScope.GLOBAL)
		{
			TCLocalDefinition ldef = (TCLocalDefinition)def;
			DefinitionExpander.getInstance().expandValue(ldef);
		}

		return node;
	}

	@Override
	public TCExpression caseIntegerLiteralExpression(TCIntegerLiteralExpression node, TCExpressionSet arg)
	{
		return node;
	}

	@Override
	public TCExpression caseRealLiteralExpression(TCRealLiteralExpression node, TCExpressionSet arg)
	{
		return node;
	}

	@Override
	public TCExpression caseBooleanLiteralExpression(TCBooleanLiteralExpression node, TCExpressionSet arg)
	{
		return node;
	}

	@Override
	public TCExpression caseCharLiteralExpression(TCCharLiteralExpression node, TCExpressionSet arg)
	{
		return node;
	}

	@Override
	public TCExpression caseStringLiteralExpression(TCStringLiteralExpression node, TCExpressionSet arg)
	{
		return node;
	}

	@Override
	public TCExpression caseQuoteLiteralExpression(TCQuoteLiteralExpression node, TCExpressionSet constraints)
	{
		return node;
	}

	@Override
	public TCExpression caseBinaryExpression(TCBinaryExpression node, TCExpressionSet constraints)
	{
		Class<?> clazz = node.getClass();
		TCExpression left = node.left.apply(this, constraints);
		TCExpression right = node.right.apply(this, constraints);

		if (left == node.left && right == node.right)
		{
			return node;	// No changes
		}

		try
		{
			// Call the node type's constructor, passing left right and the operator.
			Constructor<?> ctor = clazz.getConstructor(TCExpression.class, LexToken.class, TCExpression.class);
			return (TCExpression)ctor.newInstance(left, node.op, right);
		}
		catch (Exception e)
		{
			throw new UnsupportedOperationException("Cannot construct " + clazz.getSimpleName() +
				": " + e.getMessage());
		}
	}

	@Override
	public TCExpression caseUnaryExpression(TCUnaryExpression node, TCExpressionSet constraints)
	{
		Class<?> clazz = node.getClass();
		TCExpression exp = node.exp.apply(this, constraints);
		
		if (exp == node.exp)
		{
			return node;	// No changes
		}

		try
		{
			// Call the node type's constructor, passing the exp.
			Constructor<?> ctor = clazz.getConstructor(LexLocation.class, TCExpression.class);
			return (TCExpression)ctor.newInstance(node.location, exp);
		}
		catch (Exception e)
		{
			throw new UnsupportedOperationException("Cannot construct " + clazz.getSimpleName() +
				": " + e.getMessage());
		}
	}

	@Override
	public TCExpression caseImpliesExpression(TCImpliesExpression node, TCExpressionSet constraints)
	{
		// Qualify the consequent here, if it acculumates any constraints.

		TCExpression left = node.left.apply(this, constraints);
		TCExpression right = subconstrain(node.right);

		return new TCImpliesExpression(left, IMPLIES, right);
	}

	@Override
	public TCExpression caseOrExpression(TCOrExpression node, TCExpressionSet constraints)
	{
		// Qualify the RHS here, if it acculumates any constraints.

		TCExpression left = node.left.apply(this, constraints);
		TCExpression right = subconstrain(node.right);

		return new TCOrExpression(left, OR, right);
	}

	@Override
	public TCExpression caseIfExpression(TCIfExpression node, TCExpressionSet constraints)
	{
		// Then and else/if clauses are subconstrained

		TCElseIfExpressionList newList = new TCElseIfExpressionList(); 

		for (TCElseIfExpression elif: node.elseList)
		{
			newList.add((TCElseIfExpression)elif.apply(this, constraints));
		}

		return new TCIfExpression(node.location,
			node.ifExp.apply(this, constraints),
			subconstrain(node.thenExp),
			newList,
			subconstrain(node.elseExp));
	}

	@Override
	public TCExpression caseCasesExpression(TCCasesExpression node, TCExpressionSet constraints)
	{
		// Case alternatives are subconstrained
		TCExpression others = null;

		if (node.others == null)
		{
			others = new TCUndefinedExpression(node.location);
			QualifiedSort qsort = node.getType().apply(new TypeToSMT("?"), env);
			globals.addUndefined(qsort.sort);
		}
		else
		{
			others = node.others.apply(this, constraints);
		}

		TCCaseAlternativeList newCaseList = new TCCaseAlternativeList();

		for (TCCaseAlternative alt: node.cases)
		{
			newCaseList.add(new TCCaseAlternative(
				alt.cexp.apply(this, constraints),
				alt.pattern,
				subconstrain(alt.result)));
		}

		return new TCCasesExpression(node.location,
			node.exp.apply(this, constraints),
			newCaseList,
			others);
	}

	@Override
	public TCExpression caseInSetExpression(TCInSetExpression node, TCExpressionSet arg)
	{
		if (node.right instanceof TCIndicesExpression)
		{
			TCIndicesExpression right = (TCIndicesExpression)node.right;

			// "i in set inds s" is the same as "i > 0 and i <= len s"
			return new TCAndExpression(
				new TCGreaterExpression(
					node.left.apply(this, arg), GT, ZERO),
				AND,
				new TCLessEqualExpression(
					node.left.apply(this, arg),
					LE,
					new TCLenExpression(node.location, right.exp.apply(this, arg))));
		}
		else
		{
			return node;
		}
	}

	@Override
	public TCExpression caseSetEnumExpression(TCSetEnumExpression node, TCExpressionSet constraints)
	{
		if (node.members.isEmpty())
		{
			return node;
		}
		else
		{
			TCExpressionList updated = new TCExpressionList();

			for (TCExpression e: node.members)
			{
				updated.add(e.apply(this, constraints));
			}

			return new TCSetEnumExpression(node.location, updated);
		}
	}

	@Override
	public TCExpression caseSeqEnumExpression(TCSeqEnumExpression node, TCExpressionSet constraints)
	{
		if (node.members.isEmpty())
		{
			return node;
		}
		else
		{
			TCExpressionList updated = new TCExpressionList();

			for (TCExpression e: node.members)
			{
				updated.add(e.apply(this, constraints));
			}

			return new TCSeqEnumExpression(node.location, updated);
		}
	}

	@Override
	public TCExpression caseHeadExpression(TCHeadExpression node, TCExpressionSet constraints)
	{
		constraints.add(new TCNotEqualExpression(
			node.exp,
			NE,
			new TCSeqEnumExpression(node.location, new TCExpressionList())));

		return new TCHeadExpression(node.location, node.exp.apply(this, constraints));
	}

	@Override
	public TCExpression caseTailExpression(TCTailExpression node, TCExpressionSet constraints)
	{
		constraints.add(new TCNotEqualExpression(
			node.exp,
			NE,
			new TCSeqEnumExpression(node.location, new TCExpressionList())));

		return new TCTailExpression(node.location, node.exp.apply(this, constraints));
	}

 	@Override
	public TCExpression caseAnnotatedExpression(TCAnnotatedExpression node, TCExpressionSet constraints)
	{
		return new TCAnnotatedExpression(node.location, node.annotation, node.expression.apply(this, constraints));
	}

 	@Override
	public TCExpression caseDefExpression(TCDefExpression node, TCExpressionSet constraints)
	{
		Pair<TCDefinitionList,TCExpression> pair = expandLetDef(node.localDefs, node.expression, constraints);
		return new TCDefExpression(node.location, pair.first, pair.second);
	}

 	@Override
	public TCExpression caseDivExpression(TCDivExpression node, TCExpressionSet constraints)
	{
		TCExpression left  = node.left.apply(this, constraints);
		TCExpression right = node.right.apply(this, constraints);
		
		// RHS is not zero!
		constraints.add(new TCNotEqualExpression(
			right,
			NE,
			new TCIntegerLiteralExpression(LexIntegerToken.ZERO)));

		return new TCDivExpression(left, DIV, right);
	}

 	@Override
	public TCExpression caseDivideExpression(TCDivideExpression node, TCExpressionSet constraints)
	{
		TCExpression left  = node.left.apply(this, constraints);
		TCExpression right = node.right.apply(this, constraints);
		
		// RHS is not zero!
		constraints.add(new TCNotEqualExpression(
			right,
			NE,
			new TCIntegerLiteralExpression(LexIntegerToken.ZERO)));

		return new TCDivideExpression(left, DIVIDE, right);
	}

 	@Override
	public TCExpression caseElseIfExpression(TCElseIfExpression node, TCExpressionSet constraints)
	{
		return new TCElseIfExpression(node.location,
			node.elseIfExp.apply(this, constraints),
			subconstrain(node.thenExp));
	}

 	@Override
	public TCExpression caseFieldExpression(TCFieldExpression node, TCExpressionSet constraints)
	{
		return new TCFieldExpression(node.object.apply(this, constraints), node.field, node.memberName);
	}

 	@Override
	public TCExpression caseFieldNumberExpression(TCFieldNumberExpression node, TCExpressionSet constraints)
	{
		return new TCFieldNumberExpression(node.tuple.apply(this, constraints), node.field);
	}

 	@Override
	public TCExpression caseMkBasicExpression(TCMkBasicExpression node, TCExpressionSet constraints)
	{
		return new TCMkBasicExpression(node.type, node.arg.apply(this, constraints));
	}

 	@Override
	public TCExpression caseMkTypeExpression(TCMkTypeExpression node, TCExpressionSet constraints)
	{
		TCExpressionList args = new TCExpressionList();

		for (TCExpression arg: node.args)
		{
			args.add(arg.apply(this, constraints));
		}

		node.getType().apply(new TypeExpander(node), env);

		return new TCMkTypeExpression(node.typename, args, false);
	}

 	@Override
	public TCExpression caseMuExpression(TCMuExpression node, TCExpressionSet constraints)
	{
		return new TCMuExpression(node.location, node.record.apply(this, constraints), node.modifiers);
	}

 	@Override
	public TCExpression caseIsExpression(TCIsExpression node, TCExpressionSet constraints)
	{
		if (node.basictype != null)
		{
			return new TCIsExpression(node.location, node.typename, node.basictype, node.test.apply(this, constraints));
		}
		else
		{
			TCDefinition def = env.findType(node.typename, node.location.module);

			if (def instanceof TCTypeDefinition)
			{
				TCTypeDefinition tdef = (TCTypeDefinition)def;
				return new TCIsExpression(node.location, node.typename, tdef.type, node.test.apply(this, constraints));
			}

			throw new UnsupportedOperationException("Cannot expand def: " + def);
		}
	}

 	@Override
	public TCExpression caseNilExpression(TCNilExpression node, TCExpressionSet constraints)
	{
		return node;
	}

 	@Override
	public TCExpression caseMapEnumExpression(TCMapEnumExpression node, TCExpressionSet constraints)
	{
		if (node.members.isEmpty())
		{
			return node;
		}
		else
		{
			TCMapletExpressionList updated = new TCMapletExpressionList();

			for (TCMapletExpression e: node.members)
			{
				updated.add(new TCMapletExpression(
					e.left.apply(this, constraints),
					e.right.apply(this, constraints)));
			}

			return new TCMapEnumExpression(node.location, updated);
		}
	}

 	@Override
	public TCExpression caseTupleExpression(TCTupleExpression node, TCExpressionSet constraints)
	{
		if (node.args.isEmpty())
		{
			return node;
		}
		else
		{
			TCExpressionList updated = new TCExpressionList();

			for (TCExpression e: node.args)
			{
				updated.add(e.apply(this, constraints));
			}

			return new TCTupleExpression(node.location, updated);
		}
	}

 	@Override
	public TCExpression caseSetRangeExpression(TCSetRangeExpression node, TCExpressionSet constraints)
	{
		return new TCSetRangeExpression(node.location,
			node.first.apply(this, constraints),
			node.last.apply(this, constraints));
	}

 	@Override
	public TCExpression caseSubseqExpression(TCSubseqExpression node, TCExpressionSet constraints)
	{
		return new TCSubseqExpression(
			node.seq.apply(this, constraints),
			node.from.apply(this, constraints),
			node.to.apply(this, constraints));
	}

	@Override
	public TCExpression casePreOpExpression(TCPreOpExpression node, TCExpressionSet constraints)
	{
		return node.expression.apply(this, constraints);
	}

	@Override
	public TCExpression casePostOpExpression(TCPostOpExpression node, TCExpressionSet constraints)
	{
		return node.postexpression.apply(this, constraints);
	}

 	@Override
	public TCExpression caseUndefinedExpression(TCUndefinedExpression node, TCExpressionSet constraints)
	{
		QualifiedSort qsort = node.getType().apply(new TypeToSMT("?"), env);
		globals.addUndefined(qsort.sort);
		return node;
	}

	// TODO cases below here are not yet implemented!

 	@Override
	public TCExpression caseFuncInstantiationExpression(TCFuncInstantiationExpression node, TCExpressionSet constraints)
	{
		return node;
	}

 	@Override
	public TCExpression caseIotaExpression(TCIotaExpression node, TCExpressionSet constraints)
	{
		return node;
	}

 	@Override
	public TCExpression caseLambdaExpression(TCLambdaExpression node, TCExpressionSet constraints)
	{
		return node;
	}

 	@Override
	public TCExpression caseMapCompExpression(TCMapCompExpression node, TCExpressionSet constraints)
	{
		return node;
	}

 	@Override
	public TCExpression casePreExpression(TCPreExpression node, TCExpressionSet constraints)
	{
		return node;
	}

 	@Override
	public TCExpression caseSeqCompExpression(TCSeqCompExpression node, TCExpressionSet constraints)
	{
		return node;
	}

 	@Override
	public TCExpression caseSetCompExpression(TCSetCompExpression node, TCExpressionSet constraints)
	{
		return node;
	}

 	@Override
	public TCExpression caseStateInitExpression(TCStateInitExpression node, TCExpressionSet constraints)
	{
		return node;
	}


	/**
	 * Private helpers.
	 */

	private TCExpressionList expandAll(TCExpressionList expressions, TCTypeList parameters, TCExpressionSet constraints)
	{
		TCExpressionList list = new TCExpressionList();

		for (int i=0; i<expressions.size(); i++)
		{
			TCType ptype = parameters.get(i);
			TCExpression arg = expressions.get(i);
			TCExpression cast = ptype.apply(new TypeCast(), arg);

			if (cast != null)	// cast needed for arg into ptype?
			{
				constraints.add(cast);
			}

			list.add(arg.apply(this, constraints));
		}

		return list;
	}

	private TCExpression subconstrain(TCExpression expression)
	{
		TCExpressionSet subconstraints = new TCExpressionSet();
		TCExpression result = expression.apply(this, subconstraints);

		if (!subconstraints.isEmpty())
		{
			if (expression.getType() instanceof TCBooleanType)
			{
				result = new TCAndExpression(Utils.andOf(subconstraints), AND, result);
			}
			else
			{
				QualifiedSort qsort = expression.getType().apply(new TypeToSMT("RESULT"), env);
				globals.addUndefined(qsort.sort);

				result = new TCIfExpression(ANYWHERE,
					Utils.andOf(subconstraints),
					expression,
					new TCElseIfExpressionList(),
					new TCUndefinedExpression(ANYWHERE));
			}
		}

		return result;
	}

	private TCDefinition findApplyDefinition(TCApplyExpression node, Environment env)
	{
		if (node.root instanceof TCVariableExpression)
		{
			TCVariableExpression var = (TCVariableExpression)node.root;
			return env.findName(var.name, NameScope.NAMESANDSTATE);
		}
		
		throw new UnsupportedOperationException("Cannot expand apply: " + node);
	}
}
