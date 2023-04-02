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

package com.fujitsu.vdmj.typechecker;

import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCTypeDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.types.TCBracketType;
import com.fujitsu.vdmj.tc.types.TCClassType;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
import com.fujitsu.vdmj.tc.types.TCInMapType;
import com.fujitsu.vdmj.tc.types.TCInvariantType;
import com.fujitsu.vdmj.tc.types.TCMapType;
import com.fujitsu.vdmj.tc.types.TCNamedType;
import com.fujitsu.vdmj.tc.types.TCNumericType;
import com.fujitsu.vdmj.tc.types.TCOperationType;
import com.fujitsu.vdmj.tc.types.TCOptionalType;
import com.fujitsu.vdmj.tc.types.TCParameterType;
import com.fujitsu.vdmj.tc.types.TCProductType;
import com.fujitsu.vdmj.tc.types.TCRecordType;
import com.fujitsu.vdmj.tc.types.TCSeq1Type;
import com.fujitsu.vdmj.tc.types.TCSeqType;
import com.fujitsu.vdmj.tc.types.TCSet1Type;
import com.fujitsu.vdmj.tc.types.TCSetType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.tc.types.TCUndefinedType;
import com.fujitsu.vdmj.tc.types.TCUnionType;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.tc.types.TCUnresolvedType;
import com.fujitsu.vdmj.tc.types.TCVoidReturnType;
import com.fujitsu.vdmj.tc.types.TCVoidType;
import com.fujitsu.vdmj.tc.types.visitors.TCParameterCollector;


/**
 * A class for static type checking comparisons.
 */

public class TypeComparator
{
	/**
	 * A vector of type pairs that have already been compared. This is to
	 * allow recursive type definitions to be compared without infinite
	 * regress.
	 */

	private static Vector<TypePair> done = new Vector<TypePair>(256);

	/**
	 * A result value for comparison of types. The "Maybe" value is needed so
	 * that the fact that a type's subtypes are being actively compared in
	 * a recursive call can be recorded. For example, if a MySetType contains
	 * references to MySetType in its element's type, the comparison of those
	 * types will see the "Maybe" result of the original call and not recurse.
	 * That will not fail the lower level comparison, but the overall "yes"
	 * will not be recorded until the recursive calls return (assuming there
	 * are no "no" votes of course).
	 */

	private static enum Result { Yes, No, Maybe }

	private static class TypePair
	{
		public TCType a;
		public TCType b;
		public Result result;

		public TypePair(TCType to, TCType from)
		{
			this.a = to;
			this.b = from;
			this.result = Result.Maybe;
		}

		@Override
		public boolean equals(Object other)
		{
			if (other instanceof TypePair)
			{
				TypePair to = (TypePair)other;
				return a == to.a && b == to.b;
			}

			return false;
		}

		@Override
		public int hashCode()
		{
			return a.hashCode() + b.hashCode();
		}
	}
	
	/**
	 * The current module name. This is set as the type checker goes from module
	 * to module, and is used to affect the processing of opaque "non-struct"
	 * type exports.
	 */
	
	private static String currentModule = null;

	public static void setCurrentModule(String module)
	{
		currentModule = module;
	}
	
	/**
	 * Test whether the two types are compatible. This means that, at runtime,
	 * it is possible that the two types are the same, or sufficiently similar
	 * that the "from" value can be assigned to the "to" value.
	 *
	 * @param to
	 * @param from
	 * @return True if types "a" and "b" are compatible.
	 */

	public synchronized static boolean compatible(TCType to, TCType from)
	{
		done.clear();
		return searchCompatible(to, from, false) == Result.Yes;
	}

	public synchronized static boolean compatible(TCType to, TCType from, boolean paramOnly)
	{
		done.clear();
		return searchCompatible(to, from, paramOnly) == Result.Yes;
	}

	/**
	 * Compare two type lists for placewise compatibility.
	 *
	 * @param to
	 * @param from
	 * @return True if all types compatible.
	 */

	public synchronized static boolean compatible(TCTypeList to, TCTypeList from)
	{
		done.clear();
		return allCompatible(to, from, false) == Result.Yes;
	}

