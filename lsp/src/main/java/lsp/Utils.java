/*******************************************************************************
 *
 *	Copyright (c) 2020 Nick Battle.
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

package lsp;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import com.fujitsu.vdmj.ast.lex.LexNameToken;
import com.fujitsu.vdmj.ast.lex.LexToken;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexException;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.tc.lex.TCNameToken;

import json.JSONArray;
import json.JSONObject;
import rpc.RPCErrors;
import workspace.Diag;

public class Utils
{
	private static int zero(int value)
	{
		return value < 0 ? 0 : value;
	}
	
	/**
	 * Note that LSP Positions and Ranges are zero-based, whereas LexLocations are 1-based.
	 * Ranges are also end-exclusive.
	 */
	
	public static JSONObject lexLocationToPosition(LexLocation location)
	{
		return new JSONObject(
			"start", new JSONObject(
				"line", zero(location.startLine - 1),
				"character", zero(location.startPos - 1)));
	}

	public static JSONObject lexLocationToRange(LexLocation location)
	{
		if (location.endPos == 0)	// end is not set, so use a single char range
		{
			return new JSONObject(
					"start", new JSONObject(
						"line", zero(location.startLine - 1),
						"character", zero(location.startPos - 1)),
					
					"end",   new JSONObject(
						"line", zero(location.startLine - 1),
						"character", zero(location.startPos)));	// One character
		}
		else
		{
			return new JSONObject(
				"start", new JSONObject(
					"line", zero(location.startLine - 1),
					"character", zero(location.startPos - 1)),
				
				"end",   new JSONObject(
					"line", zero(location.endLine - 1),
					"character", zero(location.endPos)));	// end excluded!
		}
	}

	public static JSONObject lexLocationsToRange(LexLocation from, LexLocation to)
	{
		return new JSONObject(
			"start", new JSONObject(
				"line", zero(from.startLine - 1),
				"character", zero(from.startPos - 1)),
			
			"end",   new JSONObject(
				"line", zero(to.endLine - 1),
				"character", zero(to.endPos)));		// end excluded!
	}
	
	public static JSONObject lexLocationToLocation(LexLocation location)
	{
		return new JSONObject(
			"uri",   location.file.toURI().toString(),
			"range", lexLocationToRange(location));
	}
	
	public static JSONObject lexLocationToSource(LexLocation location)
	{
		return new JSONObject(
			"name", location.file.getName(),
			"sourceReference", 0,
			"path", location.file.getAbsolutePath().toString()
			);
	}
	
	public static LexLocation rangeToLexLocation(File file, JSONObject range)
	{
		JSONObject start = range.get("start");
		Long startLine = start.get("line");
		Long startPos = start.get("character");
		
		JSONObject end = range.get("end");
		Long endLine = end.get("line");
		Long endPos = end.get("character");
		
		return new LexLocation(file, "?",
				startLine.intValue() + 1, startPos.intValue() + 1,
				endLine.intValue() + 1, endPos.intValue() + 1);
	}
	
	public static boolean lexLocationInRange(LexLocation location, File file, JSONObject range)
	{
		LexLocation rangeLoc = rangeToLexLocation(file, range);
		return location.within(rangeLoc);
	}
	
	public static TCNameToken stringToName(String name) throws LSPException
	{
		try
		{
			LexTokenReader ltr = new LexTokenReader(name, Dialect.VDM_SL);
			LexToken token = ltr.nextToken();
			ltr.close();

			if (token.is(Token.NAME))
			{
				return new TCNameToken((LexNameToken) token);
			}
		}
		catch (LexException e)
		{
			// Fall through
		}

		throw new LSPException(RPCErrors.InvalidParams, "Name is not fully qualified: " + name);
	}

	public static File uriToFile(String s) throws URISyntaxException, IOException
	{
		if (s == null)
		{
			return null;
		}
		else
		{
			// Some URIs have illegal spaces - like the Kate editor - so fix here
			s = s.replaceAll(" ", "%20");
			
			URI uri = new URI(s);
			return new File(uri).getAbsoluteFile();
		}
	}

	public static File pathToFile(String s) throws URISyntaxException, IOException
	{
		if (s == null)
		{
			return null;
		}
		else
		{
			return new File(s).getAbsoluteFile();
		}
	}
	
	/**
	 * Return Ranges for the locations of word in the buffer.
	 */
	public static JSONArray findWords(StringBuilder buffer, String word)
	{
		long currentLine = 0;
		long currentCharacter = 0;
		char wstart = word.charAt(0);
		int wlen = word.length();
		int limit = buffer.length() - wlen + 1;
		JSONArray results = new JSONArray();
		
		for (int i=0; i<limit; i++)
		{
			if (buffer.charAt(i) == wstart &&
				buffer.substring(i, i + wlen).equals(word) &&
				!restOfName(buffer.charAt(i + wlen)))	// end of a word
			{
				results.add(
					new JSONObject(
						"start",
							new JSONObject(
								"line", currentLine,
								"character", currentCharacter),
						
						"end",
							new JSONObject(
								"line", currentLine,
								"character", currentCharacter + wlen)));
				
				currentCharacter += wlen;
				i += wlen - 1;
			}
			else if (buffer.charAt(i) == '\n')
			{
				currentLine++;
				currentCharacter = 0;
			}
			else
			{
				currentCharacter++;
			}
		}
		
		return results;
	}

	public static int findPosition(StringBuilder buffer, JSONObject position) throws Exception
	{
		long line = position.get("line");
		long character = position.get("character");
		return findPosition(buffer, line, character);
	}
		
	public static int findPosition(StringBuilder buffer, long zline, long zcol)
	{
		long currentLine = 0;
		long currentCharacter = 0;

		for (int i=0; i<buffer.length(); i++)
		{
			if (currentLine == zline && currentCharacter == zcol)
			{
				return i;
			}
			
			if (buffer.charAt(i) == '\n')
			{
				currentLine++;
				currentCharacter = 0;
			}
			else
			{
				currentCharacter++;
			}
		}
		
		// Catch position at end of file...
		if (currentLine == zline && currentCharacter == zcol)
		{
			return buffer.length();
		}
		
		Diag.error("Cannot locate line %d character %s in buffer length %d", zline, zcol, buffer.length());
		return -1;
	}
	
	/**
	 * Fix the "range" fields of the DocumentSymbol array passed in, such that each
	 * range starts at the selectionRange and ends at the start of the next symbol,
	 * or the end passed (for the last one). Recurse into any children.
	 */
	public static void fixRanges(JSONArray symbols, JSONObject endPosition)
	{
		for (int s = 0; s < symbols.size(); s++)
		{
			JSONObject symbol = symbols.index(s);
			JSONObject start = symbol.getPath("selectionRange.start");

			JSONObject nextstart = null;
			
			for (int n = s + 1; n <= symbols.size(); n++)
			{
				if (n == symbols.size())
				{
					nextstart = endPosition;
				}
				else
				{
					JSONObject next = symbols.index(n);
					nextstart = next.getPath("selectionRange.start");
				}
				
				if (!nextstart.get("line").equals(start.get("line")))
				{
					break;	// Guaranteed exit for endPosition
				}
			}
			
			JSONObject range = symbol.get("range");
			range.put("start", startLine(start));
			range.put("end", beforeNext(nextstart));
			
			verifyRange(symbol.get("name"), range, symbol.getPath("selectionRange"));
			
			JSONArray children = symbol.get("children");
			
			if (children != null)
			{
				fixRanges(children, nextstart);
			}
		}
	}
	
	private static void verifyRange(String name, JSONObject range, JSONObject selectionRange)
	{
		File file = new File("?");
		LexLocation rloc = Utils.rangeToLexLocation(file, range);
		LexLocation sloc = Utils.rangeToLexLocation(file, selectionRange);
		
		if (!sloc.within(rloc))
		{
			Diag.error("Selection not within range at symbol %s", name);
			Diag.error("Range %s", range);
			Diag.error("Selection %s", selectionRange);
		}
	}

	public static JSONObject afterLine(JSONObject position)
	{
		long line = position.get("line");
		return new JSONObject("line", line+1, "character", 0);
	}
	
	private static JSONObject startLine(JSONObject position)
	{
		long line = position.get("line");
		return new JSONObject("line", line, "character", 0);
	}
	
	private static JSONObject beforeNext(JSONObject next)
	{
		long line = next.get("line");
		return new JSONObject("line", line - 1, "character", 999999999);
	}
	
	public static JSONObject getEndPosition(StringBuilder buffer)
	{
		long currentLine = 0;
		long currentCharacter = 0;

		for (int i=0; i<buffer.length(); i++)
		{
			if (buffer.charAt(i) == '\n')
			{
				currentLine++;
				currentCharacter = 0;
			}
			else
			{
				currentCharacter++;
			}
		}
		
		return new JSONObject(
					"line", currentLine,
					"character", currentCharacter);
	}
	
	public static JSONObject getLineEndPosition(StringBuilder buffer, int zline)
	{
		long currentLine = 0;
		long currentCharacter = 0;

		for (int i=0; i<buffer.length(); i++)
		{
			if (buffer.charAt(i) == '\n')
			{
				if (currentLine == zline)
				{
					break;
				}
				
				currentLine++;
				currentCharacter = 0;
			}
			else
			{
				currentCharacter++;
			}
		}
		
		return new JSONObject(
					"line", currentLine,
					"character", currentCharacter);
	}
	
	public static void diff(String message, String s1, String s2)
	{
		int shortest = s1.length() > s2.length() ? s2.length() : s1.length();
		int start = -1;
		StringBuilder diff1 = new StringBuilder();
		StringBuilder diff2 = new StringBuilder();
		
		for (int i=0; i < shortest; i++)
		{
			if (s1.charAt(i) != s2.charAt(i))
			{
				start = i;
				diff1.append(s1.charAt(i));
				diff2.append(s2.charAt(i));
			}
			else if (start >= 0)
			{
				break;
			}
		}
		
		if (start >= 0)
		{
			Diag.error(message, start);
			Diag.info("(1): %s", quote(diff1));
			Diag.info("(2): %s", quote(diff2));
		}
	}
	
	private static String quote(StringBuilder value)
	{
		StringBuilder sb = new StringBuilder();
		
		for (int i=0; i<value.length(); i++)
		{
			char c = value.charAt(i);
			
			switch (c)
			{
    		    case '\r':	sb.append("\\r"); break;
    		    case '\n':	sb.append("\\n"); break;
    		    case '\t':	sb.append("\\t"); break;
    		    case '\f':	sb.append("\\f"); break;
    		    case '\b':	sb.append("\\b"); break;
    		    case '\u000B':	sb.append("\\v"); break;

    		    case '\"':	sb.append("\\\""); break;
    		    case '\\':	sb.append("\\\\"); break;

    		    default:
    		    	sb.append(c);
			}
		}

		return sb.toString();
	}
	
	public static Long getLong(JSONObject message, String field)	// eg. "id" is number | string
	{
		Object raw = message.get(field);

		if (raw == null)
		{
			return null;
		}
		else if (raw instanceof String)
		{
			return Long.parseLong((String)raw);
		}
		else if (raw instanceof Long)
		{
			return (Long)raw;
		}
		else
		{
			throw new RuntimeException("Field " + field + " unexpected type: " + raw.getClass().getSimpleName());
		}
	}

	public static Boolean getBoolean(JSONObject message, String field)
	{
		Object raw = message.get(field);

		if (raw == null)
		{
			return null;
		}
		else if (raw instanceof String)
		{
			return Boolean.parseBoolean((String)raw);
		}
		else if (raw instanceof Boolean)
		{
			return (Boolean)raw;
		}
		else
		{
			throw new RuntimeException("Field " + field + " unexpected type: " + raw.getClass().getSimpleName());
		}
	}
	
	/**
	 * @return True if the character passed can be part of a variable name.
	 */
	private static boolean restOfName(char c)
	{
		if (c < 0x0100)
		{
			return Character.isLetterOrDigit(c) || c == '$' || c == '_' || c == '\'';
		}
		else
		{
			switch (Character.getType(c))
			{
				case Character.CONTROL:
				case Character.LINE_SEPARATOR:
				case Character.PARAGRAPH_SEPARATOR:
				case Character.SPACE_SEPARATOR:
				case Character.SURROGATE:
				case Character.UNASSIGNED:
					return false;

				default:
					return true;
			}
		}
	}
	
	public static JSONObject contextToJSON(Context ctxt)
	{
		JSONObject json = new JSONObject();
		
		for (TCNameToken vname: ctxt.keySet())
		{
			json.put(vname.getName(), ctxt.get(vname).toString());
		}
		
		return json;
	}
}
