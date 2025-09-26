package utopia.reach.component.interactive

import utopia.firmament.context.ScrollingContext
import utopia.firmament.context.text.VariableTextContext
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.model.enumeration.{SizeCategory, StackLayout}
import utopia.firmament.model.stack.StackSize
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, Pair, Single}
import utopia.flow.operator.equality.EqualsFunction
import utopia.flow.operator.filter.Filter
import utopia.flow.util.RangeExtensions._
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.immutable.eventful.{AlwaysFalse, Fixed}
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.template.eventful.{Changing, Flag}
import utopia.genesis.graphics.DrawLevel.{Background, Foreground}
import utopia.genesis.graphics.{DrawLevel, DrawSettings, Drawer, StrokeSettings}
import utopia.genesis.handling.event.consume.ConsumeChoice
import utopia.genesis.handling.event.mouse.{MouseButtonStateEvent, MouseButtonStateListener}
import utopia.paradigm.color.{Color, ColorRole}
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.{Bounds, HasBounds}
import utopia.paradigm.shape.shape2d.line.Line
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.reach.component.factory.{ContextualMixed, Mixed}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.interactive.Table.{Column, ColumnSeparatorsDrawer, TableBackgroundDrawer}
import utopia.reach.component.label.empty.EmptyLabel
import utopia.reach.component.template.focus.FocusableWithState
import utopia.reach.component.template.{ReachComponent, ReachComponentWrapper}
import utopia.reach.container.multi._
import utopia.reach.container.wrapper.Framing
import utopia.reach.container.wrapper.scrolling.{ScrollView, ScrollingSettings}
import utopia.reach.focus.{FocusListener, FocusStateTracker}

object Table
{
	// NESTED   --------------------------------
	
	trait Column[-A]
	{
		def layout: StackLayout
		
