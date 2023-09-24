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
import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.ast.expressions.ASTExpression;
import com.fujitsu.vdmj.ast.lex.LexToken;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.messages.VDMError;
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

@SuppressWarnings("serial")
public class ProofObligationList extends Vector<ProofObligation>
{
	// Convenience class to hold lists of POs.

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

			if (po.status == POStatus.TRIVIAL)
			{
				sb.append(" by ");
				sb.append(po.proof);
			}

			sb.append(")\n");
			sb.append(po);
			sb.append("\n");
		}

		return sb.toString();
	}

	public void trivialCheck()
	{
		for (ProofObligation po: this)
		{
			po.trivialCheck();
		}
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
				Console.err.println(e.getMessage());
			}
		}
	}

	public void typeCheck(TCNameToken name, Environment env)
	{
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
				Console.err.println(e.getMessage());
			}
		}
	}

	private void typeCheck(ProofObligation obligation, String mname, Environment env) throws Exception
	{
		// Some POs from VDM++ specs can include "new" etc, so parse as the given dialect
		LexTokenReader ltr = new LexTokenReader(obligation.getValue(), Settings.dialect);
		ExpressionReader reader = new ExpressionReader(ltr);
		reader.setCurrentModule(mname);
		ASTExpression ast = reader.readExpression();
		LexToken end = ltr.getLast();
		
		if (!end.is(Token.EOF))
		{
			throw new ParserException(2330, "Tokens found after expression at " + end, LexLocation.ANY, 0);
		}
		
		TCExpression tcexp = ClassMapper.getInstance(TCNode.MAPPINGS).convert(ast);
		Environment tcenv = addTypeParams(obligation, env);
		
		TypeChecker.clearErrors();
		TCType potype = tcexp.typeCheck(tcenv, null, NameScope.NAMESANDANYSTATE, null);
		
		if (!potype.isType(TCBooleanType.class, obligation.location))
		{
			throw new ParserException(2330, "PO is not boolean?", obligation.location, 0);
		}
		
		// Weed out errors that we can cope with
		
		List<VDMError> errs = TypeChecker.getErrors();
		Iterator<VDMError> iter = errs.iterator();
		
		while (iter.hasNext())
		{
			VDMError message = iter.next();
			
			switch (message.number)
			{
				case 3336:	// "Illegal use of RESULT reserved identifier"
				case 3350:	// "Polymorphic function has not been instantiated"
					iter.remove();
					obligation.isCheckable = false;			// No point
					break;
					
				case 3182:	// "Name 'xxx' is not in scope"
					if (message.message.startsWith("Name 'measure_"))
					{
						// Probably an implicit missing measure
						iter.remove();
						obligation.isCheckable = false;		// No point
					}
					break;
					
				default:	// fine
					break;
			}
		}
		
		if (TypeChecker.getErrorCount() > 0)
		{
			throw new ParserException(2330, "PO has type errors?", obligation.location, 0);
		}
		
		obligation.setCheckedExpression(tcexp);
	}

	private Environment addTypeParams(ProofObligation obligation, Environment env)
	{
		if (obligation.typeParams != null && !obligation.typeParams.isEmpty())
		{
			TCDefinitionList pdefs = new TCDefinitionList();
			
			for (TCType ptype: obligation.typeParams)
			{
				TCParameterType param = (TCParameterType)ptype;
				TCDefinition p = new TCLocalDefinition(param.location, param.name, param);
				p.markUsed();
				pdefs.add(p);
			}
			
			return new FlatEnvironment(pdefs, env);
		}
		else
		{
			return env;
		}
	}
}
