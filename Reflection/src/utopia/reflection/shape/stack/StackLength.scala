package utopia.reflection.shape.stack

import utopia.flow.util.Equatable
import utopia.reflection.shape.stack.LengthPriority.{Expanding, Low, Normal, Shrinking}

object StackLength
{
    // ATTRIBUTES    ---------------
	
	/**
	  * A stack length that allows any value (from 0 and up, preferring 0)
	  */
	val any = new StackLength(0, 0, None)
	
	/**
	  * A stack length that allows only 0
	  */
	val fixedZero = fixed(0)
    
    
    // CONSTRUCTORS    -------------
	
	/**
	  * @param min Minimum length
	  * @param optimal Optimal length
	  * @param max Maximum length. None if not limited. Default = None
	  * @param priority Priority used with this stack length
	  * @return A new stack length
	  */
    def apply(min: Double, optimal: Double, max: Option[Double] = None,
			  priority: LengthPriority = Normal) = new StackLength(min, optimal, max, priority)
	
	/**
	  * @param min Minimum length
	  * @param optimal Optimal length
	  * @param max Maximum length
	  * @return A new stack length
	  */
    def apply(min: Double, optimal: Double, max: Double) = new StackLength(min, optimal, Some(max))
	
	/**
	  * @param l Fixed length
	  * @return A new stack length with min, optimal and max set to specified single value (no variance)
	  */
    def fixed(l: Double) = apply(l, l, l)
	
	/**
	  * @param optimal Optimal length
	  * @return A stack length with no minimum or maximum, preferring specified value
	  */
    def any(optimal: Double) = apply(0, optimal)
	
	/**
	  * @param min Minimum length
	  * @param optimal Optimal length
	  * @return A stack length with no maximum
	  */
    def upscaling(min: Double, optimal: Double) = apply(min, optimal)
	
	/**
	  * @param optimal Minimum & Optimal length
	  * @return A stack length with no maximum, preferring the minimum value
	  */
    def upscaling(optimal: Double): StackLength = upscaling(optimal, optimal)
	
	/**
	  * @param optimal Optimal length
	  * @param max Maximum length
	  * @return A stack length with no minimum
	  */
    def downscaling(optimal: Double, max: Double) = apply(0, optimal, max)
	
	/**
	  * @param max Maximum length
	  * @return A stack length with no miminum, preferring the maximum length
	  */
    def downscaling(max: Double): StackLength = downscaling(max, max)
	
	
	// OTHER	--------------------------
	
	// Attempts to intelligently combine the two length priorities. Prefers to minimize priority where reasonable
	private def combinedPriority(newMin: Double, newOptimal: Double, newMax: Option[Double], firstOptimal: Double,
								 firstPriority: LengthPriority, secondOptimal: Double, secondPriority: LengthPriority) =
	{
		val isSameLength = firstOptimal == secondOptimal
		val options = Vector(firstOptimal -> firstPriority, secondOptimal -> secondPriority).sortBy { _._1 }
		/* Shrinking is allowed if:
			- If Smaller optimal value allows shrinking
			- If the two optimal values are equal AND one of them allows shrinking
			- If new optimal value is larger than the smaller AND larger of them allows shrinking
			- BUT NOT if new minimum is same or larger than new optimal
		 */
		val allowsShrinking = newMin < newOptimal && (options.head._2.shrinksFirst ||
				(isSameLength && options.exists { _._2.shrinksFirst } ||
					(newOptimal > options.head._1 && options.last._2.shrinksFirst)))
		/* Expansion is allowed if:
			- If larger of the two values allows expansion
			- If the two optimal values are equal AND one of them allows expansion
			- If the new optimal value is smaller than the larger AND smaller of them allows expansion
			- BUT NOT if new maximum is same or smaller than new optimal
		 */
		val allowsExpansion = newMax.forall { _ > newOptimal } && (options.last._2.expandsFirst ||
			(isSameLength && options.exists { _._2.expandsFirst }) ||
			(newOptimal < options.last._1 && options.head._2.expandsFirst))
		LengthPriority(allowsShrinking, allowsExpansion)
	}
}

