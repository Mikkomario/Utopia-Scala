package utopia.firmament.context.color

import utopia.firmament.context.color.ColorAccess.MappingColorAccess
import utopia.flow.util.Mutate
import utopia.paradigm.color.{Color, ColorLevel, ColorRole, ColorSet}

object ColorAccess
{
	// NESTED   -----------------------
	
	private class MappingColorAccess[CO, +CR](origin: ColorAccess[CO], f: CO => CR) extends ColorAccess[CR]
	{
		// IMPLEMENTED  ---------------
		
		override def expectingSmallObjects: ColorAccess[CR] = mapOrigin { _.expectingSmallObjects }
		override def expectingLargeObjects: ColorAccess[CR] = mapOrigin { _.expectingLargeObjects }
		override def forText: ColorAccess[CR] = mapOrigin { _.forText
		}
		override def preferring(level: ColorLevel): ColorAccess[CR] = mapOrigin { _.preferring(level) }
		
		override def apply(color: ColorSet): CR = f(origin(color))
		override def apply(role: ColorRole): CR = f(origin(role))
		override def differentFrom(role: ColorRole, competingColor: Color, moreColors: Color*): CR =
			f(origin.differentFrom(role, competingColor, moreColors: _*))
		
		
		// OTHER    -------------------
		
		private def mapOrigin(f: Mutate[ColorAccess[CO]]) = new MappingColorAccess[CO, CR](f(origin), this.f(_))
	}
}

/**
  * Common trait for color access points. Removes Repr type parameter from [[ColorAccessLike]].
  * @tparam C Type of color representations accessed
  * @author Mikko Hilpinen
  * @since 29.09.2024, v1.4
  */
trait ColorAccess[+C] extends ColorAccessLike[C, ColorAccess[C]]
{
	/**
	  * @param f A mapping function applied to output color instances
	  * @tparam C2 Type of mapping results
	  * @return Color access that applies the specified mapping function
	  */
	def map[C2](f: C => C2): ColorAccess[C2] = new MappingColorAccess(this, f)
}
