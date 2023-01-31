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

package examples.vdmjplugin;

import static com.fujitsu.vdmj.plugins.PluginConsole.println;
import static com.fujitsu.vdmj.plugins.PluginConsole.fail;

import java.util.Iterator;
import java.util.List;

import com.fujitsu.vdmj.ast.lex.LexNameToken;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.plugins.AnalysisPlugin;
import com.fujitsu.vdmj.plugins.EventListener;
import com.fujitsu.vdmj.plugins.events.CheckSyntaxEvent;
import com.fujitsu.vdmj.plugins.events.Event;

/**
 * An example plugin for VDMJ.
 */
abstract public class ExamplePlugin extends AnalysisPlugin implements EventListener
{
	private boolean enabled;
	protected int maxLength;

	@Override
	public String getName()
	{
		return "Example";
	}

	public static ExamplePlugin factory(Dialect dialect) throws Exception
	{
		switch (dialect)
		{
			case VDM_SL:
				return new ExamplePluginSL();
				
			case VDM_PP:
				return new ExamplePluginPP();
				
			case VDM_RT:
				return new ExamplePluginRT();
				
			default:
				throw new Exception("Unknown dialect: " + dialect);
		}
	}

	@Override
	public void init()
	{
		enabled = false;
		maxLength = 0;
		eventhub.register(CheckSyntaxEvent.class, this);
	}

	@Override
	public void processArgs(List<String> argv)
	{
		Iterator<String> iter = argv.iterator();
		
		while (iter.hasNext())
		{
			switch (iter.next())
			{
				case "-check":
					iter.remove();
					enabled = true;
					
					if (iter.hasNext())
					{
						maxLength = Integer.parseInt(iter.next());
						iter.remove();
					}
					else
					{
						fail("-check <max> option missing");
					}
					break;
			}
		}
	}

	@Override
	public void usage()
	{
		println("-check <maxlen>: Check definition names against naming comvention");
	}

	@Override
	public <T> T handleEvent(Event event) throws Exception
	{
		if (event instanceof CheckSyntaxEvent)
		{
			if (enabled)
			{
				return checkDefinitions();
			}
		}

		return null;
	}

	abstract protected <T> T checkDefinitions();
	
	protected String InitialUpper(LexNameToken name)
	{
		return String.valueOf(name.name.charAt(0)).toUpperCase() + name.name.substring(1).toLowerCase();
	}
}
