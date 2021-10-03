package utopia.genesis.util

import utopia.flow.operator.LinearMeasurable

/**
  * Common trait for items that have a measurable length
  * @author Mikko Hilpinen
  * @since 13.9.2019, v2.1+
  */
@deprecated("Please use LinearMeasurable instead", "v2.6")
trait DistanceLike extends LinearMeasurable
