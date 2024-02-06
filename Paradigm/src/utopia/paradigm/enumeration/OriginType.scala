package utopia.paradigm.enumeration

import utopia.flow.collection.immutable.Pair

/**
  * An enumeration for two different origin coordinate approaches:
  * The (0,0) coordinate may either be considered
  * "relative" (i.e. relative to some other coordinate chosen as the origin), or
  * it may be "absolute", which means that it is located at the highest possible reference frame.
  *
  * E.g. In view component systems, a relative (0,0) often refers to a view component's top left corner,
  * while an absolute (0,0) refers to the top left corner of the monitor.
  * Similarly, a relative coordinate might refer to a pixel in an image, relative to a chosen origin point,
  * while an absolute image coordinate would typically refer to a location relative to the image's top left corner.
  *
  * @author Mikko Hilpinen
  * @since 06/02/2024, v3.6
  */
sealed trait OriginType

object OriginType
{
	// ATTRIBUTES   ------------------
	
	/**
	  * Both possible values of OriginType. First Relative and then Absolute.
	  */
	val values = Pair[OriginType](Relative, Absolute)
	
	
	// VALUES   ----------------------
	
	/**
	  * Represents a relative origin (i.e. a custom origin chosen within that specific context)
	  */
	case object Relative extends OriginType
	/**
	  * Represents an absolute origin
	  * (i.e. the standard and "highest" available origin within that context,
	  * such as the top left image corner or the top left monitor pixel)
	  */
	case object Absolute extends OriginType
}
