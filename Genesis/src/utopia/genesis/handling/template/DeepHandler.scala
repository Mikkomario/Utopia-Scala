package utopia.genesis.handling.template

import utopia.flow.collection.immutable.Empty
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.template.eventful.Flag

/**
  * An abstract Handler implementation which extends the Handleable trait
  * @author Mikko Hilpinen
  * @since 01/02/2024, v4.0
  */
abstract class DeepHandler[A <: Handleable](initialItems: IterableOnce[A] = Empty,
                                            additionalHandleCondition: Flag = AlwaysTrue)
                                           (implicit log: Logger)
	extends AbstractHandler[A](initialItems) with Handleable
{
	// ATTRIBUTES   --------------------
	
	override val handleCondition: Flag = itemsPointer.nonEmptyFlag && additionalHandleCondition
}
