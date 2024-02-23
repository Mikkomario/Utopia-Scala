package utopia.genesis.handling.template

import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.template.eventful.{Changing, FlagLike}
import FlagLike._

/**
  * An abstract Handler implementation which extends the Handleable trait
  * @author Mikko Hilpinen
  * @since 01/02/2024, v4.0
  */
abstract class DeepHandler[A <: Handleable](initialItems: IterableOnce[A] = Vector.empty,
                                            additionalHandleCondition: Changing[Boolean] = AlwaysTrue)
	extends AbstractHandler[A](initialItems) with Handleable
{
	// ATTRIBUTES   --------------------
	
	override val handleCondition: FlagLike = itemsPointer.readOnly.map { _.nonEmpty } && additionalHandleCondition
}
