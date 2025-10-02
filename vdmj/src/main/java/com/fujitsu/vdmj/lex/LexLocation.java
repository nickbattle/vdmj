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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.lex;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Vector;

import com.fujitsu.vdmj.ast.lex.LexNameList;
import com.fujitsu.vdmj.ast.lex.LexNameToken;
import com.fujitsu.vdmj.ast.lex.LexToken;

/**
 * A class to hold the location of a token.
 */
public class LexLocation implements Serializable, Comparable<LexLocation>
{
	private static final long serialVersionUID = 1L;

	/** An ordered collection of all LexLocation objects. */
	private static List<LexLocation> allLocations = new Vector<LexLocation>();
	
	/** A unique map of LexLocation objects, for rapid searching. */
	private static Map<LexLocation, LexLocation> uniqueLocations = new HashMap<LexLocation, LexLocation>();

	/** A map of f/op/class names to their lexical span, for coverage. */
	private static Map<LexNameToken, LexLocation> nameSpans = new HashMap<LexNameToken, LexLocation>();

	/** A dummy location meaning "nowhere" */
	public static final LexLocation ANY = new LexLocation();
	
	/** True if the location is executable. */
	private boolean executable = false;

	/** The filename of the token. */
	public final File file;
	/** The module/class name of the token. */
	public final String module;
	
	/**
	 * The line and character positions of a LexLocation are 1-relative. That is,
	 * the first character of the first line of a spec is line 1, char 1. The
	 * end markers are that of the last character of the range, not one beyond. So
	 * at token "abc" at the start of line 1 would be startPos=1, endPos=3.
	 * 
	 * Line or character positions of zero indicate that they are not set.
	 */
	
	/** The line number of the start of the token. */
	public final int startLine;
	/** The character position of the start of the token. */
	public final int startPos;
	/** The last line of the token. */
	public final int endLine;
	/** The position of the last character of the token. */
	public final int endPos;

	/** The number of times the location has been executed. */
	public long hits = 0;

	/**
	 * Create a location with the given fields.
	 */
	public LexLocation(File file, String module,
		int startLine, int startPos, int endLine, int endPos)
	{
		this.file = file;
		this.module = module;
		this.startLine = startLine;
		this.startPos = startPos;
		this.endLine = endLine;
		this.endPos = endPos;

		if (!file.getName().equals("console") &&
			!file.getName().equals("?"))	// Don't remember junk
		{
			allLocations.add(this);
			uniqueLocations.put(this, this);
		}
	}

	/**
	 * Create a default location - only used by ANY above.
	 */
	private LexLocation()
	{
		this(new File("?"), "?", 0, 0, 0, 0);
	}
	
	@Override
	public String toString()
	{
		if (file.getPath().equals("?"))
		{
			return "";		// Default LexLocation has no location string
		}
		else if (module.equals(""))
		{
			return "in '" + file + "' at line " + startLine + ":" + startPos;
		}
		else
		{
			return "in '" + module + "' (" + file + ") at line " + startLine + ":" + startPos;
		}
	}

	public String toShortString()
	{
		if (file.getPath().equals("?"))
		{
			return "";		// Default LexLocation has no location string
		}
		else
		{
			return "at " + startLine + ":" + startPos;
		}
	}

	@Override
	public boolean equals(Object other)
	{
		if (other instanceof LexLocation)
		{
			LexLocation lother = (LexLocation)other;

			return  startPos == lother.startPos &&
					startLine == lother.startLine &&
					module.equals(lother.module) &&
					file.equals(lother.file);
		}

		return false;
	}

	@Override
	public int hashCode()
	{
		return file.hashCode() + module.hashCode() + startLine + startPos;
	}

	/**
	 * Method to resolve existing locations during de-serialise - as used
	 * during deep copy. This is to avoid problems with coverage.
	 */
	private Object readResolve() throws ObjectStreamException
	{
		LexLocation existing = uniqueLocations.get(this);
		
		if (existing == null)
		{
			return this;
		}
		else
		{
			return existing;
		}
	}
	
