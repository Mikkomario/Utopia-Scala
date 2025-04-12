package utopia.reach.component.interactive

import utopia.firmament.context.ScrollingContext
import utopia.firmament.context.text.VariableTextContext
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.drawing.view.BackgroundViewDrawer
import utopia.firmament.model.enumeration.{SizeCategory, StackLayout}
import utopia.firmament.model.stack.StackSize
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Single
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.immutable.eventful.{AlwaysFalse, Fixed}
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.template.eventful.{Changing, Flag}
import utopia.paradigm.color.{Color, ColorRole}
import utopia.reach.component.factory.{ContextualMixed, Mixed}
import utopia.reach.component.interactive.Table.Column
import utopia.reach.component.label.empty.EmptyLabel
import utopia.reach.component.template.{ReachComponent, ReachComponentWrapper}
import utopia.reach.container.multi.{ContextualViewStackFactory, SegmentGroup, Stack}
import utopia.reach.container.wrapper.scrolling.ScrollingSettings
import utopia.reach.focus.FocusListener

object Table
{
	// NESTED   --------------------------------
	
	trait Column[-A]
	{
		def layout: StackLayout
		
		def actualize(factories: ContextualMixed[VariableTextContext], valuePointer: Changing[A],
		              lazySelectedFlag: Lazy[Flag], drawer: Option[CustomDrawer]): ReachComponent
		
		def createHeader(factories: ContextualMixed[VariableTextContext]): ReachComponent
	}
	
	/*
	private class _Column[-A](f: (ContextualMixed[VariableTextContext], Changing[A], Option[CustomDrawer], Flag) => ReachComponent,
	                  hf: ContextualMixed[VariableTextContext] => ReachComponent)
		extends Column[A]
	{
		override def actualize(factories: ContextualMixed[VariableTextContext], valuePointer: Changing[A],
		                       drawer: Option[CustomDrawer], selectedFlag: Flag): ReachComponent =
			f(factories, valuePointer, drawer, selectedFlag)
		override def createHeader(factories: ContextualMixed[VariableTextContext]): ReachComponent = hf(factories)
	}*/
}
/**
  * A component that displays n rows of items with n columns for each, all of which are aligned to form a table.
  * Optionally supports scrolling as well as selection.
  * @author Mikko Hilpinen
  * @since 10.04.2025, v1.6
  */
