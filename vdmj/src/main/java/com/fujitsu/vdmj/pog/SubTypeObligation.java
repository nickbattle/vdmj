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

package com.fujitsu.vdmj.pog;

import java.util.Iterator;

import com.fujitsu.vdmj.po.definitions.POExplicitFunctionDefinition;
import com.fujitsu.vdmj.po.definitions.POExplicitOperationDefinition;
import com.fujitsu.vdmj.po.definitions.POImplicitFunctionDefinition;
import com.fujitsu.vdmj.po.definitions.POImplicitOperationDefinition;
import com.fujitsu.vdmj.po.expressions.POApplyExpression;
import com.fujitsu.vdmj.po.expressions.POBooleanLiteralExpression;
import com.fujitsu.vdmj.po.expressions.POCharLiteralExpression;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.expressions.POExpressionList;
import com.fujitsu.vdmj.po.expressions.POFuncInstantiationExpression;
import com.fujitsu.vdmj.po.expressions.POMapEnumExpression;
import com.fujitsu.vdmj.po.expressions.POMapletExpression;
import com.fujitsu.vdmj.po.expressions.POMkTypeExpression;
import com.fujitsu.vdmj.po.expressions.PONotYetSpecifiedExpression;
import com.fujitsu.vdmj.po.expressions.POSeqEnumExpression;
import com.fujitsu.vdmj.po.expressions.POSetEnumExpression;
import com.fujitsu.vdmj.po.expressions.POSetRangeExpression;
import com.fujitsu.vdmj.po.expressions.POSubclassResponsibilityExpression;
import com.fujitsu.vdmj.po.expressions.POSubseqExpression;
import com.fujitsu.vdmj.po.expressions.POTupleExpression;
import com.fujitsu.vdmj.po.expressions.POVariableExpression;
import com.fujitsu.vdmj.po.patterns.POIdentifierPattern;
import com.fujitsu.vdmj.po.patterns.POPattern;
import com.fujitsu.vdmj.po.patterns.POTuplePattern;
import com.fujitsu.vdmj.po.types.POPatternListTypePair;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCBasicType;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCCharacterType;
import com.fujitsu.vdmj.tc.types.TCField;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
import com.fujitsu.vdmj.tc.types.TCIntegerType;
import com.fujitsu.vdmj.tc.types.TCInvariantType;
import com.fujitsu.vdmj.tc.types.TCMapType;
import com.fujitsu.vdmj.tc.types.TCNamedType;
import com.fujitsu.vdmj.tc.types.TCNaturalOneType;
import com.fujitsu.vdmj.tc.types.TCNaturalType;
import com.fujitsu.vdmj.tc.types.TCNumericType;
import com.fujitsu.vdmj.tc.types.TCProductType;
import com.fujitsu.vdmj.tc.types.TCRecordType;
import com.fujitsu.vdmj.tc.types.TCSeq1Type;
import com.fujitsu.vdmj.tc.types.TCSeqType;
import com.fujitsu.vdmj.tc.types.TCSet1Type;
import com.fujitsu.vdmj.tc.types.TCSetType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.tc.types.TCUnionType;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.typechecker.TypeComparator;

public class SubTypeObligation extends ProofObligation
{
	public SubTypeObligation(
		POExpression exp, TCType etype, TCType atype, POContextStack ctxt)
	{
		super(exp.location, POType.SUB_TYPE, ctxt);
		value = ctxt.getObligation(oneType(false, exp, etype, atype));
		return;
	}

	public SubTypeObligation(
		POExplicitFunctionDefinition func, TCType etype, TCType atype, POContextStack ctxt)
	{
		super(func.location, POType.SUB_TYPE, ctxt);

		POExpression body = null;

		if (func.body instanceof PONotYetSpecifiedExpression ||
			func.body instanceof POSubclassResponsibilityExpression)
		{
			// We have to say "f(a)" because we have no body
			POExpression root = new POVariableExpression(func.name, null);;
			
			if (func.typeParams != null)
			{
				TCTypeList actuals = new TCTypeList();
				
				for (TCType p: func.typeParams)
				{
					actuals.add(new TCUnknownType(p.location));	// "?"
				}
				
				root = new POFuncInstantiationExpression(root, actuals, func.type, func, null);
			}
			
			POExpressionList args = new POExpressionList();

			for (POPattern p: func.paramPatternList.get(0))
			{
				args.add(p.getMatchingExpression());
			}
			
			TCFunctionType type = (TCFunctionType)func.getType();
			body = new POApplyExpression(root, args, type, type.parameters, null);
		}
		else
		{
			body = func.body;
		}

		value = ctxt.getObligation(oneType(false, body, etype, atype));
		definition = func;
	}