	/**
	 * Compare two type lists for placewise compatibility. This is used
	 * to check ordered lists of types such as those in a TCProductType or
	 * parameters to a function or operation.
	 *
	 * @param to
	 * @param from
	 * @return Yes or No.
	 */

	private static Result allCompatible(TCTypeList to, TCTypeList from, boolean paramOnly)
	{
		if (to.size() != from.size())
		{
			return Result.No;
		}
		else
		{
			for (int i=0; i<to.size(); i++)
			{
				if (searchCompatible(to.get(i), from.get(i), paramOnly) == Result.No)
				{
					return Result.No;
				}
			}
		}

		return Result.Yes;
	}

	/**
	 * Search the {@link #done} vector for an existing comparison of two
	 * types before either returning the previous result, or making a new
	 * comparison and adding that result to the vector.
	 *
	 * @param to
	 * @param from
	 * @return Yes or No.
	 */

	private static Result searchCompatible(TCType to, TCType from, boolean paramOnly)
	{
		TypePair pair = new TypePair(to, from);
		int i = done.indexOf(pair);

		if (i >= 0)
		{
			return done.get(i).result;		// May be "Maybe".
		}
		else
		{
			done.add(pair);
		}

		// The pair.result is "Maybe" until this call returns.
		pair.result = comptest(to, from, paramOnly);

		return pair.result;
	}

	/**
	 * The main implementation of the compatibility checker. If "a" and "b" are
	 * the same object the result is "yes"; if either is an {@link TCUnknownType},
	 * we are dealing with earlier parser errors and so "yes" is returned to
	 * avoid too many errors; if either is a {@link TCParameterType} the result is
	 * also "yes" on the grounds that the type cannot be tested at compile time.
	 *
	 * If either type is a {@link TCBracketType} or a {@link TCNamedType} the types
	 * are reduced to their underlying type before proceeding; if either is an
	 * {@link TCOptionalType} and the other is optional also, the result is
	 * "yes", otherwise the underlying type of the optional type is taken before
	 * proceeding; the last two steps are repeated until the types will not
	 * reduce further.
	 *
	 * To compare the reduced types, if "a" is a union type, then all the
	 * component types of "a" are compared to "b" (or b's components, if it too
	 * is a union type) until a match is found; otherwise basic type comparisons
	 * are made, involving any subtypes - for example, if they are both sets,
	 * then the result depends on whether their "set of" subtypes are
	 * compatible, by a recursive call. Similarly with maps and sequences,
	 * function/operation parameter types, and record field types. Lastly, a
	 * simple {@link com.fujitsu.vdmj.tc.types.TCType#equals} operation is
	 * performed on two basic types to decide the result.
	 *
	 * @param to
	 * @param from
	 * @param paramOnly
	 * @return Yes or No.
	 */

