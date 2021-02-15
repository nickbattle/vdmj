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

package com.fujitsu.vdmj.mapper;

import java.util.List;

public class Mapping
{
	public enum Type { MAP, UNMAPPED, PACKAGE, INIT, EOF, ERROR }

	public static final Mapping EOF = new Mapping(0, Type.EOF, null, null, null, null, null);
	public static final Mapping ERROR = new Mapping(0, Type.ERROR, null, null, null, null, null);
	
	public final int lineNo;
	public final Type type;
	public final String source;
	public final List<String> varnames;
	public final String destination;
	public final List<String> paramnames;
	public final List<String> setnames;
	
	public Mapping(int lineNo, Type type, String srcClass,
			List<String> varnames, String destClass, List<String> paramnames, List<String> setnames)
	{
		this.lineNo = lineNo;
		this.type = type;
		this.source = srcClass;
		this.varnames = varnames;
		this.destination = destClass;
		this.paramnames = paramnames;
		this.setnames = setnames;
	}
}
