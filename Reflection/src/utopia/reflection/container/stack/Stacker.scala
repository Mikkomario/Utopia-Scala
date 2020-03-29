package utopia.reflection.container.stack

import utopia.flow.datastructure.mutable.Pointer
import utopia.genesis.shape.Axis2D
import utopia.genesis.shape.shape2D.Bounds
import utopia.reflection.component.stack.Stackable
import utopia.reflection.container.stack.StackLayout.{Fit, Leading, Trailing}
import utopia.reflection.shape.{StackLength, StackSize}

import scala.collection.immutable.VectorBuilder

/**
 * Used for placing stackable items in a stack
 * @author Mikko Hilpinen
 * @since 17.1.2020, v1
 */
object Stacker
{
	/**
	 * Calculates a stack size for a stack of items
	 * @param visibleComponentSizes Sizes of components that form the stack (usually invisible items are excluded)
	 * @param stackAxis Axis along which the components are stacked
	 * @param margin Margin between components (default = 0 or more)
	 * @param cap Cap at each end of the stack (default = fixed to 0)
	 * @param layout How the items are placed perpendicular to target axis (default = Fit = each item has same breadth)
	 * @return A stack size for the stacked items
	 */
	def calculateStackSize(visibleComponentSizes: Vector[StackSize], stackAxis: Axis2D,
						   margin: StackLength = StackLength.any, cap: StackLength = StackLength.fixed(0),
						   layout: StackLayout = Fit) =
	{
		if (visibleComponentSizes.isEmpty)
			StackSize.any.withLowPriorityFor(stackAxis)
		else
		{
			// Checks component sizes
			val lengths = visibleComponentSizes map { _ along stackAxis }
			val breadths = visibleComponentSizes map { _ perpendicularTo stackAxis }
			
			// Determines total length & breadth
			val componentsLength = lengths reduce { _ + _ }
			// Length has low priority if any of the items has one
			val numberOfMargins = (visibleComponentSizes.size - 1) max 0
			val length = componentsLength + margin * numberOfMargins + (cap * 2)
			
			// Non-fit stacks don't have max breadth while fit versions do
			// Breadth is considered low priority only if all items are low priority
			val breadth =
			{
				if (layout == Fit)
				{
					val min = breadths.map { _.min }.max
					val optimal = breadths.map { _.optimal }.max
					val max = breadths.flatMap { _.max }.reduceOption { _ min _ }
					
					StackLength(min, optimal, max)
				}
				else
					breadths.reduce { _ max _ }.withMax(None)
					
			}.withPriority(breadths.map { _.priority }.reduce { _ max _ })
			
			// Returns the final size
			StackSize(length, breadth, stackAxis)
		}
	}
	
	/**
	 * Stacks specified items over the specified area
	 * @param components Components that are stacked
	 * @param area Area over which the components are stacked
	 * @param optimalLength The optimal length of the stack (use calculateStackSize method). Components are shrinked or
	 *                      stretched according to difference between this value and the area's length
	 * @param stackAxis Axis along which the items are stacked
	 * @param margin Margin between the items (default = 0 or more)
	 * @param cap Cap at each end of the stack (default = fixed to 0)
	 * @param layout How the items are placed perpendicular to target axis (default = Fit = each item has same breadth)
	 */
	def apply(components: Vector[Stackable], area: Bounds, optimalLength: Double, stackAxis: Axis2D,
			  margin: StackLength = StackLength.any, cap: StackLength = StackLength.fixed(0), layout: StackLayout = Fit) =
	{
		val visibleComponents = components.filter { _.isVisible }
		
		if (visibleComponents.nonEmpty)
		{
			// Calculates the necessary length adjustment
			val lengthAdjustment = area.size.along(stackAxis) - optimalLength
			
			// Arranges the mutable items in a vector first. Treats margins and caps as separate items
			val caps = Vector.fill(2)(Pointer(0.0))
			val numberOfMargins = (visibleComponents.size - 1) max 0
			val margins = Vector.fill(numberOfMargins)(Pointer(0.0))
			val targets =
			{
				val builder = new VectorBuilder[LengthAdjust]()
				
				// Starts with a cap
				builder += new GapLengthAdjust(caps.head, cap)
				
				// Next adds items with margins
				visibleComponents.zip(margins).foreach
				{
					case (component, marginPointer) =>
						builder += new StackableLengthAdjust(component, stackAxis)
						builder += new GapLengthAdjust(marginPointer, margin)
				}
				
				// Adds final component and final cap
				builder += new StackableLengthAdjust(visibleComponents.last, stackAxis)
				builder += new GapLengthAdjust(caps.last, cap)
				
				builder.result()
			}
			
			// First adjusts the length of low priority items, then of all remaining items (if necessary)
			val groupedTargets = targets.groupBy { _.isLowPriorityFor(lengthAdjustment) }
			val lowPrioTargets = groupedTargets.getOrElse(true, Vector())
			
			val remainingAdjustment =
			{
				if (lowPrioTargets.isEmpty)
					lengthAdjustment
				else
					adjustLength(lowPrioTargets, lengthAdjustment)
			}
			
			if (remainingAdjustment != 0.0)
				groupedTargets.get(false).foreach { adjustLength(_, remainingAdjustment) }
			
			// Applies the length adjustments
			targets.foreach { _() }
			
			// Positions the components length-wise (first components with margin and then the final component)
			var cursor = area.position.along(stackAxis) + caps.head.get
			visibleComponents.zip(margins).foreach
			{
				case (component, marginPointer) =>
					component.setCoordinate(cursor, stackAxis)
					cursor += component.lengthAlong(stackAxis) + marginPointer.get
			}
			visibleComponents.last.setCoordinate(cursor, stackAxis)
			
			// Handles the breadth of the components too, as well as their perpendicular positioning
			val breadthAxis = stackAxis.perpendicular
			val newBreadth = area.size.along(breadthAxis)
			visibleComponents.foreach
			{
				component =>
					val breadth = component.stackSize.along(breadthAxis)
					
					// Component breadth may be affected by minimum and maximum
					val newComponentBreadth =
					{
						if (breadth.min > newBreadth)
							breadth.min
						else if (breadth.max.exists { newBreadth > _ })
							breadth.max.get
						else
						{
							// In fit-style stacks, stack breadth is used over component optimal
							// whereas in other styles optimal is prioritized
							if (layout == Fit)
								newBreadth
							else
								newBreadth min breadth.optimal
						}
					}
					
					component.setLength(newComponentBreadth, breadthAxis)
					
					// Component positioning depends on the layout
					val newComponentPosition =
					{
						if (layout == Leading)
							0
						else if (layout == Trailing)
							newBreadth - newComponentBreadth
						else
							(newBreadth - newComponentBreadth) / 2
					}
					
					component.setCoordinate(area.position.along(breadthAxis) + newComponentPosition, breadthAxis)
			}
		}
	}
	
