package utopia.reflection.container.swing

import utopia.reflection.component.swing.AwtComponentRelated

/**
  * This trait is extended by classes that are somehow related with an awt container
  * @author Mikko Hilpinen
  * @since 28.4.2019, v1+
  */
trait AwtContainerRelated extends AwtComponentRelated
{
	def component: java.awt.Container
}
