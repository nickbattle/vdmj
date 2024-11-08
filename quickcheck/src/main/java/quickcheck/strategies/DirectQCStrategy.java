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

package quickcheck.strategies;

import static com.fujitsu.vdmj.plugins.PluginConsole.println;
import static quickcheck.commands.QCConsole.verbose;
import static quickcheck.commands.QCConsole.verboseln;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fujitsu.vdmj.in.expressions.INCaseAlternative;
import com.fujitsu.vdmj.in.expressions.INCasesExpression;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.expressions.INExpressionList;
import com.fujitsu.vdmj.in.patterns.INBindingOverride;
import com.fujitsu.vdmj.in.types.visitors.INGetAllValuesVisitor;
import com.fujitsu.vdmj.in.types.visitors.INTypeSizeVisitor;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.po.definitions.POExplicitFunctionDefinition;
import com.fujitsu.vdmj.po.expressions.POCaseAlternative;
import com.fujitsu.vdmj.po.patterns.POPattern;
import com.fujitsu.vdmj.pog.CasesExhaustiveObligation;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.pog.TotalFunctionObligation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.runtime.PatternMatchException;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.typechecker.TypeComparator;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;

import quickcheck.QuickCheck;
import quickcheck.visitors.TotalExpressionVisitor;

/**
 * A QC strategy to look for particular obligation types, to discharge them by
 * some form of direct analysis, rather than looking at the obligation exp.
 */
public class DirectQCStrategy extends QCStrategy
{
	private int errorCount = 0;

	public DirectQCStrategy(List<?> argv)
	{
		if (!argv.isEmpty() && argv.get(0) instanceof String)
		{
			for (int i=0; i < argv.size(); i++)
			{
				String arg = (String)argv.get(i);
				
				if (arg.startsWith("-direct:"))
				{
					println("Unknown direct option: " + argv);
					println(help());
					errorCount ++;
					argv.remove(i);
				}
			}
		}
		else
		{
			@SuppressWarnings({ "unchecked", "unused" })
			Map<String, Object> map = getParams((List<Map<String, Object>>) argv, "direct");
		}
	}

	@Override
	public String getName()
	{
		return "direct";
	}

	@Override
	public boolean hasErrors()
	{
		return errorCount > 0;
	}

	@Override
	public boolean useByDefault()
	{
		return true;
	}

	@Override
	public boolean init(QuickCheck qc)
	{
		return true;
	}

	@Override
	public StrategyResults getValues(ProofObligation po, List<INBindingOverride> binds, Context ctxt)
	{
		switch (po.kind)
		{
			case TOTAL_FUNCTION:
				verboseln("Trying direct proof of total obligation");
				return directTotalObligation((TotalFunctionObligation) po);
			
			case CASES_EXHAUSTIVE:
				verboseln("Trying direct proof of exhaustive cases obligation");
				return directCasesObligation((CasesExhaustiveObligation) po);
				
			default:
				verboseln("Obligation cannot be proved directly");
				return new StrategyResults();
		}
	}

	private StrategyResults directCasesObligation(CasesExhaustiveObligation po)
	{
		try
		{
			long before = System.currentTimeMillis();
			Interpreter in = Interpreter.getInstance();
			Context ctxt = in.getInitialContext();
			LexLocation loc = po.exp.location;
			INExpressionList list = in.findExpressions(loc.file, loc.startLine);
			INCasesExpression cases = null;
			
			if (list != null)
			{
				for (INExpression exp: list)	// All on the same line
				{
					if (exp instanceof INCasesExpression && exp.location.equals(loc))
					{
						cases = (INCasesExpression) exp;
						break;
					}
				}
			}
			
			if (cases != null)		// Should always be found, but...
			{
				ValueList values = po.exp.expType.apply(new INGetAllValuesVisitor(), ctxt);
				verbose("Checking %d values of type %s\n", values.size(), po.exp.expType);
				
				for (Value value: values)
				{
					boolean matched = false;
					
					for (INCaseAlternative alt: cases.cases)
					{
						try
						{
							alt.pattern.getNamedValues(value, ctxt);
							matched = true;
							break;
						}
						catch (PatternMatchException e)
						{
							// Did not match
						}
					}
					
					if (!matched)
					{
						verbose("Value %s does not match any case patterns\n", value);
						
						TCNameToken name = new TCNameToken(po.location, po.location.module, po.exp.exp.toString());
						Context cex = new Context(po.location, "Counterexample", Interpreter.getInstance().getInitialContext());
						cex.put(name, value);
						return new StrategyResults(getName(), cex, "(case unmatched)", System.currentTimeMillis() - before);
					}
				}
				
				return new StrategyResults(getName(), "(patterns match all type values)", null, System.currentTimeMillis() - before);
			}
			else
			{
				/**
				 * Plan B. The cases expression has been type checked, so all of the patterns can
				 * match the type. So we count whether there are the right number of simple patterns
				 * to match the type size - a simple pattern can only match one value.
				 */
				long typeSize = po.exp.expType.apply(new INTypeSizeVisitor(), ctxt);
				
				if (typeSize > po.exp.cases.size())
				{
					return new StrategyResults();	// Impossible with simple patterns
				}
				
				Set<POPattern> unique = new HashSet<POPattern>();
				
				for (POCaseAlternative p: po.exp.cases)
				{
					if (p.pattern.isSimple())
					{
						unique.add(p.pattern);
					}
				}
				
				if (unique.size() == typeSize)
				{
					return new StrategyResults(getName(), "(patterns match all type values)", null, System.currentTimeMillis() - before);
				}
			}
		}
		catch (Throwable e)
		{
			// Probably infinite subtype encountered, or stack/arithmetic overflow
			verboseln("Direct failed with " + e);
		}
		
		return new StrategyResults();	// Complex patterns, so we're not sure
	}

	private StrategyResults directTotalObligation(TotalFunctionObligation po)
	{
		TotalExpressionVisitor visitor = new TotalExpressionVisitor();
		POExplicitFunctionDefinition exdef = (POExplicitFunctionDefinition) po.definition;
		
		if (exdef.bodyObligationCount > 0)
		{
			verboseln("Function body raises some proof obligations, so not total");
			return new StrategyResults();
		}
		
		verboseln("Function body does not raise any obligations");

		if (exdef.isUndefined ||
			!TypeComparator.isSubType(exdef.actualResult, exdef.expectedResult))
		{
			verboseln("But function body type is not a subtype of the return type, so not total");
			return new StrategyResults();
		}

		verboseln("Function body type always matches the return type");

		long before = System.currentTimeMillis();
		exdef.body.apply(visitor, null);
		
		if (visitor.isTotal())
		{
			verboseln("Function body has no partial operators");
			return new StrategyResults(getName(), "(body is total)", null, System.currentTimeMillis() - before);
		}
		else
		{
			verboseln("But function body contains some partial operators");
			return new StrategyResults();
		}
	}

	@Override
	public String help()
	{
		return getName() + " (no options)";
	}

}
