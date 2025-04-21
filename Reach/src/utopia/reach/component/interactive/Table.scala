package utopia.reach.component.interactive

import utopia.firmament.context.ScrollingContext
import utopia.firmament.context.text.VariableTextContext
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.drawing.view.BackgroundViewDrawer
import utopia.firmament.model.enumeration.{SizeCategory, StackLayout}
import utopia.firmament.model.stack.StackSize
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Pair, Single}
import utopia.flow.util.RangeExtensions._
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.immutable.eventful.{AlwaysFalse, Fixed}
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.template.eventful.{Changing, Flag}
import utopia.genesis.graphics.DrawLevel.Background
import utopia.genesis.graphics.{DrawLevel, DrawSettings, Drawer}
import utopia.paradigm.color.{Color, ColorRole}
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.{Bounds, HasBounds}
import utopia.reach.component.factory.{ContextualMixed, Mixed}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.interactive.Table.Column
import utopia.reach.component.label.empty.EmptyLabel
import utopia.reach.component.template.{ReachComponent, ReachComponentWrapper}
import utopia.reach.container.multi.{ContextualStackFactory, ContextualViewStackFactory, SegmentGroup, Stack, ViewStack}
import utopia.reach.container.wrapper.Framing
import utopia.reach.container.wrapper.scrolling.{ScrollView, ScrollingSettings}
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
	
	private class TableBackgroundDrawer(primaryBgView: Option[View[Color]], secondaryBgView: Option[View[Color]],
	                                    selectedRowBgView: Option[View[Color]], selectedIndexView: Option[View[Int]],
	                                    componentsView: View[Seq[HasBounds]])
		extends CustomDrawer
	{
		// ATTRIBUTES   -------------------------
		
		override val opaque: Boolean = true
		override val drawLevel: DrawLevel = Background
		
		
		// IMPLEMENTED  -------------------------
		
		override def draw(drawer: Drawer, bounds: Bounds): Unit = {
			val components = componentsView.value
			val componentCount = components.size
			if (componentCount > 0) {
				val maxIndex = componentCount - 1
				// Case: Only one component => Simplifies the drawing
				if (componentCount == 1) {
					secondaryBgView.orElse(primaryBgView).foreach { c =>
						DrawSettings.onlyFill(c.value).use { implicit ds => drawer.draw(bounds) }
					}
					selectedRowBgView.foreach { c =>
						selectedIndexView.foreach { indexP =>
							if (indexP.value == 0)
								DrawSettings.onlyFill(c.value).use { implicit ds => drawer.draw(bounds) }
						}
					}
				}
				// Case: Multiple components
				else {
					val drawnArea = drawer.clippingBounds match {
						case Some(clip) => clip.overlapWith(bounds)
						case None => Some(bounds)
					}
					drawnArea.foreach { drawArea(drawer, _, components, maxIndex) }
				}
			}
		}
		
		
		// OTHER    --------------------------
		
		private def drawArea(drawer: Drawer, bounds: Bounds, components: Seq[HasBounds], maxIndex: Int) =
		{
			// Draws the background, if applicable
			primaryBgView.foreach { c =>
				DrawSettings.onlyFill(c.value).use { implicit ds => drawer.draw(bounds) }
			}
			// Draws every other row with the secondary background, if applicable
			secondaryBgView.foreach { c =>
				DrawSettings.onlyFill(c.value).use { implicit ds =>
					components.indices.foreach { i =>
						if (i % 2 == 0)
							drawRowBackground(drawer, bounds, components, i, maxIndex)
					}
				}
			}
			// Draws the selected row
			selectedRowBgView.foreach { c =>
				selectedIndexView.map { _.value }.filter { _ >= 0 }.foreach { index =>
					DrawSettings.onlyFill(c.value).use { implicit ds =>
						drawRowBackground(drawer, bounds, components, index, maxIndex)
					}
				}
			}
		}
		
		// NB: Assumes that there are >1 rows
		private def drawRowBackground(drawer: Drawer, bounds: Bounds, components: Seq[HasBounds], index: Int,
		                              maxIndex: Int)
		                             (implicit ds: DrawSettings) =
		{
			// Determines the drawn area y range
			val main = components(index).bounds
			val topY = {
				if (index == 0)
					main.topY
				else
					(components(index - 1).bottomY + main.topY) / 2
			}
			val bottomY = {
				if (index == maxIndex)
					main.bottomY
				else
					(main.bottomY + components(index + 1).topY) / 2
			}
			// If the range fits within the (clipping) bounds, draws it
			if (bottomY >= bounds.topY && topY <= bounds.bottomY)
				drawer.draw(bounds.withY(topY spanTo bottomY))
		}
	}
}
/**
  * A component that displays n rows of items with n columns for each, all of which are aligned to form a table.
  * Optionally supports scrolling as well as selection.
  * @author Mikko Hilpinen
  * @since 10.04.2025, v1.6
  */
