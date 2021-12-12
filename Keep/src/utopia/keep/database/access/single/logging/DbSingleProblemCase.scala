package utopia.keep.database.access.single.logging

import utopia.keep.model.stored.logging.ProblemCase
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual ProblemCases, based on their id
  * @author Mikko Hilpinen
  * @since 12.12.2021, v0.2
  */
case class DbSingleProblemCase(id: Int) 
	extends UniqueProblemCaseAccess with SingleIntIdModelAccess[ProblemCase]

