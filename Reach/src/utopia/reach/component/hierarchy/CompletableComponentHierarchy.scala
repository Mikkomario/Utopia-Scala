package utopia.reach.component.hierarchy

import utopia.reach.component.template.ReachComponentLike

/**
  * Common trait for component hierarchies that can be completed (connected to a parent) after they have been created
  * @author Mikko Hilpinen
  * @since 7.10.2020, v2
  */
trait CompletableComponentHierarchy extends ComponentHierarchy
{
	/**
	  * Completes this component hierarchy by attaching it to a parent component
	  * @param parent Parent component to connect to
	  * @throws IllegalStateException If this hierarchy was already completed (These hierarchies mustn't be completed twice)
	  */
	@throws[IllegalStateException]("If already completed previously")
	def complete(parent: ReachComponentLike): Unit
}
