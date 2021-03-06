package org.iag.utility.mirror;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * MemberWrap simplifies the reflection Api. This object is innate immutable,
 * however due to {@link AccessibleObject#setAccessible(boolean)} and
 * {@link MemberWrap#setModifier(int)}; it is possible to work around the
 * security measures put in place.
 * 
 * <p>
 * MemberWrap aims to simplify
 * <ul>
 * <li>Casting - using generics to roughly keep track of the declared
 * Classes</li>
 * <li>Modifiers - using {@link Modifier} methods</li>
 * <li>Simplify - {@link Field}, {@link Method}, {@link Constructor} all inherit
 * {@link AccessibleObject}, {@link AnnotatedElement}, {@link Member}, while
 * just {@link Method} and {@link Constructor} inherit {@link Executable} and
 * {@link GenericDeclaration}. Third party Apis put preference on which
 * Reflection class they use, {@link Hibernate} prefers placing
 * {@link Annotation}s on {@link Field}s, while Jackson prefers
 * {@link Method}s. Thus to help simplify thus different apis that all use
 * java-reflection.</li>
 * </ul>
 * 
 * @author Isaac A. Gainey
 *
 * @param <C>
 *            the [expected] declaring class
 * @param <V>
 *            the [expected] value type
 */
public class MemberWrap< C,
						 V >
					   implements
					   AnnotatedElement,
					   GenericDeclaration,
					   Member {
	/**
	 * Can be {@link Field}, {@link Method} or {@link Constructor}.
	 * 
	 */
	@Nullable
	private final AccessibleObject member;

	/**
	 * Creates an empty MemberWrap.
	 */
	public MemberWrap(){
		this( (AccessibleObject) null );
	}

	/**
	 * Sets the {@code local} {@link member} to the {@code parameter}.
	 * 
	 * @param member
	 */
	public MemberWrap( @Nullable AccessibleObject member ){
		this.member = member;
	}

	/**
	 * Sets the {@code local} {@link member} to the first non-null element.
	 * 
	 * @see Stream#of(Object...)
	 * 
	 * @param members
	 */
	public MemberWrap( @NonNull AccessibleObject... members ){
		this( Stream.of( members ) );
	}

	/**
	 * Sets the {@code local} {@link member} to the first non-null element.
	 * 
	 * @see Collection#stream()
	 * 
	 * @param members
	 */
	public MemberWrap( @NonNull Collection<? extends AccessibleObject> collection ){
		this( collection.stream() );
	}

	/**
	 * Sets the {@code local} {@link member} to the first non-null element.
	 * 
	 * @param stream
	 */
	private MemberWrap( @NonNull Stream<? extends AccessibleObject> stream ){
		this( stream.filter( Objects::nonNull )
					.findFirst()
					.orElseGet( null ) );
	}

	/**
	 * Sets the {@code local} {@link member} to either the {@link Field} in
	 * {@code clazz} with that matches the parameter String, or a {@link Method}
	 * in {@code clazz} with no parameters and has a name in [ name, getName,
	 * hasName, isName ].
	 * 
	 * @see Class#getField(String)
	 * @see Class#getDeclaredField(String)
	 * @see Class#getMethod(String, Class...)
	 * @see Class#getDeclaredMethod(String, Class...)
	 * 
	 * @see MemberWrap#getField(Class, String)
	 * @see MemberWrap#getGetterMethod(Class, String)
	 * 
	 * @param clazz
	 * @param name
	 */
	public MemberWrap( @NonNull Class<C> clazz,
					   @NonNull String name ){
		this( getField( clazz,
						name ),
			  getGetterMethod( clazz,
							   name ) );
	}

	@Override
	public < T extends Annotation >
		   T
		   getAnnotation( Class<T> arg0 ){
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Annotation[]
		   getAnnotations(){
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Annotation[]
		   getDeclaredAnnotations(){
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public @Nullable Class<? super C>
		   getDeclaringClass(){
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int
		   getModifiers(){
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public @NonNull String
		   getName(){
		// TODO Auto-generated method stub
		return "null";
	}

	@Override
	public TypeVariable<?>[]
		   getTypeParameters(){
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean
		   isSynthetic(){
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * 
	 * @param onMember
	 * @return if {@link member} is not {@code null}, then
	 *         onMembers.apply(Object), else {@code null}.
	 */
	protected < O >
			  @Nullable O
			  runByAccessible( @NonNull Function<? super AccessibleObject,
												 ? extends O> onMember ){
		return runByAccessible( onMember,
								null );
	}

	/**
	 * 
	 * @param onMember
	 * @return if {@link member} is not {@code null}, then
	 *         onMembers.apply(Object), else onNull.
	 */
	protected < O >
			  O
			  runByAccessible( @NonNull Function<? super AccessibleObject,
												 ? extends O> onMember,
							   O onNull ){
		return member != null ? onMember.apply( member )
							  : onNull;
	}

	/**
	 * 
	 * @param onMember
	 * @return if {@link member} is not {@code null} and is either
	 *         {@link Method} or {@link Constructor}, then
	 *         onMembers.apply(Object), else null.
	 */
	protected < O >
			  @Nullable O
			  runByExecutable( @NonNull Function<? super Executable,
												 ? extends O> onExecutable ){
		return runByExecutable( onExecutable,
								null );
	}

	/**
	 * 
	 * @param onMember
	 * @return if {@link member} is not {@code null} and is either
	 *         {@link Method} or {@link Constructor}, then
	 *         onMembers.apply(Object), else onNull.
	 */
	protected < O >
			  O
			  runByExecutable( @NonNull Function<? super Executable,
												 ? extends O> onExecutable,
							   O onNull ){
		if( member instanceof Executable ){
			return onExecutable.apply( (Executable) member );
		}
		return onNull;
	}

	protected < O >
			  @Nullable O
			  runByMember( @NonNull Function<? super Member,
											 ? extends O> onMember ){
		return runByExecutable( onMember,
								null );
	}

	/**
	 * 
	 * @param onMember
	 * @return if {@link member} is not {@code null} , then
	 *         onMembers.apply(Object), else onNull.
	 */
	protected < O >
			  O
			  runByMember( @NonNull Function<? super Member,
											 ? extends O> onMember,
						   O onNull ){
		if( member instanceof Member ){
			return onMember.apply( (Member) member );
		}
		return onNull;
	}

	/**
	 * The Function used is dependent on the declare class of {@link member}. If
	 * {@link member} is a {@link Field} then will return ifField's output. If
	 * {@link member} is a {@link Method} then will return ifMethod's output. If
	 * {@link member} is a {@link Constructor} then will return ifConstructor's
	 * output.
	 * 
	 * @param ifField
	 * @param ifMethod
	 * @param ifConstructor
	 * 
	 * @return the output of the {@link Function} that can be applied to
	 *         {@link member}. If {@link member} is {@code null} then returns
	 *         {@code null}.
	 */
	protected < O >
			  @Nullable O
			  runByDeclaration( @Nullable Function<? super Field,
												   ? extends O> ifField,
								@Nullable Function<? super Method,
												   ? extends O> ifMethod,
								@Nullable Function<? super Constructor<?>,
												   ? extends O> ifConstructor ){
		return runByDeclaration( ifField,
								 ifMethod,
								 ifConstructor,
								 null );
	}

	/**
	 * The Function used is dependent on the declare class of {@link member}. If
	 * {@link member} is a {@link Field} then will return ifField's output. If
	 * {@link member} is a {@link Method} then will return ifMethod's output. If
	 * {@link member} is a {@link Constructor} then will return ifConstructor's
	 * output.
	 * 
	 * @param ifField
	 * @param ifMethod
	 * @param ifConstructor
	 * @param onNull
	 * 
	 * @return the output of the {@link Function} that can be applied to
	 *         {@link member}. If {@link member} is {@code null} then returns
	 *         onNull.
	 */
	protected < O >
			  O
			  runByDeclaration( @Nullable Function<? super Field,
												   ? extends O> ifField,
								@Nullable Function<? super Method,
												   ? extends O> ifMethod,
								@Nullable Function<? super Constructor<?>,
												   ? extends O> ifConstructor,
								@Nullable O onNull ){
		if( member instanceof Field && ifField != null ){
			return ifField.apply( (Field) member );
		}
		if( member instanceof Method && ifMethod != null ){
			return ifMethod.apply( (Method) member );
		}
		if( member instanceof Constructor && ifConstructor != null ){
			return ifConstructor.apply( (Constructor<?>) member );
		}
		return onNull;
	}

	/**
	 * @see Class#getField(String)
	 * @see Class#getDeclaredField(String)
	 * 
	 * @param clazz
	 * @param fieldName
	 * @return a {@link Field} that matches whose {@link Member#getName()} is
	 *         equals to fieldName.
	 */
	protected static @Nullable Field
			  getField( @NonNull Class<?> clazz,
						@NonNull String fieldName ){
		try{
			return clazz.getField( fieldName );
		}catch( NoSuchFieldException nsfe ){
			try{
				return clazz.getDeclaredField( fieldName );
			}catch( NoSuchFieldException | SecurityException e1 ){
				return null;
			}
		}catch( SecurityException se ){
			return null;
		}
	}

	/**
	 * If there exists {@link Method}s that
	 * {@link MemberWrap#getGetterMethod(Class, String)} and
	 * {@link MemberWrap#getSetterMethod(Class, String, Class)} would return,
	 * preference is given to the Getter {@link Method}.
	 * 
	 * @see MemberWrap#getGetterMethod(Class, String)
	 * @see MemberWrap#getSetterMethod(Class, String, Class)
	 * 
	 * @param clazz
	 * @param methodName
	 * @param parameter
	 * @return {@link MemberWrap#getGetterMethod(Class, String)}, if it returns
	 *         {@code null}, and {@code parameter} is not {@code null}, then
	 *         returns {@link MemberWrap#getSetterMethod(Class, String, Class)}
	 */
	static @Nullable Method
		   getMethod( @NonNull Class<?> clazz,
					  @NonNull String methodName,
					  @Nullable Class<?> parameter ){
		Method method = getGetterMethod( clazz,
										 methodName );
		if( method == null
			&& parameter != null ){
			method = getSetterMethod( clazz,
									  methodName,
									  parameter );
		}
		return method;
	}

	/**
	 * @see MemberWrap#getRawMethod(Class, String, Class...)
	 * @see MemberWrap#formatGetMethodName(String)
	 * @see MemberWrap#formatHasMethodName(String)
	 * @see MemberWrap#formatIsMethodName(String)
	 * 
	 * @param clazz
	 * @param methodName
	 * @return the first {@link Method} from
	 *         {@link MemberWrap#getRawMethod(Class, String, Class...)} that is
	 *         not null, has
	 *         no parameters, and it's name is in [ methodName, getMethodName,
	 *         hasMethodName, isMethodName ]. If none exist, returns
	 *         {@code null}.
	 */
	protected static @Nullable Method
			  getGetterMethod( @NonNull Class<?> clazz,
							   @NonNull String methodName ){
		return Stream.of( (Supplier<String>) ()-> methodName,
						  (Supplier<String>) ()-> formatGetMethodName( methodName ),
						  (Supplier<String>) ()-> formatHasMethodName( methodName ),
						  (Supplier<String>) ()-> formatIsMethodName( methodName ) )
					 .map( name-> getRawMethod( clazz,
												name.get() ) )
					 .filter( Objects::nonNull )
					 .findFirst()
					 .orElse( null );
	}

	/**
	 * @see MemberWrap#getRawMethod(Class, String, Class...)
	 * @see MemberWrap#formatSetMethodName(String)
	 * 
	 * @param clazz
	 * @param methodName
	 * @param parameterType
	 * @return the first {@link Method} from
	 *         {@link MemberWrap#getRawMethod(Class, String, Class...)} that is
	 *         not null, has no parameters, and it's name is in [ methodName,
	 *         setMethodName ]. If none exist, returns {@code null}.
	 */
	protected static @Nullable Method
			  getSetterMethod( @NonNull Class<?> clazz,
							   @NonNull String methodName,
							   @NonNull Class<?> parameterType ){
		return Stream.of( (Supplier<String>) ()-> methodName,
						  (Supplier<String>) ()-> formatSetMethodName( methodName ) )
					 .map( name-> getRawMethod( clazz,
												name.get(),
												parameterType ) )
					 .filter( Objects::nonNull )
					 .findFirst()
					 .orElse( null );
	}

	/**
	 * 
	 * @param methodJsonName
	 * @return "get" + MethodJsonName
	 */
	private static @NonNull String
			formatGetMethodName( @NonNull String methodJsonName ){
		return formatMethodName( "get",
								 methodJsonName );
	}

	/**
	 * 
	 * @param methodJsonName
	 * @return "has" + MethodJsonName
	 */
	private static @NonNull String
			formatHasMethodName( @NonNull String methodJsonName ){
		return formatMethodName( "has",
								 methodJsonName );
	}

	/**
	 * 
	 * @param methodJsonName
	 * @return "is" + MethodJsonName
	 */
	private static @NonNull String
			formatIsMethodName( @NonNull String methodJsonName ){
		return formatMethodName( "is",
								 methodJsonName );
	}

	/**
	 * 
	 * @param methodJsonName
	 * @return "set" + MethodJsonName
	 */
	private static @NonNull String
			formatSetMethodName( @NonNull String methodJsonName ){
		return formatMethodName( "set",
								 methodJsonName );
	}

	/**
	 * 
	 * @param prefix
	 * @param methodJsonName
	 * @return prefix + MethodJsonName
	 */
	private static @NonNull String
			formatMethodName( @NonNull String prefix,
							  @NonNull String methodJsonName ){
		return String.format( "%s%s%s",
							  prefix,
							  methodJsonName.substring( 0,
														1 )
											.toUpperCase(),
							  methodJsonName.substring( 1 ) );
	}

	/**
	 * 
	 * @see Class#getMethod(String, Class...)
	 * @see Class#getDeclaredMethod(String, Class...)
	 * 
	 * @param clazz
	 * @param methodName
	 * @param parameters
	 * @return a Method that matches the methodName and the parameter
	 *         declaration; from either {@link clazz#getMethod(String,Class...)}
	 *         or {@link clazz#getDeclaredMethod(String,Class...)}
	 */
	protected static @Nullable Method
			  getRawMethod( @NonNull Class<?> clazz,
							@NonNull String methodName,
							@Nullable Class<?>... parameters ){
		try{
			return clazz.getMethod( methodName,
									parameters );
		}catch( NoSuchMethodException nsme ){
			try{
				return clazz.getDeclaredMethod( methodName,
												parameters );
			}catch( NoSuchMethodException | SecurityException e ){
				return null;
			}
		}catch( SecurityException se ){
			return null;
		}
	}
}