// TODO: Continue implementation
class Table[A](override val hierarchy: ComponentHierarchy, context: VariableTextContext, columns: Seq[Seq[Column[A]]],
               contentP: Changing[Seq[A]], selectionEnabledFlag: Flag,
               scrollingSettings: Option[(ScrollingSettings, ScrollingContext)],
               betweenRowsMargin: Option[SizeCategory], headerBackground: ColorRole,
               background: Option[Either[Color, ColorRole]], selectionBackground: Option[ColorRole],
               columnSeparatorWidth: Double, borderWidth: Double, customDrawers: Seq[CustomDrawer],
               focusListeners: Seq[FocusListener], alternateBackground: Boolean)
	extends ReachComponentWrapper
{
	// ATTRIBUTES   ---------------------------
	
	private lazy val segmentGroup = SegmentGroup.rowsWithLayouts(columns.view.flatten.map { _.layout }.toOptimizedSeq)
	
	// TODO: Add locking based on selectionEnabledFlag and possibly link state
	private val selectedIndexP = Pointer.eventful(-1)
	
	private lazy val headerColorP = background match {
		case None => context.colorPointer.dark(headerBackground)
		case Some(Left(bgColor)) => context.colorPointer.dark.differentFrom(headerBackground, bgColor)
		case Some(Right(bgColorRole)) =>
			if (bgColorRole == headerBackground) {
				val background = context.colors(bgColorRole).light
				context.colorPointer.dark.differentFrom(headerBackground, background)
			}
			else
				context.colorPointer.dark(headerBackground)
	}
	
	override protected lazy val wrapped: ReachComponent = {
		// If borders are applied, the whole component is wrapped in a colored Framing
		if (borderWidth >= 0.5) {
			val component = Framing.withContext(hierarchy, context)
				.withBackground(headerColorP).withCustomDrawers(customDrawers)
				.build(Stack) { stackF => createHeaderAndContentView(stackF, hasHeaderBackground = true) }.parent
			
			// Adds automated repaint when header/border color changes
			headerColorP.addListenerWhile(linkedFlag) { _ => component.repaint() }
			
			component
		}
		else
			createHeaderAndContentView(
				Stack.withContext(hierarchy, context).withCustomDrawers(customDrawers), hasHeaderBackground = false)
	}
	
	
	// TODO: Add auto-repaint when some pointers are updated (if needed. BG already handled)
	
	
	// COMPUTED -------------------------------
	
	private def selectionPermanentlyDisabled = selectionEnabledFlag.isAlwaysFalse
	
	
	// OTHER    -------------------------------
	
	private def createHeaderAndContentView(factory: ContextualStackFactory[VariableTextContext],
	                                       hasHeaderBackground: Boolean) =
	{
		factory.withoutMargin.build(Mixed) { factories =>
			val header = createHeaderView(factories, if (hasHeaderBackground) None else Some(headerColorP))
			// The content may be wrapped in a scroll view
			val content = scrollingSettings match {
				case Some((scrollingSettings, scrollContext)) =>
					implicit val c: ScrollingContext = scrollContext
					factories(ScrollView).withSettings(scrollingSettings)
						.build(ViewStack) { createContentView(_, headerColorP, hasHeaderBackground) }.parent
				
				case None => createContentView(factories(ViewStack), headerColorP, hasHeaderBackground)
			}
			
			// Adds automated repaints when the header color changes, either here or for the wrapper
			// TODO: If we have separators, needs content repaint, also
			if (!hasHeaderBackground)
				headerColorP.addListenerWhile(linkedFlag) { _ => header.repaint() }
			
			Pair(header, content)
		}
	}
	
	private def createHeaderView(rowF: ContextualMixed[VariableTextContext], appliedBgP: Option[Changing[Color]]) =
		viewFromColumnGroups(rowF, appliedBgP) { _.createHeader(_) }
	/**
	  * Creates the main content view, which consists of 0-n row components within a view stack.
	  * @param listF A factory for constructing the stack
	  * @return The stack that contains the table rows
	  */
	private def createContentView(listF: ContextualViewStackFactory[VariableTextContext],
	                              headerColorP: Changing[Color], hasHeaderBackground: Boolean) =
	{
		val (coloredListF, altBgP, repaintTriggeringBgP) = determineContentColors(
			listF, if (hasHeaderBackground) None else Some(headerColorP))
		val appliedSelectionBgP = selectionBackground.map { role => coloredListF.context.colorPointer(role) }
		// TODO: Add separator drawer
		val contentView = coloredListF.withMargin(betweenRowsMargin).mapPointer(contentP, Mixed) { (rowF, valueP, index) =>
			createRowView(rowF, valueP, index, altBgP, appliedSelectionBgP)
		}
		
		// Adds automated repaints on background color changes
		repaintTriggeringBgP.foreach { _.addListenerWhile(linkedFlag) { _ => contentView.repaint() } }
		
		contentView
	}
	
	/**
	  * Determines the background and/or context color to apply when creating content
	  * @param contentF Content factory that needs color handling
	  * @param additionalHeaderColorP A pointer that contains the applied header color.
	  *                               None if the header color is already the current context's background color.
	  * @return Returns 3 values:
	  *             1. A copy of the specified factory with color handling included,
	  *             1. A pointer that contains the applied alternating row background color, if applicable.
	  *             1. A color pointer that should trigger content repaint when changed, if applicable
	  */
	private def determineContentColors(contentF: ContextualViewStackFactory[VariableTextContext],
	                                   additionalHeaderColorP: Option[Changing[Color]]) =
		background match {
			// Case: Draws a custom background
			//       => Applies it to the context, and uses it to determine the alternating color, if applicable
			case Some(bg) =>
				val bgColorP = bg match {
					case Left(color) => Fixed(color)
					case Right(role) =>
						val colorF = contentF.context.colorPointer.light
						additionalHeaderColorP match {
							case Some(headerColorP) => colorF.differentFromVariable(Fixed(role), headerColorP)
							case None => colorF(role)
						}
				}
				val coloredListF = contentF.withCustomBackgroundDrawer(BackgroundViewDrawer(bgColorP))
				// Case: Alternating color also requested
				//       => Determines it based on the primary background.
				//          The context color is an average between these two.
				if (alternateBackground) {
					val altBgP = bgColorP.map(alternativeBackgroundWith)
					val avgColorP = bgColorP.mergeWith(altBgP) { _.average(_) }
					(coloredListF.mapContext { _.withBackgroundPointer(avgColorP) }, Some(altBgP), Some(bgColorP))
				}
				else
					(coloredListF.mapContext { _.withBackgroundPointer(bgColorP) }, None, Some(bgColorP))
			
			// Case: No custom background => Checks whether alternating background should still be applied
			case None =>
				// Case: Alternating background applied => Determines it based on the context background
				if (alternateBackground) {
					val bgP = contentF.context.backgroundPointer
					val altBgP = bgP.map(alternativeBackgroundWith)
					val avgColorP = bgP.mergeWith(altBgP) { _.average(_) }
					
					(contentF.mapContext { _.withBackgroundPointer(avgColorP) }, Some(altBgP), Some(altBgP))
				}
				// Case: No background used
				else
					(contentF, None, None)
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
	// TODO: Alternating background drawing doesn't work correctly when there's margin between rows
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
		viewFromColumnGroups(rowF) { _.actualize(_, valueP, lazySelectedFlag, rowBgDrawer) }
	}
	
	// TODO: Return access to locations where the separators would be placed
	private def viewFromColumnGroups(rowF: ContextualMixed[VariableTextContext],
	                                 backgroundP: Option[Changing[Color]] = None)
	                                (construct: (Column[A], ContextualMixed[VariableTextContext]) => ReachComponent) =
	{
		// Simplifies the row construction if there's only a single column group to display
		columns.oneOrMany match {
			// Case: Only a single column group => Wraps it directly
			case Left(onlyColumnGroup) =>
				viewFromColumnGroup(rowF, segmentGroup, onlyColumnGroup, backgroundP)(construct)
				
			// Case: Multiple column groups => Wraps them in a stack
			case Right(columnGroups) =>
				val appliedRowF = {
					val base = rowF(Stack).row
					backgroundP match {
						case Some(bgP) => base.withBackground(bgP)
						case None => base
					}
				}
				appliedRowF
					.build(Mixed) { groupF =>
						// Shifts the segment group in order to span all columns
						var actualizedColumnCount = 0
						columnGroups.map { columnGroup =>
							val localSegmentGroup = segmentGroup.drop(actualizedColumnCount)
							actualizedColumnCount += columnGroup.size
							viewFromColumnGroup(groupF, localSegmentGroup, columnGroup)(construct)
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
	  * @param construct A function that creates a view component for a single column
	  * @return A new component to represent this column group
	  */
	private def viewFromColumnGroup(groupF: ContextualMixed[VariableTextContext], segmentGroup: SegmentGroup,
	                                columns: Seq[Column[A]], backgroundP: Option[Changing[Color]] = None)
	                               (construct: (Column[A], ContextualMixed[VariableTextContext]) => ReachComponent) =
	{
		// Case: No columns on this group (unexpected) => Creates a placeholder component
		if (columns.isEmpty) {
			val labelF = groupF.withoutContext(EmptyLabel)
			backgroundP match {
				case Some(bgP) => labelF.withBackground(bgP)(StackSize.any)
				case None => labelF(StackSize.any)
			}
		}
		// Case: Only a single column in this group (with no custom background)
		//       => Wraps it using segmentation, but not using a Stack
		else if (backgroundP.isEmpty && columns.hasSize(1))
			segmentGroup.contextual(groupF.context)
				.buildUnderSingle(groupF.hierarchy, Mixed) { factories =>
					Single(construct(columns.head, factories.next()))
				}
				._1.head
		// Case: Multiple columns => Wraps them in a stack
		else {
			val rowF = {
				val base = groupF(Stack).row.related
				backgroundP match {
					case Some(bgP) => base.withBackground(bgP)
					case None => base
				}
			}
			rowF.buildSegmented(Mixed, segmentGroup) { factories => columns.map { construct(_, factories.next()) } }
				.parent
		}
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
