package utopia.paradigm.angular

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Pair, Single}
import utopia.flow.operator.Reversible
import utopia.flow.operator.enumeration.End
import utopia.flow.operator.enumeration.End.{First, Last}
import utopia.flow.util.Mutate
import utopia.paradigm.enumeration.RotationDirection
import utopia.paradigm.enumeration.RotationDirection.Clockwise
import utopia.paradigm.path.Path

object AngleRange
{
	/**
	  * Creates a new angle range
	  * @param start The starting angle
	  * @param end The final / ending angle
	  * @param direction Direction taken
	  * @return An angle range that starts from 'start' and ends at 'end', taking the route determined by 'direction'
	  */
	def apply(start: Angle, end: Angle, direction: RotationDirection): AngleRange =
		apply(start, (end - start).towardsPreservingEndAngle(direction))
	/**
	  * Creates a new angle range
	  * @param ends The start and end angle of this range
	  * @param direction Direction taken
	  * @return An angle range that ends at 'ends', taking the route determined by 'direction'
	  */
	def apply(ends: Pair[Angle], direction: RotationDirection): AngleRange = apply(ends.first, ends.second, direction)
	
	/**
	  * Creates a new angle range, selecting the rotation direction so that the range size is minimized
	  * @param start The starting angle
	  * @param end The ending angle
	  * @return An angle range that starts at 'start' and ends at 'end', taking the shorter of the two routes
	  */
	def shortestRangeBetween(start: Angle, end: Angle) = apply(start, end - start)
	/**
	  * Creates a new angle range, selecting the rotation direction so that the range size is maximized
	  * @param start The starting angle
	  * @param end The ending angle
	  * @return An angle range that starts at 'start' and ends at 'end', taking the longer of the two routes
	  */
	def longestRangeBetween(start: Angle, end: Angle) = apply(start, (end - start).complementary)
	
	/**
	  * Creates a new angle range with the specified central angle
	  * @param center Central angle within this range
	  * @param spread How much the start and end are different from 'center'.
	  *               If positive, the resulting range will use clockwise progression.
	  *               If negative, the resulting progression will be counter-clockwise.
	  *
	  *               It is recommended to not use spreads larger than 180 degrees,
	  *               as that will cause this range to overlap with itself.
	  *
	  * @return A new angle range centering around the specified angle
	  */
	def around(center: Angle, spread: Rotation) =
		apply(center + spread.counterclockwise, center + spread.clockwise, Clockwise)
	
	/**
	  * Creates an angle range which only covers a single angle
	  * @param angle The only angle covered by this range
	  * @return A new range
	  */
	def singleValue(angle: Angle) = apply(angle, DirectionalRotation.zero)
}

/**
  * Represents a range of angles, e.g. "clockwise from 90 to 120 degrees".
  * @param start The starting angle
  * @param rotation Rotation applied during this range.
  *                 Typical rotation values are 360 degrees or less.
  *                 If larger rotations are used, this class might not behave accurately.
  * @author Mikko Hilpinen
  * @since 28.08.2024, v1.7
  */