// TODO: Continue implementation
class Table[A](columns: Seq[Seq[Column[A]]], context: VariableTextContext, contentP: Changing[Seq[A]],
               selectionEnabledFlag: Flag, scrollingSettings: Option[(ScrollingSettings, ScrollingContext)],
               betweenRowsMargin: Option[SizeCategory], headerBackground: ColorRole,
               background: Option[Either[Color, ColorRole]], selectionBackground: Option[ColorRole],
               columnSeparatorWidth: Double, borderWidth: Double, additionalDrawers: Seq[CustomDrawer],
               focusListeners: Seq[FocusListener], alternateBackground: Boolean)
	extends ReachComponentWrapper
{
	// ATTRIBUTES   ---------------------------
	
	private lazy val segmentGroup = SegmentGroup.rowsWithLayouts(columns.view.flatten.map { _.layout }.toOptimizedSeq)
	
	// TODO: Add locking based on selectionEnabledFlag and possibly link state
	private val selectedIndexP = Pointer.eventful(-1)
	
	override protected lazy val wrapped: ReachComponent = ???
	
	
	// TODO: Add auto-repaint when some pointers are updated
	
	
	// COMPUTED -------------------------------
	
	private def selectionPermanentlyDisabled = selectionEnabledFlag.isAlwaysFalse
	
	
	// OTHER    -------------------------------
	
	/**
	  * Creates the main content view, which consists of 0-n row components within a view stack.
	  * @param listF A factory for constructing the stack
	  * @return The stack that contains the table rows
	  */
	// TODO: Add required contrast against header & borders
	private def createContentView(listF: ContextualViewStackFactory[VariableTextContext]) = {
		val (coloredListF, altBgP) = determineContentColors(listF)
		val appliedSelectionBgP = selectionBackground.map { role => coloredListF.context.colorPointer(role) }
		// TODO: Add separator drawer and border drawer
		coloredListF.withMargin(betweenRowsMargin).mapPointer(contentP, Mixed) { (rowF, valueP, index) =>
			createRowView(rowF, valueP, index, altBgP, appliedSelectionBgP)
		}
	}
	
	/**
	  * Determines the background and/or context color to apply when creating content
	  * @param contentF Content factory that needs color handling
	  * @return A copy of the specified factory with color handling included,
	  *         plus a pointer that contains the applied alternating row background color, if applicable.
	  */
	private def determineContentColors(contentF: ContextualViewStackFactory[VariableTextContext]) =
		background match {
			// Case: Draws a custom background
			//       => Applies it to the context, and uses it to determine the alternating color, if applicable
			case Some(bg) =>
				val bgColorP = bg match {
					case Left(color) => Fixed(color)
					case Right(role) => contentF.context.colorPointer.light(role)
				}
				val coloredListF = contentF.withCustomBackgroundDrawer(BackgroundViewDrawer(bgColorP))
				// Case: Alternating color also requested
				//       => Determines it based on the primary background.
				//          The context color is an average between these two.
				if (alternateBackground) {
					val altBgP = bgColorP.map(alternativeBackgroundWith)
					val avgColorP = bgColorP.mergeWith(altBgP) { _.average(_) }
					coloredListF.mapContext { _.withBackgroundPointer(avgColorP) } -> Some(altBgP)
				}
				else
					coloredListF.mapContext { _.withBackgroundPointer(bgColorP) } -> None
			
			// Case: No custom background => Checks whether alternating background should still be applied
			case None =>
				// Case: Alternating background applied => Determines it based on the context background
				if (alternateBackground) {
					val bgP = contentF.context.backgroundPointer
					val altBgP = bgP.map(alternativeBackgroundWith)
					val avgColorP = bgP.mergeWith(altBgP) { _.average(_) }
					contentF.mapContext { _.withBackgroundPointer(avgColorP) } -> Some(altBgP)
				}
				// Case: No background used
				else
					contentF -> None
		}
	
	/**
	  * Creates a single table row component
	  * @param rowF A factory for constructing this component
	  * @param valueP A pointer that contains the value displayed on this row
	  * @param index Index of this row
	  * @param altBgP A pointer that contains the alternative background color applied to every second row.
	  *               None if no alternative background coloring is used.
	  * @param selectionBgP A pointer that contains the background color for the selected row.
	  *                     None if selection shouldn't be visualized this way.
	  * @return A new row component
	  */
	private def createRowView(rowF: ContextualMixed[VariableTextContext], valueP: Changing[A], index: Int,
	                          altBgP: Option[Changing[Color]], selectionBgP: Option[Changing[Color]]) =
	{
		val appliedAltBgP = if (index % 2 == 0) altBgP else None
		
		// Determines how selection and background-drawing are handled
		val (lazySelectedFlag, rowBgDrawer) = {
			// Case: Selection is not enabled => Only applies the alternating background-drawing, if applicable
			if (selectionPermanentlyDisabled)
				Lazy.initialized(AlwaysFalse) -> appliedAltBgP.map(BackgroundViewDrawer.apply)
			// Case: Selection is or may become enabled
			else
				selectionBgP match {
					// Case: Selection is highlighted using a background color
					//       => Specifies a custom background drawer based around selection
					case Some(selectionBgP) =>
						val selectedFlag: Flag = selectedIndexP.lightMap { _ == index }
						// Also takes the alternating background color into account, if applicable
						val drawer = appliedAltBgP match {
							case Some(altColorP) =>
								BackgroundViewDrawer(View {
									if (selectedFlag.value) selectionBgP.value else altColorP.value
								})
							case None => BackgroundViewDrawer(selectionBgP).conditional(selectedFlag)
						}
						Lazy.initialized(selectedFlag) -> Some(drawer)
					
					// Case: Selection is visualized in some other manner
					//       => Tracks selection but only applies the alternating background drawer, if applicable
					case None =>
						val lazySelectedFlag = Lazy[Flag] { selectedIndexP.lightMap { _ == index } }
						val drawer = appliedAltBgP.map(BackgroundViewDrawer.apply)
						lazySelectedFlag -> drawer
				}
		}
		// Simplifies the row construction if there's only a single column group to display
		columns.oneOrMany match {
			// Case: Only a single column group => Wraps it directly
			case Left(onlyColumnGroup) =>
				createColumnGroupView(rowF, segmentGroup, onlyColumnGroup, valueP, lazySelectedFlag, rowBgDrawer)
			// Case: Multiple column groups => Wraps them in a stack
			case Right(columnGroups) =>
				rowF(Stack).row
					.build(Mixed) { groupF =>
						// Shifts the segment group in order to span all columns
						var actualizedColumnCount = 0
						columnGroups.map { columnGroup =>
							val localSegmentGroup = segmentGroup.drop(actualizedColumnCount)
							actualizedColumnCount += columnGroup.size
							createColumnGroupView(groupF, localSegmentGroup, columnGroup, valueP, lazySelectedFlag,
								rowBgDrawer)
						}
					}
					.parent
		}
	}
	
	/**
	  * Creates a view component for a column group (i.e. a related sequence of columns)
	  * @param groupF A factory for constructing this component
	  * @param segmentGroup A segment group that will manage the segmentation for these columns
	  * @param columns Columns to create components for
	  * @param valueP A pointer that contains this row's current value
	  * @param lazySelectedFlag A flag that contains true while this row is selected. Lazily constructed.
	  * @param rowBgDrawer A drawer for setting row background (optional)
	  * @return A new component to represent this column group
	  */
	private def createColumnGroupView(groupF: ContextualMixed[VariableTextContext], segmentGroup: SegmentGroup,
	                                  columns: Seq[Column[A]], valueP: Changing[A], lazySelectedFlag: Lazy[Flag],
	                                  rowBgDrawer: Option[CustomDrawer]) =
		columns.emptyOneOrMany match {
			// Case: No columns on this group (unexpected) => Creates a placeholder component
			case None => groupF.withoutContext(EmptyLabel)(StackSize.any)
			// Case: Only a single column in this group => Wraps it using segmentation, but not using a Stack
			case Some(Left(onlyColumn)) =>
				segmentGroup.contextual(groupF.context)
					.buildUnderSingle(groupF.hierarchy, Mixed) { factories =>
						Single(onlyColumn.actualize(factories.next(), valueP, lazySelectedFlag, rowBgDrawer))
					}
					._1.head
			// Case: Multiple columns => Wraps them in a stack
			case Some(Right(columns)) =>
				groupF(Stack).row.related
					.buildSegmented(Mixed, segmentGroup) { factories =>
						columns.map { _.actualize(factories.next(), valueP, lazySelectedFlag, rowBgDrawer) }
					}
					.parent
		}
	
	/**
	  * Determines an alternating background color
	  * @param defaultBackground Default background color
	  * @return A slightly modified version of the default color to be used on every other row
	  */
	private def alternativeBackgroundWith(defaultBackground: Color) = {
		// Darkens dark colors (unless already near black) and lightens light colors (unless already near white)
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
