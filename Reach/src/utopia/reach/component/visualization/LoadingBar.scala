package utopia.reach.component.visualization

import utopia.firmament.context.color.VariableColorContext
import utopia.firmament.drawing.immutable.CustomDrawableFactory
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.model.enumeration.SizeCategory
import utopia.firmament.model.stack.{StackLength, StackSize}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.range.NumericSpan
import utopia.flow.collection.immutable.{Empty, Pair, Single}
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.Mutate
import utopia.flow.util.RangeExtensions._
import utopia.flow.view.mutable.async.Volatile
import utopia.flow.view.template.eventful.{Changing, Flag}
import utopia.genesis.graphics.DrawLevel.Normal
import utopia.genesis.graphics.Priority.High
import utopia.genesis.graphics.{DrawLevel, DrawSettings, Drawer}
import utopia.genesis.handling.action.{Actor, ActorHandler}
import utopia.paradigm.angular.Angle
import utopia.paradigm.color.{Color, ColorRole}
import utopia.paradigm.motion.motion1d.LinearVelocity
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.reach.component.factory.FromContextComponentFactoryFactory
import utopia.reach.component.factory.contextual.ContextualFactory
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.{ConcreteCustomDrawReachComponent, PartOfComponentHierarchy}
import utopia.reach.component.visualization.LoadingBar.animationVelocity

import scala.concurrent.duration.FiniteDuration
import scala.language.implicitConversions

/**
  * Common trait for loading bar factories and settings
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 13.02.2025, v1.6
  */
trait LoadingBarSettingsLike[+Repr] extends CustomDrawableFactory[Repr] with BarSettingsLike[Repr]
{
	// ABSTRACT	--------------------
	
	def barSettings: BarSettings
	
	/**
	  * @param settings New bar settings to use.
	  * @return Copy of this factory with the specified bar settings
	  */
	def withBarSettings(settings: BarSettings): Repr
	
	
	// IMPLEMENTED	--------------------
	
	override def activeToInactiveHeightRatio = barSettings.activeToInactiveHeightRatio
	override def colorRolePointer = barSettings.colorRolePointer
	override def customColorsPointer = barSettings.customColorsPointer
	override def height = barSettings.height
	override def rounds = barSettings.rounds
	
	override def withActiveToInactiveHeightRatio(ratio: Double) =
		withBarSettings(barSettings.withActiveToInactiveHeightRatio(ratio))
	override def withColorRolePointer(p: Changing[ColorRole]) =
		withBarSettings(barSettings.withColorRolePointer(p))
	override def withCustomColorsPointer(p: Option[Changing[Pair[Color]]]) =
		withBarSettings(barSettings.withCustomColorsPointer(p))
	override def withHeight(height: SizeCategory) = withBarSettings(barSettings.withHeight(height))
	override def withRounds(round: Boolean) = withBarSettings(barSettings.withRounds(round))
	
	
	// OTHER	--------------------
	
	def mapBarSettings(f: Mutate[BarSettings]) = withBarSettings(f(barSettings))
}

object LoadingBarSettings
{
	// ATTRIBUTES	--------------------
	
	val default = apply()
	
	
	// IMPLICIT ------------------------
	
	implicit def wrap(barSettings: BarSettings): LoadingBarSettings = apply(barSettings = barSettings)
}

/**
  * Combined settings used when constructing loading bars
  * @param customDrawers Custom drawers to assign to created components
  * @author Mikko Hilpinen
  * @since 13.02.2025, v1.6
  */
case class LoadingBarSettings(customDrawers: Seq[CustomDrawer] = Empty,
                              barSettings: BarSettings = BarSettings.default)
	extends LoadingBarSettingsLike[LoadingBarSettings]
{
	// IMPLEMENTED	--------------------
	
	override def withBarSettings(settings: BarSettings) = copy(barSettings = settings)
	override def withCustomDrawers(drawers: Seq[CustomDrawer]) = copy(customDrawers = drawers)
}

/**
  * Common trait for factories that wrap a loading bar settings instance
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 13.02.2025, v1.6
  */
