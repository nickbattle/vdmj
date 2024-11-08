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

package com.fujitsu.vdmj.po.expressions;

import java.io.Serializable;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.po.PONode;
import com.fujitsu.vdmj.po.POVisitorSet;
import com.fujitsu.vdmj.po.expressions.visitors.POExpressionVisitor;
import com.fujitsu.vdmj.po.statements.visitors.POStatementStateFinder;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POGState;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.pog.SubTypeObligation;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeQualifier;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.tc.types.TCUnionType;
import com.fujitsu.vdmj.typechecker.Environment;

/**
 *	The parent class of all VDM expressions.
 */
public abstract class POExpression extends PONode implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	/** The type of this subexpression */
	private TCType exptype;

	/**
	 * Generate an expression at the given location.
	 *
	 * @param location	The location of the new expression.
	 */
	public POExpression(LexLocation location)
	{
		super(location);
	}

	/**
	 * Generate an expression at the same location as the expression passed.
	 * This is used when a compound expression, comprising several
	 * subexpressions, is being constructed. The expression passed "up" is
	 * considered the location of the overall expression. For example, a
	 * function application involves an expression for the function to apply,
	 * plus a list of expressions for the arguments. The location of the
	 * expression for the function (often just a variable name) is considered
	 * the location of the entire function application.
	 *
	 * @param exp The expression containing the location.
	 */
	public POExpression(POExpression exp)
	{
		this(exp.location);
	}

	@Override
	public abstract String toString();

	@Override
	public boolean equals(Object other)
	{
		if (other instanceof POExpression)
		{
			POExpression oe = (POExpression)other;
			return toString().equals(oe.toString());	// For now...
		}
		else
		{
			return false;
		}
	}

	@Override
	public int hashCode()
	{
		return toString().hashCode();
	}

	/**
	 * Get a list of proof obligations from the expression.
	 *
	 * @param ctxt The call context.
	 * @param pogState TODO
	 * @return The list of proof obligations.
	 */

	public ProofObligationList getProofObligations(POContextStack ctxt, POGState pogState, Environment env)
	{
		return new ProofObligationList();
	}
	
	/**
	 * Get the name of the precondition function of this expression, if it is
	 * a function expression that identifies a function with a precondition.
	 * This is used during proof obligation generation. It is implemented in
	 * the VariableExpression class.
	 * 
	 * null => expression is not a function or operation.
	 * "" => expression is a fn/op without a precondition.
	 * "pre_<name>" => expression is a fn/op with a precondition.
	 */
	public String getPreName()
	{
		return null;	// Not a fn/op, by default
	}
	
	/**
	 * Get and set the exptype. This is used by the ClassMapper.
	 */
	public void setExptype(TCType exptype)
	{
		this.exptype = exptype;
	}
	
	public TCType getExptype()
	{
		return exptype;
	}
	
	/**
	 * Get any obligations for unions that are qualified.
	 */
	public ProofObligationList checkUnionQualifiers(POExpression exp, TCTypeQualifier qualifier, POContextStack ctxt)
	{
		ProofObligationList obligations = new ProofObligationList();
		
		if (exp.getExptype() != null && exp.getExptype().isUnion(location))
		{
			TCUnionType ut = exp.getExptype().getUnion();
			TCTypeSet sets = ut.getMatches(qualifier);
			
			if (sets.size() < ut.types.size() && sets.size() > 0)
			{
				obligations.add(new SubTypeObligation(exp, sets.getType(location), exp.getExptype(), ctxt));
			}
		}

		return obligations;
	}

	public TCNameSet updatesState()
	{
		POStatementStateFinder finder = new POStatementStateFinder();
		POVisitorSet<TCNameToken, TCNameSet, Boolean> vset = finder.getVistorSet();
		return vset.applyExpressionVisitor(this, true);
	}

	public TCNameSet readsState()
	{
		POStatementStateFinder finder = new POStatementStateFinder();
		POVisitorSet<TCNameToken, TCNameSet, Boolean> vset = finder.getVistorSet();
		return vset.applyExpressionVisitor(this, false);
	}

	/**
	 * Implemented by all expressions to allow visitor processing.
	 */
	abstract public <R, S> R apply(POExpressionVisitor<R, S> visitor, S arg);
}