		def actualize(factories: ContextualMixed[VariableTextContext], valuePointer: Changing[A],
		              lazySelectedFlag: Lazy[Flag]): ReachComponent
		
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
	                                    selectedRowBgView: Option[View[Color]],
	                                    selectedIndexView: Option[View[Option[Int]]],
	                                    componentsView: View[Seq[HasBounds]])
		extends CustomDrawer
	{
		// ATTRIBUTES   -------------------------
		
		override lazy val opaque: Boolean = primaryBgView.exists { _.value.opaque }
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
							if (indexP.value.contains(0))
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
		
		private def drawArea(drawer: Drawer, bounds: Bounds, components: Seq[HasBounds], maxIndex: Int) = {
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
				selectedIndexView.flatMap { _.value }.foreach { index =>
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
	
	private class ColumnSeparatorsDrawer(separatorXCoordinatesView: View[Iterator[Double]], colorView: View[Color],
	                                     strokeWidth: Double)
		extends CustomDrawer
	{
		// ATTRIBUTES   --------------------------
		
		override val opaque: Boolean = false
		override val drawLevel: DrawLevel = Foreground
		
		
		// IMPLEMENTED  -------------------------
		
		override def draw(drawer: Drawer, bounds: Bounds): Unit = {
			implicit val ds: DrawSettings = StrokeSettings(colorView.value, strokeWidth)
			separatorXCoordinatesView.value.foreach { relativeX =>
				val x = bounds.leftX + relativeX
				drawer.draw(Line(bounds.y.ends.map { Point(x, _) }))
			}
		}
	}
	
	private class SelectMouseListener(stack: Stack, override val handleCondition: Flag)
		extends MouseButtonStateListener
	{
		// ATTRIBUTES   ------------------------
		
		override lazy val mouseButtonStateEventFilter: Filter[MouseButtonStateEvent] =
			MouseButtonStateEvent.filter.leftPressed.over(stack.bounds)
		
		
		// IMPLEMENTED  -----------------------
		
		override def onMouseButtonStateEvent(event: MouseButtonStateEvent): ConsumeChoice = ???
	}
}
/**
  * A component that displays n rows of items with n columns for each, all of which are aligned to form a table.
  * Optionally supports scrolling as well as selection.
  * @author Mikko Hilpinen
  * @since 10.04.2025, v1.6
  */
// TODO: Continue implementation (needs keyboard & mouse interactions)
class Table[A](override val hierarchy: ComponentHierarchy, context: VariableTextContext, columns: Seq[Seq[Column[A]]],
               contentP: Changing[Seq[A]], selectionEnabledFlag: Flag,
               scrollingSettings: Option[(ScrollingSettings, ScrollingContext)],
               betweenRowsMargin: Option[SizeCategory], headerBackground: ColorRole,
               background: Option[Either[Color, ColorRole]], selectionBackground: Option[(ColorRole, ColorRole)],
               columnSeparatorWidth: Double, borderWidth: Double, customDrawers: Seq[CustomDrawer],
               additionalFocusListeners: Seq[FocusListener], alternateBackground: Boolean)
              (implicit itemEquals: EqualsFunction[A])
	extends ReachComponentWrapper with FocusableWithState
{
	// ATTRIBUTES   ---------------------------
	
	private lazy val segmentGroup = SegmentGroup.rowsWithLayouts(columns.view.flatten.map { _.layout }.toOptimizedSeq)
	
	override lazy val focusId: Int = hashCode()
	private val focusTracker = new FocusStateTracker()
	override lazy val focusListeners: Seq[FocusListener] = focusTracker +: additionalFocusListeners
	/**
	  * A flag that contains true while this component may be focused.
	  * Requires selection to be enabled.
	  */
	val focusableFlag = selectionEnabledFlag && linkedFlag
	
	private val rawSelectedIndexP = Pointer.eventful.empty[Int]
	/**
	  * A mutable pointer that determines, which row is currently selected.
	  * Note: Mutating this pointer while selection is disabled has no immediate effect,
	  *       but will only take place once selection is enabled again.
	  */
	lazy val selectedIndexPointer = Pointer.indirect[Option[Int]](
		selectionEnabledFlag.flatMap { if (_) rawSelectedIndexP else Fixed.never }) {
		rawSelectedIndexP.value = _ }
	/**
	  * A mutable pointer that contains the currently selected item.
	  * Note: Mutating this pointer while selection is disabled has no immediate effect,
	  *       but will only take place once selection is enabled again.
	  */
	lazy val selectedItemPointer = Pointer.indirect[Option[A]](
		contentP.mergeWith(selectedIndexPointer) { (content, index) => index.flatMap(content.lift) }) {
		case Some(selected) => rawSelectedIndexP.value = contentP.value.findIndexWhere { itemEquals(selected, _) }
		case None => rawSelectedIndexP.value = None
	}
	
	private lazy val defaultHeaderColorP = background match {
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
	// Applies highlighting while focused
	private lazy val headerColorP = defaultHeaderColorP.mergeWithWhile(focusFlag, linkedFlag) { (color, focused) =>
		if (focused) color.highlighted else color
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
	
	
	// INITIAL CODE ---------------------------
	
	// When content changes, attempts to preserve selection
	// Also limits the selection to the valid index range
	contentP.addHighPriorityListener { e =>
		// Case: Content cleared => Clears selection as well
		if (e.newValue.isEmpty)
			clearSelection()
		else
			rawSelectedIndexP.value.foreach { previouslySelectedIndex =>
				// Looks which item was previously selected, and whether that item may be selected again
				e.oldValue.lift(previouslySelectedIndex)
					.flatMap { previouslySelected => e.newValue.findIndexWhere { itemEquals(previouslySelected, _) } } match
				{
					// Case: Item selection possible => Preserves selection
					case Some(newlySelectedIndex) => rawSelectedIndexP.value = Some(newlySelectedIndex)
					// Case: Selected item was removed => Makes sure selection stays within the valid range
					case None =>
						if (previouslySelectedIndex >= e.newValue.size)
							rawSelectedIndexP.value = Some(e.newValue.size - 1)
				}
			}
	}
	
	// Enables focus while focusable
	focusableFlag.addContinuousListenerAndSimulateEvent(false) { e =>
		if (e.newValue) enableFocusHandling() else disableFocusHandling()
	}
	
	
	// COMPUTED -------------------------------
	
	private def selectionPermanentlyDisabled = selectionEnabledFlag.isAlwaysFalse
	
	
	// IMPLEMENTED  ---------------------------
	
	override def focusFlag: Flag = focusTracker.focusFlag
	
	override def allowsFocusEnter: Boolean = selectionEnabledFlag.value
	override def allowsFocusLeave: Boolean = true
	
	
	// OTHER    -------------------------------
	
	/**
	  * Removes selection from the currently selected item
	  */
	def clearSelection() = rawSelectedIndexP.value = None
	
	private def createHeaderAndContentView(factory: ContextualStackFactory[VariableTextContext],
	                                       hasHeaderBackground: Boolean) =
	{
		val headerAndContent = factory.withoutMargin.build(Mixed) { factories =>
			// Creates the header
			val (header, separatorXCoordinatesView) = createHeaderView(factories,
				if (hasHeaderBackground) None else Some(headerColorP))
			
			// Prepares the column group separator drawer, if applicable
			val separatorsDrawer = {
				if (columnSeparatorWidth < 0.5)
					None
				else
					separatorXCoordinatesView.map { new ColumnSeparatorsDrawer(_, headerColorP, columnSeparatorWidth) }
			}
			
			// The content may be wrapped in a scroll view
			val content = scrollingSettings match {
				case Some((scrollingSettings, scrollContext)) =>
					implicit val c: ScrollingContext = scrollContext
					factories(ScrollView).withSettings(scrollingSettings).withAdditionalCustomDrawers(separatorsDrawer)
						.build(ViewStack) { createContentView(_, headerColorP, hasHeaderBackground = hasHeaderBackground) }
						.parent
				
				case None => createContentView(factories(ViewStack), headerColorP, separatorsDrawer, hasHeaderBackground)
			}
			
			// Adds automated repaints when the header color changes, either here or for the wrapper
			if (!hasHeaderBackground && separatorsDrawer.isEmpty)
				headerColorP.addListenerWhile(linkedFlag) { _ =>
					header.repaint()
				}
			
			Pair(header, content) -> separatorsDrawer.isDefined
		}
		
		// If automated repaints apply to this whole component, applies them here
		if (!hasHeaderBackground && headerAndContent.result)
			headerColorP.addListenerWhile(linkedFlag) { _ => headerAndContent.repaint() }
			
		headerAndContent.parent
	}
	
	private def createHeaderView(rowF: ContextualMixed[VariableTextContext], appliedBgP: Option[Changing[Color]]) =
		viewFromColumnGroups(rowF, appliedBgP) { _.createHeader(_) }
	/**
	  * Creates the main content view, which consists of 0-n row components within a view stack.
	  * @param listF A factory for constructing the stack
	  * @return The stack that contains the table rows
	  */
	private def createContentView(listF: ContextualViewStackFactory[VariableTextContext],
	                              headerColorP: Changing[Color], separatorsDrawer: Option[CustomDrawer] = None,
	                              hasHeaderBackground: Boolean) =
	{
		// Determines content coloring
		val (coloredListF, bgP, altBgP) = determineContentColors(
			listF, if (hasHeaderBackground) None else Some(headerColorP))
		// Determines selection coloring
		val appliedSelectionBgP = selectionBackground.map { case (defaultRole, focusedRole) =>
			val colorPointer = coloredListF.context.colorPointer.light
			val defaultP = colorPointer(defaultRole)
			// Case: Non-focusable component => No separate focus color
			if (focusableFlag.isAlwaysFalse)
				defaultP
			// Case: Using same color role for focused & non-focused state => Applies highlighting when focused
			else if (focusedRole == defaultRole)
				defaultP.mergeWith(focusFlag) { (color, focus) => if (focus) color.darkenedBy(2) else color }
			// Case: Different color while focused => Swaps between two color pointers
			else {
				lazy val focusColorP = colorPointer(focusedRole)
				focusFlag.flatMap { if (_) focusColorP else defaultP }
			}
		}
		
		// Creates the actual view
		var initializedContentView: Option[Stack] = None
		val contentView = coloredListF.withMargin(betweenRowsMargin)
			.withCustomBackgroundDrawer(
				new TableBackgroundDrawer(bgP, altBgP, appliedSelectionBgP,
					if (selectionPermanentlyDisabled) None else Some(selectedIndexPointer),
					View {
						initializedContentView match {
							case Some(stack) => stack.components
							case None => Empty
						}
					}))
			.withAdditionalCustomDrawers(separatorsDrawer)
			.mapPointer(contentP, Mixed)(createRowView)
		initializedContentView = Some(contentView)
		
		// Automatically repaints the affected area when selection changes
		if (selectionBackground.isDefined)
			selectedIndexPointer.addListenerWhile(linkedFlag) { e =>
				val components = contentView.components
				e.values.iterator.flatten.flatMap(components.lift).map { _.bounds }
					.reduceOption { (b1, b2) => Bounds.around(Pair(b1, b2)) }
					.foreach { contentView.repaintArea(_) }
			}
		
		contentView
	}
	
	/**
	  * Determines the background and/or context color to apply when creating content
	  * @param contentF Content factory that needs color handling
	  * @param additionalHeaderColorP A pointer that contains the applied header color.
	  *                               None if the header color is already the current context's background color.
	  * @return Returns 3 values:
	  *             1. A copy of the specified factory with color handling included,
	  *             1. A pointer that contains the applied background color, if applicable
	  *             1. A pointer that contains the applied alternating row background color, if applicable.
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
				// Case: Alternating color also requested
				//       => Determines it based on the primary background.
				//          The context color is an average between these two.
				if (alternateBackground) {
					val altBgP = bgColorP.map(alternativeBackgroundWith)
					val avgColorP = bgColorP.mergeWith(altBgP) { _.average(_) }
					(contentF.mapContext { _.withBackgroundPointer(avgColorP) }, Some(bgColorP), Some(altBgP))
				}
				else
					(contentF.mapContext { _.withBackgroundPointer(bgColorP) }, Some(bgColorP), None)
			
			// Case: No custom background => Checks whether alternating background should still be applied
			case None =>
				// Case: Alternating background applied => Determines it based on the context background
				if (alternateBackground) {
					val bgP = contentF.context.backgroundPointer
					val altBgP = bgP.map(alternativeBackgroundWith)
					val avgColorP = bgP.mergeWith(altBgP) { _.average(_) }
					
					(contentF.mapContext { _.withBackgroundPointer(avgColorP) }, None, Some(altBgP))
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
	  * @return A new row component
	  */
	private def createRowView(rowF: ContextualMixed[VariableTextContext], valueP: Changing[A], index: Int) =
	{
		// Determines how selection and background-drawing are handled
		val lazySelectedFlag = {
			// Case: Selection is not enabled => Only applies the alternating background-drawing, if applicable
			if (selectionPermanentlyDisabled)
				Lazy.initialized(AlwaysFalse)
			// Case: Selection is or may become enabled
			else
				Lazy[Flag] { selectedIndexPointer.lightMap { _.contains(index) } }
		}
		viewFromColumnGroups(rowF) { _.actualize(_, valueP, lazySelectedFlag) }._1
	}
	
	private def viewFromColumnGroups(rowF: ContextualMixed[VariableTextContext],
	                                 backgroundP: Option[Changing[Color]] = None)
	                                (construct: (Column[A], ContextualMixed[VariableTextContext]) => ReachComponent) =
	{
		// Simplifies the row construction if there's only a single column group to display
		columns.oneOrMany match {
			// Case: Only a single column group => Wraps it directly
			case Left(onlyColumnGroup) =>
				viewFromColumnGroup(rowF, segmentGroup, onlyColumnGroup, backgroundP)(construct) -> None
				
			// Case: Multiple column groups => Wraps them in a stack
			case Right(columnGroups) =>
				val appliedRowF = {
					val base = rowF(Stack).row
					backgroundP match {
						case Some(bgP) => base.withBackground(bgP)
						case None => base
					}
				}
				val row = appliedRowF
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
				val separatorXCoordinatesView = View {
					row.components.iterator.map { _.bounds.x }
						.paired.map { bounds => (bounds.first.end + bounds.second.start) / 2 }
				}
				row -> Some(separatorXCoordinatesView)
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
				.component.head
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
