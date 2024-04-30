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

package com.fujitsu.vdmj.scheduler;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.debug.DebugLink;
import com.fujitsu.vdmj.in.annotations.INAnnotation;
import com.fujitsu.vdmj.in.definitions.INClassDefinition;
import com.fujitsu.vdmj.in.definitions.INClassList;
import com.fujitsu.vdmj.in.modules.INModule;
import com.fujitsu.vdmj.in.modules.INModuleList;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.messages.VDMThreadDeath;
import com.fujitsu.vdmj.runtime.Breakpoint;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.StateContext;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.values.CPUValue;
import com.fujitsu.vdmj.values.TransactionValue;

/**
 * A class representing the VDM initialization thread.
 */
public class InitThread extends SchedulableThread
{
	private static final long serialVersionUID = 1L;

	private final INModuleList modules;
	private final INClassList classes;
	private final Context globalContext;
	private Exception exception = null;


	public InitThread(INModuleList modules, Context ctxt)
	{
		super(CPUResource.vCPU, null, 0, false, 0);

		this.modules = modules;
		this.classes = null;
		this.globalContext = ctxt;
		this.exception = null;

		setName("InitThread");
	}

	public InitThread(INClassList classes, Context ctxt)
	{
		super(CPUResource.vCPU, null, 0, false, 0);

		this.classes = classes;
		this.modules = null;
		this.globalContext = ctxt;
		this.exception = null;

		setName("InitThread");
	}

	@Override
	public int hashCode()
	{
		return (int)getId();
	}

	@Override
	public synchronized void reschedule(Context ctxt, LexLocation location)
	{
		notify();	// Wakes up the sleep in start method
	}

	@Override
	public void body()
	{
		DebugLink link = DebugLink.getInstance();
		Breakpoint.setExecInterrupt(Breakpoint.NONE);

		try
		{
			if (modules != null)
			{
				initializeModules();
			}
			else if (classes != null)
			{
				initializeClasses();
			}
		}
		catch (ContextException e)
		{
			setException(e);
			suspendOthers();
			link.stopped(e.ctxt, e.location, e);
		}
		catch (Exception e)
		{
			while (e instanceof InvocationTargetException)
			{
				e = (Exception)e.getCause();
			}
			
			setException(e);
			suspendOthers();
		}
		catch (VDMThreadDeath th)
		{
			// Fine
		}
		catch (Throwable th)	// Java errors not caught above
		{
			setException(new Exception("Internal error: " + th.getMessage()));
			suspendOthers();
		}
		finally
		{
			TransactionValue.commitAll();
		}
	}

	public void initializeModules()
	{
		StateContext initialContext = (StateContext)globalContext;
		initialContext.setThreadState(null);
		Set<ContextException> problems = new HashSet<ContextException>();
		Set<TCIdentifierToken> passed = new HashSet<TCIdentifierToken>();
		int retries = 5;
		int lastProblemCount;
		boolean exceptions = Settings.exceptions;
		Settings.exceptions = false;

		do
		{
			lastProblemCount = problems.isEmpty() ? Integer.MAX_VALUE : problems.size();
			problems.clear();

        	for (INModule m: modules)
    		{
        		if (passed.contains(m.name))
        		{
        			continue;
        		}

        		long before = System.currentTimeMillis();
        		Set<ContextException> e = m.initialize(initialContext);
        		long after = System.currentTimeMillis();
        		
        		if (Settings.verbose && (after-before) > 200)
        		{
        			Console.out.printf("Pass %d: %s = %.3f secs\n", (6-retries), m.name, (double)(after-before)/1000);
        		}

        		if (e != null && !e.isEmpty())
        		{
        			problems.addAll(e);

        			if (e.size() == 1 &&
        				(e.iterator().next().isStackOverflow() || e.iterator().next().isUserCancel()))
        			{
        				retries = 0;
        				lastProblemCount = 0;
        				break;
        			}
        		}
        		else
        		{
        			passed.add(m.name);
        		}
     		}
        	
        	if (Settings.verbose && !problems.isEmpty())
        	{
        		Console.out.printf("Pass %d:\n", (6-retries));

    			for (ContextException e: problems)
    			{
    				Console.out.println(e.toString());
    			}        		
        	}

        	if (problems.size() == lastProblemCount)
			{
				retries--;
			}
		}
		while ((retries > 0 || problems.size() < lastProblemCount) && !problems.isEmpty());

		if (!problems.isEmpty())
		{
			ContextException toThrow = problems.iterator().next();

			for (ContextException e: problems)
			{
				Console.err.println(e.toString());

				if (e.number != 4034)	// Not in scope err
				{
					toThrow = e;
				}
			}

			throw toThrow;
		}

		INAnnotation.init(globalContext);

		Settings.exceptions = exceptions;
	}

	public void initializeClasses()
	{
		globalContext.setThreadState(CPUValue.vCPU);

		// Initialize all the functions/operations first because the values
		// "statics" can call them.

		for (INClassDefinition cdef: classes)
		{
			cdef.forceStaticInit(globalContext);
		}

		// Values can forward reference each other, which means that we don't
		// know what order to initialize the classes in. So we have a crude
		// retry mechanism, looking for "forward reference" like exceptions.

		ContextException failed = null;
		int retries = 3;	// Potentially not enough.
		int lastProblemCount;
		Set<ContextException> trouble = new HashSet<ContextException>();
		Set<TCNameToken> passed = new HashSet<TCNameToken>();
		boolean exceptions = Settings.exceptions;
		Settings.exceptions = false;

		do
		{
			lastProblemCount = trouble.isEmpty() ? Integer.MAX_VALUE : trouble.size();
			failed = null;
			trouble.clear();

    		for (INClassDefinition cdef: classes)
    		{
				if (passed.contains(cdef.name))
				{
					continue;
				}

    			long before = System.currentTimeMillis();
    			long after;

    			try
    			{
            		cdef.forceStaticValuesInit(globalContext);
    				passed.add(cdef.name);
    			}
    			catch (ContextException e)
    			{
    				if (e.isStackOverflow() || e.isUserCancel())
    				{
    					trouble.clear();
    					trouble.add(e);
    					retries = 0;
    					break;
    				}
    				
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
    				Console.out.println(e.toString());
    			}        		
        	}

			if (trouble.size() == lastProblemCount)
			{
				retries--;
			}
		}
		while ((retries > 0 || trouble.size() < lastProblemCount) && !trouble.isEmpty() && failed != null);

		if (!trouble.isEmpty())
		{
			ContextException toThrow = trouble.iterator().next();

			for (ContextException e: trouble)
			{
				Console.err.println(e.toString());

				if (e.number != 4034)	// Not in scope err
				{
					toThrow = e;
				}
			}

			throw toThrow;
		}

		INAnnotation.init(globalContext);

		Settings.exceptions = exceptions;
	}

	public void setException(Exception e)
	{
		exception = e;
	}
	
	public Exception getException()
	{
		return exception;
	}
}
