package utopia.flow.datastructure.immutable

import utopia.flow.datastructure.mutable.Flag
import utopia.flow.datastructure.template.FlagLike
import utopia.flow.event.ChangingWrapper

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
