package utopia.logos.database.factory.text

import utopia.logos.database.props.text.StatementPlacementDbProps

/**
  * Common trait for factories which parse statement placement data from database-originated 
  * models
  * @tparam A Type of read instances
  * @author Mikko Hilpinen
  * @since 30.12.2024, v0.4
  */
trait StatementPlacementDbFactoryLike[+A] extends TextPlacementDbFactoryLike[A]
{
	// ABSTRACT	--------------------
	
	/**
	  * Database properties used when parsing column data
	  */
	override def dbProps: StatementPlacementDbProps
}