	public SubTypeObligation(
		POImplicitFunctionDefinition func, TCType etype, TCType atype, POContextStack ctxt)
	{
		super(func.location, POType.SUB_TYPE, ctxt);

		POExpression body = null;

		if (func.body instanceof PONotYetSpecifiedExpression ||
			func.body instanceof POSubclassResponsibilityExpression)
		{
			// We have to say "f(a)" because we have no body

			POExpression root = new POVariableExpression(func.name, null);
			POExpressionList args = new POExpressionList();

			for (POPatternListTypePair pltp: func.parameterPatterns)
			{
				for (POPattern p: pltp.patterns)
				{
					args.add(p.getMatchingExpression());
				}
			}

			TCFunctionType type = (TCFunctionType)func.getType();
			body = new POApplyExpression(root, args, type, type.parameters, null);
		}
		else
		{
			body = func.body;
		}

		value = ctxt.getObligation(oneType(false, body, etype, atype));
		definition = func;
	}

	public SubTypeObligation(
		POExplicitOperationDefinition def, TCType actualResult, POContextStack ctxt)
	{
		super(def.location, POType.SUB_TYPE, ctxt);

		POVariableExpression result = new POVariableExpression(
			new TCNameToken(def.location, def.name.getModule(), "RESULT"), null);

		value = ctxt.getObligation(oneType(false, result, def.type.result, actualResult));
	}

	public SubTypeObligation(
		POImplicitOperationDefinition def, TCType actualResult, POContextStack ctxt)
	{
		super(def.location, POType.SUB_TYPE, ctxt);
		POExpression result = null;

		if (def.result.pattern instanceof POIdentifierPattern)
		{
			POIdentifierPattern ip = (POIdentifierPattern)def.result.pattern;
			result = new POVariableExpression(ip.name, null);
		}
		else
		{
			POTuplePattern tp = (POTuplePattern)def.result.pattern;
			POExpressionList args = new POExpressionList();

			for (POPattern p: tp.plist)
			{
				POIdentifierPattern ip = (POIdentifierPattern)p;
				args.add(new POVariableExpression(ip.name, null));
			}

			result = new POTupleExpression(def.location, args, null);
		}

		value = ctxt.getObligation(oneType(false, result, def.type.result, actualResult));
	}

