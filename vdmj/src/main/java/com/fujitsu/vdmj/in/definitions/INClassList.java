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

package com.fujitsu.vdmj.in.definitions;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import com.fujitsu.vdmj.in.INMappedList;
import com.fujitsu.vdmj.in.expressions.INExpressionList;
import com.fujitsu.vdmj.in.statements.INStatementList;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.RootContext;
import com.fujitsu.vdmj.runtime.StateContext;
import com.fujitsu.vdmj.scheduler.InitThread;
import com.fujitsu.vdmj.scheduler.ResourceScheduler;
import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCClassList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
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
				LexLocation.ANY, "global environment");
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
		try
		{
			InitThread initThread = new InitThread(this, globalContext);
			initThread.start();
			initThread.join();
			
			Exception e = initThread.getException();
			
			if (e instanceof ContextException)
			{
				throw (ContextException)e;
			}
			else if (e != null)
			{
				throw new RuntimeException(e);
			}
		}
		catch (InterruptedException e)
		{
			// ignore
		}
	}
	
	public INStatementList findStatements(File file, int lineno)
	{
		for (INClassDefinition c: this)
		{
			if (c.name.getLocation().file.equals(file))
			{
    			INStatementList stmts = c.findStatements(lineno);

    			if (stmts != null && !stmts.isEmpty())
    			{
    				return stmts;
    			}
			}
		}

		return null;
	}

	public INExpressionList findExpressions(File file, int lineno)
	{
		for (INClassDefinition c: this)
		{
			if (c.name.getLocation().file.equals(file))
			{
    			INExpressionList exps = c.findExpressions(lineno);

    			if (exps != null && !exps.isEmpty())
    			{
    				return exps;
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
