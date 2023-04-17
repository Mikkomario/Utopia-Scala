package utopia.reach.component.factory

/**
  * A common trait for instances that produce new container builders. Container builders are used for creating
  * containers and their content in a close sequence
  * @author Mikko Hilpinen
  * @since 11.10.2020, v0.1
  */
trait BuilderFactory[+Builder[+_]]
{
	/**
	  * Creates a new container builder
	  * @param contentFactory A factory that produces container content factories
	  * @tparam FF Type of content factory factory used
	  * @return A new container builder
	  */
	def build[FF](contentFactory: ComponentFactoryFactory[FF]): Builder[FF]
}
