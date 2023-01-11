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

package com.fujitsu.vdmj.tc.types;

import java.io.Serializable;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.definitions.TCAccessSpecifier;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.types.visitors.TCInfiniteTypeFinder;
import com.fujitsu.vdmj.tc.types.visitors.TCRecursiveTypeFinder;
import com.fujitsu.vdmj.tc.types.visitors.TCTypeVisitor;
import com.fujitsu.vdmj.tc.types.visitors.TCUnresolvedTypeFinder;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.TypeChecker;

/**
 * The parent class of all static type checking types.
 */
public abstract class TCType extends TCNode implements Comparable<TCType>, Serializable
{
	private static final long serialVersionUID = 1L;

	/** The location of the type */
	public final LexLocation location;
	/** True if the type's and its subtype's names have been resolved. */
	public boolean resolved = false;
	
	/** The type's possible definition(s) (if a named type) */
	public transient TCDefinitionList definitions = null;
	
	/**
	 * Create a new type at the given location.
	 *
	 * @param location
	 */
	public TCType(LexLocation location)
	{
		this.location = location;
	}

	abstract protected String toDisplay();

	/** A flag to prevent recursive types from failing toString(). */
	private boolean inToString = false;

	/**
	 * Note that this is synchronized so that multiple threads calling
	 * toString will both get the same string, not "...". This causes
	 * problems with VDM-RT trace logs which are threaded, and use
	 * this method for operation names. It is final, because subclasses
	 * must implement toDisplay and not override toString.
	 */
	@Override
	public final synchronized String toString()
	{
		if (inToString)
		{
			return "...";
		}
		else
		{
			inToString = true;
			String s = toDisplay();
			inToString = false;
			return s;
		}
	}

	/**
	 * The type with expanded detail, in the case of record types.
	 *
	 * @return The detailed type string.
	 */
	public String toDetailedString()
	{
		return toString();
	}

	/**
	 * Resolve the type. After syntax checking, all named type references are
	 * created as {@link com.fujitsu.vdmj.ast.types.TCUnresolvedType}, which simply have a name.
	 * The process of resolving a type in a given {@link Environment} will
	 * lookup any UnresolvedTypes and replace them with the type of the actual
	 * definition. This process is performed across all of the subtypes of a
	 * type (eg. in the element types in a TCSetType).
	 *
	 * @param env The other type names defined in this scope.
	 */
	public TCType typeResolve(Environment env)
	{
		resolved = true;
		return this;
	}

	/**
	 * Clear the recursive "resolved" flag. This does a deep search of a
	 * type structure, clearing the flag. It is used when type checking
	 * errors require multiple passes of the type tree. If there are too
	 * many errors, we are probably looping, so we leave the flag set.
	 */
	public void unResolve()
	{
		resolved = false;
	}

	/**
	 * Remove layers of BracketTypes.
	 */
	public TCType deBracket()
	{
		return (TCType) deBracket(this);
	}

	public Object deBracket(Object other)	// Useful in equals(Object) methods
	{
		while (other instanceof TCBracketType)
		{
			other = ((TCBracketType)other).type;
		}

		return other;
	}

	public boolean narrowerThan(TCAccessSpecifier accessSpecifier)
	{
		if (definitions != null)
		{
			boolean result = false;

			for (TCDefinition d: definitions)
			{
				result = result || d.accessSpecifier.narrowerThan(accessSpecifier);
			}

			return result;
		}
		else
		{
			return false;
		}
	}

	/**
	 * @param typename
	 * @param from
	 */
	public TCType isType(String typename, LexLocation from)
	{
		return (toDisplay().equals(typename)) ? this : null;
	}

	/**
	 * @param typeclass
	 * @param from
	 */
	public boolean isType(Class<? extends TCType> typeclass, LexLocation from)
	{
		return typeclass.isInstance(this);
	}

	/**
	 * @param from
	 */
	public boolean isUnion(LexLocation from)
	{
		return false;	// Unions, or names of unions etc are not unique.
	}

	/**
	 * @param from
	 */
	public boolean isUnknown(LexLocation from)
	{
		return false;	// Parameter types and type check errors are unknown.
	}

	public boolean isVoid()
	{
		return false;	// TCVoidType and TCVoidReturnType are void.
	}

	public boolean hasVoid()
	{
		return false;	// TCVoidType and TCVoidReturnType are void.
	}

	/**
	 * @param from
	 */
	public boolean isSeq(LexLocation from)
	{
		return false;
	}

	/**
	 * @param from
	 */
	public boolean isSet(LexLocation from)
	{
		return false;
	}

	/**
	 * @param from Where the test is being made from.
	 */
	public boolean isMap(LexLocation from)
	{
		return false;
	}