	private static Result comptest(TCType to, TCType from, boolean paramOnly)
	{
		if (to instanceof TCUnresolvedType)
		{
			throw new TypeCheckException("Unknown type: " + to, to.location);
		}

		if (from instanceof TCUnresolvedType)
		{
			throw new TypeCheckException("Unknown type: " + from, from.location);
		}

		if (to == from)
		{
			return Result.Yes;	// Same object!
		}

		if (to instanceof TCUnknownType || from instanceof TCUnknownType)
		{
			return Result.Yes;	// Hmmm... too many errors otherwise
		}

		if (to instanceof TCUndefinedType || from instanceof TCUndefinedType)
		{
			return Result.Yes;	// Not defined "yet"...?
		}

		// Obtain the fundamental type of BracketTypes, NamedTypes and
		// OptionalTypes.

		boolean resolved = false;

		while (!resolved)
		{
    		if (to instanceof TCBracketType)
    		{
    			to = ((TCBracketType)to).type;
    			continue;
    		}

    		if (from instanceof TCBracketType)
    		{
    			from = ((TCBracketType)from).type;
    			continue;
    		}
    		
    		if (to instanceof TCInvariantType)
    		{
    			TCInvariantType ito =(TCInvariantType)to;
    			
	    		if (to instanceof TCNamedType &&
	    			(!ito.opaque || ito.location.module.equals(currentModule)))
	    		{
	    			to = ((TCNamedType)to).type;
	    			continue;
	    		}
    		}

    		if (from instanceof TCInvariantType)
    		{
    			TCInvariantType ifrom =(TCInvariantType)from;
    			
	    		if (from instanceof TCNamedType &&
	    			(!ifrom.opaque || ifrom.location.module.equals(currentModule)))
	    		{
	    			from = ((TCNamedType)from).type;
	    			continue;
	    		}
    		}

    		if (to instanceof TCOptionalType)
    		{
    			if (from instanceof TCOptionalType)
    			{
    				return Result.Yes;
    			}

    			to = ((TCOptionalType)to).type;
    			continue;
    		}

    		if (from instanceof TCOptionalType)
    		{
    			// Can't assign nil to a non-optional type? This should maybe
    			// generate a warning here?

    			if (to instanceof TCOptionalType)
    			{
    				return Result.Yes;
    			}

    			from = ((TCOptionalType)from).type;
    			continue;
    		}

    		resolved = true;
		}

		if (from instanceof TCParameterType)
		{
			String fstr = from.apply(new TCParameterCollector(), null).get(0);
			List<String> tstr = to.apply(new TCParameterCollector(), null);
			
			if (tstr.contains(fstr) && !(to instanceof TCParameterType))
			{
				from.warning(5031, "Type " + from + " must be a union");	// See bug #562
			}
			
			return Result.Yes;	// Runtime checked...
		}

		// OK... so we have fully resolved the basic types...

		if (to instanceof TCUnionType)
		{
			TCUnionType ua = (TCUnionType)to;

			for (TCType ta: ua.types)
			{
				if (searchCompatible(ta, from, paramOnly) == Result.Yes)
				{
					return Result.Yes;
				}
			}
		}
		else
		{
			if (from instanceof TCUnionType)
			{
				TCUnionType ub = (TCUnionType)from;

				for (TCType tb: ub.types)
				{
					if (searchCompatible(to, tb, paramOnly) == Result.Yes)
					{
						return Result.Yes;
					}
				}
			}
			else if (to instanceof TCNumericType)
			{
				return (from instanceof TCNumericType) ? Result.Yes : Result.No;
			}
			else if (to instanceof TCProductType)
			{
				if (!(from instanceof TCProductType))
				{
					return Result.No;
				}

				TCTypeList ta = ((TCProductType)to).types;
				TCTypeList tb = ((TCProductType)from).types;
				return allCompatible(ta, tb, paramOnly);
			}
			else if (to instanceof TCMapType)
			{
				if (!(from instanceof TCMapType))
				{
					return Result.No;
				}

				TCMapType ma = (TCMapType)to;
				TCMapType mb = (TCMapType)from;

				return (ma.empty || mb.empty ||
					(searchCompatible(ma.from, mb.from, paramOnly) == Result.Yes &&
					 searchCompatible(ma.to, mb.to, paramOnly) == Result.Yes)) ?
							Result.Yes : Result.No;
			}
			else if (to instanceof TCSetType)	// Includes set1
			{
				if (!(from instanceof TCSetType))
				{
					return Result.No;
				}

				TCSetType sa = (TCSetType)to;
				TCSetType sb = (TCSetType)from;

				if (to instanceof TCSet1Type && sb.empty)
				{
					return Result.No;
				}

				return (sa.empty || sb.empty ||
						searchCompatible(sa.setof, sb.setof, paramOnly) == Result.Yes) ?
							Result.Yes : Result.No;
			}
			else if (to instanceof TCSeqType)	// Includes seq1
			{
				if (!(from instanceof TCSeqType))
				{
					return Result.No;
				}

				TCSeqType sa = (TCSeqType)to;
				TCSeqType sb = (TCSeqType)from;

				if (to instanceof TCSeq1Type && sb.empty)
				{
					return Result.No;
				}

				return (sa.empty || sb.empty ||
						searchCompatible(sa.seqof, sb.seqof, paramOnly) == Result.Yes) ?
							Result.Yes : Result.No;
			}
			else if (to instanceof TCFunctionType)
			{
				if (!(from instanceof TCFunctionType))
				{
					return Result.No;
				}

				TCFunctionType fa = (TCFunctionType)to;
				TCFunctionType fb = (TCFunctionType)from;
				
//				if (fb.partial && !fa.partial)
//				{
//					return Result.No;
//				}

				return (allCompatible(fa.parameters, fb.parameters, paramOnly) == Result.Yes &&
						(paramOnly ||
						 searchCompatible(fa.result, fb.result, paramOnly) == Result.Yes)) ?
							Result.Yes : Result.No;
			}
			else if (to instanceof TCOperationType)
			{
				if (!(from instanceof TCOperationType))
				{
					return Result.No;
				}

				TCOperationType fa = (TCOperationType)to;
				TCOperationType fb = (TCOperationType)from;

				return (allCompatible(fa.parameters, fb.parameters, paramOnly) == Result.Yes &&
						(paramOnly ||
						 searchCompatible(fa.result, fb.result, paramOnly) == Result.Yes)) ?
							Result.Yes : Result.No;
			}
			else if (to instanceof TCRecordType)
			{
				if (!(from instanceof TCRecordType))
				{
					return Result.No;
				}

				TCRecordType rf = (TCRecordType)from;
				TCRecordType rt = (TCRecordType)to;

				return rf.equals(rt) ? Result.Yes : Result.No;
			}
			else if (to instanceof TCClassType)
			{
				if (!(from instanceof TCClassType))
				{
					return Result.No;
				}

				TCClassType cfrom = (TCClassType)from;
				TCClassType cto = (TCClassType)to;

				// VDMTools doesn't seem to worry about sub/super type
				// assignments. This was "cfrom.equals(cto)".

				if (cfrom.hasSupertype(cto) || cto.hasSupertype(cfrom))
				{
					return Result.Yes;
				}
			}
			else if (from instanceof TCVoidReturnType)
			{
				if (to instanceof TCVoidType || to instanceof TCVoidReturnType)
				{
					return Result.Yes;
				}
				else
				{
					return Result.No;
				}
			}
			else if (to instanceof TCVoidReturnType)
			{
				if (from instanceof TCVoidType || from instanceof TCVoidReturnType)
				{
					return Result.Yes;
				}
				else
				{
					return Result.No;
				}
			}
			else if (to instanceof TCParameterType)
			{
				String tstr = to.apply(new TCParameterCollector(), null).get(0);
				List<String> fstr = from.apply(new TCParameterCollector(), null);
				
				if (fstr.contains(tstr) && !(from instanceof TCParameterType))
				{
					to.warning(5031, "Type " + to + " must be a union");	// See bug #562
				}
				
				return Result.Yes;	// Runtime checked...
			}
			else
			{
				return to.equals(from) ? Result.Yes : Result.No;
			}
		}

		return Result.No;
	}

