package utopia.paradigm.color

import utopia.flow.collection.immutable.caching.cache.{Cache, WeakCache}
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.template.eventful.Changing

private object FromVariableShadeFactory
{
	// ATTRIBUTES   ---------------------
	
	private val shadePointerCache = WeakCache { colorP: Changing[Color] => colorP.map { _.shade } }
}

/**
  * Common trait for factories that construct items based on shade, supporting variable (i.e. changing) shades.
  * Note: This trait introduces concrete attributes used in pointer-optimization.
  * As a consequence, this trait should not be extended by wrapper-like implementations that rely
  * on another instance's [[withVariableShade]] and [[againstVariableShade]] implementations.
  * @tparam A Type of constructed shaded items
  * @author Mikko Hilpinen
  * @since 02.11.2024, v1.7.1
  */
trait FromVariableShadeFactory[+A] extends FromShadeFactory[A]
{
	import FromVariableShadeFactory._
	
	// ATTRIBUTES   ---------------------
	
	// Weakly caches the pointer-mappings in order to avoid creating unnecessary pointers
	private val shadedPointersCache: Cache[Changing[ColorShade], Changing[A]] =
		WeakCache { shadeP: Changing[ColorShade] => shadeP.map(apply) }
	private val againstPointersCache: Cache[Changing[ColorShade], Changing[A]] =
		WeakCache { shadeP: Changing[ColorShade] => shadeP.map(against) }
	
	
	// ABSTRACT -------------------------
	
	/**
	  * @param shadePointer A pointer that determines the current shade of this item
	  * @return A pointer that contains a properly shaded version of this item, according to the 'shadePointers' state
	  */
	def withVariableShade(shadePointer: Changing[ColorShade]): Changing[A] = shadePointer.fixedValue match {
		case Some(fixedShade) => Fixed(apply(fixedShade))
		case None => shadedPointersCache(shadePointer)
	}
	/**
	  * @param backgroundColorPointer A pointer that contains the currently applicable background color
	  * @return A pointer that contains a properly shaded version of this item,
	  *         with always the most visible version against the current 'backgroundColorPointer' value selected.
	  */
	def againstVariableBackground(backgroundColorPointer: Changing[Color]) =
		backgroundColorPointer.fixedValue match {
			case Some(fixedBg) => Fixed(against(fixedBg))
			case None => againstPointersCache(shadePointerCache(backgroundColorPointer))
		}
	/**
	  * @param shadePointer A pointer that contains the overall shade of the background color
	  * @return A pointer that contains a properly shaded version of this item,
	  *         that is most visible against the current 'shadePointer' value.
	  */
	def againstVariableShade(shadePointer: Changing[ColorShade]): Changing[A] = shadePointer.fixedValue match {
		case Some(fixedShade) => Fixed(against(fixedShade))
		case None => againstPointersCache(shadePointer)
	}
}