	private String oneType(boolean rec, POExpression exp, TCType etype, TCType atype)
	{
		if (atype != null && rec)
		{
			if (TypeComparator.isSubType(atype, etype))
			{
				return "";		// A sub comparison is OK without checks
			}
		}

		StringBuilder sb = new StringBuilder();
		String prefix = "";
		etype = etype.deBracket();
		
		if (etype instanceof TCUnionType)
		{
			TCUnionType ut = (TCUnionType)etype;
			TCTypeSet possibles = new TCTypeSet();

			for (TCType pos: ut.types)
			{
				if (atype == null || TypeComparator.compatible(pos, atype))
				{
					possibles.add(pos);
				}
			}

			prefix = "";

			for (TCType poss: possibles)
			{
				String s = oneType(true, exp, poss, null);

				sb.append(prefix);
				sb.append("(");
				addIs(sb, exp, poss);

				if (s.length() > 0 && !s.startsWith("is_("))
				{
					sb.append(" and ");
					sb.append(s);
				}

				sb.append(")");
				prefix = " or\n";
			}
		}
		else if (etype instanceof TCInvariantType)
		{
			TCInvariantType et = (TCInvariantType)etype;
			prefix = "";

			if (et.invdef != null && !et.isMaximal())
			{
				TCNameToken invname = et.invdef.name;
				
				if (invname.getModule().equals(location.module))
				{
	    			sb.append(invname.getName());	// inv_T
				}
				else
				{
					sb.append(invname.getModule());
					sb.append("`");
					sb.append(invname.getName());	// Module`inv_T
				}
				
    			sb.append("(");
				sb.append(exp);
    			sb.append(")");
			}
			else if (etype instanceof TCNamedType)
			{
				TCNamedType nt = (TCNamedType)etype;

				if (atype instanceof TCNamedType)
				{
					atype = ((TCNamedType)atype).type;
				}
				else
				{
					atype = null;
				}

				String s = oneType(true, exp, nt.type, atype);

				if (s.length() > 0)
				{
					sb.append(prefix);
					sb.append("(");
					sb.append(s);
					sb.append(")");
				}
				else
				{
					sb.append(prefix);
					addIs(sb, exp, etype);
				}
			}
			else if (etype instanceof TCRecordType)
			{
				if (exp instanceof POMkTypeExpression)
				{
					TCRecordType rt = (TCRecordType)etype;
					POMkTypeExpression mk = (POMkTypeExpression)exp;

					if (rt.fields.size() == mk.args.size())
					{
    					Iterator<TCField> fit = rt.fields.iterator();
    					Iterator<TCType> ait = mk.argTypes.iterator();

    					for (POExpression e: mk.args)
    					{
    						String s = oneType(true, e, fit.next().type, ait.next());

    						if (s.length() > 0)
    						{
    							sb.append(prefix);
    							sb.append("(");
    							sb.append(s);
    							sb.append(")");
    							prefix = "\nand ";
    						}
    					}
					}
				}
				else
				{
					sb.append(prefix);
					addIs(sb, exp, etype);
				}
			}
			else
			{
				sb.append(prefix);
				addIs(sb, exp, etype);
			}
		}
		else if (etype instanceof TCSeqType)
		{
			prefix = "";

			if (etype instanceof TCSeq1Type && atype != null && !(atype.getSeq() instanceof TCSeq1Type))
			{
    			sb.append(exp);
    			sb.append(" <> []");
    			prefix = " and ";
			}

			if (exp instanceof POSeqEnumExpression)
			{
				TCSeqType stype = (TCSeqType)etype;
				POSeqEnumExpression seq = (POSeqEnumExpression)exp;
				Iterator<TCType> it = seq.types.iterator();

				for (POExpression m: seq.members)
				{
					String s = oneType(true, m, stype.seqof, it.next());

					if (s.length() > 0)
					{
						sb.append(prefix);
						sb.append("(");
						sb.append(s);
						sb.append(")");
						prefix = "\nand ";
					}
				}
			}
			else if (exp instanceof POSubseqExpression)
			{
				POSubseqExpression subseq = (POSubseqExpression)exp;
				TCType itype = new TCNaturalOneType(exp.location);
				String s = oneType(true, subseq.from, itype, subseq.ftype);

				if (s.length() > 0)
				{
					sb.append("(");
					sb.append(s);
					sb.append(")");
					sb.append(" and ");
				}

				s = oneType(true, subseq.to, itype, subseq.ttype);

				if (s.length() > 0)
				{
					sb.append("(");
					sb.append(s);
					sb.append(")");
					sb.append(" and ");
				}

				sb.append(subseq.to);
				sb.append(" <= len ");
				sb.append(subseq.seq);

				sb.append(" and ");
				addIs(sb, exp, etype);		// Like set range does
			}
			else
			{
				sb = new StringBuilder();	// remove any "x <> []"
				addIs(sb, exp, etype);
			}
		}
		else if (etype instanceof TCMapType)
		{
			if (exp instanceof POMapEnumExpression)
			{
				TCMapType mtype = (TCMapType)etype;
				POMapEnumExpression seq = (POMapEnumExpression)exp;
				Iterator<TCType> dit = seq.domtypes.iterator();
				Iterator<TCType> rit = seq.rngtypes.iterator();
				prefix = "";

				for (POMapletExpression m: seq.members)
				{
					String s = oneType(true, m.left, mtype.from, dit.next());

					if (s.length() > 0)
					{
						sb.append(prefix);
						sb.append("(");
						sb.append(s);
						sb.append(")");
						prefix = "\nand ";
					}

					s = oneType(true, m.right, mtype.to, rit.next());

					if (s.length() > 0)
					{
						sb.append(prefix);
						sb.append("(");
						sb.append(s);
						sb.append(")");
						prefix = "\nand ";
					}
				}
			}

			sb.append(prefix);
			addIs(sb, exp, etype);	// eg. is injective as well as the above
		}
		else if (etype instanceof TCSetType)
		{
			prefix = "";

			if (etype instanceof TCSet1Type && atype != null && !(atype.getSet() instanceof TCSet1Type))
			{
    			sb.append(exp);
    			sb.append(" <> {}");
    			prefix = " and ";
			}

			if (exp instanceof POSetEnumExpression)
			{
				TCSetType stype = (TCSetType)etype;
				POSetEnumExpression set = (POSetEnumExpression)exp;
				Iterator<TCType> it = set.types.iterator();

				for (POExpression m: set.members)
				{
					String s = oneType(true, m, stype.setof, it.next());

					if (s.length() > 0)
					{
						sb.append(prefix);
						sb.append("(");
						sb.append(s);
						sb.append(")");
						prefix = "\nand ";
					}
				}
			}
			else if (exp instanceof POSetRangeExpression)
			{
				TCSetType stype = (TCSetType)etype;
				POSetRangeExpression range = (POSetRangeExpression)exp;
				TCType itype = new TCIntegerType(exp.location);

				String s = oneType(true, range.first, itype, range.ftype);

				if (s.length() > 0)
				{
					sb.append(prefix);
					sb.append("(");
					sb.append(s);
					sb.append(")");
					prefix = "\nand ";
				}

				s = oneType(true, range.first, stype.setof, range.ftype);

				if (s.length() > 0)
				{
					sb.append(prefix);
					sb.append("(");
					sb.append(s);
					sb.append(")");
					prefix = "\nand ";
				}

				s = oneType(true, range.last, itype, range.ltype);

				if (s.length() > 0)
				{
					sb.append(prefix);
					sb.append("(");
					sb.append(s);
					sb.append(")");
					prefix = "\nand ";
				}

				s = oneType(true, range.last, stype.setof, range.ltype);

				if (s.length() > 0)
				{
					sb.append(prefix);
					sb.append("(");
					sb.append(s);
					sb.append(")");
					prefix = "\nand ";
				}
			}

			sb.append(prefix);
			addIs(sb, exp, etype);
		}
		else if (etype instanceof TCProductType)
		{
			if (exp instanceof POTupleExpression)
			{
				TCProductType pt = (TCProductType)etype;
				POTupleExpression te = (POTupleExpression)exp;
				Iterator<TCType> eit = pt.types.iterator();
				Iterator<TCType> ait = te.types.iterator();
				prefix = "";

				for (POExpression e: te.args)
				{
					String s = oneType(true, e, eit.next(), ait.next());

					if (s.length() > 0)
					{
						sb.append(prefix);
						sb.append("(");
						sb.append(s);
						sb.append(")");
						prefix = " and ";
					}
				}
			}
			else
			{
				addIs(sb, exp, etype);
			}
		}
		else if (etype instanceof TCBasicType)
		{
    		if (etype instanceof TCNumericType)
    		{
    			TCNumericType nt = (TCNumericType)etype;

    			if (atype instanceof TCNumericType)
    			{
    				TCNumericType ant = (TCNumericType)atype;

    				if (ant.getWeight() > nt.getWeight())
    				{
    					boolean isWhole = ant.getWeight() < 3;
    					
            			if (isWhole && nt instanceof TCNaturalOneType)
            			{
          					sb.append(exp);
           					sb.append(" > 0");
            			}
            			else if (isWhole && nt instanceof TCNaturalType)
            			{
           					sb.append(exp);
           					sb.append(" >= 0");
            			}
            			else
            			{
                			sb.append("is_");
                			sb.append(nt);
                			sb.append("(");
                			sb.append(exp);
                			sb.append(")");
            			}
    				}
    			}
    			else
    			{
        			sb.append("is_");
        			sb.append(nt);
        			sb.append("(");
        			sb.append(exp);
        			sb.append(")");
    			}
    		}
    		else if (etype instanceof TCBooleanType)
    		{
    			if (!(exp instanceof POBooleanLiteralExpression))
    			{
        			addIs(sb, exp, etype);
    			}
    		}
    		else if (etype instanceof TCCharacterType)
    		{
    			if (!(exp instanceof POCharLiteralExpression))
    			{
        			addIs(sb, exp, etype);
    			}
    		}
    		else
    		{
    			addIs(sb, exp, etype);
    		}
		}
		else
		{
			addIs(sb, exp, etype);
		}

		return sb.toString();
	}

	private void addIs(StringBuilder sb, POExpression exp, TCType type)
	{
		sb.append("is_(");
		sb.append(exp);
		sb.append(", ");
		sb.append(explicitType(type, exp.location));
		sb.append(")");
	}
}