	/**
	 * Test whether one type is a subtype of another.
	 *
	 * @param sub
	 * @param sup
	 * @param invignore True if type invariants should be unmapped 
	 * @return True if sub is a subtype of sup.
	 */

	public synchronized static boolean isSubType(TCType sub, TCType sup)
	{
		return isSubType(sub, sup, false);	// By default, invariants fail a match 
	}

	public synchronized static boolean isSubType(TCType sub, TCType sup, boolean invignore)
	{
		done.clear();
		return searchSubType(sub, sup, invignore) == Result.Yes;
	}

	/**
	 * Compare two type lists for placewise subtype compatibility. This is used
	 * to check ordered lists of types such as those in a TCProductType or
	 * parameters to a function or operation.
	 *
	 * @param sub
	 * @param sup
	 * @return Yes or No.
	 */

	private static Result allSubTypes(TCTypeList sub, TCTypeList sup, boolean invignore)
	{
		if (sub.size() != sup.size())
		{
			return Result.No;
		}
		else
		{
			for (int i=0; i<sub.size(); i++)
			{
				if (searchSubType(sub.get(i), sup.get(i), invignore) == Result.No)
				{
					return Result.No;
				}
			}
		}

		return Result.Yes;
	}

	/**
	 * Search the {@link #done} vector for an existing subtype comparison of two
	 * types before either returning the previous result, or making a new
	 * comparison and adding that result to the vector.
	 *
	 * @param sub
	 * @param sup
	 * @param invignore 
	 * @return Yes or No, if sub is a subtype of sup.
	 */

