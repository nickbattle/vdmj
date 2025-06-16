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

package json;

import java.io.IOException;
import java.io.Reader;

public class JSONReader
{
	private static final char EOF = (char)-1;
	private final Reader ireader;
	private char ch = 0;
	private boolean quotedQuote;

	public JSONReader(Reader ireader) throws IOException
	{
		this.ireader = ireader;
		init();
	}
	
	private void init() throws IOException
	{
		rdCh();
		quotedQuote = false;
	}
	
	/**
	 * Check the next character is as expected. If the character is
	 * not as expected, throw the message as an IOException.
	 *
	 * @param c	The expected next character.
	 * @param message The error message.
	 * @throws IOException
	 */
	private void checkFor(char c, String message) throws IOException
	{
		skipWhitespace();
		
		if (ch == c)
		{
			rdCh();
		}
		else
		{
			throw new IOException(message);
		}
	}

	/**
	 * Read the next character from the stream.
	 *
	 * The next character is set in the "ch" field, as well as being returned for
	 * convenience.
	 *
	 * @return the next character.
	 */
	private char rdCh() throws IOException
	{
		ch = (char) ireader.read();
		return ch;
	}

	/**
	 * Skip any whitespace characters.
	 */
	private void skipWhitespace() throws IOException
	{
		while (Character.isWhitespace(ch))
		{
			rdCh();
		}
	}

	/**
	 * Read a backslash quoted character from the stream. This method is used
	 * when parsing strings which may include things like "\n".
	 *
	 * @return The actual character value (eg. "\n" returns 10).
	 * @throws IOException 
	 */
	private char rdQuotedCh() throws IOException
	{
		char c = rdCh();
		quotedQuote = false;

	    if (c == '\\')
	    {
    		rdCh();

    		switch (ch)
    		{
    		    case 'r':  ch = '\r'; break;
    		    case 'n':  ch = '\n'; break;
    		    case 't':  ch = '\t'; break;
    		    case 'f':  ch = '\f'; break;
    		    case 'b':  ch = '\b'; break;
    		    case 'v':  ch = '\u000B'; break;

    		    case '\"': ch = '\"'; quotedQuote = true; break;
    		    case '\\': ch = '\\'; break;
    		    case '/':  ch = '/'; break;

    		    case 'u':
    		    	ch = (char)(valCh(rdCh(), 16)*4096 + valCh(rdCh(), 16)*256 +
    		    				valCh(rdCh(), 16)*16 + valCh(rdCh(), 16));
    		    	break;

    		    default:
    		    	throw new IOException("Malformed quoted character");
    		}
	    }

	    return ch;
	}
	
	/**
	 * Check and return the value of a character in a particular base.
	 * @throws LexException 
	 */
	private int valCh(char c, int base) throws IOException
	{
		int val = value(c);
		
		if (val == -1 || val >= base)
		{
			throw new IOException("Illegal character for base " + base);
		}
		
		return val;
	}

	/**
	 * Return the value of a character for parsing numbers. The ASCII characters
	 * 0-9 are turned into decimal 0-9, while a-f and A-F are turned into the
	 * hex values 10-15. Characters outside these ranges return -1.
	 *
	 * @param c	The ASCII value to convert.
	 * @return	The converted value.
	 */
	private int value(char c)
	{
		switch (c)
		{
			case '0': case '1':	case '2': case '3': case '4':
			case '5': case '6':	case '7': case '8': case '9':
				return c - '0';

			case 'a': case 'b':	case 'c': case 'd':
			case 'e': case 'f':
				return c - 'a' + 10;

			case 'A': case 'B': case 'C': case 'D':
			case 'E': case 'F':
				return c - 'A' + 10;

			default:
				return -1;
		}
	}

	/**
	 * Read a decimal number. Parsing terminates when a character
	 * not within the number base is read.
	 *
	 * @return The string value of the number read.
	 */
	private String rdDecimal() throws IOException
	{
		StringBuilder v = new StringBuilder();
		
		if (ch == '+' || ch == '-')
		{
			v.append(ch);
			rdCh();
		}
		
		int n = value(ch);
		v.append(ch);

		if (n < 0 || n >= 10)
		{
			throw new IOException("Invalid char [" + ch + "] in base 10");
		}

		while (true)
		{
			rdCh();
			n = value(ch);

			if (n < 0 || n >= 10)
			{
				return v.toString();
			}

			v.append(ch);
		}
	}

