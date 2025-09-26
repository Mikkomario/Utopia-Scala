package utopia.reach.component

/**
 * @author Mikko Hilpinen
 * @since 25.09.2025, v1.7
 */
package object wrapper
{
	@deprecated("Renamed to Creation", "v1.7")
	type ComponentCreationResult[+C, +R] = Creation[C, R]
	@deprecated("Renamed to ContainerCreation", "v1.7")
	type ComponentWrapResult[+P, +C, +R] = ContainerCreation[P, C, R]
	@deprecated("Renamed to ContextualOpenFactory", "v1.7")
	type ContextualOpenComponentFactory[N] = ContextualOpenFactory[N]
}
