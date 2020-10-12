package utopia.reflection.component.reach.factory

/**
  * A common trait for instances that produce new container builders. Container builders are used for creating
  * containers and their content in a close sequence
  * @author Mikko Hilpinen
  * @since 11.10.2020, v2
  */
trait BuilderFactory[+Builder[+CF]]
{
	/**
	  * Creates a new container builder
	  * @param contentFactory A factory that produces container content factories
	  * @tparam CF Type of content factory used
	  * @return A new container builder
	  */
	def builder[CF](contentFactory: ComponentFactoryFactory[CF]): Builder[CF]
}
