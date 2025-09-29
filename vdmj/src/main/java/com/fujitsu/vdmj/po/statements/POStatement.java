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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.po.statements;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

import com.fujitsu.vdmj.ast.lex.LexBooleanToken;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.po.PONode;
import com.fujitsu.vdmj.po.annotations.POAnnotation;
import com.fujitsu.vdmj.po.annotations.POAnnotationList;
import com.fujitsu.vdmj.po.definitions.POAssignmentDefinition;
import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.definitions.PODefinitionList;
import com.fujitsu.vdmj.po.definitions.visitors.PODefinitionOperationExtractor;
import com.fujitsu.vdmj.po.expressions.POApplyExpression;
import com.fujitsu.vdmj.po.expressions.POBooleanLiteralExpression;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.expressions.visitors.POOperationExtractionException;
import com.fujitsu.vdmj.po.expressions.visitors.POExpressionOperationExtractor;
import com.fujitsu.vdmj.po.statements.visitors.PODesignatorOperationExtractor;
import com.fujitsu.vdmj.po.statements.visitors.POStatementStateUpdates;
import com.fujitsu.vdmj.po.statements.visitors.POStatementVisitor;
import com.fujitsu.vdmj.pog.POAmbiguousContext;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POGState;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;

/**
 * The parent class of all statements.
 */
public abstract class POStatement extends PONode
{
	private static final long serialVersionUID = 1L;

	/** The type of this sub-expression */
	private TCType stmttype;
	
	/** A list of annotations, if any. See POAnnotatedStatement */
	protected POAnnotationList annotations = new POAnnotationList();

	/**
	 * Create a statement at the given location.
	 * @param location
	 */
	public POStatement(LexLocation location)
	{
		super(location);
	}

	@Override
	abstract public String toString();

	/**
	 * Get a list of proof obligations from the statement.
	 *
	 * @param ctxt The call context.
	 * @param pogState The global context created by this statement, if any.
	 * @param env The Environment to lookup symbols.
	 * @return The list of proof obligations.
	 */
	public ProofObligationList getProofObligations(POContextStack ctxt, POGState pogState, Environment env)
	{
		return new ProofObligationList();
	}

	/**
	 * Get and set the statement type field.
	 * The setter returns the type too, so return T can change to return setType(T). 
	 */
	public TCType getStmttype()
	{
		return stmttype;
	}
	
	public TCType setStmttype(TCType stmttype)
	{
		this.stmttype = stmttype;
		return stmttype;
	}

	/**
	 * State variables updated or read by this statement.
	 */
	public TCNameSet updatesState(POContextStack ctxt)
	{
		POStatementStateUpdates visitor = new POStatementStateUpdates(ctxt);
		return this.apply(visitor, null);
	}

	/**
	 * Add annotations from POAnnotatedAnnotation
	 */
	public POStatement addAnnotation(POAnnotation annotation)
	{
		annotations.add(annotation);
		return this;
	}

	/**
	 * Create the missing @LoopInvariant for substitution into the POs by name.
	 * 
	 *   (-- Missing @LoopInvariant, assuming true at 9:9
	 *     (let  : bool = true in
	 *       ( and (condition) =>
	 */
	protected PODefinition getLoopInvDef()
	{
		TCNameToken invname = new TCNameToken(location, location.module, "$LoopInvariant");
		POExpression invvalue = new POBooleanLiteralExpression(new LexBooleanToken(true, location));
		TCBooleanType BOOL = new TCBooleanType(location);
		return new POAssignmentDefinition(invname, BOOL, invvalue, BOOL);
	}

