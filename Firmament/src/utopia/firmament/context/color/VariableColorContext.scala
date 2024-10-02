package utopia.firmament.context.color

import utopia.firmament.context.base.{VariableBaseContext, VariableBaseContextWrapper}
import utopia.flow.collection.immutable.caching.cache.{Cache, WeakCache}
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.immutable.eventful.{AlwaysFalse, AlwaysTrue}
import utopia.flow.view.template.eventful.{Changing, Flag}
import utopia.genesis.text.Font
import utopia.paradigm.color.ColorLevel.Standard
import utopia.paradigm.color.ColorShade.{Dark, Light}
import utopia.paradigm.color.{Color, ColorLevel, ColorRole, ColorSet}
import utopia.paradigm.enumeration.ColorContrastStandard

object VariableColorContext
{
	// ATTRIBUTES   ----------------------
	
	// Weakly caches the default text color pointer for each background color pointer
	private val backgroundDefaultTextColorPointerCache = WeakCache { bgP: Changing[Color] =>
		bgP.map { _.shade match {
			case Dark => Color.textWhite
			case Light => Color.textBlack
		} }
	}
	// Weakly caches hint text color pointers
	private val hintTextColorPointerCache = WeakCache { tcP: Changing[Color] => tcP.map { _.timesAlpha(0.625) } }
	
	private val textIsLargePointerCache =
		WeakCache[Changing[Font], Flag] { fontPointer: Changing[Font] => fontPointer.map { _.isLargeOnScreen } }
	
	// 4 levels deep cache system for weakly storing generated pointers
	// Keys are:
	//      1) Background color pointer
	//      2) Expects small objects -flag
	//      3) Applied color contrast standard
	//      4) Either
	//          4.1) Color set + preferred color level => Best contextual color pointer
	//          4.2) Color set + preferred color level + competing colors => Best contextual color pointer
	private val colorPointersCache = WeakCache.weakKeys { bgP: Changing[Color] =>
		WeakCache.weakKeys { smallFlag: Changing[Boolean] =>
			Cache { standard: ColorContrastStandard =>
				val colorCache = WeakCache.weakValues[(ColorSet, ColorLevel), Changing[Color]] { case (color, level) =>
					bgP.mergeWith(smallFlag) { (bg, small) =>
						color.against(bg, level, standard.minimumContrast(large = !small))
					}
				}
				val colorsCache = WeakCache
					.weakValues[(ColorSet, ColorLevel, Seq[Color]), Changing[Color]] { case (color, level, others) =>
						bgP.mergeWith(smallFlag) { (bg, small) =>
							color.againstMany(bg +: others, level, standard.minimumContrast(large = !small))
						}
					}
				(colorCache, colorsCache)
			}
		}
	}
	
	// 2 levels deep cache for custom text color pointers
	// Keys are:
	//      1) Custom text color set pointer
	//      2) Background color pointer + font pointer + color contrast standard
	// NB: Only use this with variable custom text color pointers
	private val customTextColorPointerCache = WeakCache.weakKeys { textColorP: Changing[ColorSet] =>
		Cache[(Changing[Color], Changing[Font], ColorContrastStandard), Changing[Color]] {
			case (bgP, fontP, standard) =>
				val textIsLargeP = textIsLargePointerCache(fontP)
				textColorP.mergeWith(bgP, textIsLargeP) { (textColor, bg, large) =>
					textColor.against(bg, minimumContrast = standard.minimumContrast(large))
				}
		}
	}
	
	
	// NESTED   --------------------------
	
