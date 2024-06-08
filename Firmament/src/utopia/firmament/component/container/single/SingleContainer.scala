package utopia.firmament.component.container.single

import utopia.firmament.component.Component
import utopia.flow.collection.immutable.Single

/**
  * A common trait for containers that always contain a single component
  * @author Mikko Hilpinen
  * @since 4.10.2020, Reflection v2
  */
trait SingleContainer[+C <: Component] extends Component
{
	// ABSTRACT	---------------------
	
	/**
	  * @return The component within this container
	  */
	protected def content: C
	
	
	// IMPLEMENTED	-----------------
	
	override def children: Seq[C] = Single(content)
}
