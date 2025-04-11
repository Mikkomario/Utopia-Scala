package utopia.reach.component.interactive

import utopia.firmament.context.ScrollingContext
import utopia.firmament.context.text.{StaticTextContext, VariableTextContext}
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.drawing.view.BackgroundViewDrawer
import utopia.firmament.model.enumeration.SizeCategory
import utopia.firmament.model.enumeration.SizeCategory.Small
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.template.eventful.{Changing, Flag}
import utopia.genesis.graphics.StrokeSettings
import utopia.paradigm.color.{Color, ColorRole}
import utopia.reach.component.factory.ContextualMixed
import utopia.reach.component.interactive.Table.Column
import utopia.reach.component.template.{ReachComponent, ReachComponentWrapper}
import utopia.reach.container.multi.ContextualViewStackFactory
import utopia.reach.container.wrapper.scrolling.ScrollingSettings
import utopia.reach.focus.FocusListener

object Table
{
	// NESTED   --------------------------------
	
	class Column[-A](actualize: (ContextualMixed[VariableTextContext], Changing[A], Option[CustomDrawer], View[Boolean], Lazy[Flag]) => ReachComponent,
	                 createHeader: ContextualMixed[VariableTextContext] => ReachComponent)
}
/**
  * A component that displays n rows of items with n columns for each, all of which are aligned to form a table.
  * Optionally supports scrolling as well as selection.
  * @author Mikko Hilpinen
  * @since 10.04.2025, v1.6
  */
// TODO: Continue implementation
class Table[A](columns: Seq[Seq[Column[A]]], context: VariableTextContext, selectionEnabledFlag: Flag,
               scrollingSettings: Option[(ScrollingSettings, ScrollingContext)],
               betweenRowsMargin: Option[SizeCategory], headerBackground: ColorRole,
               background: Option[Either[Color, ColorRole]], selectionBackground: Option[ColorRole],
               columnSeparatorWidth: Double, additionalDrawers: Seq[CustomDrawer],
               focusListeners: Seq[FocusListener], alternateBackground: Boolean)
	extends ReachComponentWrapper
{
	// ATTRIBUTES   ---------------------------
	
	override protected lazy val wrapped: ReachComponent = ???
	
	
	// OTHER    -------------------------------
	
	private def createContentView(listF: ContextualViewStackFactory[VariableTextContext]) = {
		val (coloredListF, altBgP) = background match {
			case Some(bg) =>
				val bgColorP = bg match {
					case Left(color) => Fixed(color)
					case Right(role) => listF.context.colorPointer.light(role)
				}
				val coloredListF = listF.withCustomBackgroundDrawer(BackgroundViewDrawer(bgColorP))
				if (alternateBackground) {
					val altBgP = bgColorP.map(alternativeBackgroundWith)
					val avgColorP = bgColorP.mergeWith(altBgP) { _.average(_) }
					coloredListF.mapContext { _.withBackgroundPointer(avgColorP) } -> Some(altBgP)
				}
				else
					coloredListF.mapContext { _.withBackgroundPointer(bgColorP) } -> None
			
			case None =>
				if (alternateBackground) {
					val bgP = listF.context.backgroundPointer
					val altBgP = bgP.map(alternativeBackgroundWith)
					val avgColorP = bgP.mergeWith(altBgP) { _.average(_) }
					listF.mapContext { _.withBackgroundPointer(avgColorP) } -> Some(altBgP)
				}
				else
					listF -> None
		}
		// TODO: Add selection drawer & separator drawer
		coloredListF.withMargin(betweenRowsMargin)
	}
	
	private def alternativeBackgroundWith(defaultBackground: Color) = {
		val lum = defaultBackground.luminosity
		if (lum < 0.1)
			defaultBackground.lightened
		else if (lum > 0.9)
			defaultBackground.darkened
		else if (lum > 0.5)
			defaultBackground.lightened
		else
			defaultBackground.darkened
	}
}
