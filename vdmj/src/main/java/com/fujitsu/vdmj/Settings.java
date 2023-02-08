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

package com.fujitsu.vdmj;

import java.nio.charset.Charset;

import com.fujitsu.vdmj.lex.Dialect;

/**
 * A class to hold global settings accessible throughout.
 */
public class Settings
{
	public static Release release = Release.DEFAULT;
	public static Dialect dialect = null;
	public static Charset filecharset = Charset.defaultCharset();

	public static boolean prechecks = true;
	public static boolean postchecks = true;
	public static boolean invchecks = true;
	public static boolean dynamictypechecks = true;
	public static boolean measureChecks = true;
	public static boolean exceptions = false;
	public static boolean annotations = false;
	public static boolean verbose = false;
	public static boolean strict = false;
}