	private static Result searchSubType(TCType sub, TCType sup, boolean invignore)
	{
		TypePair pair = new TypePair(sub, sup);
		int i = done.indexOf(pair);

		if (i >= 0)
		{
			return done.get(i).result;		// May be "Maybe".
		}
		else
		{
			done.add(pair);
		}

		// The pair.result is "Maybe" until this call returns.
		pair.result = subtest(sub, sup, invignore);

		return pair.result;
	}

	/**
	 * The main implementation of the subtype checker. If "a" and "b" are
	 * the same object the result is "yes"; if either is an {@link TCUnknownType},
	 * we are dealing with earlier parser errors and so "yes" is returned to
	 * avoid too many errors; if either is a {@link TCParameterType} the result is
	 * also "yes" on the grounds that the type cannot be tested at compile time.
	 *
	 * If either type is a {@link TCBracketType} or a {@link TCNamedType} the types
	 * are reduced to their underlying type before proceeding; if either is an
	 * {@link TCOptionalType} and the other is optional also, the result is
	 * "yes", otherwise the underlying type of the optional type is taken before
	 * proceeding; the last two steps are repeated until the types will not
	 * reduce further.
	 *
	 * To compare the reduced types, if "a" is a union type, then all the
	 * component types of "a" are compared to "b" (or b's components, if it too
	 * is a union type); otherwise basic type comparisons
	 * are made, involving any subtypes - for example, if they are both sets,
	 * then the result depends on whether their "set of" subtypes are
	 * subtypes, by a recursive call. Similarly with maps and sequences,
	 * function/operation parameter types, and record field types. Lastly, a
	 * simple {@link com.fujitsu.vdmj.tc.types.TCType#equals} operation is
	 * performed on two basic types to decide the result.
	 *
	 * @param sub
	 * @param sup
	 * @param invariants 
	 * @return Yes or No.
	 */

