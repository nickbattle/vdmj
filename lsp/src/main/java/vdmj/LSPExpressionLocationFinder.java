/*******************************************************************************
 *
 *	Copyright (c) 2020 Nick Battle.
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

package vdmj;

import java.util.HashSet;
import java.util.Set;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.TCVisitorSet;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.visitors.TCDefinitionVisitor;
import com.fujitsu.vdmj.tc.expressions.TCCaseAlternative;
import com.fujitsu.vdmj.tc.expressions.TCCasesExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCFieldExpression;
import com.fujitsu.vdmj.tc.expressions.TCFuncInstantiationExpression;
import com.fujitsu.vdmj.tc.expressions.TCHistoryExpression;
import com.fujitsu.vdmj.tc.expressions.TCIsExpression;
import com.fujitsu.vdmj.tc.expressions.TCLetDefExpression;
import com.fujitsu.vdmj.tc.expressions.TCMkTypeExpression;
import com.fujitsu.vdmj.tc.expressions.TCNewExpression;
import com.fujitsu.vdmj.tc.expressions.TCVariableExpression;
import com.fujitsu.vdmj.tc.expressions.visitors.TCLeafExpressionVisitor;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.patterns.TCBind;
import com.fujitsu.vdmj.tc.patterns.TCMultipleBind;
import com.fujitsu.vdmj.tc.patterns.TCMultipleSeqBind;
import com.fujitsu.vdmj.tc.patterns.TCMultipleSetBind;
import com.fujitsu.vdmj.tc.patterns.TCMultipleTypeBind;
import com.fujitsu.vdmj.tc.patterns.TCPattern;
import com.fujitsu.vdmj.tc.patterns.TCSeqBind;
import com.fujitsu.vdmj.tc.patterns.TCSetBind;
import com.fujitsu.vdmj.tc.patterns.TCTypeBind;

public class LSPExpressionLocationFinder extends TCLeafExpressionVisitor<TCNode, Set<TCNode>, LexLocation>
{
	private final LSPPatternLocationFinder patternFinder;

	public LSPExpressionLocationFinder(TCVisitorSet<TCNode, Set<TCNode>, LexLocation> visitors)
	{
		visitorSet = visitors;
		patternFinder = new LSPPatternLocationFinder();
	}

	@Override
	protected Set<TCNode> newCollection()
	{
		return new HashSet<TCNode>();
	}

	@Override
	public Set<TCNode> caseExpression(TCExpression node, LexLocation arg)
	{
		return newCollection();
	}
	
	@Override
	public Set<TCNode> caseMkTypeExpression(TCMkTypeExpression node, LexLocation arg)
	{
		Set<TCNode> all = super.caseMkTypeExpression(node, arg);
		
		if (arg.within(node.typename.getLocation()))
		{
			all.add(node);
		}
		
		return all;
	}

	@Override
	public Set<TCNode> caseVariableExpression(TCVariableExpression node, LexLocation arg)
	{
		Set<TCNode> result = newCollection();

		if (arg.within(node.location))
		{
			result.add(node);
		}

		return result;
	}
	
	@Override
	public Set<TCNode> caseLetDefExpression(TCLetDefExpression node, LexLocation arg)
	{
		Set<TCNode> all = super.caseLetDefExpression(node, arg);
		TCDefinitionVisitor<Set<TCNode>, LexLocation> defVisitor = visitorSet.getDefinitionVisitor();

		for (TCDefinition def: node.localDefs)
 		{
 			all.addAll(def.apply(defVisitor, arg));
 		}
 		
		return all;
	}
	
	@Override
	public Set<TCNode> caseFuncInstantiationExpression(TCFuncInstantiationExpression node, LexLocation arg)
	{
		Set<TCNode> all = super.caseFuncInstantiationExpression(node, arg);
		all.addAll(node.unresolved.matchUnresolved(arg));
		return all;
	}
	
	@Override
	public Set<TCNode> caseFieldExpression(TCFieldExpression node, LexLocation arg)
	{
		Set<TCNode> all = super.caseFieldExpression(node, arg);
		
		if (arg.within(node.field.getLocation()))
		{
			all.add(node);
		}
	
		return all;
	}
	
	@Override
	public Set<TCNode> caseNewExpression(TCNewExpression node, LexLocation arg)
	{
		Set<TCNode> all = super.caseNewExpression(node, arg);
		
		if (arg.within(node.classname.getLocation()))
		{
			if (node.ctordef != null)
			{
				all.add(node.ctordef.name);
			}
			else
			{
				all.add(node.classname);	// Anon ctor
			}
		}
	
		return all;
	}
	
	@Override
	public Set<TCNode> caseIsExpression(TCIsExpression node, LexLocation arg)
	{
		Set<TCNode> all = super.caseIsExpression(node, arg);
		
		if (node.typename != null && arg.within(node.typename.getLocation()))
		{
			all.add(node);
		}
		// Can't find basic type as yet...!
	
		return all;
	}
	
	@Override
	public Set<TCNode> caseHistoryExpression(TCHistoryExpression node, LexLocation arg)
	{
		Set<TCNode> all = newCollection();
		
		for (TCNameToken opname: node.opnames)
		{
			if (arg.within(opname.getLocation()))
			{
				all.add(opname);
			}
		}
	
		return all;
	}
	
	@Override
	public Set<TCNode> caseCasesExpression(TCCasesExpression node, LexLocation arg)
	{
		Set<TCNode> all = super.caseCasesExpression(node, arg);
		
		for (TCCaseAlternative a: node.cases)
		{
			all.addAll(patternFinder.patternCheck(a.pattern, arg));
		}
		
		return all;
	}
	
	@Override
	protected Set<TCNode> caseBind(TCBind bind, LexLocation arg)
	{
		Set<TCNode> all = super.caseBind(bind, arg);
		
		if (all.isEmpty())
		{
			if (bind instanceof TCTypeBind)
			{
				TCTypeBind tbind = (TCTypeBind)bind;
				all.addAll(tbind.unresolved.matchUnresolved(arg));
			}
			else if (bind instanceof TCSetBind)
			{
				TCSetBind sbind = (TCSetBind)bind;
				all.addAll(patternFinder.patternCheck(sbind.pattern, arg));
			}
			else if (bind instanceof TCSeqBind)
			{
				TCSeqBind sbind = (TCSeqBind)bind;
				all.addAll(patternFinder.patternCheck(sbind.pattern, arg));
			}
		}
		
		return all;
	}

	@Override
 	protected Set<TCNode> caseMultipleBind(TCMultipleBind bind, LexLocation arg)
	{
		Set<TCNode> all = super.caseMultipleBind(bind, arg);
		
		if (all.isEmpty())
		{
			if (bind instanceof TCMultipleTypeBind)
			{
				TCMultipleTypeBind mbind = (TCMultipleTypeBind)bind;
				all.addAll(mbind.unresolved.matchUnresolved(arg));
			}
			else if (bind instanceof TCMultipleSetBind)
			{
				TCMultipleSetBind sbind = (TCMultipleSetBind)bind;
				
				for (TCPattern p: sbind.plist)
				{
					all.addAll(patternFinder.patternCheck(p, arg));
				}
			}
			else if (bind instanceof TCMultipleSeqBind)
			{
				TCMultipleSeqBind sbind = (TCMultipleSeqBind)bind;

				for (TCPattern p: sbind.plist)
				{
					all.addAll(patternFinder.patternCheck(p, arg));
				}
			}
		}
		
		return all;
	}
}