trait LoadingBarSettingsWrapper[+Repr] extends LoadingBarSettingsLike[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * Settings wrapped by this instance
	  */
	protected def settings: LoadingBarSettings
	
	/**
	  * @return Copy of this factory with the specified settings
	  */
	def withSettings(settings: LoadingBarSettings): Repr
	
	
	// IMPLEMENTED	--------------------
	
	override def barSettings = settings.barSettings
	override def customDrawers = settings.customDrawers
	
	override def withBarSettings(settings: BarSettings) = mapSettings { _.withBarSettings(settings) }
	override def withCustomDrawers(drawers: Seq[CustomDrawer]) = mapSettings { _.withCustomDrawers(drawers) }
	
	
	// OTHER	--------------------
	
	def mapSettings(f: LoadingBarSettings => LoadingBarSettings) = withSettings(f(settings))
}

/**
  * Factory class used for constructing loading bars using contextual component creation
  * information
  * @author Mikko Hilpinen
  * @since 13.02.2025, v1.6
  */
case class ContextualLoadingBarFactory(hierarchy: ComponentHierarchy, context: VariableColorContext,
                                       settings: LoadingBarSettings = LoadingBarSettings.default)
	extends LoadingBarSettingsWrapper[ContextualLoadingBarFactory]
		with ContextualFactory[VariableColorContext, ContextualLoadingBarFactory]
		with PartOfComponentHierarchy
{
	// IMPLEMENTED	--------------------
	
	override def withContext(context: VariableColorContext) = copy(context = context)
	override def withSettings(settings: LoadingBarSettings) = copy(settings = settings)
	
	
	// OTHER    ------------------------
	
	/**
	  * @param stackWidth Width of this bar
	  * @return A new loading bar
	  */
	def apply(stackWidth: StackLength) = {
		val colorsP = customColorsPointer.getOrElse {
			context.colorPointer.forRole(colorRolePointer).map { mainColor =>
				Pair(mainColor, mainColor.timesAlpha(0.5))
			}
		}
		val appliedHeight = context.margins.around(height) * 0.5
		new LoadingBar(hierarchy, context.actorHandler, colorsP, StackSize(stackWidth, appliedHeight),
			activeToInactiveHeightRatio, customDrawers, rounds)
	}
}

/**
  * Used for defining loading bar creation settings outside the component building process
  * @author Mikko Hilpinen
  * @since 13.02.2025, v1.6
  */
case class LoadingBarSetup(settings: LoadingBarSettings = LoadingBarSettings.default)
	extends LoadingBarSettingsWrapper[LoadingBarSetup]
		with FromContextComponentFactoryFactory[VariableColorContext, ContextualLoadingBarFactory]
{
	// IMPLEMENTED	--------------------
	
	override def withContext(hierarchy: ComponentHierarchy, context: VariableColorContext) =
		ContextualLoadingBarFactory(hierarchy, context, settings)
	
	override def withSettings(settings: LoadingBarSettings) = copy(settings = settings)
}

object LoadingBar extends LoadingBarSetup()
{
	// ATTRIBUTES	--------------------
	
	private lazy val animationVelocity = LinearVelocity(1.0, 6.seconds)
	
	
	// OTHER	--------------------
	
	def apply(settings: LoadingBarSettings) = withSettings(settings)
}
/**
  * A component used for displaying a loading process where the rate of progress cannot be determined
  * @author Mikko Hilpinen
  * @since 12.2.2025, v1.6
  */