	public boolean within(LexLocation span)
	{
		return
			(startLine > span.startLine ||
				(startLine == span.startLine && startPos >= span.startPos)) &&
			(startLine < span.endLine ||
				(startLine == span.endLine && startPos <= span.endPos)) &&
			file.equals(span.file);
	}
	
	/**
	 * This is almost identical to the within method, but this returns true when the
	 * cursor is sitting at the extreme end of the symbol. This is useful for some UIs
	 * where a symbol is considered "touched" when the cursor sits on the RHS. 
	 */
	public boolean touches(LexLocation span)
	{
		return
			(startLine > span.startLine ||
				(startLine == span.startLine && startPos >= span.startPos)) &&
			(startLine < span.endLine ||
				(startLine == span.endLine && startPos <= span.endPos + 1)) &&	// NOTE +1 !
			file.equals(span.file);
	}

	
	/**
	 * Check whether a location is later in the same file as this location.
	 */
	public boolean isLater(LexLocation other)
	{
		return file.equals(other.file) && other.startLine > startLine;
	}

	/**
	 * Check whether a different location is in the same class/module as this one.
	 */
	public boolean sameModule(LexLocation other)
	{
		return other.module.equals(module);
	}

	/**
	 * Mark as executable (so hit() will have effect)
	 */
	public void executable(boolean exe)
	{
		executable = exe;
	}

	/**
	 * Increment hit count, if executable.
	 */
	public void hit()
	{
		if (executable) hits++;
	}

	/**
	 * Manage global static location counters.
	 */
	public static void clearLocations()
	{
		for (LexLocation loc: allLocations)
		{
			loc.hits = 0;
		}
	}

	public static void resetLocations()
	{
		allLocations = new Vector<LexLocation>();
		uniqueLocations = new HashMap<LexLocation, LexLocation>();
		nameSpans =	new HashMap<LexNameToken, LexLocation>();
	}

	public static void clearAfter(File file, int linecount, int charpos)
	{
		// Called from the LexTokenReader's pop method, to remove any
		// locations "popped". We assume any pushes are on the end of
		// the vector.

		ListIterator<LexLocation> it =
			allLocations.listIterator(allLocations.size());

		while (it.hasPrevious())
		{
			LexLocation l = it.previous();

			if (!l.file.equals(file) ||
				l.startLine < linecount ||
				(l.startLine == linecount && l.startPos < charpos))
			{
				break;
			}
			else
			{
				it.remove();
				uniqueLocations.remove(l);
			}
		}
	}

	public static void addSpan(LexNameToken name, LexToken upto)
	{
		LexLocation span = new LexLocation(
			name.location.file,
			name.location.module,
			name.location.startLine,
			name.location.startPos,
			upto.location.endLine,
			upto.location.endPos);
		
		if (nameSpans.get(name) != null)
		{
			// Overloaded names use line numbers to distinguish them
			name = new LexNameToken(name.module, name.name + ":" + name.location.startLine, name.location);
		}
		
		nameSpans.put(name, span);
	}
	
	public static LexLocation getSpan(LexNameToken name)
	{
		return nameSpans.get(name);
	}
	
	public static LexNameList getSpanNames(File filename)
	{
		LexNameList list = new LexNameList();

		for (LexNameToken name: nameSpans.keySet())
		{
			LexLocation span = nameSpans.get(name);

			if (span.file.equals(filename))
			{
				list.add(name);
			}
		}

		return list;
	}

	public static float getSpanPercent(LexNameToken name)
	{
		int hits = 0;
		int misses = 0;
		LexLocation span = nameSpans.get(name);

		for (LexLocation l: allLocations)
		{
			if (l.executable && l.within(span))
			{
				if (l.hits > 0)
    			{
    				hits++;
    			}
    			else
    			{
    				misses++;
    			}
			}
		}

		int sum = hits + misses;
		return sum == 0 ? 0 : (float)(1000 * hits/sum)/10;		// NN.N%
	}

	public static long getSpanCalls(LexNameToken name)
	{
		// The assumption is that the first executable location in
		// the span for the name is hit as many time as the span is called.

		LexLocation span = nameSpans.get(name);

		for (LexLocation l: allLocations)
		{
			if (l.executable && l.within(span))
			{
				return l.hits;
			}
		}

		return 0;
	}

