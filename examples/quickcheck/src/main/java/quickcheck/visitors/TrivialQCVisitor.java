/*******************************************************************************
 *
 *	Copyright (c) 2023 Nick Battle.
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

package quickcheck.visitors;

import java.util.List;
import java.util.Stack;
import java.util.Vector;

import com.fujitsu.vdmj.ast.lex.LexKeywordToken;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCValueDefinition;
import com.fujitsu.vdmj.tc.expressions.TCDefExpression;
import com.fujitsu.vdmj.tc.expressions.TCElseIfExpression;
import com.fujitsu.vdmj.tc.expressions.TCEqualsExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCForAllExpression;
import com.fujitsu.vdmj.tc.expressions.TCIfExpression;
import com.fujitsu.vdmj.tc.expressions.TCImpliesExpression;
import com.fujitsu.vdmj.tc.expressions.TCInSetExpression;
import com.fujitsu.vdmj.tc.expressions.TCIntegerLiteralExpression;
import com.fujitsu.vdmj.tc.expressions.TCLetDefExpression;
import com.fujitsu.vdmj.tc.expressions.TCMapEnumExpression;
import com.fujitsu.vdmj.tc.expressions.TCNotEqualExpression;
import com.fujitsu.vdmj.tc.expressions.TCNotExpression;
import com.fujitsu.vdmj.tc.expressions.TCSeqEnumExpression;
import com.fujitsu.vdmj.tc.expressions.TCSetEnumExpression;
import com.fujitsu.vdmj.tc.expressions.TCVariableExpression;
import com.fujitsu.vdmj.tc.expressions.visitors.TCExpressionVisitor;
import com.fujitsu.vdmj.tc.patterns.TCIdentifierPattern;
import com.fujitsu.vdmj.tc.patterns.TCMultipleBind;
import com.fujitsu.vdmj.tc.patterns.TCMultipleSetBind;
import com.fujitsu.vdmj.tc.patterns.TCPattern;
import com.fujitsu.vdmj.util.Utils;

/**
 * Search for trivial truths in POs. Each method is passed a stack of expressions
 * which are known to be true in the current context and they return true if they
 * are always true, given this stack.
 * 
 * eg. X => Y will pass "X" as a truth when evaluating Y. If Y is just X, then
 * that will always be true and so the whole implication is always true, etc.
 */
public class TrivialQCVisitor extends TCExpressionVisitor<Boolean, Stack<TCExpression>>
{
	private final List<String> evaluated;
	
	public TrivialQCVisitor()
	{
		evaluated = new Vector<String>();
	}
	
	public String getMessage()
	{
		return Utils.listToString(evaluated);
	}
	
	private void pops(Stack<TCExpression> truths, int pushes)
	{
		for (int i=0; i<pushes; i++)
		{
			truths.pop();
		}
	}

	@Override
	public Boolean caseForAllExpression(TCForAllExpression node, Stack<TCExpression> truths)
	{
		int pushes = 0;
		
		for (TCMultipleBind bind: node.bindList)
		{
			if (bind instanceof TCMultipleSetBind)
			{
				TCMultipleSetBind sbind = (TCMultipleSetBind)bind;
				
				for (TCPattern p: sbind.plist)
				{
					if (p instanceof TCIdentifierPattern)
					{
						TCIdentifierPattern id = (TCIdentifierPattern)p;

						truths.push(new TCInSetExpression(
							new TCVariableExpression(id.location, id.name, p.toString()),
							new LexKeywordToken(Token.INSET, sbind.location),
							sbind.set));
						
						pushes++;
					}
				}
			}
		}
		
		boolean result = node.predicate.apply(this, truths);
		pops(truths, pushes);
		return result;
	}
	
	@Override
	public Boolean caseLetDefExpression(TCLetDefExpression node, Stack<TCExpression> truths)
	{
		int pushes = 0;
		
		for (TCDefinition def: node.localDefs)
		{
			if (def instanceof TCValueDefinition)
			{
				TCValueDefinition vdef = (TCValueDefinition)def;
				
				if (vdef.pattern instanceof TCIdentifierPattern)
				{
					TCIdentifierPattern id = (TCIdentifierPattern)vdef.pattern;

					truths.push(new TCEqualsExpression(
							new TCVariableExpression(id.location, id.name, id.toString()),
							new LexKeywordToken(Token.EQUALS, id.location),
							vdef.exp));
						
					pushes++;
				}
			}
		}
		
		boolean result = node.expression.apply(this, truths);
		pops(truths, pushes);
		return result;
	}
	
