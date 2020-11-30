/*******************************************************************************
 *
 *	Copyright (c) 2017 Fujitsu Services Ltd.
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

package com.fujitsu.vdmj.debug;

/**
 * Class to represent one debugger command or response.
 */
public class DebugCommand
{
	/** static values for commands with no payload */
	public static final DebugCommand STEP		= new DebugCommand(DebugType.STEP);
	public static final DebugCommand NEXT		= new DebugCommand(DebugType.NEXT);
	public static final DebugCommand OUT		= new DebugCommand(DebugType.OUT);
	public static final DebugCommand CONTINUE	= new DebugCommand(DebugType.CONTINUE);
	public static final DebugCommand STACK		= new DebugCommand(DebugType.STACK);
	public static final DebugCommand UP			= new DebugCommand(DebugType.UP);
	public static final DebugCommand DOWN		= new DebugCommand(DebugType.DOWN);
	public static final DebugCommand SOURCE		= new DebugCommand(DebugType.SOURCE);
	public static final DebugCommand STOP		= new DebugCommand(DebugType.STOP);
	public static final DebugCommand THREADS	= new DebugCommand(DebugType.THREADS);
	public static final DebugCommand QUIT		= new DebugCommand(DebugType.QUIT);
	public static final DebugCommand ACK		= new DebugCommand(DebugType.ACK);
	public static final DebugCommand RESUME		= new DebugCommand(DebugType.RESUME);
	public static final DebugCommand TERMINATE	= new DebugCommand(DebugType.TERMINATE);
	public static final DebugCommand HELP		= new DebugCommand(DebugType.HELP);
	
	private final DebugType type;
	private final Object payload;

	public DebugCommand(DebugType type)
	{
		this.type = type;
		this.payload = null;
	}
	
	public DebugCommand(DebugType type, Object payload)
	{
		this.type = type;
		this.payload = payload;
	}
	
	public DebugType getType()
	{
		return type;
	}
	
	public Object getPayload()
	{
		return payload;
	}
	
	@Override
	public String toString()
	{
		return payload == null ? type.name().toLowerCase() : payload.toString();
	}
}
