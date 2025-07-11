package utopia.logos.database.reader.text

import utopia.logos.database.props.text.StatementPlacementDbProps

/**
  * Common trait for factories which parse statement placement data from database-originated 
  * models
  * @tparam A Type of read instances
  * @author Mikko Hilpinen
  * @since 11.07.2025, v0.4
  */
trait StatementPlacementDbReaderLike[+A] extends TextPlacementDbReaderLike[A]
{
	// ABSTRACT	--------------------
	
	/**
	  * Database properties used when parsing column data
	  */
	override def dbProps: StatementPlacementDbProps
}