	/**
	 * Read a decimal floating point number.
	 *
	 * @throws IOException
	 */
	private Number rdReal() throws IOException
	{
		String floatSyntax = "Expecting [+/-]<digits>[.<digits>][e[+/-]<digits>]";
		String value = rdDecimal();
		String fraction = null;
		String exponent = null;
		boolean negative = false;

		if (ch == '.')
		{
			rdCh();

			if (ch >= '0' && ch <= '9')
			{
				fraction = rdDecimal();
				exponent = "0";
			}
			else
			{
				throw new IOException("Expecting digits after decimal point");
			}
		}

		if (ch == 'e' || ch == 'E')
		{
			if (fraction == null) fraction = "0";

			switch (rdCh())
			{
				case '+':
				{
					rdCh();
					exponent = rdDecimal();
					break;
				}

				case '-':
				{
					rdCh();
					exponent = rdDecimal();
					negative = true;
					break;
				}

				case '0': case '1': case '2': case '3': case '4':
				case '5': case '6': case '7': case '8': case '9':
				{
					exponent = rdDecimal();
					break;
				}

				default:
					throw new IOException(floatSyntax);
			}
		}

		if (fraction != null)
		{
			String real = value + "." + fraction + "e" + (negative ? "-" : "+") + exponent;
			return Double.parseDouble(real);
		}

		return Long.parseLong(value);
	}
	
	/**
	 * Read a JSON name.
	 *
	 * @throws IOException
	 */
	private String rdName() throws IOException
	{
		if (Character.isJavaIdentifierStart(ch))
		{
			StringBuilder sb = new StringBuilder();
			sb.append(ch);
			rdCh();
			
			while (Character.isJavaIdentifierPart(ch))
			{
				sb.append(ch);
				rdCh();
			}
			
			return sb.toString();
		}
		
		throw new IOException("Illegal name");
	}

	/**
	 * Read a JSON quoted string.
	 *
	 * @throws IOException
	 */
	private String rdString() throws IOException
	{
		if (ch != '\"')
		{
			throw new IOException("Expecting \"quoted\" string");
		}
		else
		{
			rdQuotedCh();	// NB. checkFor would use rdCh()
		}
		
		StringBuilder sb = new StringBuilder();
		
		while ((ch != '"' || quotedQuote) && ch != EOF)
		{
			sb.append(ch);
			rdQuotedCh();
		}
		
		if (ch != '\"')		// NB. checkFor would call skipWhitespace
		{
			throw new IOException("Missing close quote in string");
		}
		
		rdCh();
		return sb.toString();
	}

	/**
	 * Read a JSON value (object, array or literal).
	 *
	 * @throws IOException
	 */
	private Object readValue() throws IOException
	{
		skipWhitespace();
		
		switch (ch)
		{
			case '{':
				return readObject();
			
			case '[':
				return readArray();
			
			case '\"':
				return rdString();
				
			default:
			{
				if ((ch >= '0' && ch <= '9') || ch == '-' || ch == '+')
				{
					return rdReal();
				}
				else if (Character.isJavaIdentifierStart(ch))
				{
					String word = rdName();
					
					switch (word)
					{
						case "true":
							return Boolean.TRUE;
							
						case "false":
							return Boolean.FALSE;
							
						case "null":
							return null;	// Hmmm...
							
						default:
							throw new IOException("Unexpected word: " + word);
					}
				}
				else
				{
					throw new IOException("Unexpected character: " + ch);
				}
			}
		}
	}

	/**
	 * Read a JSON array of values.
	 *
	 * @throws IOException
	 */
	private JSONArray readArray() throws IOException
	{
		checkFor('[', "Expecting '[' at start of array");
		JSONArray array = new JSONArray();
		skipWhitespace();
		
		while (ch != ']' && ch != EOF)
		{
			array.add(readValue());
			skipWhitespace();
			
			if (ch == ',')
			{
				rdCh();
				skipWhitespace();
			}
		}
		
		checkFor(']', "Missing closing brace in array");
		return array;
	}

	/**
	 * Read a JSON object.
	 *
	 * @throws IOException
	 */
	public JSONObject readObject() throws IOException
	{
		JSONObject map = new JSONObject();
	
		checkFor('{', "Expecting '{' at start of object");
		skipWhitespace();
		
		if (ch == '}')	// Empty object allowed
		{
			rdCh();
			return map;
		}
		
		String key = rdString();
		checkFor(':', "Expecting <name> : <value>");
		Object value = readValue();
		
		map.put(key, value);
		skipWhitespace();
		
		while (ch == ',')
		{
			rdCh();
			skipWhitespace();
			key = rdString();
			checkFor(':', "Expecting <name> : <value>");
			value = readValue();
			map.put(key, value);
			skipWhitespace();
		}
		
		checkFor('}', "Missing closing bracket in object");
		return map;
	}
}