	/**
	 * Analyse an expression to extract the operation apply calls, and add context to the stack,
	 * before returning the substituted expression. Exceptions lead to an ambiguous context.
	 */
	public POExpression extractOpCalls(POExpression exp,
		ProofObligationList obligations, POGState pogState, POContextStack ctxt, Environment env)
	{
		try
		{
			POExpressionOperationExtractor visitor = new POExpressionOperationExtractor();
			POExpression subs = exp.apply(visitor);
			LinkedHashMap<TCNameToken, POApplyExpression> table = visitor.getSubstitutions();

			for (Entry<TCNameToken, POApplyExpression> entry: table.entrySet())	// In order
			{
				TCNameToken var = entry.getKey();
				POApplyExpression apply = entry.getValue();

				// Get any obligations from the preconditions or arguments passed to the apply,
				// without the rest of the ApplyExpression processing here, because we've removed
				// the apply calls from the exp.
				apply.getFuncOpObligations(ctxt, obligations);

				// Then add the "forall" version of the ambiguity for the apply call, which cannot
				// return at this point (within an expression).
				if (!ctxt.makeOperationCall(exp.location, apply.opdef, apply.args, var, false, pogState, env))
				{
					return exp;		// failed for some reason, eg. exceptions?
				}
			}

			return subs;
		}
		catch (POOperationExtractionException e)
		{
			return exp;		// Caller decides if this is ambiguous
		}
	}

	/**
	 * This version of the extraction is used inside assignment statements to analyse the LHS
	 * for operation calls within map/seq designators.
	 */
	protected POStateDesignator extractOpCalls(POStateDesignator designator,
		ProofObligationList obligations, POGState pogState, POContextStack ctxt, Environment env)
	{
		try
		{
			PODesignatorOperationExtractor visitor = new PODesignatorOperationExtractor();
			POStateDesignator subs = designator.apply(visitor);
			LinkedHashMap<TCNameToken, POApplyExpression> table = visitor.getSubstitutions();

			for (Entry<TCNameToken, POApplyExpression> entry: table.entrySet())	// In order
			{
				TCNameToken var = entry.getKey();
				POApplyExpression apply = entry.getValue();

				// Get any obligations from the preconditions or arguments passed to the apply,
				// without the rest of the ApplyExpression processing here, because we've removed
				// the apply calls from the exp.
				apply.getFuncOpObligations(ctxt, obligations);

				// Then add the "forall" version of the ambiguity for the apply call, which cannot
				// return at this point (within an expression).
				if (!ctxt.makeOperationCall(location, apply.opdef, apply.args, var, false, pogState, env))
				{
					return designator;
				}
			}

			return subs;
		}
		catch (POOperationExtractionException e)
		{
			return designator;		// Caller decides if this is ambiguous
		}
	}

	
	/**
	 * This version of the extraction is used inside let def statements to analyse the definitions
	 * for operation calls.
	 */
	protected PODefinitionList extractOpCalls(PODefinitionList definitions,
		ProofObligationList obligations, POGState pogState, POContextStack ctxt, Environment env)
	{
		try
		{
			PODefinitionList subs = new PODefinitionList();
			PODefinitionOperationExtractor visitor = new PODefinitionOperationExtractor();

			for (PODefinition definition: definitions)
			{
				PODefinition sub = definition.apply(visitor);
				subs.add(sub);
			}

			LinkedHashMap<TCNameToken, POApplyExpression> table = visitor.getSubstitutions();

			for (Entry<TCNameToken, POApplyExpression> entry: table.entrySet())	// In order
			{
				TCNameToken var = entry.getKey();
				POApplyExpression apply = entry.getValue();

				// Get any obligations from the preconditions or arguments passed to the apply,
				// without the rest of the ApplyExpression processing here, because we've removed
				// the apply calls from the exp.
				apply.getFuncOpObligations(ctxt, obligations);

				// Then add the "forall" version of the ambiguity for the apply call, which cannot
				// return at this point (within an expression).
				if (!ctxt.makeOperationCall(location, apply.opdef, apply.args, var, false, pogState, env))
				{
					return definitions;
				}
			}

			return subs;
		}
		catch (POOperationExtractionException e)
		{
			if (e.ambiguous)
			{
				ctxt.push(new POAmbiguousContext(e.getMessage(), ctxt.getStateVariables(), e.node.location));
			}

			return definitions;		// Caller decides if this is ambiguous
		}
	}

	/**
	 * Implemented by all definitions to allow visitor processing.
	 */
	abstract public <R, S> R apply(POStatementVisitor<R, S> visitor, S arg);

	public <R, S> R apply(POStatementVisitor<R, S> visitor)
	{
		return apply(visitor, null);
	}
}
