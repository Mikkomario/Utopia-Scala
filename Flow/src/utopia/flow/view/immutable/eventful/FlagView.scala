package utopia.flow.view.immutable.eventful

import utopia.flow.view.mutable.eventful.Flag
import utopia.flow.view.template.eventful.{ChangingWrapper, FlagLike}

/**
  * An immutable view to a changing (mutable) flag
  * @author Mikko Hilpinen
  * @since 18.9.2022, v1.17
  */
class FlagView(flag: Flag) extends FlagLike with ChangingWrapper[Boolean]
{
	// IMPLEMENTED  -------------------
	
	override protected def wrapped = flag
}