class LoadingBar(override val hierarchy: ComponentHierarchy, actorHandler: ActorHandler, colorsP: Changing[Pair[Color]],
                 override val stackSize: StackSize, activeToInactiveHeightRatio: Double = 1,
                 additionalDrawers: Seq[CustomDrawer] = Empty, round: Boolean = false)
	extends ConcreteCustomDrawReachComponent
{
	// ATTRIBUTES   --------------------------
	
	private val advanceP = Volatile.eventful(0.0)
	private val barRangesP = advanceP.map { advance =>
		val origin = (advance * 4) % 1
		val sideWidth = 0.1 + (Angle.circles(advance).sine + 1) / 8.0
		// Case: Covers 100% of this bar
		if (sideWidth >= 0.5)
			Single(0.0 spanTo 1.0)
		// Case: Covers 0% of this bar
		else if (sideWidth <= 0)
			Empty
		else {
			val left = origin - sideWidth
			val right = origin + sideWidth
			// Case: Wraps around left
			if (left < 0)
				Pair(0.0 spanTo right, (1 + left) spanTo 1.0)
			// Case: Wraps around right
			else if (right > 1)
				Pair(0.0 spanTo (right - 1), left spanTo 1.0)
			// Case: Default
			else
				Single(left spanTo right)
		}
	}
	
	override val customDrawers: Seq[CustomDrawer] = Drawer +: additionalDrawers
	
	
	// INITIAL CODE --------------------------
	
	actorHandler += Animator
	
	barRangesP.addListener { event =>
		(event.newValue ++ event.oldValue).view.flatMap { _.ends }.minMaxOption.foreach { minMaxRange =>
			val size = this.size
			val affectedArea = Bounds(
				x = NumericSpan(minMaxRange.map { _ * size.width }),
				y = NumericSpan(0.0, size.height))
			
			repaintArea(affectedArea, High)
		}
	}
	colorsP.addListenerWhile(linkedFlag) { _ => repaint() }
	
	
	// IMPLEMENTED  --------------------------
	
	override def calculatedStackSize: StackSize = stackSize
	
	override def updateLayout(): Unit = ()
	
	
	// NESTED   ------------------------------
	
	private object Drawer extends CustomDrawer
	{
		override val opaque: Boolean = false
		override val drawLevel: DrawLevel = Normal
		
		override def draw(drawer: Drawer, bounds: Bounds): Unit = {
			// Uses antialiasing when drawing with rounding enabled
			val d = if (round) drawer.antialiasing else drawer
			val startX = bounds.position.x
			val ranges = barRangesP.value.map { _.mapEnds { startX + _ * bounds.width } }
			val (activeColor, inactiveColor) = colorsP.value.toTuple
			
			def drawBounds(b: Bounds)(implicit ds: DrawSettings) =
				if (round) d.draw(b.toRoundedRectangle(1.0)) else d.draw(b)
			
			// Draws the inactive area(s) first
			val inactiveYRange = {
				if (activeToInactiveHeightRatio == 1.0)
					bounds.y
				else {
					val inactiveAdvance = bounds.height / activeToInactiveHeightRatio / 2
					val center = bounds.centerY
					NumericSpan(center - inactiveAdvance, center + inactiveAdvance)
				}
			}
			DrawSettings.onlyFill(inactiveColor).use { implicit ds =>
				val fullInactiveBounds = bounds.withY(inactiveYRange)
				// Case: Fully inactive, or rounding enabled => Draws the full bar width
				if (round || ranges.isEmpty)
					drawBounds(fullInactiveBounds)
				else {
					// Draws the leftmost part
					val firstRange = ranges.head
					if (firstRange.start > startX)
						drawBounds(fullInactiveBounds.mapX { _.withEnd(firstRange.start) })
					// Draws the area(s) between the active ranges
					ranges.paired.foreach { p =>
						drawBounds(fullInactiveBounds.withX(p.first.end spanTo p.second.start))
					}
					// Draws the rightmost part
					val lastRange = ranges.last
					if (lastRange.end < bounds.rightX)
						drawBounds(fullInactiveBounds.mapX { _.withStart(lastRange.end) })
				}
			}
			
			// Next draws the active areas
			if (ranges.nonEmpty)
				DrawSettings.onlyFill(activeColor).use { implicit ds =>
					ranges.foreach { xRange => drawBounds(bounds.withX(xRange)) }
				}
		}
	}
	
	private object Animator extends Actor
	{
		override def handleCondition: Flag = linkedFlag
		
		override def act(duration: FiniteDuration): Unit =
			advanceP.update { a => (a + animationVelocity(duration)) % 1 }
	}
}
