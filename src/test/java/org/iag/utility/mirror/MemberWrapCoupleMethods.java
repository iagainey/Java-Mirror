package org.iag.utility.mirror;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith( Parameterized.class )
public class MemberWrapCoupleMethods {

	@Parameter( 0 )
	@NonNull
	public String testName;

	@Parameter( 1 )
	@NonNull
	public MemberWrap<Object,
					  Object> wrap;

	@Parameter( 2 )
	@NonNull
	public Function<MemberWrap<?,
							   ?>,
					Object> method;

	@Parameter( 3 )
	@Nullable
	public Object expect;

	@Test
	public void
		   test(){
		assertEquals( expect,
					  method.apply( wrap ) );
	}

	@Parameters( name = "{index}:{0}" )
	public static List<Object[]>
		   cases(){
		final Object positiveResult = "string";

		Map<Class<?>,
			Function<MemberWrap<Object,
								Object>,
					 Object>> runMap = new LinkedHashMap<>();
		runMap.put( Executable.class,
					wrap-> wrap.runByExecutable( exe-> positiveResult ) );
		runMap.put( Member.class,
					wrap-> wrap.runByMember( mem-> positiveResult ) );

		Set<AccessibleObject> possValues = new LinkedHashSet<>();
		possValues.add( null );
		possValues.add( MockClass.getMethod() );
		possValues.add( MockClass.getField() );

		return runMap.entrySet()
					 .stream()
					 .flatMap( inherianceFunc-> possValues.stream()
														  .map( type-> new Object[] { String.format( "Run on %s - on %s - %s",
																									 inherianceFunc.getKey()
																												   .getName(),
																									 type == null ? "empty"
																												  : type,
																									 type != null && inherianceFunc.getKey()
																																   .isAssignableFrom( type.getClass() ) ? "positive"
																																										: "negative" ),
																					  type == null ? new MemberWrap<Object,
																													Object>()
																								   : new MemberWrap<Object,
																													Object>( type ),
																					  inherianceFunc.getValue(),
																					  type != null && inherianceFunc.getKey()
																													.isAssignableFrom( type.getClass() ) ? positiveResult
																																						 : null } ) )
					 .collect( Collectors.toList() );
	}

	static class MockClass {

		Integer field;

		void method(){
			// empty method
		}

		static Method
			   getMethod(){
			try{
				return MockClass.class.getDeclaredMethod( "method" );
			}catch( NoSuchMethodException | SecurityException e ){
				throw new NullPointerException( "target Method<method()> was not found or accessible in MockClass class."
												+ e.getMessage() );
			}
		}

		static Field
			   getField(){
			try{
				return MockClass.class.getDeclaredField( "field" );
			}catch( NoSuchFieldException | SecurityException e ){
				throw new NullPointerException( "target Field<field> was not found or accessible in MockClass class."
												+ e.getMessage() );
			}
		}
	}
}
