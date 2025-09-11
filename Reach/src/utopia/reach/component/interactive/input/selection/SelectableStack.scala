package utopia.reach.component.interactive.input.selection

import utopia.firmament.context.color.VariableColorContextLike
import utopia.firmament.controller.StackItemAreas
import utopia.firmament.controller.data.SelectionKeyListener
import utopia.firmament.drawing.template.CustomDrawer
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.equality.EqualsFunction
import utopia.flow.operator.sign.Sign
import utopia.flow.operator.sign.Sign.{Negative, Positive}
import utopia.flow.view.immutable.eventful.{AlwaysFalse, LazilyInitializedChanging}
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.flow.view.template.eventful.{Changing, Flag}
import utopia.genesis.graphics.{DrawLevel, Drawer}
import utopia.genesis.handling.event.keyboard.Key.{LeftArrow, RightArrow, UpArrow}
import utopia.genesis.handling.event.keyboard.{Key, KeyboardEvents}
import utopia.paradigm.color.Color
import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.reach.component.factory.FromContextComponentFactoryFactory.Ccff
import utopia.reach.component.factory.contextual.ContextualFactory
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.focus.FocusableWithState
import utopia.reach.component.template.{ReachComponent, ReachComponentWrapper}
import utopia.reach.container.multi.{ViewStack, ViewStackSettings}
import utopia.reach.focus.{FocusListener, FocusStateTracker}

/**
 * Adds selection features to a view stack
 *
 * @author Mikko Hilpinen
 * @since 09.09.2025, v1.7
 */
class SelectableStack[A, N <: VariableColorContextLike[N, _], F <: ContextualFactory[N, F]]
(override val hierarchy: ComponentHierarchy, context: N, stackSettings: ViewStackSettings, contentP: Changing[Seq[A]],
 selectionP: EventfulPointer[Option[A]], viewFactory: Ccff[N, F],
 makeView: (F, Changing[A], Flag, Int) => ReachComponent, selectedBgP: Option[Changing[Color]],
 selectionDrawer: Option[SelectionDrawer], arrowKeySelectionEnabled: Boolean, otherSelectionKeys: Map[Key, Sign],
 alternativeKeySelectionCondition: Flag = AlwaysFalse, otherFocusListeners: Seq[FocusListener])
(implicit eq: EqualsFunction[A])
	extends ReachComponentWrapper with FocusableWithState
{
	// ATTRIBUTES   ---------------------------
	
	override lazy val focusId: Int = hashCode()
	private val focusTracker = new FocusStateTracker()
	override lazy val focusListeners: Seq[FocusListener] = focusTracker +: otherFocusListeners
	override val allowsFocusLeave: Boolean = true
	
	private lazy val selectedIndexP = selectionP.mergeWith(contentP) { (selected, content) =>
		selected.flatMap { selected => content.findIndexWhere { eq(selected, _) } }
	}
	
	override protected lazy val wrapped = ViewStack.withContext(hierarchy, context).withSettings(stackSettings)
		.withAdditionalCustomDrawers(selectionDrawer.map { new LocalSelectionDrawer(_) })
		.mapPointer(contentP, viewFactory) { (factory, pointer, index) =>
			// Tracks selection status and modifies the background pointer accordingly
			val (selectedFlag, correctBgFactory) = selectedBgP match {
				// Case: Selected item background is different from normal => Creates a custom background color -pointer
				case Some(bgP) =>
					val selectedFlag: Flag = selectedIndexP.lightMap { _.contains(index) }
					val modifiedFactory = factory.mapContext { _.mapBackgroundPointer { original =>
						selectedFlag.flatMap { if (_) bgP else original }
					} }
					
					selectedFlag -> modifiedFactory
					
				// Case: Selected components have the same background
				case None =>
					LazilyInitializedChanging { selectedIndexP.lightMap { _.contains(index) } } -> factory
			}
			makeView(correctBgFactory, pointer, selectedFlag, index)
		}
	
	private lazy val locationTracker = new StackItemAreas[ReachComponent](wrapped, componentsP)
	
	private val listensToKeyboard = arrowKeySelectionEnabled || otherSelectionKeys.nonEmpty
	
	
	// INITIAL CODE -----------------------
	
	// Sets up key listening
	if (listensToKeyboard) {
		val keysP = stackSettings.axisPointer.map { axis =>
			val axisKeys: Map[Key, Sign] = axis match {
				case X => Map(LeftArrow -> Negative, RightArrow -> Positive)
				case Y => Map(UpArrow -> Negative, RightArrow -> Positive)
			}
			axisKeys ++ otherSelectionKeys
		}
		val listener = SelectionKeyListener
			.withEnabledFlag(linkedFlag && (focusFlag || alternativeKeySelectionCondition))
			.listeningTo(keysP)
			.apply(moveSelectionBy)
		
		context.actorHandler += listener
		KeyboardEvents += listener
	}
	
	// TODO: Adjust selection when displayed components change
	
	
	// COMPUTED ---------------------------
	
	private def content = contentP.value
	private def selectedIndex = selectedIndexP.value
	
	private def componentsP = wrapped.componentsPointer
	private def components = wrapped.components
	
	
	// IMPLEMENTED  -----------------------
	
	override def allowsFocusEnter: Boolean = listensToKeyboard && components.nonEmpty
	override def focusFlag: Flag = focusTracker.focusFlag
	
	
	// OTHER    ---------------------------
	
	/**
	 * Adjusts the selection by the specified amount
	 * @param adjustment Number of items to traverse, in terms of selection
	 */
	def moveSelectionBy(adjustment: Int): Unit = {
		if (adjustment != 0) {
			val currentContent = content
			val optionsCount = currentContent.size
			if (optionsCount > 0)
				selectedIndex match {
					// Case: An item was selected => Adjusts the selection
					case Some(previous) =>
						var index = previous + adjustment
						while (index < 0) {
							index += optionsCount
						}
						currentContent.lift(index % optionsCount).foreach { selectionP.setOne(_) }
					
					// Case: No items selected yet => Selects the first or the last item
					case None =>
						if (adjustment > 0)
							selectionP.setOne(currentContent.head)
						else
							selectionP.setOne(currentContent.last)
				}
		}
	}
	
	
	// NESTED   ---------------------------
	
	private class LocalSelectionDrawer(wrapped: SelectionDrawer) extends CustomDrawer
	{
		// ATTRIBUTES   -------------------
		
		override val opaque: Boolean = false
		
		private val selectedAreaP = selectedIndexP.mergeWith(componentsP) { (index, components) =>
			index.flatMap(components.lift).flatMap(locationTracker.areaOf)
		}
		
		
		// INITIAL CODE -------------------
		
		// Repaints this component when selected area changes
		selectedAreaP.addListenerWhile(linkedFlag) { e =>
			e.values.flatten.reduceOption { (prev, now) => Bounds.around(Pair(prev, now)) }.foreach { repaintArea(_) }
		}
		
		
		// IMPLEMENTED  -------------------
		
		override def drawLevel: DrawLevel = wrapped.drawLevel
		
		override def draw(drawer: Drawer, bounds: Bounds): Unit = selectedAreaP.value.foreach { selected =>
			wrapped.draw(drawer, bounds, selected)
		}
	}
}