	/**
	  * Adjusts the specified linear lengths to match the available space, if possible
	  * @param lengths Lengths of items that cover the targeted space
	  * @param availableSpace Linear space to cover
	  * @return Final lengths of each item (in same order as 'lengths')
	  */
	def adjustLengths(lengths: Seq[StackLength], availableSpace: Double) =
	{
		// Calculates the required adjustment
		val optimalLength = lengths.foldLeft(0.0) { (total, length) => total + length.optimal }
		val requiredAdjustment = availableSpace - optimalLength
		
		// Performs the adjustment
		val targets = lengths.map { new GapLengthAdjust(new Pointer(0.0), _) }
		adjustLength(targets, requiredAdjustment)
		targets.foreach { _() }
		
		// Returns the final lengths
		targets.map { _.target.get }
	}
	
	@scala.annotation.tailrec
	private def adjustLength(targets: Traversable[LengthAdjust], adjustment: Double): Double =
	{
		// Finds out how much each item should be adjusted
		val adjustmentPerComponent = adjustment / targets.size
		
		// Adjusts the items (some may be maxed) and caches results
		val results = targets map { target => target -> (target += adjustmentPerComponent) }
		
		// Finds out the remaining adjustment and available targets
		val remainingAdjustment = results.foldLeft(0.0) { case (total, next) => total + next._2 }
		val availableTargets = results.filter { _._2 == 0.0 }.map { _._1 }
		
		// If necessary and possible, goes for the second round. Returns remaining adjustment
		if (availableTargets.isEmpty)
			remainingAdjustment
		else if (remainingAdjustment == 0.0)
			0.0
		else
			adjustLength(availableTargets, remainingAdjustment)
	}
	
	private trait LengthAdjust
	{
		// ATTRIBUTES    -----------------
		
		private var currentAdjust = 0.0
		
		
		// ABSTRACT    -------------------
		
		def length: StackLength
		
		protected def setLength(length: Double): Unit
		
		
		// COMPUTED    -------------------
		
		def isLowPriorityFor(adjustment: Double) = length.priority.isFirstAdjustedBy(adjustment)
		
		private def max = length.max map { _ - length.optimal }
		
		private def min = length.min - length.optimal
		
		
		// OPERATORS    ------------------
		
		// Adjusts length, returns remaining adjustment
		def +=(amount: Double) =
		{
			val target = currentAdjust + amount
			
			// Adjustment may be negative (shrinking) or positive (enlarging)
			if (amount < 0)
			{
				if (target > min)
				{
					currentAdjust = target
					0.0
				}
				// If trying to shrink below minimum, goes to minimum and returns remaining amount (negative)
				else
				{
					currentAdjust = min
					target - min
				}
			}
			else if (amount > 0)
			{
				// If trying to enlarge beyond maximum, caps at maximum and returns remaining amount (positive)
				if (max exists { target > _ })
				{
					currentAdjust = max.get
					target - max.get
				}
				else
				{
					currentAdjust = target
					0.0
				}
			}
			else
				0.0
		}
		
		
		// OTHER    ---------------------
		
		def apply() = setLength(length.optimal + currentAdjust)
	}
	
	private class StackableLengthAdjust(private val target: Stackable, private val direction: Axis2D) extends LengthAdjust
	{
		def length = target.stackSize.along(direction)
		
		def setLength(length: Double) = target.setLength(length, direction)
	}
	
	private class GapLengthAdjust(val target: Pointer[Double], val length: StackLength) extends LengthAdjust
	{
		def setLength(length: Double) = target.set(length)
	}
}
