package utopia.reach.component.visualization

import utopia.firmament.factory.FromSizeCategoryFactory
import utopia.firmament.model.enumeration.SizeCategory
import utopia.flow.collection.immutable.Pair
import utopia.flow.util.Mutate
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.template.eventful.Changing
import utopia.paradigm.color.{Color, ColorRole, FromColorRoleFactory}

/**
  * Common trait for bar factories and settings
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 13.02.2025, v1.6
  */
trait BarSettingsLike[+Repr] extends FromSizeCategoryFactory[Repr] with FromColorRoleFactory[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * A pointer that contains the primary color of this bar
	  */
	def colorRolePointer: Changing[ColorRole]
	/**
	  * If specified, overrides this bar's active & inactive colors
	  */
	def customColorsPointer: Option[Changing[Pair[Color]]]
	
	/**
	  * General height of this bar
	  */
	def height: SizeCategory
	/**
	  * The ratio between the heights of this bar's active and inactive parts (as active/inactive). 
	  * If 1.0, both sides are the same height. Should always be >= 1.
	  */
	def activeToInactiveHeightRatio: Double
	
	/**
	  * Whether the ends of this bar should be rounded (true) or rectangular (false).
	  */
	def rounds: Boolean
	
	/**
	  * The ratio between the heights of this bar's active and inactive parts (as active/inactive). 
	  * If 1.0, both sides are the same height. Should always be >= 1.
	  * @param ratio New active to inactive height ratio to use. 
	  *              The ratio between the heights of this bar's active and inactive parts (as 
	  *              active/inactive). 
	  *              If 1.0, both sides are the same height. Should always be >= 1.
	  * @return Copy of this factory with the specified active to inactive height ratio
	  */
	def withActiveToInactiveHeightRatio(ratio: Double): Repr
	/**
	  * A pointer that contains the primary color of this bar
	  * @param p New color role pointer to use. 
	  *          A pointer that contains the primary color of this bar
	  * @return Copy of this factory with the specified color role pointer
	  */
	def withColorRolePointer(p: Changing[ColorRole]): Repr
	/**
	  * If specified, overrides this bar's active & inactive colors
	  * @param p New custom colors pointer to use. 
	  *          If specified, overrides this bar's active & inactive colors
	  * @return Copy of this factory with the specified custom colors pointer
	  */
	def withCustomColorsPointer(p: Option[Changing[Pair[Color]]]): Repr
	/**
	  * General height of this bar
	  * @param height New height to use. 
	  *               General height of this bar
	  * @return Copy of this factory with the specified height
	  */
	def withHeight(height: SizeCategory): Repr
	/**
	  * Whether the ends of this bar should be rounded (true) or rectangular (false).
	  * @param round New rounds to use. 
	  *              Whether the ends of this bar should be rounded (true) or rectangular (false).
	  * @return Copy of this factory with the specified rounds
	  */
	def withRounds(round: Boolean): Repr
	
	
	// COMPUTED --------------------
	
	/**
	  * @return A copy of this item that applies rounding
	  */
	def rounded = withRounds(true)
	/**
	  * @return A copy of this item that doesn't apply rounding
	  */
	def rectangular = withRounds(false)
	
	
	// IMPLEMENTED  ----------------
	
	override def apply(size: SizeCategory): Repr = withHeight(size)
	override def apply(role: ColorRole): Repr = withColorRole(role)
	
	
	// OTHER	--------------------
	
	/**
	  * @param colorRole Color role associated with the constructed bars
	  * @return A copy of this item that applies the specified coloring
	  */
	def withColorRole(colorRole: ColorRole) = withColorRolePointer(Fixed(colorRole))
	def withCustomColors(colors: Pair[Color]) = withCustomColorsPointer(Fixed(colors))
	def withCustomColors(active: Color, inactive: Color): Repr = withCustomColors(Pair(active, inactive))
	def withCustomColor(color: Color) = withCustomColors(color, color.timesAlpha(0.5))
	def withCustomColorsPointer(p: Changing[Pair[Color]]): Repr = withCustomColorsPointer(Some(p))
	
	def mapColorRolePointer(f: Mutate[Changing[ColorRole]]) = withColorRolePointer(f(colorRolePointer))
	def mapHeight(f: Mutate[SizeCategory]) = withHeight(f(height))
}

object BarSettings
{
	// ATTRIBUTES	--------------------
	
	val default = apply()
}

/**
  * Combined settings used when constructing bars
  * @param colorRolePointer            A pointer that contains the primary color of this bar
  * @param customColorsPointer         If specified, overrides this bar's active & inactive colors
  * @param height                      General height of this bar
  * @param activeToInactiveHeightRatio The ratio between the heights of this bar's active and 
  *                                    inactive parts (as active/inactive). 
  *                                    If 1.0, both sides are the same height. Should always be 
  *                                    >= 1.
  * @param rounds                      Whether the ends of this bar should be rounded (true) or 
  *                                    rectangular (false).
  * @author Mikko Hilpinen
  * @since 13.02.2025, v1.6
  */
case class BarSettings(colorRolePointer: Changing[ColorRole] = Fixed(ColorRole.Secondary),
                       customColorsPointer: Option[Changing[Pair[Color]]] = None,
                       height: SizeCategory = SizeCategory.Medium, activeToInactiveHeightRatio: Double = 1.0,
                       rounds: Boolean = false)
	extends BarSettingsLike[BarSettings]
{
	// IMPLEMENTED	--------------------
	
	override def withActiveToInactiveHeightRatio(ratio: Double) = copy(activeToInactiveHeightRatio = ratio)
	override def withColorRolePointer(p: Changing[ColorRole]) = copy(colorRolePointer = p)
	override def withCustomColorsPointer(p: Option[Changing[Pair[Color]]]) = copy(customColorsPointer = p)
	override def withHeight(height: SizeCategory) = copy(height = height)
	override def withRounds(round: Boolean) = copy(rounds = round)
}

/**
  * Common trait for factories that wrap a bar settings instance
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 13.02.2025, v1.6
  */
trait BarSettingsWrapper[+Repr] extends BarSettingsLike[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * Settings wrapped by this instance
	  */
	protected def settings: BarSettings
	
	/**
	  * @return Copy of this factory with the specified settings
	  */
	def withSettings(settings: BarSettings): Repr
	
	
	// IMPLEMENTED	--------------------
	
	override def activeToInactiveHeightRatio = settings.activeToInactiveHeightRatio
	override def colorRolePointer = settings.colorRolePointer
	override def customColorsPointer = settings.customColorsPointer
	override def height = settings.height
	override def rounds = settings.rounds
	
	override def withActiveToInactiveHeightRatio(ratio: Double) = 
		mapSettings { _.withActiveToInactiveHeightRatio(ratio) }
	override def withColorRolePointer(p: Changing[ColorRole]) = mapSettings { _.withColorRolePointer(p) }
	override def withCustomColorsPointer(p: Option[Changing[Pair[Color]]]) = 
		mapSettings { _.withCustomColorsPointer(p) }
	override def withHeight(height: SizeCategory) = mapSettings { _.withHeight(height) }
	override def withRounds(round: Boolean) = mapSettings { _.withRounds(round) }
	
	
	// OTHER	--------------------
	
	def mapSettings(f: BarSettings => BarSettings) = withSettings(f(settings))
}
