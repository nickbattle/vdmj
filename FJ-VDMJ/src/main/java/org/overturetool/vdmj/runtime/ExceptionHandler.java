/*******************************************************************************
 *
 *	Copyright (C) 2014 Fujitsu Services Ltd.
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

package org.overturetool.vdmj.runtime;

import org.overturetool.vdmj.Settings;
import org.overturetool.vdmj.messages.Console;
import org.overturetool.vdmj.values.QuoteValue;

public class ExceptionHandler
{
	public static void handle(ContextException e)
	{
		if (Settings.exceptions)
		{
			Console.err.println(e.getMessage());
			throw new ExitException(new QuoteValue("RuntimeError"), e.location, e.ctxt);
		}
		else
		{
			throw e;
		}
	}
}