	/**
	 * @param from Where the test is being made from.
	 */
	public boolean isRecord(LexLocation from)	// ie. does it contain fields (see isTag)
	{
		return false;
	}

	public boolean isTag()		// ie. can we call mk_T (see isRecord)
	{
		return false;
	}

	/**
	 * @param env  
	 */
	public boolean isClass(Environment env)
	{
		return false;
	}

	/**
	 * @param from
	 */
	public boolean isNumeric(LexLocation from)
	{
		return false;
	}

	/**
	 * True if the type is numeric, or defines an ord clause (so can use "<").
	 * @param from
	 */
	public boolean isOrdered(LexLocation from)
	{
		return false;
	}

	/**
	 * True if the type defines an eq clause (all types can be compared for equality)
	 * @param from
	 */
	public boolean isEq(LexLocation from)
	{
		return false;
	}

	/**
	 * @param from
	 */
	public boolean isProduct(LexLocation from)
	{
		return false;
	}

	/**
	 * @param n
	 * @param from
	 */
	public boolean isProduct(int n, LexLocation from)
	{
		return false;
	}

	/**
	 * @param from
	 */
	public boolean isFunction(LexLocation from)
	{
		return false;
	}

	public static boolean isFunctionType(TCType type, LexLocation from)
	{
		if (type instanceof TCUnionType)
		{
			TCUnionType union = (TCUnionType)type;
			
			for (TCType element: union.types)
			{
				if (isFunctionType(element, from))
				{
					return true;
				}
			}
			
			return false;
		}
		else
		{
			return type.isFunction(from) &&
				!(type instanceof TCOptionalType) &&	// eg. nil is not a function
				!(type instanceof TCUnknownType);		// eg. ? is not a function
		}
	}

	/**
	 * @param from
	 */
	public boolean isOperation(LexLocation from)
	{
		return false;
	}

	public TCUnionType getUnion()
	{
		assert false : "Can't getUnion of a non-union";
		return null;
	}

	public TCSeqType getSeq()
	{
		assert false : "Can't getSeq of a non-sequence";
		return null;
	}

	public TCSetType getSet()
	{
		assert false : "Can't getSet of a non-set";
		return null;
	}

	public TCMapType getMap()
	{
		assert false : "Can't getMap of a non-map";
		return null;
	}

	public TCRecordType getRecord()
	{
		assert false : "Can't getRecord of a non-record";
		return null;
	}

	/**
	 * @param env  
	 */
	public TCClassType getClassType(Environment env)
	{
		assert false : "Can't getClassType of a non-class";
		return null;
	}

	public TCNumericType getNumeric()
	{
		assert false : "Can't getNumeric of a non-numeric";
		return null;
	}

	public TCProductType getProduct()
	{
		assert false : "Can't getProduct of a non-product";
		return null;
	}

	public TCProductType getProduct(int n)
	{
		assert false : "Can't getProduct of a non-product: " + n;
		return null;
	}

	public TCFunctionType getFunction()
	{
		assert false : "Can't getFunction of a non-function";
		return null;
	}

	public TCOperationType getOperation()
	{
		assert false : "Can't getOperation of a non-operation";
		return null;
	}

	public TCTypeList getComposeTypes()
	{
		return new TCTypeList();
	}

	@Override
	public boolean equals(Object other)
	{
		other = deBracket(other);

		return this.getClass() == other.getClass();
	}

	@Override
	public int compareTo(TCType o)
	{
		// This is used by the TreeSet to do inserts, not equals!!
		return toString().compareTo(o.toString());
	}

	@Override
	public int hashCode()
	{
		return getClass().hashCode();
	}
	
	/**
	 * Return a list of TCUnresolvedTypes contained within this type. This is
	 * used for the location of type names in the LSP server. Note that the
	 * method must be called before type resolution!
	 */
	public TCTypeList unresolvedTypes()
	{
		return this.apply(new TCUnresolvedTypeFinder(), null);
	}

	/**
	 * Identify recursive and infinite types.
	 */
	public boolean isInfinite()
	{
		return !this.apply(new TCInfiniteTypeFinder(), this).isEmpty();
	}

	public boolean isRecursive()
	{
		return !this.apply(new TCRecursiveTypeFinder(), this).isEmpty();
	}

	public void report(int number, String msg)
	{
		TypeChecker.report(number, msg, location);
	}

	public void warning(int number, String msg)
	{
		TypeChecker.warning(number, msg, location);
	}

	public void detail(String tag, Object obj)
	{
		TypeChecker.detail(tag, obj);
	}

	public void detail2(String tag1, Object obj1, String tag2, Object obj2)
	{
		TypeChecker.detail2(tag1, obj1, tag2, obj2);
	}
	
	/**
	 * Implemented by all types to allow visitor processing.
	 */
	abstract public <R, S> R apply(TCTypeVisitor<R, S> visitor, S arg);
}