	private static Result subtest(TCType sub, TCType sup, boolean invignore)
	{
		if (sub instanceof TCUnresolvedType)
		{
			throw new TypeCheckException("Unknown type: " + sub, sub.location);
		}

		if (sup instanceof TCUnresolvedType)
		{
			throw new TypeCheckException("Unknown type: " + sup, sup.location);
		}

		if (sub instanceof TCUnknownType || sup instanceof TCUnknownType)
		{
			return Result.Yes;	// Hmmm... too many errors otherwise
		}

		if (sub instanceof TCParameterType || sup instanceof TCParameterType)
		{
			return Result.Yes;	// Runtime checked...
		}

		if (sub instanceof TCUndefinedType || sup instanceof TCUndefinedType)
		{
			return Result.Yes;	// Usually uninitialized variables etc.
		}

		// Obtain the fundamental type of BracketTypes, NamedTypes and
		// OptionalTypes.

		boolean resolved = false;

		while (!resolved)
		{
    		if (sub instanceof TCBracketType)
    		{
    			sub = ((TCBracketType)sub).type;
    			continue;
    		}

    		if (sup instanceof TCBracketType)
    		{
    			sup = ((TCBracketType)sup).type;
    			continue;
    		}

    		// NOTE: not testing opaque InvariantTypes here.
    		
    		if (sub instanceof TCNamedType)
    		{
    			TCNamedType nt = (TCNamedType)sub;

       			if (nt.invdef == null || invignore)
       			{
        			sub = nt.type;
        			continue;
        		}
    		}

    		if (sup instanceof TCNamedType)
    		{
    			TCNamedType nt = (TCNamedType)sup;

       			if (nt.invdef == null || invignore)
       			{
        			sup = nt.type;
        			continue;
        		}
    		}

    		if (sub instanceof TCOptionalType && sup instanceof TCOptionalType)
    		{
       			sub = ((TCOptionalType)sub).type;
       			sup = ((TCOptionalType)sup).type;
    			continue;
    		}

    		resolved = true;
		}

		if (sub instanceof TCUnknownType || sup instanceof TCUnknownType)
		{
			return Result.Yes;		// Hmmm... too many errors otherwise
		}

		if (sub == sup)
		{
			return Result.Yes;		// Same object!
		}

		// OK... so we have fully resolved the basic types...

		if (sub instanceof TCUnionType)
		{
			TCUnionType subu = (TCUnionType)sub;

			for (TCType suba: subu.types)
			{
				if (searchSubType(suba, sup, invignore) == Result.No)
				{
					return Result.No;
				}
			}

			return Result.Yes;	// Must be all of them
		}
		else
		{
			if (sup instanceof TCUnionType)
			{
				TCUnionType supu = (TCUnionType)sup;

				for (TCType supt: supu.types)
				{
					if (searchSubType(sub, supt, invignore) == Result.Yes)
					{
						return Result.Yes;	// Can be any of them
					}
				}

				return Result.No;
			}
			else if (sub instanceof TCNamedType)
    		{
				if (sup instanceof TCNamedType)
				{
					// both have an invariant and we're not ignoring them, so check for equality
					return sub.equals(sup) && !sub.isMaximal() ? Result.Yes : Result.No;
				}
				else
				{
					// sub has an invariant and we're not ignoring it, so No.
					return Result.No;
				}
			}
			else if (sup instanceof TCOptionalType)
    		{
				// Supertype includes a nil value, and the subtype is not
				// optional (stripped above), so we test the optional's type.

				TCOptionalType op = (TCOptionalType)sup;
				return searchSubType(sub, op.type, invignore);
			}
			else if (sub instanceof TCNumericType)
			{
				if (sup instanceof TCNumericType)
				{
					TCNumericType subn = (TCNumericType)sub;
					TCNumericType supn = (TCNumericType)sup;

					return (subn.getWeight() <= supn.getWeight()) ?
						Result.Yes : Result.No;
				}
			}
			else if (sub instanceof TCProductType)
			{
				if (!(sup instanceof TCProductType))
				{
					return Result.No;
				}

				TCTypeList subl = ((TCProductType)sub).types;
				TCTypeList supl = ((TCProductType)sup).types;

				return allSubTypes(subl, supl, invignore);
			}
			else if (sub instanceof TCMapType)
			{
				if (!(sup instanceof TCMapType))
				{
					return Result.No;
				}

				TCMapType subm = (TCMapType)sub;
				TCMapType supm = (TCMapType)sup;

				if (subm.empty || supm.empty)
				{
					return Result.Yes;
				}

				if (searchSubType(subm.from, supm.from, invignore) == Result.Yes &&
					searchSubType(subm.to, supm.to, invignore) == Result.Yes)
				{
					if (!(sub instanceof TCInMapType) &&
						 (sup instanceof TCInMapType))
					{
						return Result.No;
					}
					
					return Result.Yes;
				}
				else
				{
					return Result.No;
				}
			}
			else if (sub instanceof TCSetType)	// Includes set1
			{
				if (!(sup instanceof TCSetType))
				{
					return Result.No;
				}

				TCSetType subs = (TCSetType)sub;
				TCSetType sups = (TCSetType)sup;

				if ((subs.empty && !(sup instanceof TCSet1Type)) || sups.empty)
				{
					return Result.Yes;
				}

				if (searchSubType(subs.setof, sups.setof, invignore) == Result.Yes)
				{
					if (!(sub instanceof TCSet1Type) &&
						 (sup instanceof TCSet1Type))
					{
						return Result.No;
					}
					
					return Result.Yes;
				}
				else
				{
					return Result.No;
				}
			}
			else if (sub instanceof TCSeqType)	// Includes seq1
			{
				if (!(sup instanceof TCSeqType))
				{
					return Result.No;
				}

				TCSeqType subs = (TCSeqType)sub;
				TCSeqType sups = (TCSeqType)sup;

				if ((subs.empty && !(sup instanceof TCSeq1Type)) || sups.empty)
				{
					return Result.Yes;
				}

				if (searchSubType(subs.seqof, sups.seqof, invignore) == Result.Yes)
				{
					if (!(sub instanceof TCSeq1Type) &&
						 (sup instanceof TCSeq1Type))
					{
						return Result.No;
					}
					
					return Result.Yes;
				}
				else
				{
					return Result.No;
				}
			}
			else if (sub instanceof TCFunctionType)
			{
				if (!(sup instanceof TCFunctionType))
				{
					return Result.No;
				}

				TCFunctionType subf = (TCFunctionType)sub;
				TCFunctionType supf = (TCFunctionType)sup;

				if (subf.partial && !supf.partial)
				{
					return Result.No;
				}

				return (allSubTypes(subf.parameters, supf.parameters, invignore) == Result.Yes &&
						searchSubType(subf.result, supf.result, invignore) == Result.Yes) ?
							Result.Yes : Result.No;
			}
			else if (sub instanceof TCOperationType)
			{
				if (!(sup instanceof TCOperationType))
				{
					return Result.No;
				}

				TCOperationType subo = (TCOperationType)sub;
				TCOperationType supo = (TCOperationType)sup;

				return (allSubTypes(subo.parameters, supo.parameters, invignore) == Result.Yes &&
						searchSubType(subo.result, supo.result, invignore) == Result.Yes) ?
							Result.Yes : Result.No;
			}
			else if (sub instanceof TCRecordType)
			{
				if (!(sup instanceof TCRecordType))
				{
					return Result.No;
				}

				TCRecordType subr = (TCRecordType)sub;
				TCRecordType supr = (TCRecordType)sup;

				return subr.equals(supr) && sub.isMaximal() == sup.isMaximal() ? Result.Yes : Result.No;
			}
			else if (sub instanceof TCClassType)
			{
				if (!(sup instanceof TCClassType))
				{
					return Result.No;
				}

				TCClassType supc = (TCClassType)sup;
				TCClassType subc = (TCClassType)sub;

				if (subc.hasSupertype(supc))
				{
					return Result.Yes;
				}
			}
			else
			{
				return sub.equals(sup) ? Result.Yes : Result.No;
			}
		}

		return Result.No;
	}
	
