/*******************************************************************************
 *
 *	Copyright (C) 2008 Fujitsu Services Ltd.
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

package com.fujitsu.vdmjc.client;

import java.io.File;
import java.io.FilenameFilter;

/**
 * An enumeration to indicate the VDM dialect being parsed.
 */

public enum Dialect
{
	VDM_SL("-vdmsl", ".+\\.vdm|.+\\.vdmsl"),
	VDM_PP("-vdmpp", ".+\\.vpp|.+\\.vdmpp"),
	VDM_RT("-vdmrt", ".+\\.vpp|.+\\.vdmrt");

	private final String argstring;
	private final Filter filter;

	Dialect(String arg, String pattern)
	{
		argstring = arg;
		filter = new Filter(pattern);
	}

	private static class Filter implements FilenameFilter
	{
		private String pattern;

		public Filter(String pattern)
		{
			this.pattern = pattern;
		}

		@Override
		public boolean accept(File dir, String filename)
		{
			return filename.matches(pattern);
		}
	}

	public FilenameFilter getFilter()
	{
		return filter;
	}

	public String getArgstring()
	{
		return argstring;
	}
}
