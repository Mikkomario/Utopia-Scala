package utopia.nexus.rest.scalable

import utopia.nexus.rest.{Context, Resource}

/**
 * An abstract implementation of the ModularResource trait that allows outside extensions
 * @author Mikko Hilpinen
 * @since 18.6.2021, v1.6
 */
abstract class ExtendableResource[C <: Context, P] extends ModularResource[C, P]
{
	// ATTRIBUTES   ------------------------------
	
	private var customUseCaseImplementations = Vector[UseCaseImplementation[C, P]]()
	private var customFollowImplementations = Vector[FollowImplementation[C]]()
	
	
	// ABSTRACT ----------------------------------
	
	/**
	 * @return The use case implementations provided by this resource by default (called in order)
	 */
	protected def defaultUseCaseImplementations: Seq[UseCaseImplementation[C, P]]
	/**
	 * @return The follow implementations provided by this resource by default (called in order)
	 */
	protected def defaultFollowImplementations: Seq[FollowImplementation[C]]
	
	
	// IMPLEMENTED  ------------------------------
	
	override def useCaseImplementations =
		customUseCaseImplementations.view ++ defaultUseCaseImplementations
	override def followImplementations =
		customFollowImplementations.view ++ defaultFollowImplementations
	
	
	// OTHER    ----------------------------------
	
	/**
	 * Extends the capabilities of this node by adding a new use case implementation.
	 * The new implementation will be the first one to use for the supported method.
	 * @param useCaseImplementation A new use case implementation
	 */
	def extendWith(useCaseImplementation: UseCaseImplementation[C, P]) =
		customUseCaseImplementations = useCaseImplementation +: customUseCaseImplementations
	/**
	 * Extends the capabilities of this node by adding a new follow implementation.
	 * The new implementation will be the first one called.
	 * @param followImplementation A new follow implementation
	 */
	def extendWith(followImplementation: FollowImplementation[C]) =
		customFollowImplementations = followImplementation +: customFollowImplementations
	
	/**
	  * Adds a child resource under this resource
	  * @param childResource A child resource to add
	  */
	def addChild(childResource: => Resource[C]) = extendWith(FollowImplementation.withChild(childResource))
}
