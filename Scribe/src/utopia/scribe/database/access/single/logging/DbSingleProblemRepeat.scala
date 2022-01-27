package utopia.scribe.database.access.single.logging

import utopia.scribe.model.stored.logging.ProblemRepeat
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual ProblemRepeats, based on their id
  * @author Mikko Hilpinen
  * @since 12.12.2021, v0.2
  */
case class DbSingleProblemRepeat(id: Int) 
	extends UniqueProblemRepeatAccess with SingleIntIdModelAccess[ProblemRepeat]

