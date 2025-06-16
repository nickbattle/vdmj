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

package com.fujitsu.vdmjc.xml;

public class XMLDataNode extends XMLNode
{
	public final String cdata;

	public XMLDataNode(String cdata)
	{
		this.cdata = dequote(cdata);
	}

	private static String dequote(String in)
	{
		return in
    		.replaceAll("&amp;", "&")
    		.replaceAll("&lt;", "<")
    		.replaceAll("&gt;", ">")
    		.replaceAll("&quot;", "\\\"");
	}

	@Override
	public String toString()
	{
		return "<![CDATA[" + cdata + "]]>";
	}
}
