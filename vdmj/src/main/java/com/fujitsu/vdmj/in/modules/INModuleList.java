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

package com.fujitsu.vdmj.in.modules;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import com.fujitsu.vdmj.in.INMappedList;
import com.fujitsu.vdmj.in.definitions.INDefinition;
import com.fujitsu.vdmj.in.definitions.INNamedTraceDefinition;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.statements.INStatement;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.RootContext;
import com.fujitsu.vdmj.runtime.StateContext;
import com.fujitsu.vdmj.scheduler.InitThread;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.modules.TCModule;
import com.fujitsu.vdmj.tc.modules.TCModuleList;
import com.fujitsu.vdmj.util.Utils;

public class INModuleList extends INMappedList<TCModule, INModule>
{
	private static final long serialVersionUID = 1L;

	public INModuleList()
	{
		super();
	}

	public INModuleList(TCModuleList from) throws Exception
	{
		super(from);
	}

	@Override
	public String toString()
	{
		return Utils.listToString(this);
	}

	public Set<File> getSourceFiles()
	{
		Set<File> files = new HashSet<File>();

		for (INModule def: this)
		{
			files.addAll(def.files);
		}

		return files;
	}

	public INModule findModule(TCIdentifierToken name)
	{
		for (INModule m: this)
		{
			if (m.name.equals(name))
			{
				return m;
			}
		}

   		return null;
	}

	public INStatement findStatement(File file, int lineno)
	{
		for (INModule m: this)
		{
			INStatement stmt = m.findStatement(file, lineno);

			if (stmt != null)
			{
				return stmt;
			}
		}

		return null;
	}

	public INExpression findExpression(File file, int lineno)
	{
		for (INModule m: this)
		{
			INExpression exp = m.findExpression(file, lineno);

			if (exp != null)
			{
				return exp;
			}
		}

		return null;
	}

	public StateContext creatInitialContext()
	{
		StateContext initialContext = null;

		if (isEmpty())
		{
			initialContext = new StateContext(
				new LexLocation(), "global environment");
		}
		else
		{
			initialContext =
				new StateContext(this.get(0).name.getLocation(), "global environment");
		}
		
		return initialContext;
	}

	public void initialize(RootContext ctxt)
	{
		try
		{
			InitThread initThread = new InitThread(this, ctxt);
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

	public INNamedTraceDefinition findTraceDefinition(TCNameToken name)
	{
		for (INModule m: this)
		{
			for (INDefinition d: m.defs)
			{
				if (name.equals(d.name))
				{
					if (d instanceof INNamedTraceDefinition)
					{
						return (INNamedTraceDefinition)d;
					}
					else
					{
						return null;
					}
				}
			}
		}

		return null;
	}
}
