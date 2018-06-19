package org.iag.utility.mirror;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

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
					.orElse( null ) );
	}

	/**
	 * Sets the {@code local} {@link member} to the first non-null element.
	 * 
	 * @param arr
	 */
	@SafeVarargs
	protected MemberWrap( @NonNull Supplier<? extends AccessibleObject>... arr ){
		this( Stream.of( arr )
					.map( Supplier::get ) );
	}

	/**
	 * Sets the {@code local} {@link member} to either a {@link Method}
	 * in {@code clazz} with no parameters and has a name in [ name, getName,
	 * hasName, isName ] or the {@link Field} in {@code clazz} with that matches
	 * the parameter String.
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
		this( ()-> getGetterMethod( clazz,
									name ),
			  ()-> getField( clazz,
							 name ) );
	}

	/**
	 * Sets the {@code local} {@link member} to the first of: a Method with one
	 * parameter that matches valClazz and whom's return type {@link void} or
	 * objClazz and, whom's name is in [ name, setName ]; or a Field whose name
	 * matches name; or a method who has no parameters, and name is in [name,
	 * getName, isName, hasName], and return type is not {@link void}, or a
	 * {@link Constructor} in valClazz that takes objClazz as the only
	 * parameter.
	 * 
	 * @param objClazz
	 * @param name
	 * @param valClazz
	 */
	public MemberWrap( @NonNull Class<C> objClazz,
					   @NonNull String name,
					   @NonNull Class<V> valClazz ){
		this( ()-> getSetterMethod( objClazz,
									name,
									valClazz ),
			  ()-> getField( objClazz,
							 name ),
			  ()-> getGetterMethod( objClazz,
									name ),
			  ()-> getConstructor( valClazz,
								   objClazz ) );
	}

	/**
	 * Sets member to the Constructor of constructedClass with one parameter of
	 * paramClass.
	 * 
	 * @param paramClass
	 * @param constructedClass
	 */
	public MemberWrap( @NonNull Class<C> paramClass,
					   @NonNull Class<V> constructedClass ){
		this( getConstructor( constructedClass,
							  paramClass ) );
	}

	/**
	 * 
	 * @return {@code true} if and only if {@link member} is a
	 *         {@link Constructor}
	 */
	public boolean
		   isConstructor(){
		return getMember() instanceof Constructor;
	}

	/**
	 * 
	 * @return {@code true} if and only if {@link member} is {@code null}
	 */
	public boolean
		   isEmpty(){
		return getMember() == null;
	}

	/**
	 * 
	 * @return {@code true} if and only if {@link member} is a {@link Field}
	 */
	public boolean
		   isField(){
		return getMember() instanceof Field;
	}

	/**
	 * 
	 * @return {@code true} if and only if {@link member} is a {@link Method}
	 */
	public boolean
		   isMethod(){
		return getMember() instanceof Method;
	}

	/**
	 * 
	 * @return {@code true} if {@link member} is a {@link Method} or
	 *         {@link Constructor}
	 */
	public boolean
		   isExecutable(){
		return getMember() instanceof Executable;
	}

	/**
	 * 
	 * @return {@code true} if {@link member} is not {@code null}
	 */
	public boolean
		   isMember(){
		return getMember() != null;
	}

	/**
	 * 
	 * @return member
	 */
	protected final @Nullable AccessibleObject
			  getMember(){
		return member;
	}

	/**
	 * @see MemberWrap#isConstructor()
	 * 
	 * @return {@link member} up-cast as {@link Constructor} if and only if
	 *         {@link member} is a {@link Constructor}, else {@code null}
	 */
	public @Nullable Constructor<?>
		   getConstructor(){
		return isConstructor() ? (Constructor<?>) getMember()
							   : null;
	}

	/**
	 * 
	 * @return {@link member} up-cast as {@link Executable} if and only if
	 *         {@link member} is a {@link Constructor} or {@link Method}, else
	 *         {@code null}
	 */
	public @Nullable Executable
		   getExecutable(){
		return isExecutable() ? (Executable) getMember()
							  : null;
	}

	/**
	 * @see MemberWrap#isField()
	 * 
	 * @return {@link member} up-cast as {@link Field} if and only if
	 *         {@link member} is a {@link Field}, else {@code null}
	 */
	public @Nullable Field
		   getField(){
		return isField() ? (Field) getMember()
						 : null;
	}

	/**
	 * @see MemberWrap#isMethod()
	 * 
	 * @return {@link member} up-cast as {@link Method} if and only if
	 *         {@link member} is a {@link Method}, else {@code null}
	 */
	public @Nullable Method
		   getMethod(){
		return isMethod() ? (Method) getMember()
						  : null;
	}

	/**
	 * 
	 * @return {@code true} if {@link member} is a {@link Field}; or is a
	 *         {@link Method} and {@link Method#getParameterCount()}
	 *         {@code == 1} and {@link Method#getReturnType()} is {@link void}
	 *         or {@link Method#getDeclaringClass()}; else {@code false}
	 */
	public boolean
		   isSettable(){
		return runByDeclaration( field-> true,
								 MemberWrap::isSetterMethod,
								 constructor-> false,
								 false );
	}

	/**
	 * 
	 * @return {@code true} if {@link member} is a {@link Field} or
	 *         {@link Constructor} or a {@link Method} with
	 *         {@link Method#getParameterCount()} is {@code 0} and
	 *         {@link Method#getReturnType()} isn't {@link void.class}
	 */
	public boolean
		   isGettable(){
		return runByDeclaration( field-> true,
								 MemberWrap::isGetterMethod,
								 constructor-> true,
								 false );
	}

	/**
	 * This method will call {@link Field#get(Object)},
	 * {@link Method#invoke(Object, Object...)}, or
	 * {@link Constructor#newInstance(Object...)}.
	 * 
	 * <p>
	 * If {@link member} has
	 * {@link AccessibleObject#isAccessible()} {@code false}, this method will
	 * <b>not</b> call {@link AccessibleObject#setAccessible(boolean)}
	 * 
	 * <p>
	 * If {@link member} is a {@link Constructor} and
	 * {@link Constructor#getParameterCount()} {@code == 0} then obj can be
	 * {@code null} and a val will be returned.
	 * 
	 * <p>
	 * If {@link member} is {@code static} then obj can be {@code null} and the
	 * val can be returned.
	 * 
	 * 
	 * @param obj
	 * @return {@code null} if {@link member} is {@code null} or an Exception is
	 *         thrown. Else returns {@link Field#get(Object)}, or
	 *         {@link Method#invoke(Object, Object...)}, or
	 *         {@link Constructor#newInstance(Object...)} depending on
	 *         {@link member}'s true type.
	 */
	@SuppressWarnings( "unchecked" )
	public @Nullable V
		   get( @NonNull C obj ){
		return (V) runByDeclaration(
									 field-> {
										 try{
											 return field.get( obj );
										 }catch( IllegalArgumentException
												 | IllegalAccessException e ){
											 return null;
										 }
									 },
									 method-> {
										 try{
											 return method.invoke( obj );
										 }catch( IllegalAccessException
												 | IllegalArgumentException
												 | InvocationTargetException e ){
											 return null;
										 }
									 },
									 constructor-> {
										 try{
											 return constructor.getParameterCount() == 0 ? constructor.newInstance()
																						 : constructor.newInstance( obj );
										 }catch( InstantiationException
												 | IllegalAccessException
												 | IllegalArgumentException
												 | InvocationTargetException e ){
											 return null;
										 }
									 } );
	}

	/**
	 * 
	 * This method will call {@link Field#get(Object)},
	 * {@link Method#invoke(Object, Object...)}, or
	 * {@link Constructor#newInstance(Object...)}.
	 * 
	 * <p>
	 * If {@link member} has
	 * {@link AccessibleObject#isAccessible()} {@code false}, this method will
	 * <b>not</b> call {@link AccessibleObject#setAccessible(boolean)}
	 * 
	 * <p>
	 * If {@link member} is a {@link Constructor} and
	 * {@link Constructor#getParameterCount()} {@code == 0} then obj can be
	 * {@code null} and a val will be returned.
	 * 
	 * <p>
	 * If {@link member} is {@code static} then obj can be {@code null} and the
	 * value will be returned.
	 * 
	 * @param obj
	 * @return {@code null} if {@link member} is {@code null}. Else returns
	 *         {@link Field#get(Object)}, or
	 *         {@link Method#invoke(Object, Object...)}, or
	 *         {@link Constructor#newInstance(Object...)} depending on
	 *         {@link member}'s true type.
	 * 
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws InstantiationException
	 */
	@SuppressWarnings( "unchecked" )
	public @Nullable V
		   getThrow( @NonNull C obj ) throws IllegalArgumentException,
									  IllegalAccessException,
									  InvocationTargetException,
									  InstantiationException{
		if( isField() ){
			return (V) getField().get( obj );
		}
		if( isMethod() ){
			return (V) getMethod().invoke( obj );
		}
		if( isConstructor() ){
			final Constructor<?> constructor = getConstructor();
			if( constructor.getParameterCount() == 0 ){
				return (V) constructor.newInstance();
			}
			if( constructor.getParameterCount() == 1
				&& constructor.getParameterTypes()[ 0 ].isAssignableFrom( obj.getClass() ) ){
				return (V) constructor.newInstance( obj );
			}
			throw new IllegalArgumentException( String.format( "Constructor<%s> is expected to have one parameter that can take %s or no parameters.",
															   getSignature(),
															   obj.getClass() ) );
		}
		return null;
	}

	/**
	 * 
	 * This method will call {@link Field#set(Object, Object)} or
	 * {@link Method#invoke(Object, Object...)}.
	 * 
	 * <p>
	 * If {@link member} has
	 * {@link AccessibleObject#isAccessible()} {@code false}, this method will
	 * <b>not</b> call {@link AccessibleObject#setAccessible(boolean)}
	 * 
	 * @param obj
	 * @param val
	 * @return {@code true} if and only if {@link Field#set(Object, Object)} or
	 *         {@link Method#invoke(Object, Object...)} did not throw
	 *         {@link IllegalArgumentException}, {@link IllegalAccessExpection},
	 *         {@link InvocationTargetException} and {@link member} is not
	 *         {@code null}.
	 */
	public boolean
		   set( @NonNull C obj,
				@Nullable V val ){
		return runByDeclaration(
								 field-> {
									 try{
										 field.set( obj,
													val );
										 return true;
									 }catch( IllegalArgumentException | IllegalAccessException e ){
										 return false;
									 }
								 },
								 method-> {
									 try{
										 method.invoke( obj,
														val );
										 return true;
									 }catch( IllegalAccessException
											 | IllegalArgumentException
											 | InvocationTargetException e ){
										 return false;
									 }
								 },
								 null,
								 false );
	}

	/**
	 * 
	 * This method will call {@link Field#set(Object, Object)} or
	 * {@link Method#invoke(Object, Object...)}.
	 * 
	 * <p>
	 * If {@link member} has
	 * {@link AccessibleObject#isAccessible()} {@code false}, this method will
	 * <b>not</b> call {@link AccessibleObject#setAccessible(boolean)}
	 * 
	 * @param obj
	 * @param val
	 * @return {@code true} if and only if {@link member} is not {@code null}.
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	public boolean
		   setThrow( @NonNull C obj,
					 @Nullable V val ) throws IllegalArgumentException,
									   IllegalAccessException,
									   InvocationTargetException{
		if( isField() ){
			getField().set( obj,
							val );
			return true;
		}
		if( isMethod() ){
			getMethod().invoke( obj,
								val );
			return true;
		}
		return false;
	}

	/**
	 * @see java.lang.reflect.AnnotatedElement#getAnnotation(java.lang.Class)
	 * @return if {@link member} is {@code null} then {@code null}, else
	 *         {@link AccessibleObject#getAnnotation(Class)}
	 */
	@Override
	public < T extends Annotation >
		   @Nullable T
		   getAnnotation( @NonNull Class<T> arg0 ){
		return isMember() ? getMember().getAnnotation( arg0 )
						  : null;
	}

	/**
	 * 
	 * @return if {@link member} is not a {@link Method} or {@link Constructor},
	 *         then {@code null}, else
	 *         {@link Executable#getAnnotatedExceptionTypes()}
	 */
	public @Nullable AnnotatedType[]
		   getAnnotatedExceptionTypes(){
		return runByExecutable( Executable::getAnnotatedExceptionTypes,
								new AnnotatedType[0] );
	}

	/**
	 * 
	 * @return if {@link member} is not a {@link Method} or {@link Constructor},
	 *         then {@code null}, else
	 *         {@link Executable#getAnnotatedParameterTypes()}
	 */
	public @Nullable AnnotatedType[]
		   getAnnotatedParameterTypes(){
		return runByExecutable( Executable::getAnnotatedParameterTypes,
								new AnnotatedType[0] );
	}

	/**
	 * 
	 * @return if {@link member} is not a {@link Method} or {@link Constructor},
	 *         then {@code null}, else
	 *         {@link Executable#getAnnotatedReturnType()}
	 */
	public @Nullable AnnotatedType
		   getAnnotatedReceiverType(){
		return runByExecutable( Executable::getAnnotatedReceiverType );
	}

	/**
	 * 
	 * @return if {@link member} is not a {@link Method} or {@link Constructor},
	 *         then {@code null}, else
	 *         {@link Executable#getAnnotatedReturnType()}
	 */
	public @Nullable AnnotatedType
		   getAnnotatedReturnType(){
		return runByExecutable( Executable::getAnnotatedReturnType );
	}

	/**
	 * @see java.lang.reflect.AnnotatedElement#getAnnotations()
	 * @return if {@link member} is {@code null} then an empty array, else
	 *         {@link AccessibleObject#getAnnotations()}
	 */
	@Override
	public @NonNull Annotation[]
		   getAnnotations(){
		return isMember() ? getMember().getAnnotations()
						  : new Annotation[0];
	}

	/**
	 * @see java.lang.reflect.AnnotatedElement#getAnnotationsByType(java.lang.Class)
	 * @return {@link member} is not {@code null} then
	 *         {@link AccessibleObject#getAnnotationsByType(Class)}, else an
	 *         empty array.
	 */
	@SuppressWarnings( "unchecked" )
	public < T extends Annotation >
		   @NonNull T[]
		   getAnnotationsByType( @NonNull Class<T> annotationClass ){
		return isMember() ? getMember().getAnnotationsByType( annotationClass )
						  : (T[]) new Annotation[0];
	}

	/**
	 * @see java.lang.reflect.AnnotatedElement#getDeclaredAnnotations()
	 * @return if {@link member} is {@code null} then an empty array, else
	 *         {@link AccessibleObject#getDeclaredAnnotations()}
	 */
	@Override
	public @NonNull Annotation[]
		   getDeclaredAnnotations(){
		return isMember() ? getMember().getDeclaredAnnotations()
						  : new Annotation[0];
	}

	/**
	 * @see java.lang.reflect.AnnotatedElement#getDeclaredAnnotationsByType(java.lang.Class)
	 * @return if {@link member} is {@code null} then an empty array, else
	 *         {@link AccessibleObject#getDeclaredAnnotationsByType()}
	 */
	@SuppressWarnings( "unchecked" )
	public < T extends Annotation >
		   @NonNull T[]
		   getDeclaredAnnotationsByType( @NonNull Class<T> annotationClass ){
		return isMember() ? getMember().getDeclaredAnnotationsByType( annotationClass )
						  : (T[]) new Annotation[0];
	}

	/**
	 * 
	 * @return if {@link member} is not a {@link Method} or {@link Constructor},
	 *         then an empty array, else {@link Executable#getExceptionTypes()}
	 */
	public @NonNull Class<?>[]
		   getExceptionTypes(){
		return runByExecutable( Executable::getExceptionTypes,
								new Class<?>[0] );
	}

	/**
	 * 
	 * @return if {@link member} is not a {@link Method} or {@link Constructor},
	 *         then an empty array, else
	 *         {@link Executable#getGenericExceptionTypes()}
	 */
	public @NonNull Type[]
		   getGenericExceptionTypes(){
		return runByExecutable( Executable::getGenericExceptionTypes,
								new Type[0] );
	}

	/**
	 * 
	 * @return if {@link member} is not a {@link Method} or {@link Constructor},
	 *         then an empty array, else
	 *         {@link Executable#getGenericParameterTypes()}
	 */
	public @NonNull Type[]
		   getGenericParameterTypes(){
		return runByExecutable( Executable::getGenericParameterTypes,
								new Type[0] );
	}

	/**
	 * @see java.lang.reflect.Member#getDeclaringClass()
	 * @return if {@link member} is {@code null} then {@code void.class}, else
	 *         {@link AccessibleObject#getDeclaringClass()}
	 */
	@SuppressWarnings( "unchecked" )
	@Override
	public @NonNull Class<? super C>
		   getDeclaringClass(){
		return (Class<? super C>) runByMember( Member::getDeclaringClass,
											   void.class );
	}

	/**
	 * @see java.lang.reflect.Member#getModifiers()
	 * 
	 * @return if the {@link member} is {@code null}, returns {@code 4096}, else
	 *         {@link Member#getModifiers()}
	 */
	@Override
	public @Min( 0 ) @Max( 4096 ) int
		   getModifiers(){
		return runByMember( Member::getModifiers,
							4096 );
	}

	/**
	 * @see java.lang.reflect.Member#getName()
	 * @return if the {@link member} is {@code null}, returns {@code "null"}
	 *         else {@link Member#getName()}
	 */
	@Override
	public @NonNull String
		   getName(){
		return runByMember( Member::getName,
							"null" );
	}

	/**
	 * 
	 * @return if {@link member} is not a {@link Method} or {@link Constructor},
	 *         then an empty array, else {@link Executable#getParameters()}
	 */
	public @NonNull Parameter[]
		   getParameters(){
		return runByExecutable( Executable::getParameters,
								new Parameter[0] );
	}

	/**
	 * 
	 * @return if {@link member} is not a {@link Method} or {@link Constructor},
	 *         then an empty two dim-array, else
	 *         {@link Executable#getParameterAnnotations()}
	 */
	public @NonNull Annotation[][]
		   getParameterAnnotations(){
		return runByExecutable( Executable::getParameterAnnotations,
								new Annotation[0][] );
	}

	/**
	 * 
	 * @return if {@link member} is not a {@link Method} or {@link Constructor},
	 *         then {@code -1}, else {@link Executable#getParameterCount()}
	 */
	public @Min( -1 ) int
		   getParameterCount(){
		return runByExecutable( Executable::getParameterCount,
								-1 );
	}

	/**
	 * @return if {@link member} is not a {@link Method} or {@link Constructor},
	 *         then an empty array, else {@link Executable#getParameterTypes()}
	 */
	public @NonNull Class<?>[]
		   getParameterTypes(){
		return runByExecutable( Executable::getParameterTypes,
								new Class<?>[0] );
	}

	/**
	 * @see java.lang.reflect.GenericDeclaration#getTypeParameters()
	 * @return if {@link member} is not a {@link Method} or {@link Constructor}
	 *         then an empty array, else
	 *         {@link GenericDeclaration#getTypeParameters()}
	 */
	@Override
	public @NonNull TypeVariable<?>[]
		   getTypeParameters(){
		return getMember() instanceof GenericDeclaration ? ((GenericDeclaration) getMember()).getTypeParameters()
														 : new TypeVariable<?>[0];
	}

	/**
	 * 
	 * @return a {@code new String} that attempts to copy the signature of the
	 *         {@link member}. If {@link member} is {@code null} then returns
	 *         {@code "null"}
	 */
	public @NonNull String
		   getSignature(){
		return isMember() ? String.format( "%s %s%s",
										   Modifier.toString( getModifiers() ),
										   getName(),
										   isExecutable() ? String.format( "(%s)",
																		   (Object[]) getParameterTypes() )
														  : "" )
						  : "null";
	}

	/**
	 * 
	 * @return {@code true} if and only if {@link member} is not {@code null}
	 *         and {@link AccessibleObject#isAccessible()} is {@code true}.
	 */
	public boolean
		   isAccessible(){
		return isMember() ? getMember().isAccessible()
						  : false;
	}

	/**
	 * @see java.lang.reflect.Member#isSynthetic()
	 * @return if {@link member} is {@code null} then {@code false}, else
	 *         {@link Member#isSynthetic}
	 */
	@Override
	public boolean
		   isSynthetic(){
		return runByMember( Member::isSynthetic,
							false );
	}

	/**
	 * @see java.lang.reflect.Executable#isVarArgs()
	 * @return if {@link member} is not a {@link Method} or {@link Constructor}
	 *         then {@code false}, else
	 *         {@link GenericDeclaration#isVarArgs()}
	 */
	public boolean
		   isVarArgs(){
		return runByExecutable( Executable::isVarArgs,
								false );
	}

	/**
	 * If {@link member} is not {@code null} then attempts to set the Accessible
	 * flag to the parameter.
	 * 
	 * @see AccessibleObject#setAccessible(boolean)
	 * @param flag
	 */
	public void
		   setAccessible( boolean flag ){
		if( isMember() )
			getMember().setAccessible( flag );
	}

	/**
	 * 
	 * @return if {@link member} is not a {@link Method} or {@link Constructor}
	 *         then {@code "null"}, else
	 *         {@link GenericDeclaration#toGenericString()}
	 */
	public @NonNull String
		   toGenericString(){
		return runByExecutable( Executable::toGenericString,
								"null" );
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
		return isExecutable() ? onExecutable.apply( getExecutable() )
							  : onNull;
	}

	/**
	 * 
	 * @param onMember
	 * @return if {@link member} is not {@code null}, then
	 *         {@link Function#apply(Object)}, else {@code null}.
	 */
	protected < O >
			  @Nullable O
			  runByMember( @NonNull Function<? super Member,
											 ? extends O> onMember ){
		return runByMember( onMember,
							null );
	}

	/**
	 * 
	 * @param onMember
	 * @return if {@link member} is not {@code null}, then
	 *         {@link Function#apply(Object)}, else onNull.
	 */
	protected < O >
			  O
			  runByMember( @NonNull Function<? super Member,
											 ? extends O> onMember,
						   O onNull ){
		return member instanceof Member ? onMember.apply( (Member) member )
										: onNull;
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
		if( isField() && ifField != null ){
			return ifField.apply( getField() );
		}
		if( isMethod() && ifMethod != null ){
			return ifMethod.apply( getMethod() );
		}
		if( isConstructor() && ifConstructor != null ){
			return ifConstructor.apply( getConstructor() );
		}
		return onNull;
	}

	protected void
			  runByDeclaration( @Nullable Consumer<? super Field> ifField,
								@Nullable Consumer<? super Method> ifMethod,
								@Nullable Consumer<? super Constructor<?>> ifConstructor ){
		if( isField() && ifField != null ){
			ifField.accept( getField() );
		}else if( isMethod() && ifMethod != null ){
			ifMethod.accept( getMethod() );
		}else if( isConstructor() && ifConstructor != null ){
			ifConstructor.accept( getConstructor() );
		}
	}

	/**
	 * @see java.lang.Object#toString()
	 * @return {@link MemberWrap#getSignature()}
	 */
	public @NonNull String
		   toString(){
		return getSignature();
	}

	/**
	 * @see java.lang.Object#hashCode()
	 * @return {@link Member#hashCode()} if {@link MemberWrap#isMember()} is
	 *         {@code true}, else {@code 0}.
	 */
	public int
		   hashCode(){
		return isMember() ? getMember().hashCode()
						  : 0;
	}

	/**
	 * @see Objects#equals(Object, Object)
	 * 
	 * @param obj
	 * @return {@code true} if {@link member} equals the obj or if obj is a
	 *         {@link MemberWrap} obj's {@link member}.
	 */
	public boolean
		   equals( Object obj ){
		if( obj instanceof MemberWrap ){
			return Objects.equals( this.getMember(),
								   ((MemberWrap<?,
												?>) obj).getMember() );
		}
		if( obj instanceof AccessibleObject ){
			return Objects.equals( this.getMember(),
								   obj );
		}
		return false;
	}

	/**
	 * @see Class#getMethods()
	 * @see Class#getFields()
	 * @see Class#getDeclaredMethods()
	 * @see Class#getDeclaredFields()
	 * 
	 * @param clazz
	 * @return a {@code new} {@link Stream} of {@link MemberWrap}ped
	 *         {@link Field}s and {@link Method}s of clazz.
	 */
	public static < C >
		   @NonNull Stream<MemberWrap<C,
									  Object>>
		   getMemberStream( @NonNull Class<C> clazz ){
		return Stream.concat( getFieldStream( clazz ),
							  getMethodStream( clazz ) );
	}

	/**
	 * @see Class#getFields()
	 * @see Class#getDeclaredFields()
	 * 
	 * @param clazz
	 * @return a {@code new} {@link Stream} of {@link MemberWrap}ped
	 *         {@link Field}s of clazz
	 */
	public static < C >
		   @NonNull Stream<MemberWrap<C,
									  Object>>
		   getFieldStream( @NonNull Class<C> clazz ){
		return Stream.concat( Stream.of( clazz.getFields() ),
							  Stream.of( clazz.getDeclaredFields() ) )
					 .distinct()
					 .map( MemberWrap::new );
	}

	/**
	 * @see Class#getMethods()
	 * @see Class#getDeclaredMethods()
	 * 
	 * @param clazz
	 * @return a {@code new} {@link Stream} of {@link MemberWrap}ped
	 *         {@link Method}s of clazz
	 */
	public static < C >
		   @NonNull Stream<MemberWrap<C,
									  Object>>
		   getMethodStream( @NonNull Class<C> clazz ){
		return Stream.concat( Stream.of( clazz.getMethods() ),
							  Stream.of( clazz.getDeclaredMethods() ) )
					 .distinct()
					 .map( MemberWrap::new );
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
			}catch( NoSuchFieldException
					| SecurityException e1 ){
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

	/**
	 * 
	 * @param method
	 * @return {@code true} if method has only one parameter and
	 *         {@link Method#getReturnType()} is {@link void.class} or the
	 *         {@link Method#getDeclaringClass()}.
	 */
	protected static boolean
			  isSetterMethod( @NonNull Method method ){
		return method.getParameterCount() == 1
			   && (method.getReturnType() == void.class
				   || method.getReturnType() == method.getDeclaringClass());
	}

	/**
	 * 
	 * @param method
	 * @return {@code true} if method has only zero parameters and
	 *         {@link Method#getReturnType()} is not {@link void.class}.
	 */
	protected static boolean
			  isGetterMethod( @NonNull Method method ){
		return method.getParameterCount() == 0
			   && method.getReturnType() != void.class;
	}

	/**
	 * 
	 * 
	 * @see Class#getConstructor(String, Class...)
	 * @see Class#getDeclaredConstructor(String, Class...)
	 * 
	 * @param constructorClass
	 * @param param
	 * @return the first of [ constructorClass.getConstructor(param),
	 *         constructorClass.getDeclaredConstructor(param),
	 *         constructorClass.getConstructor(),
	 *         constructorClass.getDeclaredConstructor() ]
	 */
	protected static < V >
			  @Nullable Constructor<V>
			  getConstructor( @NonNull Class<V> constructorClass,
							  @NonNull Class<?> param ){
		return Stream.of( (Supplier<Constructor<V>>) ()-> getParamConstructor( constructorClass,
																			   param ),
						  (Supplier<Constructor<V>>) ()-> getEmptyConstructor( constructorClass ) )
					 .map( Supplier::get )
					 .filter( Objects::nonNull )
					 .findFirst()
					 .orElse( null );
	}

	/**
	 * 
	 * @see Class#getConstructor(String, Class...)
	 * @see Class#getDeclaredConstructor(String, Class...)
	 * 
	 * @param clazz
	 * @param methodName
	 * @param parameters
	 * @return a Constructor that matches the parameter declaration; from either
	 *         {@link clazz#getConstructor(Class...)} or
	 *         {@link clazz#getDeclaredConstructor(Class...)}
	 */
	protected static < V >
			  @Nullable Constructor<V>
			  getParamConstructor( @NonNull Class<V> constructorClass,
								   @NonNull Class<?> param ){
		try{
			return constructorClass.getConstructor( param );
		}catch( NoSuchMethodException e ){
			return null;
		}catch( SecurityException e ){
			try{
				return constructorClass.getDeclaredConstructor( param );
			}catch( NoSuchMethodException
					| SecurityException e1 ){
				return null;
			}
		}
	}

	/**
	 * 
	 * @see Class#getConstructor(String, Class...)
	 * @see Class#getDeclaredConstructor(String, Class...)
	 * 
	 * @param clazz
	 * @param methodName
	 * @param parameters
	 * @return a Constructor that has no parameter declared; from either
	 *         {@link clazz#getConstructor(Class...)} or
	 *         {@link clazz#getDeclaredConstructor(Class...)}
	 */
	protected static < V >
			  @Nullable Constructor<V>
			  getEmptyConstructor( @NonNull Class<V> clazz ){
		try{
			return clazz.getConstructor();
		}catch( NoSuchMethodException e ){
			return null;
		}catch( SecurityException e ){
			try{
				return clazz.getDeclaredConstructor();
			}catch( NoSuchMethodException | SecurityException e1 ){
				return null;
			}
		}
	}
}
