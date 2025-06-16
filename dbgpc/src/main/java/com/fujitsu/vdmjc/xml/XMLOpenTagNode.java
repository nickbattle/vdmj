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

import java.util.List;
import java.util.Properties;

public class XMLOpenTagNode extends XMLTagNode
{
	public final List<XMLNode> children;
	public final String text;	// for <tag>text</tag> cases...

	public XMLOpenTagNode(String tag, Properties attrs, List<XMLNode> children)
	{
		super(tag, attrs);
		this.children = children;

		if (children.size() == 1 && children.get(0) instanceof XMLTextNode)
		{
			text = ((XMLTextNode)children.get(0)).text;
		}
		else
		{
			text = null;
		}
	}

	public XMLNode getChild(int n)
	{
		return children.get(n);
	}

	public XMLNode getChild(String sought)
	{
		for (XMLNode node: children)
		{
			if (node instanceof XMLTagNode)
			{
				XMLTagNode tn = (XMLTagNode)node;

				if (tn.tag.equals(sought))
				{
					return tn;
				}
			}
		}

		return null;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("<");
		sb.append(tag);

		for (Object name: attrs.keySet())
		{
			sb.append(" ");
			sb.append(name);
			sb.append("=\"");
			sb.append(attrs.get(name));
			sb.append("\"");
		}

		sb.append(">");

		for (XMLNode node: children)
		{
			sb.append(node);
		}

		sb.append("</" + tag + ">");
		return sb.toString();
	}
}
