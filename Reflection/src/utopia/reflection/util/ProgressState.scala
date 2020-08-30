package utopia.reflection.util

import utopia.reflection.localization.LocalizedString

object ProgressState
{
	/**
	  * @param description Progress start description
	  * @return A new progress state with 0% progress
	  */
	def initial(description: LocalizedString) = ProgressState(0, description)
	
	/**
	  * @param description Description of progress completion
	  * @return A new progress state with 100% progress
	  */
	def finished(description: LocalizedString) = ProgressState(1, description)
}

/**
  * Represents a state of progress between 0 and 100%. Contains a progress description as well.
  * @author Mikko Hilpinen
  * @since 30.8.2020, v1.2.1
  * @param progress Process progress, between 0 and 1
  * @param description Displayable progress description
  */
case class ProgressState(progress: Double, description: LocalizedString)
{
	// IMPLEMENTED	---------------------------
	
	override def toString = s"${(progress * 100).toInt}%: $description"
	
	
	// OTHER	-------------------------------
	
	/**
	  * @param progressAmount Amount of progress to advance
	  * @return A copy of this state with advanced progress
	  */
	def +(progressAmount: Double) = copy(progress = (progress + progressAmount) min 1.0)
	
	/**
	  * @param progressAmount Amount of progress to advance
	  * @param description A new progress description
	  * @return A copy of this state with advanced progress and a new description
	  */
	def +(progressAmount: Double, description: LocalizedString) =
		ProgressState((progress + progressAmount) min 1.0, description)
}
