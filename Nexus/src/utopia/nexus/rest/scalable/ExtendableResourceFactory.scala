package utopia.nexus.rest.scalable

import utopia.access.http.Method
import utopia.nexus.rest.Context

/**
 * A common trait for resource factory classes that can be extended with custom functionality
 * @author Mikko Hilpinen
 * @since 25.6.2021, v1.6
 */
abstract class ExtendableResourceFactory[A, C <: Context, P, +R <: ExtendableResource[C, P]]
{
	// ATTRIBUTES   --------------------
	
	private var useCaseExtensions = Map[Method, Vector[A => UseCaseImplementation[C, P]]]()
	private var followExtensions = Vector[A => FollowImplementation[C]]()
	
	
	// ABSTRACT ------------------------
	
	/**
	 * Creates a new resource based on the specified additional parameter.
	 * Extensions will be added on top of the returned resource
	 * @param param A parameter passed for the resource
	 * @return A new extendable resource
	 */
	protected def buildBase(param: A): R
	
	
	// OTHER    ------------------------
	
	/**
	 * Creates a new resource that includes all factory extensions
	 * @param param Resource creation parameter
	 * @return A new resource
	 */
	def apply(param: A) =
	{
		// Creates the base resource version
		val resource = buildBase(param)
		// Applies extensions
		useCaseExtensions.foreach { case (method, useCases) =>
			useCases.foreach { useCase => resource.extendWith(method, useCase(param)) }
		}
		followExtensions.view.map { _(param) }.foreach(resource.extendWith)
		// Returns the extended resource
		resource
	}
	
	/**
	 * Adds a new use case to all resources that will be generated from this factory
	 * @param useCase A function that creates use cases based on the specified parameters
	 */
	def addUseCase(method: Method, useCase: A => UseCaseImplementation[C, P]) =
		useCaseExtensions += (method -> (useCase +: useCaseExtensions.getOrElse(method, Vector())))
	/**
	 * Adds a new follow implementation to all resources that will be generated from this factory
	 * @param follow A function that creates follow implementations based on the specified parameters
	 */
	def addFollow(follow: A => FollowImplementation[C]) = followExtensions :+= follow
}
