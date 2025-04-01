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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.ast.expressions.ASTExpression;
import com.fujitsu.vdmj.ast.lex.LexToken;
import com.fujitsu.vdmj.config.Properties;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.messages.VDMError;
import com.fujitsu.vdmj.po.PONode;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.expressions.visitors.POTotalExpressionVisitor;
import com.fujitsu.vdmj.po.modules.MultiModuleEnvironment;
import com.fujitsu.vdmj.syntax.ExpressionReader;
import com.fujitsu.vdmj.syntax.ParserException;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.definitions.TCLocalDefinition;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.modules.TCModule;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCParameterType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.TypeChecker;
import com.fujitsu.vdmj.typechecker.TypeComparator;

@SuppressWarnings("serial")
public class ProofObligationList extends Vector<ProofObligation>
{
	// Convenience class to hold lists of POs.
	
	@Override
	public synchronized boolean add(ProofObligation e)
	{
		if (!this.contains(e))		// Eliminate duplicates
		{
			return super.add(e);
		}
		
		return false;
	}
	
	@Override
	public synchronized boolean addAll(Collection<? extends ProofObligation> poList)
	{
		boolean changed = false;
		
		for (ProofObligation po: poList)
		{
			changed = this.add(po) || changed;
		}
		
		return changed;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();

		for (ProofObligation po: this)
		{
			sb.append("Proof Obligation ");
			sb.append(po.number);
			sb.append(": (");
			sb.append(po.status);
			
			if (po.qualifier != null)
			{
				sb.append(" ");
				sb.append(po.qualifier);
			}

			if (po.counterexample != null && !po.counterexample.isEmpty())
			{
				sb.append(", Counterexample: ");
				String sep = "";
				
				for (TCNameToken name: po.counterexample.keySet())
				{
					sb.append(sep);
					sb.append(name);
					sb.append(" = ");
					sb.append(po.counterexample.get(name));
					sep = ", ";
				}
			}
			
			sb.append(")\n");
			
			if (po.message != null)
			{
				sb.append(po.message);
				sb.append("\n");
			}
			
			sb.append(po);
			sb.append("\n");
		}

		return sb.toString();
	}

	public void renumber()
	{
		renumber(1);
	}

	public void renumber(int from)
	{
		int n = from;

		for (ProofObligation po: this)
		{
			po.number = n++;
		}
	}
	
	public void typeCheck(TCModule tcmodule, MultiModuleEnvironment menv)
	{
		String tc = TypeComparator.getCurrentModule();
		
		for (ProofObligation po: this)
		{
			try
			{
				if (po.isCheckable)
				{
					typeCheck(po, po.location.module, menv);
				}
			}
			catch (Exception e)
			{
				Console.err.println(po.toString());
				TypeChecker.printErrors(Console.err);
				Console.err.println(e.toString());
				po.markUnchecked(ProofObligation.PO_HAS_ERRORS);
			}
		}
		
		TypeComparator.setCurrentModule(tc);
	}

	public void typeCheck(TCNameToken name, Environment env)
	{
		String tc = TypeComparator.getCurrentModule();
		
		for (ProofObligation po: this)
		{
			try
			{
				if (po.isCheckable)
				{
					typeCheck(po, name.getName(), env);
				}
			}
			catch (Exception e)
			{
				Console.err.println(po.toString());
				TypeChecker.printErrors(Console.err);
				Console.err.println(e.toString());
				po.markUnchecked(ProofObligation.PO_HAS_ERRORS);
			}
		}
		
		TypeComparator.setCurrentModule(tc);
	}

	private void typeCheck(ProofObligation obligation, String mname, Environment env) throws Exception
	{
		// Some POs from VDM++ specs can include "new" etc, so parse as the given dialect
		LexTokenReader ltr = new LexTokenReader(obligation.getSource(), Settings.dialect);
		ExpressionReader reader = new ExpressionReader(ltr);
		
		// Treat the PO as if it is an expression within the module that defines it.
		reader.setCurrentModule(mname);
		TypeComparator.setCurrentModule(mname);
		
		boolean oldmax = Properties.parser_maximal_types;
		Properties.parser_maximal_types = true;		// For parse of PO on inv_T(T!)
		ASTExpression ast = reader.readExpression();
		Properties.parser_maximal_types = oldmax;
		
		LexToken end = ltr.getLast();
		
		if (!end.is(Token.EOF))
		{
			throw new ParserException(2330, "POG: Tokens found after obligation at " + end, LexLocation.ANY, 0);
		}
		
		TCExpression tcexp = ClassMapper.getInstance(TCNode.MAPPINGS).convertLocal(ast);
		TypeChecker.clearErrors();
		
		if (obligation.typeParams != null)
		{
			// Add local definitions for the @T parameters in the obligation
			env = new FlatEnvironment(getTypeParamDefinitions(obligation), env);
		}
		
		TCType potype = tcexp.typeCheck(env, null, NameScope.NAMESANDANYSTATE, null);
		
		if (!potype.isType(TCBooleanType.class, obligation.location))
		{
			throw new ParserException(2336, "POG: Obligation is not boolean?", obligation.location, 0);
		}
		
		// Weed out errors that we can cope with
		
		List<VDMError> errs = TypeChecker.getErrors();
		Iterator<VDMError> iter = errs.iterator();
		
		while (iter.hasNext())
		{
			VDMError message = iter.next();
			
			switch (message.number)
			{
				case 3182:	// "Name 'xxx' is not in scope"
					if (message.message.startsWith("Name 'measure_"))
					{
						// Probably an implicit missing measure
						iter.remove();
						obligation.markUnchecked(ProofObligation.MISSING_MEASURE);
					}
					break;
					
				case 3336:	// Illegal use of RESULT reserved identifier
					iter.remove();
					break;
					
				default:	// fine
					break;
			}
		}
		
		if (TypeChecker.getErrorCount() > 0)
		{
			throw new ParserException(2337, "POG Obligation has type errors?", obligation.location, 0);
		}

		obligation.setCheckedExpression(tcexp);

		// Note whether the obligation expression itself has obligations. This is used
		// in some proof strategies.
		
		POExpression poexp = ClassMapper.getInstance(PONode.MAPPINGS).convertLocal(tcexp);
		POTotalExpressionVisitor visitor = new POTotalExpressionVisitor();
		poexp.apply(visitor, null);
		obligation.setHasObligations(!visitor.isTotal());
	}

	public TCDefinitionList getTypeParamDefinitions(ProofObligation po)
	{
		TCDefinitionList defs = new TCDefinitionList();

		for (TCType ptype: po.typeParams)
		{
			TCParameterType param = (TCParameterType)ptype;
			TCDefinition p = new TCLocalDefinition(param.location, param.name, param);
			p.markUsed();
			defs.add(p);
		}

		return defs;
	}

	/**
	 * This is used by POG to mark all obligations as unchecked, with a reason. Preferably,
	 * use the reason code string constants in ProofObligation.
	 */
	public ProofObligationList markUnchecked(String message)
	{
		for (ProofObligation obligation: this)
		{
			obligation.markUnchecked(message);
		}
		
		return this;	// Convenient for getProofObligations(ctxt, env).markUnchecked("Some reason")
	}
}