	/**
	 * Check that the compose types that are referred to in a type have a matching
	 * definition in the environment. The method returns a list of types that do not
	 * exist if the newTypes parameter is passed. 
	 */
	public static TCTypeList checkComposeTypes(TCType type, Environment env, boolean newTypes)
	{
		TCTypeList undefined = new TCTypeList();
		
		for (TCType compose: type.getComposeTypes())
		{
			TCRecordType composeType = (TCRecordType)compose;
			TCDefinition existing = env.findType(composeType.name, null);
			
			if (existing != null)
			{
				// If the type is already defined, check that it has the same shape and
				// does not have an invariant (which cannot match a compose definition).
				boolean matches = false;
				
				if (existing instanceof TCTypeDefinition)
				{
					TCTypeDefinition edef = (TCTypeDefinition)existing;
					TCType etype = existing.getType();
					
					if (edef.invExpression == null && etype instanceof TCRecordType)
					{
						TCRecordType retype = (TCRecordType)etype;
						
						if (retype.fields.equals(composeType.fields))
						{
							matches = true;
						}
					}
				}
					
				if (!matches)
				{
					TypeChecker.report(3325, "Mismatched compose definitions for " + composeType.name, composeType.location);
					TypeChecker.detail2(composeType.name.getName(), composeType.location, existing.name.getName(), existing.location);
				}
			}
			else
			{
				if (newTypes)
				{
					undefined.add(composeType);
				}
				else
				{
					TypeChecker.report(3113, "Unknown type name '" + composeType.name + "'", composeType.location);
				}
			}
		}
		
		// Lastly, check that the compose types extracted are compatible
		TCNameList done = new TCNameList();
		
		for (TCType c1: undefined)
		{
			for (TCType c2: undefined)
			{
				if (c1 != c2)
				{
					TCRecordType r1 = (TCRecordType)c1;
					TCRecordType r2 = (TCRecordType)c2;
					
					if (r1.name.equals(r2.name) && !done.contains(r1.name) && !r1.fields.equals(r2.fields))
					{
						TypeChecker.report(3325, "Mismatched compose definitions for " + r1.name, r1.location);
						TypeChecker.detail2(r1.name.getName(), r1.location, r2.name.getName(), r2.location);
						done.add(r1.name);
					}
				}
			}
		}
		
		return undefined;
	}
	
