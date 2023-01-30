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

package com.fujitsu.vdmj.plugins.analyses;

import static com.fujitsu.vdmj.plugins.PluginConsole.println;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.mapper.Mappable;
import com.fujitsu.vdmj.messages.VDMError;
import com.fujitsu.vdmj.messages.VDMWarning;
import com.fujitsu.vdmj.plugins.AnalysisPlugin;
import com.fujitsu.vdmj.plugins.EventListener;
import com.fujitsu.vdmj.plugins.events.CheckPrepareEvent;
import com.fujitsu.vdmj.plugins.events.CheckSyntaxEvent;
import com.fujitsu.vdmj.plugins.events.Event;

/**
 * TC analysis plugin
 */
abstract public class TCPlugin extends AnalysisPlugin implements EventListener
{
	protected List<VDMError> errors;
	protected List<VDMWarning> warnings;
	protected boolean nowarn;
	
	@Override
	public String getName()
	{
		return "TC";
	}

	@Override
	public void init()
	{
		errors = new Vector<VDMError>();
		warnings = new Vector<VDMWarning>();
		nowarn = false;
		
		eventhub.register(CheckPrepareEvent.class, this);
		eventhub.register(CheckSyntaxEvent.class, this);
	}

	public static TCPlugin factory(Dialect dialect) throws Exception
	{
		switch (dialect)
		{
			case VDM_SL:
				return new TCPluginSL();
				
			case VDM_PP:
				return new TCPluginPP();
				
			case VDM_RT:
				return new TCPluginRT();
				
			default:
				throw new Exception("Unknown dialect: " + dialect);
		}
	}
	
	@Override
	public void getUsage()
	{
		println("-w: suppress warning messages");
	}
	
	@Override
	public void processArgs(List<String> argv)
	{
		Iterator<String> iter = argv.iterator();
		
		while (iter.hasNext())
		{
			String arg = iter.next();
			
			if (arg.equals("-w"))
			{
				nowarn = true;
				iter.remove();
			}
		}
	}

	@Override
	public <T> T handleEvent(Event event) throws Exception
	{
		if (event instanceof CheckPrepareEvent)
		{
			errors.clear();
			warnings.clear();
			return typeCheckPrepare();
		}
		else if (event instanceof CheckSyntaxEvent)
		{
			return typeCheck();
		}
		else
		{
			throw new Exception("Unhandled event: " + event);
		}
	}

	abstract protected <T> T typeCheckPrepare();

	abstract protected <T> T typeCheck();

	abstract public <T extends Mappable> T getTC();
}
