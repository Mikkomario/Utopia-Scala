package utopia.firmament.context.color

import utopia.firmament.context.ColorAccessLike
import utopia.firmament.context.base.{BaseContext, VariableBaseContext, VariableBaseContextWrapper}
import utopia.firmament.context.color.VariableColorContext.ColorPointerAccess
import utopia.firmament.context.text.VariableTextContext
import utopia.flow.collection.immutable.Pair
import utopia.flow.collection.immutable.caching.cache.{Cache, WeakCache}
import utopia.flow.util.EitherExtensions._
import utopia.flow.util.Mutate
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.immutable.eventful.{AlwaysFalse, AlwaysTrue}
import utopia.flow.view.template.eventful.{Changing, Flag}
import utopia.genesis.text.Font
import utopia.paradigm.color.ColorLevel.Standard
import utopia.paradigm.color.ColorShade.{Dark, Light}
import utopia.paradigm.color.{Color, ColorLevel, ColorRole, ColorScheme, ColorSet}
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
	
	// 2 levels deep cache, where the keys are:
	//      1) Color scheme
	//      2) Color role pointer
	// And values are color set pointers
	private val rolePointerToSetPointerCache = WeakCache.weakKeys { colorScheme: ColorScheme =>
		WeakCache { roleP: Changing[ColorRole] => roleP.map { colorScheme(_) } }
	}
	
	// 4 levels deep cache system for weakly storing generated pointers
	// Keys are:
	//      1) Background color pointer
	//      2) Expects large objects -flag
	//      3) Applied color contrast standard
	//      4) Either
	//          4.1) Color set + preferred color level => Best contextual color pointer
	//          4.2) Color set + preferred color level + competing colors => Best contextual color pointer
	private val colorPointersCache = WeakCache.weakKeys { bgP: Changing[Color] =>
		WeakCache.weakKeys { largeFlag: Changing[Boolean] =>
			Cache { standard: ColorContrastStandard =>
				val colorCache = WeakCache.weakValues[(ColorSet, ColorLevel), Changing[Color]] { case (color, level) =>
					bgP.mergeWith(largeFlag) { (bg, large) =>
						color.against(bg, level, standard.minimumContrast(large = large))
					}
				}
				val colorsCache = WeakCache
					.weakValues[(ColorSet, ColorLevel, Seq[Color]), Changing[Color]] { case (color, level, others) =>
						bgP.mergeWith(largeFlag) { (bg, large) =>
							color.againstMany(bg +: others, level, standard.minimumContrast(large = large))
						}
					}
				(colorCache, colorsCache)
			}
		}
	}
	// 2 levels deep cache for custom color set pointer mappings
	// Keys are:
	//      1) Custom color set pointer
	//      2) Background color pointer + is large flag + color contrast standard + preferred shade
	// NB: Only use this with variable custom color set pointers
	private val colorSetMappingPointerCache = WeakCache.weakKeys { setP: Changing[ColorSet] =>
		Cache[(Changing[Color], Flag, ColorContrastStandard, ColorLevel), Changing[Color]] {
			case (bgP, isLargeP, standard, preferredLevel) =>
				setP.mergeWith(bgP, isLargeP) { (colorSet, bg, large) =>
					colorSet.against(bg, preferredLevel, minimumContrast = standard.minimumContrast(large))
				}
		}
	}
	/**
	  * A cache for weakly storing created color set against multiple backgrounds -pointers.
	  * Keys are:
	  *     1. Mapped color set pointer
	  *     1. Additional background color -pointer
	  *     1. Primary background color -pointer
	  *     1. Applied color contrast standard, is large -flag and preferred color shade
	  */
	private val colorSetAgainstManyCache = WeakCache.weakKeys { setP: Changing[ColorSet] =>
		WeakCache.weakKeys { otherBgP: Changing[Color] =>
			WeakCache.weakKeys { bgP: Changing[Color] =>
				Cache[(ColorContrastStandard, Flag, ColorLevel), Changing[Color]] {
					case (standard, isLargeFlag, preferredLevel) =>
						setP.mergeWith(Vector(bgP, otherBgP, isLargeFlag)) { set =>
							set.againstMany(Pair(bgP, otherBgP).map { _.value }, preferredLevel,
								standard.minimumContrast(isLargeFlag.value))
						}
				}
			}
		}
	}
	
	
	// OTHER    --------------------------
	
	/**
	  * Creates a new variable color context instance
	  * @param base Base context to wrap
	  * @param backgroundPointer Pointer that contains the background color to apply
	  * @param customTextColorPointer A custom text color pointer to apply.
	  *                                 - None (default) if default text color should be applied.
	  *                                 - Some(Left) for a custom pointer
	  *                                 - Some(Right) for a custom color set pointer,
	  *                                 where the applied shade is selected based on this context
	  * @return A new color context instance
	  */
	def apply(base: BaseContext, backgroundPointer: Changing[Color],
	          customTextColorPointer: Option[Either[Changing[Color], Changing[ColorSet]]] = None): VariableColorContext =
	{
		val lazyTextColorP = Lazy {
			customTextColorPointer match {
				case Some(Left(colorP)) => colorP
				case Some(Right(colorSetP)) =>
					colorFromSetPointer(base.contrastStandard, colorSetP, backgroundPointer,
						isLargeFlag = textIsLargePointerCache(base.fontPointer))
				case None => backgroundDefaultTextColorPointerCache(backgroundPointer)
			}
		}
		val lazyHintTextColorP = lazyTextColorP.map { hintTextColorPointerCache(_) }
		_VariableColorContext(VariableBaseContext.from(base), backgroundPointer, lazyTextColorP, lazyHintTextColorP,
			None)
	}
	/**
	  * Converts a color context instance into a variable color context instance
	  * @param context Context instance to convert
	  * @return A variable color context instance, based on the specified context instance
	  */
	@deprecated("Deprecated for removal. Replaced with .toVariableContext", "v1.4")
	def from(context: ColorContext): VariableColorContext = context match {
		case v: VariableColorContext => v
		case s: StaticColorContext => s.toVariableContext
		case c => apply(c, c.backgroundPointer)
	}
	
	private def colorFromSetPointer(contrastStandard: ColorContrastStandard, colorSetPointer: Changing[ColorSet],
	                                backgroundPointer: Changing[Color], preferredShade: ColorLevel = Standard,
	                                isLargeFlag: Flag = AlwaysTrue) =
	{
		// Uses a slightly different logic / caching between fixed and variable color sets
		colorSetPointer.fixedValue match {
			case Some(colorSet) =>
				colorPointersCache(backgroundPointer)(isLargeFlag)(contrastStandard)
					._1(colorSet -> preferredShade)
			case None =>
				colorSetMappingPointerCache(colorSetPointer)(
					(backgroundPointer, isLargeFlag, contrastStandard, preferredShade))
		}
	}
	
	
	// NESTED   --------------------------
	
	private case class _VariableColorContext(base: VariableBaseContext, backgroundPointer: Changing[Color],
	                                         lazyTextColorPointer: View[Changing[Color]],
	                                         lazyHintTextColorPointer: View[Changing[Color]],
	                                         customTextColorPointer: Option[Either[Changing[Color], Changing[ColorSet]]])
		extends VariableColorContext with VariableBaseContextWrapper[VariableBaseContext, VariableColorContext]
	{
		// ATTRIBUTES   ------------------
		
		override lazy val forTextComponents = VariableTextContext(this)
		override lazy val colorPointer = new ColorPointerAccess(this)
		
		
		// IMPLEMENTED  ------------------
		
		override def self: VariableColorContext = this
		
		override def textColorPointer: Changing[Color] = lazyTextColorPointer.value
		override def hintTextColorPointer: Changing[Color] = lazyHintTextColorPointer.value
		
		override def current = StaticColorContext(base.current, backgroundPointer.value,
			customTextColorPointer.map { _.mapBoth { _.value } { _.value } })
		override def toVariableContext = this
		
		override def withBase(base: VariableBaseContext): VariableColorContext = copy(base = base)
		
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
				copy(backgroundPointer = p, lazyTextColorPointer = newLazyTextColorP,
					lazyHintTextColorPointer = newLazyHintColorP)
			}
		}
		override def withGeneralBackgroundPointer(p: Changing[ColorSet], preference: ColorLevel) =
			withBackgroundPointer(backgroundFromSetPointer(p, preference))
		override def withBackgroundRolePointer(p: Changing[ColorRole], preference: ColorLevel) =
			withGeneralBackgroundPointer(rolePointerToSetPointerCache(colors)(p), preference)
		
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
		
		// Modifying the font may also affect text color, if the text color is based on a color set
		override def withFontPointer(p: Changing[Font]) =
			modifyBaseAffectingFont { _.withFontPointer(p) }
		
		override def *(mod: Double): VariableColorContext = modifyBaseAffectingFont { _ * mod }
		
		
		// OTHER    --------------------------
		
		// When font changes, that may also affect text color under certain settings
		private def modifyBaseAffectingFont(f: Mutate[VariableBaseContext]) = {
			val newBase = f(base)
			customTextColorPointer.flatMap { _.rightOption } match {
				case Some(colorSetPointer) =>
					val newLazyTextColorPointer = Lazy {
						customTextColorFromSetPointer(colorSetPointer, fontPointer = newBase.fontPointer) }
					copy(base = newBase, lazyTextColorPointer = newLazyTextColorPointer,
						lazyHintTextColorPointer = newLazyTextColorPointer.map { hintTextColorPointerCache(_) })
				case None => withBase(newBase)
			}
		}
		
		private def backgroundFromSetPointer(colorSetPointer: Changing[ColorSet], preferredShade: ColorLevel) =
			colorFromSetPointer(contrastStandard, colorSetPointer, backgroundPointer, preferredShade)
		
		private def customTextColorFromSetPointer(colorSetPointer: Changing[ColorSet],
		                                          backgroundPointer: Changing[Color] = this.backgroundPointer,
		                                          fontPointer: Changing[Font] = this.fontPointer): Changing[Color] =
			VariableColorContext.colorFromSetPointer(contrastStandard, colorSetPointer, backgroundPointer,
				isLargeFlag = textIsLargePointerCache(fontPointer))
	}
	
	class ColorPointerAccess(context: VariableColorContext, preferredLevel: ColorLevel = Standard,
	                         expectsLargeObjectsFlag: Flag = AlwaysTrue)
		extends ColorAccess[Changing[Color]] with ColorAccessLike[Changing[Color], ColorPointerAccess]
	{
		// ATTRIBUTES   ------------------
		
		private lazy val (colorCache, colorsCache) =
			colorPointersCache(context.backgroundPointer)(expectsLargeObjectsFlag)(context.contrastStandard)
		
		
		// IMPLEMENTED  ----------------------
		
		override def expectingSmallObjects: ColorPointerAccess = withExpectingLargeObjectsFlag(AlwaysTrue)
		override def expectingLargeObjects: ColorPointerAccess = withExpectingLargeObjectsFlag(AlwaysFalse)
		override def forText: ColorPointerAccess =
			withExpectingLargeObjectsFlag(textIsLargePointerCache(context.fontPointer))
		
		override def preferring(level: ColorLevel): ColorPointerAccess =
			new ColorPointerAccess(context, level, expectsLargeObjectsFlag)
		
		override def apply(color: ColorSet): Changing[Color] = colorCache(color -> preferredLevel)
		override def apply(role: ColorRole): Changing[Color] = apply(context.colors(role))
		
		override def differentFrom(role: ColorRole, competingColor: Color, moreColors: Color*): Changing[Color] =
			colorsCache((context.colors(role), preferredLevel, competingColor +: moreColors))
		
		
		// OTHER    ---------------------------
		
		/**
		  * @param colorSetPointer A pointer that contains the color set to apply
		  * @return A pointer that contains version from the specified pointer's current set value,
		  *         which is best within the current context
		  */
		def apply(colorSetPointer: Changing[ColorSet]) =
			colorFromSetPointer(context.contrastStandard, colorSetPointer, context.backgroundPointer, preferredLevel,
				expectsLargeObjectsFlag)
		/**
		  * @param rolePointer A pointer that contains the color role to apply
		  * @return A pointer that contains version from the specified pointer's current color role,
		  *         which is best within the current context
		  */
		def forRole(rolePointer: Changing[ColorRole]) =
			apply(rolePointerToSetPointerCache(context.colors)(rolePointer))
		
		/**
		  * Selects a color that's different from the variable background color, plus another variable color
		  * @param rolePointer Targeted color role
		  * @param competingColorPointer Another background or competing color pointer
		  * @return A pointer that contains the most suitable version of that role's color
		  *         in the targeted variable context
		  */
		def differentFromVariable(rolePointer: Changing[ColorRole], competingColorPointer: Changing[Color]) =
			colorSetAgainstManyCache(rolePointerToSetPointerCache(context.colors)(rolePointer))(
				competingColorPointer)(context.backgroundPointer)(
				(context.contrastStandard, expectsLargeObjectsFlag, preferredLevel))
		
		/**
		  * @param f A flag that determines whether to expect larger text or other color areas
		  * @return Copy of this color access with the specified flag applied
		  */
		def withExpectingLargeObjectsFlag(f: Flag) = new ColorPointerAccess(context, preferredLevel, f)
	}
}

/**
  * Common trait for variable (i.e. pointer-based) color context implementations.
  * Removes generic type parameters from [[VariableColorContextLike]].
  * @author Mikko Hilpinen
  * @since 01.10.2024, v1.4
  */
trait VariableColorContext
	extends VariableBaseContext with ColorContext
		with VariableColorContextLike[VariableColorContext, VariableTextContext]
{
	override def colorPointer: ColorPointerAccess
}