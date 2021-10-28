/*******************************************************************************
 *
 *	Copyright (c) 2019 Nick Battle.
 *
 *	Author: Nick Battle
 *
 *	This file is part of Overture
 *
 ******************************************************************************/

package annotations.po;

import com.fujitsu.vdmj.po.annotations.POAnnotation;
import com.fujitsu.vdmj.po.definitions.POClassDefinition;
import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.expressions.POExpressionList;
import com.fujitsu.vdmj.po.modules.POModule;
import com.fujitsu.vdmj.po.statements.POStatement;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;

public class POTestAnnotation extends POAnnotation
{
	private static final long serialVersionUID = 1L;

	public POTestAnnotation(TCIdentifierToken name, POExpressionList args)
	{
		super(name, args);
	}
	
	public static void doInit()
	{
		System.out.printf("doInit POTestAnnotation\n");
	}
	
	@Override
	public ProofObligationList poBefore(PODefinition def, POContextStack ctxt)
	{
		System.out.printf("poBefore %s %s\n", def.getClass().getSimpleName(), this);
		return new ProofObligationList();
	}

	@Override
	public ProofObligationList poBefore(POStatement stmt, POContextStack ctxt)
	{
		System.out.printf("poBefore %s %s\n", stmt.getClass().getSimpleName(), this);
		return new ProofObligationList();
	}

	@Override
	public ProofObligationList poBefore(POExpression exp, POContextStack ctxt)
	{
		System.out.printf("poBefore %s %s\n", exp.getClass().getSimpleName(), this);
		return new ProofObligationList();
	}

	@Override
	public ProofObligationList poBefore(POModule module)
	{
		System.out.printf("poBefore %s %s\n", module.getClass().getSimpleName(), this);
		return new ProofObligationList();
	}

	@Override
	public ProofObligationList poBefore(POClassDefinition clazz)
	{
		System.out.printf("poBefore %s %s\n", clazz.getClass().getSimpleName(), this);
		return new ProofObligationList();
	}

	@Override
	public void poAfter(PODefinition def, ProofObligationList obligations, POContextStack ctxt)
	{
		System.out.printf("poAfter %s %s\n", def.getClass().getSimpleName(), this);
	}

	@Override
	public void poAfter(POStatement stmt, ProofObligationList obligations, POContextStack ctxt)
	{
		System.out.printf("poAfter %s %s\n", stmt.getClass().getSimpleName(), this);
	}

	@Override
	public void poAfter(POExpression exp, ProofObligationList obligations, POContextStack ctxt)
	{
		System.out.printf("poAfter %s %s\n", exp.getClass().getSimpleName(), this);
	}

	@Override
	public void poAfter(POModule module, ProofObligationList obligations)
	{
		System.out.printf("poAfter %s %s\n", module.getClass().getSimpleName(), this);
	}

	@Override
	public void poAfter(POClassDefinition clazz, ProofObligationList obligations)
	{
		System.out.printf("poAfter %s %s\n", clazz.getClass().getSimpleName(), this);
	}
}
