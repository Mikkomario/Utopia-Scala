package utopia.reflection.component.swing.input

import utopia.firmament.component.display.{Refreshable, RefreshableWithPointer}
import utopia.firmament.context.AnimationContext
import utopia.firmament.context.ComponentCreationDefaults.componentLogger
import utopia.firmament.context.text.StaticTextContext
import utopia.firmament.controller.data.ContainerContentDisplayer
import utopia.firmament.image.SingleColorIcon
import utopia.firmament.model.enumeration.StackLayout.Center
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.paradigm.color.{Color, ColorRole, ColorSet}
import utopia.paradigm.enumeration.Axis.X
import utopia.reflection.component.swing.button.ImageButton
import utopia.reflection.component.swing.label.ItemLabel
import utopia.reflection.component.swing.template.StackableAwtComponentWrapperWrapper
import utopia.reflection.container.swing.layout.multi.{CollectionView, Stack}
import utopia.reflection.container.swing.layout.wrapper.TagFraming

import scala.concurrent.ExecutionContext

object TagView
{
	/**
	  * Creates a new tag view
	  * @param rowSplitThreshold Pixel threshold at which a new tag line is started
	  * @param contentPointer Pointer to displayed tags (default = new pointer)
	  * @param removeIcon An icon used in tag remove buttons (None if tag removal is not enabled, default)
	  * @param context Component creation context (implicit)
	  * @param animationContext Component animation context (implicit)
	  * @param exc Execution context (implicit)
	  * @return A new tag view
	  */
	def withPointer(rowSplitThreshold: Double,
	                contentPointer: EventfulPointer[Vector[(String, Color)]] = EventfulPointer(Vector()),
	                removeIcon: Option[SingleColorIcon] = None)
	         (implicit context: StaticTextContext, animationContext: AnimationContext, exc: ExecutionContext) =
		new TagView(context, rowSplitThreshold, removeIcon, contentPointer)
	
	/**
	  * Creates a new tag view
	  * @param rowSplitThreshold Pixel threshold at which a new tag line is started
	  * @param initialTags Initially displayed tags (default = empty)
	  * @param removeIcon An icon used in tag remove buttons (None if tag removal is not enabled, default)
	  * @param context Component creation context (implicit)
	  * @param animationContext Component animation context (implicit)
	  * @param exc Execution context (implicit)
	  * @return A new tag view
	  */
	def apply(rowSplitThreshold: Double, initialTags: Vector[(String, Color)] = Vector(),
	          removeIcon: Option[SingleColorIcon] = None)
	         (implicit context: StaticTextContext, animationContext: AnimationContext, exc: ExecutionContext) =
		withPointer(rowSplitThreshold, EventfulPointer(initialTags), removeIcon)
	
	/**
	  * Creates a new tag view
	  * @param rowSplitThreshold Pixel threshold at which a new tag line is started
	  * @param removeIcon An icon used in tag remove buttons
	  * @param contentPointer Pointer to displayed tags (default = new pointer)
	  * @param context Component creation context (implicit)
	  * @param animationContext Component animation context (implicit)
	  * @param exc Execution context (implicit)
	  * @return A new tag view
	  */
	def withPointerWithRemovalEnabled(rowSplitThreshold: Double, removeIcon: SingleColorIcon,
	                                  contentPointer: EventfulPointer[Vector[(String, Color)]] = EventfulPointer(Vector()))
	                                 (implicit context: StaticTextContext, animationContext: AnimationContext,
	                                  exc: ExecutionContext) =
		withPointer(rowSplitThreshold, contentPointer, Some(removeIcon))
	