	// TODO: May need to reset text color calculations when font changes
	private case class _VariableColorContext(base: VariableBaseContext, backgroundPointer: Changing[Color],
	                                         lazyTextColorPointer: View[Changing[Color]],
	                                         lazyHintTextColorPointer: View[Changing[Color]],
	                                         customTextColorPointer: Option[Either[Changing[Color], Changing[ColorSet]]])
		extends VariableColorContext with VariableBaseContextWrapper[VariableBaseContext, VariableColorContext]
	{
		// IMPLEMENTED  ------------------
		
		override def self: VariableColorContext = this
		
		override def textColorPointer: Changing[Color] = lazyTextColorPointer.value
		override def hintTextColorPointer: Changing[Color] = lazyHintTextColorPointer.value
		
		override def colorPointer = new ColorPointerAccess(this)
		
		override def forTextComponents: VariableColorContext = ???
		
		override def withDefaultTextColor: VariableColorContext = {
			if (customTextColorPointer.isEmpty)
				this
			else {
				val textColorP = Lazy { backgroundDefaultTextColorPointerCache(backgroundPointer) }
				copy(lazyTextColorPointer = textColorP,
					lazyHintTextColorPointer = textColorP.map { hintTextColorPointerCache(_) },
					customTextColorPointer = None)
			}
		}
		
		override def withBase(base: VariableBaseContext): VariableColorContext = copy(base = base)
		
		override def withBackgroundPointer(p: Changing[Color]): VariableColorContext = {
			// Case: No modification
			if (p == backgroundPointer)
				this
			else {
				// Background color change may also affect text color logic
				val (newLazyTextColorP, newLazyHintColorP) = customTextColorPointer match {
					// Case: Using a custom text color pointer => Not dynamically adjusted
					case Some(Left(_)) => lazyTextColorPointer -> lazyHintTextColorPointer
					// Case: Using a custom text color set => Resets the final text color calculation logic
					case Some(Right(customSetP)) =>
						val colorP = Lazy { customTextColorFromSetPointer(customSetP, p) }
						colorP -> colorP.map { hintTextColorPointerCache(_) }
					// Case: Text color is either black or white => Resets the calculation
					case None =>
						val colorP = Lazy { backgroundDefaultTextColorPointerCache(p) }
						colorP -> colorP.map { hintTextColorPointerCache(_) }
				}
				copy(backgroundPointer = backgroundPointer, lazyTextColorPointer = newLazyTextColorP,
					lazyHintTextColorPointer = newLazyHintColorP)
			}
		}
		override def withBackground(color: ColorSet, preferredShade: ColorLevel): VariableColorContext =
			withBackgroundPointer(colorPointer.preferring(preferredShade)(color))
		override def withBackground(role: ColorRole, preferredShade: ColorLevel): VariableColorContext =
			withBackground(colors(role), preferredShade)
		
		override def withTextColorPointer(p: Changing[Color]): VariableColorContext =
			copy(lazyTextColorPointer = Lazy.initialized(p),
				lazyHintTextColorPointer = Lazy { hintTextColorPointerCache(p) },
				customTextColorPointer = Some(Left(p)))
		override def withGeneralTextColorPointer(p: Changing[ColorSet]): VariableColorContext = {
			val lazyPointer = Lazy { customTextColorFromSetPointer(p) }
			copy(lazyTextColorPointer = lazyPointer,
				lazyHintTextColorPointer = lazyPointer.map { hintTextColorPointerCache(_) },
				customTextColorPointer = Some(Right(p)))
		}
		
		// TODO: Consider font size changes
		override def *(mod: Double): VariableColorContext = mapBase { _ * mod }
		
		
		// OTHER    --------------------------
		
		private def customTextColorFromSetPointer(colorSetPointer: Changing[ColorSet],
		                                          backgroundPointer: Changing[Color] = this.backgroundPointer) =
		{
			// Uses a slightly different logic / caching between fixed and variable color sets
			colorSetPointer.fixedValue match {
				case Some(colorSet) =>
					colorPointersCache(backgroundPointer)(textIsLargePointerCache(fontPointer))(contrastStandard)
						._1(colorSet -> Standard)
				case None =>
					customTextColorPointerCache(colorSetPointer)((backgroundPointer, fontPointer, contrastStandard))
			}
		}
	}
	
	class ColorPointerAccess(context: VariableColorContext, preferredLevel: ColorLevel = Standard,
	                         expectsSmallObjectsFlag: Flag = AlwaysFalse)
		extends ColorAccess[Changing[Color]] with ColorAccessLike[Changing[Color], ColorPointerAccess]
	{
		// ATTRIBUTES   ------------------
		
		private lazy val (colorCache, colorsCache) =
			colorPointersCache(context.backgroundPointer)(expectsSmallObjectsFlag)(context.contrastStandard)
		
		
		// IMPLEMENTED  ----------------------
		
		override def expectingSmallObjects: ColorPointerAccess = withExpectingSmallObjectsFlag(AlwaysTrue)
		override def expectingLargeObjects: ColorPointerAccess = withExpectingSmallObjectsFlag(AlwaysFalse)
		override def forText: ColorPointerAccess =
			withExpectingSmallObjectsFlag(textIsLargePointerCache(context.fontPointer))
		
		override def preferring(level: ColorLevel): ColorPointerAccess =
			new ColorPointerAccess(context, level, expectsSmallObjectsFlag)
		
		override def apply(color: ColorSet): Changing[Color] = colorCache(color -> preferredLevel)
		override def apply(role: ColorRole): Changing[Color] = apply(context.colors(role))
		
		override def differentFrom(role: ColorRole, competingColor: Color, moreColors: Color*): Changing[Color] =
			colorsCache((context.colors(role), preferredLevel, competingColor +: moreColors))
		
		
		// OTHER    ---------------------------
		
		/**
		  * @param f A flag that determines whether to expect small text or other small objects
		  * @return Copy of this color access with the specified flag applied
		  */
		def withExpectingSmallObjectsFlag(f: Flag) = new ColorPointerAccess(context, preferredLevel, f)
	}
}

/**
  * Common trait for variable (i.e. pointer-based) color context implementations.
  * Removes generic type parameters from [[VariableColorContextLike]].
  * @author Mikko Hilpinen
  * @since 01.10.2024, v1.3.1
  */
// FIXME: Change textual type once possible
trait VariableColorContext
	extends VariableBaseContext with ColorContext2
		with VariableColorContextLike[VariableColorContext, VariableColorContext]