case class AngleRange(start: Angle, rotation: DirectionalRotation) extends Path[Angle] with Reversible[AngleRange]
{
	// ATTRIBUTES   ------------------------
	
	override lazy val end: Angle = start + rotation
	
	/**
	  * The center / middle angle within this range.
	  * E.g. If start is 90 degrees, end is 130 degrees and direction is clockwise/positive, the center is 110 degrees.
	  */
	lazy val center = apply(0.5)
	
	
	// COMPUTED ----------------------------
	
	/**
	  * @return Size of this range as an absolute rotation
	  */
	def size = rotation.absolute
	/**
	  * @return Progression direction of this range
	  */
	def direction = rotation.direction
	
	/**
	  * @return A complementary range covering all the areas which this range doesn't cover.
	  *         E.g. If this range is 90 to 120 degrees clockwise (a total of 30 degrees),
	  *         the resulting range will be 90 to 120 degrees counter-clockwise (a total of 330 degrees).
	  */
	def complementary = copy(rotation = rotation.complementary)
	
	
	// IMPLEMENTED  ------------------------
	
	override def self: AngleRange = this
	
	/**
	  *  @return Copy of this range with reversed progression / rotation direction,
	  *          still covering the exact same range of angles.
	  */
	override def unary_- : AngleRange = AngleRange(end, -rotation)
	
	override def toString = s"$direction from $start to $end"
	
	override def apply(progress: Double): Angle = start + rotation * progress
	
	
	// OTHER    ----------------------------
	
	/**
	  * @param angle An angle
	  * @return Progress along this range that matches the specified angle.
	  *         If the specified angle falls within this range, the resulting progress is between 0 and 1.
	  */
	def progressOf(angle: Angle) = (angle - center).towards(direction) / size + 0.5
	
	/**
	  * @param angle An angle
	  * @return Whether this range contains the specified angle
	  */
	def contains(angle: Angle) = {
		val progress = (angle - start).towards(direction)
		progress >= Rotation.zero && progress <= size
	}
	/**
	  * @param range Another range of angles
	  * @return Whether the specified range is fully contained within this range
	  */
	def contains(range: AngleRange): Boolean = {
		if (range.direction == direction)
			range.ends.forall(contains)
		else {
			val relativeStart = progressOf(range.start)
			if (relativeStart >= 0 && relativeStart <= 1) {
				val relativeEnd = progressOf(range.end)
				relativeEnd <= 1 && relativeEnd <= relativeStart
			}
			else
				false
		}
	}
	
	/**
	  * @param range Another range of angles
	  * @return Whether these ranges overlap at some point
	  */
	def overlapsWith(range: AngleRange) = {
		if (range.direction == direction) {
			val progressValues = range.ends.map(progressOf)
			progressValues.first <= 1.0 && progressValues.second >= 0
		}
		else
			range.ends.exists(contains)
	}
	
	/**
	  * Splits this range into 1-n segments.
	  * E.g. If splitting range 20 to 80 clockwise into 3 ranges, that would yield 20-40, 40-60 and 60-80.
	  * @param numberOfSegments Number of segments to split this range into (1 or larger)
	  * @throws IllegalArgumentException If numberOfSegments is 0 or less
	  * @return A sequence of smaller consecutive ranges, which together cover this range.
	  */
	@throws[IllegalArgumentException]("If numberOfSegments is 0 or less")
	def split(numberOfSegments: Int): IndexedSeq[AngleRange] = {
		if (numberOfSegments <= 0)
			throw new IllegalArgumentException(s"Can't split into $numberOfSegments segments")
		else if (numberOfSegments == 1)
			Single(this)
		else {
			val iter = _splitIterator(numberOfSegments)
			if (numberOfSegments == 2)
				Pair.fill { iter.next() }
			else
				iter.toVector
		}
	}
	/**
	  * Iteratively splits this range into 1-n segments.
	  * E.g. If splitting range 20 to 80 clockwise into 3 ranges, that would yield 20-40, 40-60 and 60-80.
	  * @param numberOfSegments Number of segments to split this range into (1 or larger)
	  * @throws IllegalArgumentException If numberOfSegments is 0 or less
	  * @return An iterator that returns smaller consecutive ranges, which together cover this range.
	  */
	@throws[IllegalArgumentException]("If numberOfSegments is 0 or less")
	def splitIterator(numberOfSegments: Int) = {
		if (numberOfSegments <= 0)
			throw new IllegalArgumentException(s"Can't split into $numberOfSegments segments")
		else if (numberOfSegments == 1)
			Iterator.single(this)
		else
			_splitIterator(numberOfSegments)
	}
	
	/**
	  * @param newDirection Targeted rotation direction
	  * @return If this range already progresses towards the specified direction returns this angle,
	  *         otherwise returns the complementary angle range, which spans the other end of the 360 degree spectrum.
	  */
	def rotating(newDirection: RotationDirection) =
		if (direction == newDirection) this else complementary
	
	/**
	  * @param newStart New start angle to assign
	  * @return Copy of this range with the specified start angle. End angle and rotation direction are preserved.
	  */
	def withStart(newStart: Angle) =
		copy(start = newStart, rotation = (end - newStart).towardsPreservingEndAngle(direction))
	/**
	  * @param newEnd New end angle to assign
	  * @return Copy of this range with the specified end angle. Start angle and rotation direction are preserved.
	  */
	def withEnd(newEnd: Angle) = copy(rotation = (newEnd - start).towardsPreservingEndAngle(direction))
	
	/**
	  * @param newStart New range starting angle
	  * @return Copy of this range with the specified starting angle. Range size and direction are preserved.
	  */
	def withStartPreservingRotation(newStart: Angle) = copy(start = newStart)
	/**
	  * @param newEnd New range ending angle
	  * @return Copy of this range with the specified ending angle. Range size and direction are preserved.
	  */
	def withEndPreservingRotation(newEnd: Angle) = copy(start = newEnd - rotation)
	
	/**
	  * @param newRotation New amount of rotation applied
	  * @param preservedAngle The angle that will be preserved. First = start & Last = end. Default = start.
	  * @return Copy of this range with the specified rotation.
	  *         Either the starting angle or the ending angle is preserved, depending on 'preservedAngle'.
	  */
	def withRotation(newRotation: DirectionalRotation, preservedAngle: End = First) = preservedAngle match {
		case First => copy(rotation = newRotation)
		case Last => AngleRange(start = end - newRotation, rotation = newRotation)
	}
	/**
	  * @param newSpread New "spread" / difference between this range's center point and the range end point
	  * @return Copy of this range with the same center point, but with start and end points adjusted so that
	  *         they fall exactly 'newSpread' away from the center.
	  *
	  *         If 'newSpread' is positive, the resulting range will progress clockwise,
	  *         otherwise the resulting range will progress counter-clockwise.
	  */
	def withSpread(newSpread: Rotation) = AngleRange.around(center, newSpread)
	
	/**
	  * @param f A mapping function applied to this range's starting angle
	  * @return Copy of this range with a mapped starting angle. The amount of rotation is preserved.
	  */
	def mapStartPreservingRotation(f: Mutate[Angle]) = withStartPreservingRotation(f(start))
	/**
	  * @param f A mapping function applied to this range's ending angle
	  * @return Copy of this range with a mapped ending angle. The amount of rotation is preserved.
	  */
	def mapEndPreservingRotation(f: Mutate[Angle]) = withEndPreservingRotation(f(end))
	
	/**
	  * @param f A mapping function applied to this range's so-called spread,
	  *          which is the angular distance between range end-points and range center.
	  * @return Copy of this range with the same center point,
	  *         but with start and end points adjusted according to the specified function.
	  */
	def mapSpread(f: Mutate[Rotation]) = withSpread(f(size / 2))
	
	/**
	  * @param adjustment How much this range is "shifted" to either clockwise or counter-clockwise
	  * @return Copy of this range shifted by the specified amount
	  */
	def shifted(adjustment: DirectionalRotation) = mapStartPreservingRotation { _ + adjustment }
	
	// Assumes that 'numberOfSegments' > 1
	private def _splitIterator(numberOfSegments: Int) = {
		val advancePerSegment = 1.0 / numberOfSegments
		(0 to numberOfSegments).iterator.map { i => apply(i * advancePerSegment) }
			.paired.map { AngleRange(_, direction) }
	}
}