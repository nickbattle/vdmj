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
 *
 ******************************************************************************/

package com.fujitsu.vdmj.in.definitions;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.in.INMappedList;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.statements.INStatement;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.RootContext;
import com.fujitsu.vdmj.runtime.StateContext;
import com.fujitsu.vdmj.scheduler.ResourceScheduler;
import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCClassList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.values.CPUValue;
import com.fujitsu.vdmj.values.TransactionValue;

/**
 * A class for holding a list of ClassDefinitions.
 */
public class INClassList extends INMappedList<TCClassDefinition, INClassDefinition>
{
	private static final long serialVersionUID = 1L;

	public INClassList(TCClassList from) throws Exception
	{
		super(from);
	}

	public INClassList()
	{
		super();
	}

	public INClassList(INClassDefinition definition)
	{
		add(definition);
	}

	public Set<File> getSourceFiles()
	{
		Set<File> files = new HashSet<File>();

		for (INClassDefinition def: this)
		{
			if (!(def instanceof INCPUClassDefinition ||
				  def instanceof INBUSClassDefinition))
			{
				files.add(def.location.file);
			}
		}

		return files;
	}

	public void systemInit(ResourceScheduler scheduler, RootContext initialContext)
	{
		INSystemDefinition systemClass = null;

		for (INClassDefinition cdef: this)
		{
			if (cdef instanceof INSystemDefinition)
			{
				systemClass = (INSystemDefinition)cdef;
				systemClass.systemInit(scheduler, initialContext);
				TransactionValue.commitAll();
			}
		}
	}

	public RootContext creatInitialContext()
	{
		StateContext globalContext = null;

		if (isEmpty())
		{
			globalContext = new StateContext(
				new LexLocation(), "global environment");
		}
		else
		{
			globalContext =	new StateContext(
				this.get(0).location, "public static environment");
		}
		
		return globalContext;
	}

	public void initialize(StateContext globalContext)
	{
		globalContext.setThreadState(CPUValue.vCPU);

		// Initialize all the functions/operations first because the values
		// "statics" can call them.

		for (INClassDefinition cdef: this)
		{
			cdef.staticInit(globalContext);
		}

		// Values can forward reference each other, which means that we don't
		// know what order to initialize the classes in. So we have a crude
		// retry mechanism, looking for "forward reference" like exceptions.

		ContextException failed = null;
		int retries = 3;	// Potentially not enough.
		Set<ContextException> trouble = new HashSet<ContextException>();
		Set<TCNameToken> passed = new HashSet<TCNameToken>();
		boolean exceptions = Settings.exceptions;
		Settings.exceptions = false;

		do
		{
			failed = null;
			trouble.clear();

    		for (INClassDefinition cdef: this)
    		{
				if (passed.contains(cdef.name))
				{
					continue;
				}

    			long before = System.currentTimeMillis();
    			long after;

    			try
    			{
            		cdef.staticValuesInit(globalContext);
    				passed.add(cdef.name);
    			}
    			catch (ContextException e)
    			{
    				trouble.add(e);
    				
    				// These two exceptions mean that a member could not be
    				// found, which may be a forward reference, so we retry...

    				if (e.number == 4034 || e.number == 6)
    				{
    					failed = e;
    				}
    				else
    				{
    					throw e;
    				}
    			}
    			finally
    			{
    				after = System.currentTimeMillis();
    				
            		if (Settings.verbose && (after-before) > 200)
            		{
            			Console.out.printf("Pass %d: %s = %.3f secs\n", (4-retries), cdef.name.getName(), (double)(after-before)/1000);
            		}
    			}
    		}
    		
        	if (Settings.verbose && !trouble.isEmpty())
        	{
        		Console.out.printf("Pass %d:\n", (4-retries));

    			for (ContextException e: trouble)
    			{
    				Console.out.println(e);
    			}        		
        	}
		}
		while (--retries > 0 && failed != null);

		if (!trouble.isEmpty())
		{
			ContextException toThrow = trouble.iterator().next();

			for (ContextException e: trouble)
			{
				Console.err.println(e);

				if (e.number != 4034)	// Not in scope err
				{
					toThrow = e;
				}
			}

			throw toThrow;
		}

		Settings.exceptions = exceptions;
	}

	public INStatement findStatement(File file, int lineno)
	{
		for (INClassDefinition c: this)
		{
			if (c.name.getLocation().file.equals(file))
			{
    			INStatement stmt = c.findStatement(lineno);

    			if (stmt != null)
    			{
    				return stmt;
    			}
			}
		}

		return null;
	}

	public INExpression findExpression(File file, int lineno)
	{
		for (INClassDefinition c: this)
		{
			if (c.name.getLocation().file.equals(file))
			{
    			INExpression exp = c.findExpression(lineno);

    			if (exp != null)
    			{
    				return exp;
    			}
			}
		}

		return null;
	}

	public INClassDefinition findClass(TCNameToken name)
	{
		for (INClassDefinition c: this)
		{
			if (c.name.equals(name))
			{
   				return c;
			}
		}

		return null;
	}

	public INDefinition findName(TCNameToken name)
	{
		for (INClassDefinition d: this)
		{
			INDefinition def = d.findName(name);

			if (def != null)
			{
				return def;
			}
		}

		return null;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();

		for (INClassDefinition c: this)
		{
			sb.append(c.toString());
			sb.append("\n");
		}

		return sb.toString();
	}
}
