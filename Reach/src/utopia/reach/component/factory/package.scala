package utopia.reach.component

/**
 * @author Mikko Hilpinen
 * @since 26.09.2025, v1.7
 */
package object factory
{
	@deprecated("Renamed to ComponentFactories", "v1.7")
	type ComponentFactoryFactory[+F] = ComponentFactories[F]
	@deprecated("Renamed to ContextualComponentFactories", "v1.7")
	type FromContextComponentFactoryFactory[-N, +CF] = ContextualComponentFactories[N, CF]
	@deprecated("Renamed to GenericContainerFactories", "v1.7")
	type FromGenericContextComponentFactoryFactory[-Top, +CF[X <: Top]] = GenericContainerFactories[Top, CF]
}