	public static List<Integer> getHitList(File file)
	{
		List<Integer> hits = new Vector<Integer>();

		for (LexLocation l: allLocations)
		{
			if (l.hits > 0 && l.file.equals(file))
			{
				hits.add(l.startLine);
			}
		}

		return hits;
	}

	public static List<Integer> getMissList(File file)
	{
		List<Integer> misses = new Vector<Integer>();

		for (LexLocation l: allLocations)
		{
			if (l.hits == 0 && l.file.equals(file))
			{
				misses.add(l.startLine);
			}
		}

		return misses;
	}

	public static List<Integer> getSourceList(File file)
	{
		List<Integer> lines = new Vector<Integer>();
		int last = 0;

		for (LexLocation l: allLocations)
		{
			if (l.executable && l.startLine != last && l.file.equals(file))
			{
				lines.add(l.startLine);
				last = l.startLine;
			}
		}

		return lines;
	}

	public static Map<Integer, List<LexLocation>> getHitLocations(File file)
	{
		Map<Integer, List<LexLocation>> map =
				new HashMap<Integer, List<LexLocation>>();

		for (LexLocation l: allLocations)
		{
			if (l.executable && l.hits > 0 && l.file.equals(file))
			{
				List<LexLocation> list = map.get(l.startLine);

				if (list == null)
				{
					list = new Vector<LexLocation>();
					map.put(l.startLine, list);
				}

				list.add(l);
			}
		}

		return map;
	}

	public static float getHitPercent(File file)
	{
		int hits = 0;
		int misses = 0;

		for (LexLocation l: allLocations)
		{
			if (l.file.equals(file) && l.executable)
			{
				if (l.hits > 0)
    			{
    				hits++;
    			}
    			else
    			{
    				misses++;
    			}
			}
		}

		int sum = hits + misses;
		return sum == 0 ? 0 : (float)(1000 * hits/sum)/10;		// NN.N%
	}

	public static Map<Integer, List<LexLocation>> getMissLocations(File file)
	{
		Map<Integer, List<LexLocation>> map =
				new HashMap<Integer, List<LexLocation>>();

		for (LexLocation l: allLocations)
		{
			if (l.executable && l.hits == 0 && l.file.equals(file))
			{
				List<LexLocation> list = map.get(l.startLine);

				if (list == null)
				{
					list = new Vector<LexLocation>();
					map.put(l.startLine, list);
				}

				list.add(l);
			}
		}

		return map;
	}

	public static List<LexLocation> getSourceLocations(File file)
	{
		List<LexLocation> locations = new Vector<LexLocation>();

		for (LexLocation l: allLocations)
		{
			if (l.executable && l.file.equals(file))
			{
				locations.add(l);
			}
		}

		return locations;
	}

	public static void mergeHits(File source, File coverage) throws IOException
	{
		List<LexLocation> locations = getSourceLocations(source);
		BufferedReader br = new BufferedReader(new FileReader(coverage));
		String line = br.readLine();

		while (line != null)
		{
			if (line.charAt(0) == '+')
			{
				// Hit lines are "+line from-to=hits"

				int s1 = line.indexOf(' ');
				int s2 = line.indexOf('-');
				int s3 = line.indexOf('=');

				int lnum = Integer.parseInt(line.substring(1, s1));
				int from = Integer.parseInt(line.substring(s1+1, s2));
				int to   = Integer.parseInt(line.substring(s2+1, s3));
				int hits = Integer.parseInt(line.substring(s3+1));

				for (LexLocation l: locations)	// Only executable locations
				{
					if (l.startLine == lnum &&
						l.startPos == from &&
						l.endPos == to)
					{
						l.hits += hits;
						break;
					}
				}
			}

			line = br.readLine();
		}

		br.close();
	}

	@Override
	public int compareTo(LexLocation lother)
	{
		if (file.equals(lother.file))
		{
			if (startLine == lother.startLine)
			{
				return startPos - lother.startPos;
			}
			else
			{
				return startLine - lother.startLine;
			}
		}
		else
		{
			return file.compareTo(lother.file);
		}
	}
}