	@Override
	public Boolean caseDefExpression(TCDefExpression node, Stack<TCExpression> truths)
	{
		int pushes = 0;
		
		for (TCDefinition def: node.localDefs)
		{
			if (def instanceof TCValueDefinition)
			{
				TCValueDefinition vdef = (TCValueDefinition)def;
				
				if (vdef.pattern instanceof TCIdentifierPattern)
				{
					TCIdentifierPattern id = (TCIdentifierPattern)vdef.pattern;

					truths.push(new TCEqualsExpression(
							new TCVariableExpression(id.location, id.name, id.toString()),
							new LexKeywordToken(Token.EQUALS, id.location),
							vdef.exp));
						
					pushes++;
				}
			}
		}
		
		boolean result = node.expression.apply(this, truths);
		pops(truths, pushes);
		return result;
	}
	
	@Override
	public Boolean caseIfExpression(TCIfExpression node, Stack<TCExpression> truths)
	{
		int pushes = 0;
		
		truths.push(node.ifExp);
		boolean result = node.thenExp.apply(this, truths);
		truths.pop();
		
		truths.push(new TCNotExpression(node.location, node.ifExp));
		pushes++;
		
		for (TCElseIfExpression elif: node.elseList)
		{
			truths.push(elif.elseIfExp);
			result = result && elif.thenExp.apply(this, truths);
			truths.pop();
			
			truths.push(new TCNotExpression(node.location, elif.thenExp));
			pushes++;
		}

		result = result && node.elseExp.apply(this, truths);
		pops(truths, pushes);
		return result;		// All paths always true
	}
	
	@Override
	public Boolean caseImpliesExpression(TCImpliesExpression node, Stack<TCExpression> truths)
	{
		truths.push(node.left);
		int pushes = 1;

		if (node.left instanceof TCEqualsExpression)
		{
			TCEqualsExpression eq = (TCEqualsExpression)node.left;
			truths.push(new TCNotEqualExpression(eq.left, new LexKeywordToken(Token.NE, eq.location), eq.right));
			pushes++;
		}
		else if (node.left instanceof TCNotExpression)
		{
			TCNotExpression nexp = (TCNotExpression)node.left;
			
			if (nexp.exp instanceof TCEqualsExpression)
			{
				TCEqualsExpression eq = (TCEqualsExpression)nexp.exp;
				truths.push(new TCNotEqualExpression(eq.left, new LexKeywordToken(Token.NE, eq.location), eq.right));
				pushes++;
			}
		}
		
		boolean result = node.right.apply(this, truths);
		pops(truths, pushes);
		return result;
	}
	
	@Override
	public Boolean caseNotEqualExpression(TCNotEqualExpression node, Stack<TCExpression> truths)
	{
		if (node.left instanceof TCSeqEnumExpression &&
			node.right instanceof TCSeqEnumExpression)
		{
			TCSeqEnumExpression sleft = (TCSeqEnumExpression)node.left;
			TCSeqEnumExpression sright = (TCSeqEnumExpression)node.right;
			
			return (sleft.members.isEmpty() != sright.members.isEmpty());	// eg. [1,2,3] <> []
		}
		else if (node.left instanceof TCSetEnumExpression &&
			node.right instanceof TCSetEnumExpression)
		{
			TCSetEnumExpression sleft = (TCSetEnumExpression)node.left;
			TCSetEnumExpression sright = (TCSetEnumExpression)node.right;
			
			return (sleft.members.isEmpty() != sright.members.isEmpty());	// eg. {1,2,3} <> {}
		}
		else if (node.left instanceof TCMapEnumExpression &&
			node.right instanceof TCMapEnumExpression)
		{
			TCMapEnumExpression sleft = (TCMapEnumExpression)node.left;
			TCMapEnumExpression sright = (TCMapEnumExpression)node.right;
				
			return (sleft.members.isEmpty() != sright.members.isEmpty());	// eg. {1|->2} <> {|->}
		}
		else if (node.left instanceof TCIntegerLiteralExpression &&
			node.right instanceof TCIntegerLiteralExpression)
		{
			TCIntegerLiteralExpression vleft = (TCIntegerLiteralExpression)node.left;
			TCIntegerLiteralExpression vright = (TCIntegerLiteralExpression)node.right;
					
			return (vleft.value.value != vright.value.value);		// eg. 123 <> 456
		}
		
		return caseExpression(node, truths);
	}

	@Override
	public Boolean caseExpression(TCExpression node, Stack<TCExpression> truths)
	{
		if (truths.contains(node))
		{
			evaluated.add(node.toString());		// This truth was used
			return true;
		}
		else
		{
			return false;
		}
	}
}