	/**
	  * Creates a new tag view
	  * @param rowSplitThreshold Pixel threshold at which a new tag line is started
	  * @param removeIcon An icon used in tag remove buttons
	  * @param initialTags Initially displayed tags (default = empty)
	  * @param context Component creation context (implicit)
	  * @param animationContext Component animation context (implicit)
	  * @param exc Execution context (implicit)
	  * @return A new tag view
	  */
	def withRemovalEnabled(rowSplitThreshold: Double, removeIcon: SingleColorIcon,
	                       initialTags: Vector[(String, Color)] = Vector())
	                      (implicit context: StaticTextContext, animationContext: AnimationContext, exc: ExecutionContext) =
		withPointerWithRemovalEnabled(rowSplitThreshold, removeIcon, EventfulPointer(initialTags))
}

/**
  * Displays words as tags
  * @author Mikko Hilpinen
  * @since 27.9.2020, v1.3
  * @param parentContext Context used in the parent component (including text settings)
  * @param rowSplitThreshold Pixel threshold where a new row of tags is initiated
  * @param removeIcon Icon displayed on the tags as a remove button (None if tag removal is not enabled, default)
  * @param contentPointer A pointer to the displayed tags and their colors (default = new pointer)
  * @param animationContext Component animation context (implicit)
  * @param exc Implicit execution context
  */
class TagView(parentContext: StaticTextContext, rowSplitThreshold: Double, removeIcon: Option[SingleColorIcon] = None,
              override val contentPointer: EventfulPointer[Vector[(String, Color)]] = EventfulPointer(Vector()))
             (implicit animationContext: AnimationContext, exc: ExecutionContext)
	extends StackableAwtComponentWrapperWrapper with RefreshableWithPointer[Vector[(String, Color)]]
{
	// ATTRIBUTES   ----------------------------
	
	private val view = parentContext.use { implicit c =>
		CollectionView.contextual[TagLabel](X, rowSplitThreshold) }
	ContainerContentDisplayer.forImmutableStates(view, contentPointer) { _._1 == _._1 } { case (text, color) =>
		new TagLabel(text, color) }
	
	
	// IMPLEMENTED  ----------------------------
	
	override protected def wrapped: CollectionView[_] = view
	
	
	// OTHER    --------------------------------
	
	/**
	  * Adds a new tag to this view
	  * @param tagName Name of the new tag
	  * @param color Background color for the new tag
	  */
	def add(tagName: String, color: Color) = this += tagName -> color
	/**
	  * Adds a new tag to this view
	  * @param tagName Name of the new tag
	  * @param color Background color for the new tag (multiple options, best of which is selected)
	  */
	def add(tagName: String, color: ColorSet): Unit = add(tagName, color.against(parentContext.background))
	/**
	  * Adds a new tag to this view
	  * @param tagName Name of the new tag
	  * @param role Role of the new tag
	  */
	def add(tagName: String, role: ColorRole): Unit = add(tagName, parentContext.color(role))
	
	/**
	  * Adds a new tag to this view
	  * @param tag Tag text + color
	  */
	def +=(tag: (String, Color)) = content :+= tag
	/**
	  * Removes a tag from this view
	  * @param tagName Name of the tag to remove
	  */
	def -=(tagName: String): Unit = content = content.filterNot { _._1 == tagName }
	
	
	// NESTED   --------------------------------
	
	private class TagLabel(initialText: String, initialColor: Color) extends StackableAwtComponentWrapperWrapper
		with Refreshable[(String, Color)]
	{
		// ATTRIBUTES   ------------------------
		
		private val (label, view) = (parentContext/initialColor).use { implicit context =>
			val label = ItemLabel.contextual(initialText)
			label.textInsets = label.textInsets.onlyVertical / 2
			val content = removeIcon.map { icon =>
				Stack.buildRowWithContext(layout = Center, isRelated = true) { s =>
					s += ImageButton
						.contextual(icon.inButton.contextual, isLowPriority = true) { TagView.this -= label.content }
					s += label
				}
			}.getOrElse(label)
			
			label -> new TagFraming(content, initialColor)
		}
		
		
		// IMPLEMENTED  ------------------------
		
		override protected def wrapped = view
		
		override def content_=(newContent: (String, Color)) = {
			label.content = newContent._1
			view.color = newContent._2
		}
		
		override def content = label.content -> view.color
	}
}
