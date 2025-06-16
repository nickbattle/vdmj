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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmjc.dbgp;

public enum DBGPRedirect
{
	DISABLE("0"), COPY("1"), REDIRECT("2");

	public String value;

	DBGPRedirect(String v)
	{
		value = v;
	}

	public static DBGPRedirect lookup(String string)
	{
		for (DBGPRedirect cmd: values())
		{
			if (cmd.value.equalsIgnoreCase(string))
			{
				return cmd;
			}
		}

		return null;
	}
}