/**
* This class represents a varying length with a minimum, optimal and a possible maximum
* @author Mikko Hilpinen
* @since 25.2.2019
  * @param rawMin Minimum length
  * @param rawOptimal Optimum length
  * @param rawMax Maximum length. None if not limited. Defaults to None.
  * @param priority The priority used for this length
**/
class StackLength(rawMin: Double, rawOptimal: Double, rawMax: Option[Double] = None,
				  val priority: LengthPriority = Normal)
	extends Equatable with StackInsetsConvertible
{
    // ATTRIBUTES    ------------------------
	
	/**
	  * Minimum length
	  */
	val min: Double = rawMin max 0
	/**
	  * Optimal / preferred length
	  */
	// Optimal must be >= min
	val optimal: Double = rawOptimal max min
	/**
	  * Maximum length. None if not limited.
	  */
	// Max must be >= optimal
	val max: Option[Double] = rawMax.map(_ max optimal)
	
	
	// COMPUTED	-----------------------------
	
	/**
	 * @return Whether this length has a maximum limit
	 */
	def hasMax = max.isDefined
	
	/**
	  * @return A version of this stack length that has low priority
	  */
	def withLowPriority = withPriority(Low)
	/**
	  * @return A copy of this length with a priority that allows easier shrinking
	  */
	def shrinking = if (priority.shrinksFirst) this else withPriority(Shrinking)
	/**
	  * @return A copy of this length with a priority that allows easier expanding
	  */
	def expanding = if (priority.expandsFirst) this else withPriority(Expanding)
	
	/**
	  * @return A version of this stack length that has no mimimum (set to 0)
	  */
	def noMin = withMin(0)
	/**
	  * @return A version of this stack length that has no maximum
	  */
	def noMax = withMax(None)
	/**
	  * @return A version of this stack length with no minimum or maximum
	  */
	def noLimits = copy(newMin = 0, newMax = None)
	
	/**
	  * @return A version of this stack length with no maximum value (same as noMax)
	  */
	def upscaling = noMax
	/**
	  * @return A version of this stack length with no mimimum value (same as noMin)
	  */
	def downscaling = noMin
	
	/**
	  * @return A stacksize that uses this length as both widht and height
	  */
	def square = StackSize(this, this)
	
	/**
	  * @return This stack length as an inset placed on the top of the area
	  */
	def top = StackInsets.top(this)
	/**
	  * @return This stack length as an inset placed at the bottom of the area
	  */
	def bottom = StackInsets.bottom(this)
	/**
	  * @return This stack length as an inset placed at the left side of the area
	  */
	def left = StackInsets.left(this)
	/**
	  * @return This stack length as an inset placed at the right side of the area
	  */
	def right = StackInsets.right(this)
	
	/**
	  * @return A copy of this length where all the values are rounded to the next full integer
	  */
	def ceil = copy(min.ceil, optimal.ceil, max.map { _.ceil })
	/**
	  * @return A copy of this length where all the values are rounded down to the full integer
	  */
	def floor = copy(min.floor, optimal.floor, max.map { _.floor })
	
	
	// IMPLEMENTED    -----------------------
	
	override def toInsets = StackInsets.symmetric(this)
	
	def properties = Vector(min, optimal, max, priority)
	
	override def toString =
	{
	    val s = new StringBuilder()
	    s append min.toInt
	    s append "-"
	    s append optimal.toInt
		s append "-"
	    
	    max foreach { m => s.append(m.toInt) }
	    
	    if (priority != Normal)
	        s append s" ($priority)"
	    
	    s.toString()
	}
	
	
	// OPERATORS    ------------------------
	
	/**
	  * @param length Increase in length
	  * @return An increased version of this stack length (min, optimal and max adjusted, if present)
	  */
	def +(length: Double) = map { _ + length }
	
	/**
	  * @param other Another stack length
	  * @return A combination of these stack sizes where minimum, optimal and maximum (if present) values are increased
	  */
	def +(other: StackLength) =
	{
	    val newMax = if (max.isDefined && other.max.isDefined) Some(max.get + other.max.get) else None
	    StackLength(min + other.min, optimal + other.optimal, newMax, priority min other.priority)
	}
	
	/**
	  * Subtracts another length from this one. Only works properly if this length is larger
	  * @param other Another stack length
	  * @return A subtraction of these stack lengths where minimum, optimal and maximum values are decreased
	  */
	def -(other: StackLength) =
	{
		val newMax = if (max.isDefined && other.max.isDefined) Some(max.get - other.max.get) else None
		StackLength(min - other.min, optimal - other.optimal, newMax, priority)
	}
	
	/**
	  * @param length A decrease in length
	  * @return A decreased version of this stack length (min, optimal and max adjusted, if present). Minimum won't go below 0
	  */
	def -(length: Double): StackLength = this + (-length)
	
	/**
	  * @param multi A multiplier
	  * @return A multiplied version of this length where min, optimal and max lengths are all affected, if present
	  */
	def *(multi: Double) = map { _ * multi }
	
	/**
	  * @param div A divider
	  * @return A divided version of this length where min, optimal and max lengths are all affected, if present
	  */
	def /(div: Double) = *(1/div)
	
	/**
	  * Combines this stack length with another in order to create a stack size
	  * @param vertical The vertical stack length component
	  * @return A stack size with this length as width and 'vertical' as height
	  */
	def x(vertical: StackLength) = StackSize(this, vertical)
	
	
	// OTHER    ---------------------------
	
	/**
	  * Creates a copy of this stack length (usually one or more parameters should be specified)
	  * @param newMin New minimum length (default = current minimum length)
	  * @param newOptimal New optimal length (default = current optimal length)
	  * @param newMax New maximum length (default = current maximum length)
	  * @param newPriority New priority (default = current priority)
	  */
	def copy(newMin: Double = min, newOptimal: Double = optimal, newMax: Option[Double] = max,
			 newPriority: LengthPriority = priority) = new StackLength(newMin, newOptimal, newMax, newPriority)
	
	/**
	  * @param f A mapping function
	  * @return A copy of this length where each value has been mapped (maximum is mapped only if present)
	  */
	def map(f: Double => Double) = StackLength(f(min), f(optimal), max.map(f), priority)
	
	/**
	  * Creates a copy of this length with a new priority
	  * @param newPriority New priority for this length
	  */
	def withPriority(newPriority: LengthPriority) =
		if (priority == newPriority) this else copy(newPriority = newPriority)
	
	/**
	  * @param f A function for mapping priority
	  * @return A copy of this length with mapped priority
	  */
	def mapPriority(f: LengthPriority => LengthPriority) = withPriority(f(priority))
	
	/**
	  * @param newMin A new minimum value
	  * @return An updated version of this length with specified minimum value (optimal and max may also be adjusted if necessary)
	  */
	def withMin(newMin: Double) = copy(newMin = newMin)
	
	/**
	  * @param newOptimal A new optimal value
	  * @return An updated version of this length with specified optimum value (maximum may also be adjusted if necessary)
	  */
	def withOptimal(newOptimal: Double) = copy(newOptimal = newOptimal)
	
	/**
	  * @param newMax A new maximum value (None if no maximum)
	  * @return An updated version of this length with specified maximum length
	  */
	def withMax(newMax: Option[Double]) = copy(newMax = newMax)
	
	/**
	  * @param newMax A new maximum value
	  * @return An updated version of this length with specified maximum length
	  */
	def withMax(newMax: Double): StackLength = withMax(Some(newMax))
	
	/**
	  * @param other Another stack length
	  * @return A minimum between this length and the other (min, optimal and max will be picked from the minimum value)
	  */
	def min(other: StackLength): StackLength =
	{
		// May prioritize larger optimum length if one of the lengths allows it
		val newMin = min min other.min
		val newOptimal =
		{
			if (priority.expandsFirst == other.priority.expandsFirst)
				optimal min other.optimal
			else
			{
				val (limit, proposed) =
				{
					if (priority.expandsFirst)
						max -> other.optimal
					else
						other.max -> optimal
				}
				limit match
				{
					case Some(limit) => limit min proposed
					case None => proposed
				}
			}
		}
		val newMax = Vector(max, other.max).flatten.reduceOption { _ min _ }
		val newPriority = StackLength.combinedPriority(newMin, newOptimal, newMax,
			optimal, priority, other.optimal, other.priority)
		
		StackLength(newMin, newOptimal, newMax, newPriority)
	}
	
	/**
	  * @param other Another stack length
	  * @return A maximum between this length and the other (min, optimal and max will be picked from the maximum value)
	  */
	def max(other: StackLength): StackLength =
	{
		val newMin = min max other.min
		val newMax = if (max.isEmpty || other.max.isEmpty) None else Some(max.get max other.max.get)
		// May prioritize smaller optimum length if the other length is low priority
		val newOptimal =
		{
			if (priority.shrinksFirst == other.priority.shrinksFirst)
				optimal max other.optimal
			else if (priority.shrinksFirst)
				other.optimal
			else
				optimal
		}
		val newPriority = StackLength.combinedPriority(newMin, newOptimal, newMax,
			optimal, priority, other.optimal, other.priority)
		
		StackLength(newMin, newOptimal, newMax, newPriority)
	}
	
	/**
	 * Combines two stack sizes to one which supports both limits
	 */
	def combineWith(other: StackLength) =
	{
	    val newMin = min max other.min
	    val newMax = Vector(max, other.max).flatten.reduceOption { _ min _ }
	    
		// May pick the smaller optimal length in case the larger optimal is easily shrank
	    val newOptimal =
		{
			if (priority.shrinksFirst == other.priority.shrinksFirst)
				optimal max other.optimal
			else if (priority.shrinksFirst)
				other.optimal
			else
				optimal
		}
		val newPriority = StackLength.combinedPriority(newMin, newOptimal, newMax,
			optimal, priority, other.optimal, other.priority)
		
		// Optimal is limited by maximum length
	    if (newMax exists { _ < newOptimal })
	        StackLength(newMin, newMax.get, newMax, newPriority)
	    else
	        StackLength(newMin, newOptimal, newMax, newPriority)
	}
	
	/**
	  * @param maximum New maximum length
	  * @return A copy of this stack length, now limited to that maximum length
	  *         (adjusts optimal and minimum, as well as priority if necessary)
	  */
	def within(maximum: Double) =
	{
		val newMin = min min maximum
		val newMax = max match
		{
			case Some(m) => m min maximum
			case None => maximum
		}
		val newOptimal = newMin max optimal min newMax
		
		val minLimitedPriority = if (newMin >= newOptimal) priority.notShrinking else priority
		val newPriority = if (newMax <= newOptimal) minLimitedPriority.notExpanding else minLimitedPriority
		
		StackLength(newMin, newOptimal, Some(newMax), newPriority)
	}
	
	/**
	  * Creates a new stack length that is within the specified limits
	  * @param minimum Minimum limit
	  * @param maximum Maximum limit. None if not limited.
	  * @return A stack length that has at least 'minimum' minimum width and at most 'maximum' maximum width
	  */
	def within(minimum: Double, maximum: Option[Double]): StackLength =
	{
		maximum match
		{
			case Some(maximum) => within(minimum, maximum)
			case None =>
				val newMin = minimum max min
				val newMax = max.map { minimum max _ }
				val newOptimal = newMin max optimal
				val newPriority = if (newMin >= newOptimal) priority.notShrinking else priority
				
				StackLength(newMin, newOptimal, newMax, newPriority)
		}
	}
	
	/**
	  * Creates a new stack length that is within the specified limits
	  * @param minimum Minimum limit
	  * @param maximum Maximum limit.
	  * @return A stack length that has at least 'minimum' minimum width and at most 'maximum' maximum width
	  */
	def within(minimum: Double, maximum: Double) =
	{
		val newMin = minimum max min min maximum
		val newMax = max.map { m => minimum max m min maximum } getOrElse maximum
		val newOptimal = newMin max optimal min newMax
		
		val minLimitedPriority = if (newMin >= newOptimal) priority.notShrinking else priority
		val newPriority = if (newMax <= newOptimal) minLimitedPriority.notExpanding else minLimitedPriority
		
		StackLength(newMin, newOptimal, Some(newMax), newPriority)
	}
	
	/**
	  * Creates a new stack length that is within the specified limits
	  * @param limits The limits applied to this length
	  * @return A stack length with limited min, max and optimal value
	  */
	def within(limits: StackLengthLimit) =
	{
		// Minimum size is limited both from minimum and maximum side (cannot be larger than specified max or max optimal)
		val newMin = min max limits.min
		val minUnderMax = limits.maxOptimal.orElse(limits.max).map { newMin min _ } getOrElse newMin
		
		val newMax =
		{
			if (max.isEmpty)
				limits.max
			else if (limits.max.isEmpty)
				max
			else
				Some(max.get min limits.max.get)
		}
		val newOptimal =
		{
			val minLimited = optimal max (limits.minOptimal getOrElse minUnderMax)
			(limits.maxOptimal orElse limits.max).map { minLimited min _ } getOrElse minLimited
		}
		
		StackLength(minUnderMax, newOptimal, newMax, priority)
	}
	
	/**
	  * @param map A mapping function
	  * @return A new length with mapped min
	  */
	def mapMin(map: Double => Double) = withMin(map(min))
	
	/**
	  * @param map A mapping function
	  * @return A new length with mapped optimal
	  */
	def mapOptimal(map: Double => Double) = withOptimal(map(optimal))
	
	/**
	  * @param map A mapping function
	  * @return A new length with mapped maximum value
	  */
	def mapMax(map: Option[Double] => Option[Double]) = withMax(map(max))
}