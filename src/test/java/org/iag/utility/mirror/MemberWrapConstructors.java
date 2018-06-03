package org.iag.utility.mirror;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.Test;

public class MemberWrapConstructors {

	private static class MockObject {
		@SuppressWarnings( "unused" )
		public Object field;

		@SuppressWarnings( "unused" )
		public MockObject(){
			// empty constructor
		}

		@SuppressWarnings( "unused" )
		public void
			   method(){
			// empty method
		}

		static Field
			   getFieldObject() throws NoSuchFieldException,
								SecurityException{
			return MockObject.class.getField( "field" );
		}

		static Method
			   getMethodObject() throws NoSuchMethodException,
								 SecurityException{
			return MockObject.class.getMethod( "method" );
		}

		static Constructor<MockObject>
			   getConstructorObject() throws NoSuchMethodException,
									  SecurityException{
			return MockObject.class.getConstructor();
		}
	}

	@Test
	public void
		   emptyConstructor(){
		MemberWrap<MockObject,
				   Object> wrap = new MemberWrap<MockObject,
												 Object>();
		isEmpty( wrap );
	}

	@Test
	public void
		   fieldParam() throws NoSuchFieldException,
						SecurityException{
		MemberWrap<MockObject,
				   Object> wrap = new MemberWrap<MockObject,
												 Object>( MockObject.getFieldObject() );
		hasField( wrap );
	}

	@Test
	public void
		   methodParam() throws NoSuchMethodException,
						 SecurityException{
		MemberWrap<MockObject,
				   Object> wrap = new MemberWrap<MockObject,
												 Object>( MockObject.getMethodObject() );
		hasMethod( wrap );
	}

	@Test
	public void
		   constructorParam() throws NoSuchMethodException,
							  SecurityException{
		MemberWrap<MockObject,
				   Object> wrap = new MemberWrap<MockObject,
												 Object>( MockObject.getConstructorObject() );
		hasConstructor( wrap );
	}

	@Test
	public void
		   arrayConstructor() throws NoSuchFieldException,
							  SecurityException,
							  NoSuchMethodException{
		MemberWrap<MockObject,
				   Object> wrap = new MemberWrap<MockObject,
												 Object>( MockObject.getFieldObject(),
														  MockObject.getMethodObject(),
														  MockObject.getConstructorObject() );
		hasField( wrap );
	}

	@Test
	public void
		   emptyArray(){
		MemberWrap<MockObject,
				   Object> wrap = new MemberWrap<MockObject,
												 Object>( new AccessibleObject[0] );
		isEmpty( wrap );
	}

	@Test
	public void
		   arrayConstructorFirstElementNull() throws NoSuchMethodException,
											  SecurityException{
		MemberWrap<MockObject,
				   Object> wrap = new MemberWrap<MockObject,
												 Object>( null,
														  MockObject.getMethodObject() );
		hasMethod( wrap );
	}

	@Test
	public void
		   collectionConstructor() throws NoSuchFieldException,
								   SecurityException,
								   NoSuchMethodException{
		MemberWrap<MockObject,
				   Object> wrap = new MemberWrap<MockObject,
												 Object>( Arrays.asList( MockObject.getFieldObject(),
																		 MockObject.getMethodObject(),
																		 MockObject.getConstructorObject() ) );
		hasField( wrap );
	}

	@Test
	public void
		   emptyCollection(){
		MemberWrap<MockObject,
				   Object> wrap = new MemberWrap<MockObject,
												 Object>( Arrays.asList() );
		isEmpty( wrap );
	}

	@Test
	public void
		   findFieldConstructor(){
		MemberWrap<MockObject,
				   Object> wrap = new MemberWrap<MockObject,
												 Object>( MockObject.class,
														  "field" );
		hasField( wrap );
	}

	@Test
	public void
		   findMethodConstructor(){
		MemberWrap<MockObject,
				   Object> wrap = new MemberWrap<MockObject,
												 Object>( MockObject.class,
														  "method" );
		hasMethod( wrap );
	}

	@Test
	public void
		   findConstructorConstructor(){
		MemberWrap<MockObject,
				   MockObject> wrap = new MemberWrap<MockObject,
													 MockObject>( MockObject.class,
																  MockObject.class );
		hasConstructor( wrap );
	}

	@Test
	public void
		   findOnlyFieldMultiConstructor(){
		hasField( new MemberWrap<>( MockObject.class,
									"field",
									Object.class ) );
	}

	@Test
	public void
		   findOnlyMethoddMultiConstructor(){
		hasMethod( new MemberWrap<>( MockObject.class,
									"method",
									Object.class ) );
	}

	@Test
	public void
		   findOnlyConstructorMultiConstructor(){
		hasConstructor( new MemberWrap<>( MockObject.class,
									"red heiring",
									Object.class ) );
	}

	private static void
			isEmpty( MemberWrap<?,
								?> wrap ){
		assertNull( wrap.getMember() );
	}

	private static void
			hasField( MemberWrap<?,
								 ?> wrap ){
		AccessibleObject mem = wrap.getMember();
		assertNotNull( mem );
		assertTrue( mem instanceof Field );
	}

	private static void
			hasMethod( MemberWrap<?,
								  ?> wrap ){
		AccessibleObject mem = wrap.getMember();
		assertNotNull( mem );
		assertTrue( mem instanceof Method );
	}

	private static void
			hasConstructor( MemberWrap<?,
									   ?> wrap ){
		AccessibleObject mem = wrap.getMember();
		assertNotNull( mem );
		assertTrue( mem instanceof Constructor );
	}
}