	/**
	 * Check that all of the named and record types within the passed type are
	 * accessible from the module. The types passed will be UnresolvedTypes.
	 */
	public static void checkImports(Environment env, TCTypeList types, String fromModule)
	{
		if (Settings.dialect != Dialect.VDM_SL)
		{
			return;
		}
		
		for (TCType type: types)
		{
			if (type instanceof TCUnresolvedType)
			{
				TCUnresolvedType utype = (TCUnresolvedType)type;
				
				if (env.findType(utype.typename, fromModule) == null)
				{
					TypeChecker.report(3430,
						"Unable to resolve type name '" + utype.typename + "'", type.location);
				}
			}
		}
	}

	/**
	 * Calculate the intersection of two types.
	 */
	public static TCType intersect(TCType a, TCType b)
	{
		TCTypeSet tsa = new TCTypeSet();
		TCTypeSet tsb = new TCTypeSet();

		// Obtain the fundamental type of BracketTypes, NamedTypes and OptionalTypes.
		boolean resolved = false;

		while (!resolved)
		{
    		if (a instanceof TCBracketType)
    		{
    			a = ((TCBracketType)a).type;
    			continue;
    		}

    		if (b instanceof TCBracketType)
    		{
    			b = ((TCBracketType)b).type;
    			continue;
    		}

    		if (a instanceof TCNamedType)
    		{
    			TCNamedType nt = (TCNamedType)a;

       			if (nt.invdef == null)
       			{
        			a = nt.type;
        			continue;
        		}
    		}

    		if (b instanceof TCNamedType)
    		{
    			TCNamedType nt = (TCNamedType)b;

       			if (nt.invdef == null)
       			{
        			b = nt.type;
        			continue;
        		}
    		}

    		if (a instanceof TCOptionalType && b instanceof TCOptionalType)
    		{
       			a = ((TCOptionalType)a).type;
       			b = ((TCOptionalType)b).type;
    			continue;
    		}

    		resolved = true;
		}

		if (a instanceof TCUnionType)
		{
			TCUnionType uta = (TCUnionType)a;
			tsa.addAll(uta.types);
		}
		else
		{
			tsa.add(a);
		}
		
		if (b instanceof TCUnionType)
		{
			TCUnionType utb = (TCUnionType)b;
			tsb.addAll(utb.types);
		}
		else
		{
			tsb.add(b);
		}
		
		// Keep largest types which are compatible (eg. nat and int choses int)
		TCTypeSet result = new TCTypeSet();
		
		for (TCType atype: tsa)
		{
			for (TCType btype: tsb)
			{
				if (isSubType(atype, btype))
				{
					result.add(btype);
				}
				else if (isSubType(btype, atype))
				{
					result.add(atype);
				}
			}
		}
		
		if (result.isEmpty())
		{
			return null;
		}
		else
		{
			return result.getType(a.location);
		}
	}
	
	/**
	 * Return the narrowest of two types/type lists.
	 */
	public static synchronized TCTypeList narrowest(TCTypeList t1, TCTypeList t2)
	{
		return allSubTypes(t1, t2, false) == Result.Yes ? t1 : t2;
	}
	
	public static synchronized TCType narrowest(TCType t1, TCType t2)
	{
		return isSubType(t1, t2) ? t1 : t2;
	}
}
