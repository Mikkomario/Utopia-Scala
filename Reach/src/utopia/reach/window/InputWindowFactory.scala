package utopia.reach.window
import utopia.flow.async.Delay
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.flow.event.{AlwaysTrue, ChangingLike}
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.genesis.shape.shape2D.Direction2D
import utopia.reach.component.button.image.ImageButton
import utopia.reach.component.factory.{ContextualMixed, Mixed}
import utopia.reach.component.label.text.TextLabel
import utopia.reach.component.template.ReachComponentLike
import utopia.reach.component.template.focus.Focusable
import utopia.reach.component.wrapper.{ComponentCreationResult, Open, OpenComponent}
import utopia.reach.container.multi.stack.{ContextualStackFactory, SegmentGroup, Stack, ViewStack}
import utopia.reach.container.ReachCanvas
import utopia.reach.container.wrapper.{AlignFrame, Framing}
import utopia.reach.focus.FocusRequestable
import utopia.reflection.component.context.{ColorContext, TextContext}
import utopia.reflection.container.stack.StackLayout.{Center, Fit, Leading, Trailing}
import utopia.reflection.container.swing.window.Popup.PopupAutoCloseLogic
import utopia.reflection.container.swing.window.Window
import utopia.reflection.container.template.window.{ManagedField, RowGroup, RowGroups, WindowButtonBlueprint}
import utopia.reflection.event.HotKey
import utopia.reflection.image.SingleColorIcon
import utopia.reflection.localization.LocalizedString
import utopia.reflection.shape.Alignment
import utopia.reflection.shape.Alignment.{Bottom, Top}
import utopia.reflection.shape.LengthExtensions._

import java.awt.event.KeyEvent
import scala.collection.immutable.VectorBuilder
import scala.concurrent.{ExecutionContext, Promise}

/**
  * Used for creating new windows which contain input fields
  * @author Mikko Hilpinen
  * @since 1.3.2021, v0.1
  * @tparam A Type of result returned by the generated windows
  * @tparam N Type of additional context used by this factory
  */
trait InputWindowFactory[A, N] extends InteractionWindowFactory[A]
{
	// TYPES	---------------------------------
	
	private type RowField = ReachComponentLike with FocusRequestable
	
	
	// ABSTRACT	---------------------------------
	
	/**
	  * @return Component creation context used as the base when creating input fields and their labels
	  */
	protected def fieldCreationContext: ColorContext
	
	/**
	  * @return Component creation context used in the warning pop-up. Determines pop-up background also.
	  */
	protected def warningPopupContext: TextContext
	
	/**
	  * @return Input creation blueprints and the context to use in subsequent creation method calls
	  */
	protected def inputTemplate: (Vector[RowGroups[InputRowBlueprint[ReachComponentLike with FocusRequestable, TextContext]]], N)
	
	/**
	  * @return Icon representing window close action (used in warning pop-ups and the default close button)
	  */
	protected def closeIcon: SingleColorIcon
	
	/**
	  * @param base The basic field creation context
	  * @return 1) Context to use for the field name labels and 2) Context to use for the fields themselves
	  */
	protected def makeFieldNameAndFieldContext(base: ColorContext): (TextContext, TextContext)
	
	/**
	  * Combines components to a single layout
	  * @param content Content to combine in open format, each has a visibility pointer as an additional result
	  * @param context Additional context item created in inputTemplate
	  * @return Combined component
	  */
	protected def buildLayout(factories: ContextualMixed[ColorContext],
							  content: Vector[OpenComponent[ReachComponentLike, ChangingLike[Boolean]]],
							  context: N): ReachComponentLike
	
	/**
	  * Specifies buttons to display on this dialog
	  * @param context Additional context item created in inputTemplate and passed to buildLayout
	  * @param input Function for reading current input state. Returns either Right: model based on input values or
	  *              Left: Warning message that was displayed, along with the field that is now focused
	  * @param warn A function for showing a warning pop-up. Accepts field string key and the message to
	  *             display on the pop-up
	  * @return Button blueprints + a pointer to whether the default button action (if any) can be triggered normally
	  */
	protected def specifyButtons(context: N,
								 input: => Either[(String, ReachComponentLike with FocusRequestable), Model],
								 warn: (String, LocalizedString) => Unit): (Vector[WindowButtonBlueprint[A]], ChangingLike[Boolean])
	
	/**
	  * @return Text to display on the default close button. Empty if no default close button should be displayed.
	  */
	protected def defaultCloseButtonText: LocalizedString
	
	
	// IMPLEMENTED	-----------------------------
	
	override protected def createContent(factories: ContextualMixed[ColorContext]) =
	{
		implicit val canvas: ReachCanvas = factories.parentHierarchy.top
		val (template, dialogContext) = inputTemplate
		val context = fieldCreationContext
		val (nameContext, fieldContext) = makeFieldNameAndFieldContext(context)
		implicit val rowContext: FieldRowContext = FieldRowContext(nameContext, fieldContext)
		val managedFieldsBuilder = new VectorBuilder[(String, ManagedField[RowField])]
		
		// Creates component layouts based on the template row groups
		val openGroups = template.map { groups =>
			Open { hierarchy =>
				groupsToComponent(Mixed(hierarchy).withContext(context), groups, managedFieldsBuilder)
			}
		}
		
		// Builds the final layout
		val content = buildLayout(factories, openGroups, dialogContext)
		
		// Creates the data interface (reading input data from fields)
		val fields = managedFieldsBuilder.result().toMap
		def tryReadData() =
		{
			val resultBuffer = new VectorBuilder[Constant]()
			// Reads values until one read fails
			val failure = fields.view.map { case (key, field) =>
				// Attempts to read field value, may fail
				field.currentValue match
				{
					case Left(errorMessage) =>
						showWarningFor(field.field, errorMessage)
						Some(key -> field.field)
					case Right(value) =>
						// Stores values in the buffer
						resultBuffer += Constant(key, value)
						None
				}
			}.find { _.isDefined }.flatten
			
			failure match
			{
				case Some(failure) => Left(failure)
				case None => Right(Model.withConstants(resultBuffer.result()))
			}
		}
		def warn(key: String, message: LocalizedString) =
		{
			// Finds the field matching the key
			fields.get(key) match
			{
				case Some(field) => showWarningFor(field.field, message)
				case None => println(s"Warning: No input field with key '$key' to display warning: $message")
			}
		}
		
		// Creates the default close button (if enabled)
		val defaultCloseButton = defaultCloseButtonText.notEmpty.map { text =>
			WindowButtonBlueprint.closeWithResult(text, Some(closeIcon),
				hotkey = Some(HotKey.keyWithIndex(KeyEvent.VK_ESCAPE))) { defaultResult }
		}
		
		// Creates the other buttons
		val (customButtons, defaultActionEnabledPointer) = specifyButtons(dialogContext, tryReadData(), warn)
		val buttonsBlueprints = customButtons ++ defaultCloseButton
		
		(content, buttonsBlueprints, defaultActionEnabledPointer)
	}
	
	
	// OTHER	--------------------------------
	
	private def showWarningFor(field: RowField, message: LocalizedString): Unit =
	{
		implicit val logger: Logger = SysErrLogger
		implicit val exc: ExecutionContext = executionContext
		val popupContext = warningPopupContext
		field.requestFocus(forceFocusLeave = true)
		
		// Creates a warning pop-up
		val windowPromise = Promise[Window[_]]()
		
		val window = field.createPopup(popupContext.actorHandler, margin = popupContext.margins.small,
			autoCloseLogic = PopupAutoCloseLogic.WhenAnyKeyPressed) { hierarchy =>
			// The pop-up contains a close button and the warning text
			Framing(hierarchy).buildFilledWithContext(popupContext, popupContext.containerBackground, Stack)
				.apply(popupContext.margins.small.any) { stackF: ContextualStackFactory[TextContext] =>
					stackF.build(Mixed).row(Center) { factories =>
						Vector(
							factories(ImageButton).withIcon(closeIcon) { windowPromise.future.foreach { _.close() } },
							factories(TextLabel)(message)
						)
					}
				}
		}.parent
		// Registers pop-up ownership if possible
		field match {
			case focusableField: Focusable => focusableField.registerOwnershipOf(window.component)
			case _ => ()
		}
		windowPromise.success(window)
		
		// Displays the pop-up and closes it automatically after a while
		window.display()
		Delay(5.seconds) { window.close() }
	}
	
	private def groupsToComponent(factories: ContextualMixed[ColorContext],
								  groups: RowGroups[InputRowBlueprint[RowField, TextContext]],
								  fieldsBuffer: VectorBuilder[(String, ManagedField[RowField])])
								 (implicit context: FieldRowContext): (ReachComponentLike, ChangingLike[Boolean]) =
	{
		// Checks whether segmentation should be used
		val segmentGroup =
		{
			if (groups.rows.count { _.usesSegmentLayout } > 1)
			{
				// Checks what layouts to use inside the segments
				val isLeftFitAllowed = groups.rows.forall { _.allowsFitSegmentLayoutForSide(Direction2D.Left) }
				val isRightFitAllowed = groups.rows.forall { _.allowsFitSegmentLayoutForSide(Direction2D.Right) }
				Some(SegmentGroup.rowsWithLayouts(
					if (isLeftFitAllowed) Fit else Trailing, if (isRightFitAllowed) Fit else Leading))
			}
			else
				None
		}
		
		// If there are multiple groups to use, wraps them in a stack. Otherwise presents the group as is
		if (groups.isSingleGroup)
			groupToComponent(factories, groups.groups.head, segmentGroup, fieldsBuffer)
		else
		{
			val rowGroups = groups.groups
			// Checks whether the groups visibility may change
			// Case: All groups always remain visible => uses a static stack
			if (rowGroups.forall { _.rows.exists { _.isAlwaysVisible } })
				factories(Stack).build(Mixed).column() { factories =>
					groups.groups.map { group => groupToComponent(factories, group, segmentGroup, fieldsBuffer)._1 }
				}.parent -> AlwaysTrue
			// Case: Some groups may appear or disappear => uses a view stack
			else
			{
				val stack = factories(ViewStack).build(Mixed).withFixedStyle() { factories =>
					groups.groups.map { group => groupToComponent(factories.next(), group, segmentGroup, fieldsBuffer) }
				}.parent
				stack -> stack.visibilityPointer
			}
		}
		
	}
	
	private def groupToComponent(factories: ContextualMixed[ColorContext],
								 group: RowGroup[InputRowBlueprint[RowField, TextContext]],
								 segmentGroup: Option[SegmentGroup],
								 fieldsBuffer: VectorBuilder[(String, ManagedField[RowField])])
								(implicit context: FieldRowContext): (ReachComponentLike, ChangingLike[Boolean]) =
	{
		// If this group consists of multiple rows, wraps them in a stack. Otherwise presents the row as is
		if (group.isSingleRow)
			actualizeRow(factories, group.rows.head, segmentGroup, fieldsBuffer)
		else
		{
			// May hide some of the group rows at times
			// Case: all rows are always visible => uses a static stack
			if (group.rows.forall { _.isAlwaysVisible })
				factories(Stack).build(Mixed).column(areRelated = true) { factories =>
					group.rows.map { blueprint => actualizeRow(factories, blueprint, segmentGroup, fieldsBuffer)._1 }
				}.parent -> AlwaysTrue
			// Case: Some rows are not always visible => uses a view stack
			else
			{
				val stack = factories(ViewStack).build(Mixed).withFixedStyle(areRelated = true) { factories =>
					group.rows.map { blueprint => actualizeRow(factories.next(), blueprint, segmentGroup, fieldsBuffer) }
				}.parent
				stack -> stack.visibilityPointer
			}
		}
	}
	
	private def actualizeRow(factories: ContextualMixed[ColorContext],
							 blueprint: InputRowBlueprint[RowField, TextContext], segmentGroup: Option[SegmentGroup],
							 fieldsBuilder: VectorBuilder[(String, ManagedField[RowField])])
							(implicit context: FieldRowContext): (ReachComponentLike, ChangingLike[Boolean]) =
	{
		// Case: Two components are used
		if (blueprint.displaysName)
		{
			// Case: The two components are next to each other horizontally
			if (blueprint.fieldAlignment.isHorizontal && !blueprint.fieldAlignment.isVertical)
			{
				val verticalLayout = blueprint.fieldAlignment.vertical match
				{
					case Top => Leading
					case Bottom => Trailing
					case _ => Center
				}
				segmentGroup match
				{
					// Case segmented grouping is used
					case Some(segmentGroup) =>
						factories(Stack).build(Mixed).segmented(segmentGroup, verticalLayout, areRelated = true) { factories =>
							createHorizontalFieldAndNameRow(factories.next(), blueprint, fieldsBuilder)
						}.parentAndResult
					// Case: Segmented grouping is not used (only single row)
					case None =>
						factories(Stack).build(Mixed).row(verticalLayout, areRelated = true) { factories =>
							createHorizontalFieldAndNameRow(factories, blueprint, fieldsBuilder)
						}.parentAndResult
				}
			}
			// Case: The two components are stacked on top of each other => segmentation is not enabled
			else
			{
				val horizontalLayout = blueprint.fieldAlignment.horizontal match
				{
					case Alignment.Left => Leading
					case Alignment.Right => Trailing
					case _ => if (blueprint.isScalable) Fit else Center
				}
				factories(Stack).build(Mixed).column(horizontalLayout, areRelated = true) { factories =>
					createFieldAndName(factories, blueprint, fieldsBuilder,
						blueprint.fieldAlignment.vertical != Top, horizontalLayout == Fit)
				}.parentAndResult
			}
		}
		// Case: Only the field component is used => no segmentation is used
		else
		{
			// Case: Field doesn't need to be aligned to any side but can fill the whole area
			// => it is attached directly to the stack
			if (blueprint.isScalable)
			{
				val field = blueprint(factories.parentHierarchy, context.fieldContext)
				fieldsBuilder += blueprint.key -> field
				field.field -> blueprint.visibilityPointer
			}
			// Case: Field needs to be aligned => it is wrapped before adding to the layout
			else
				factories(AlignFrame).build(Mixed)(blueprint.fieldAlignment) { factories =>
					val field = blueprint(factories.parentHierarchy, context.fieldContext)
					fieldsBuilder += blueprint.key -> field
					field.field
				}.parent -> blueprint.visibilityPointer
		}
	}
	
	private def createHorizontalFieldAndNameRow(factories: => ContextualMixed[ColorContext],
												blueprint: InputRowBlueprint[RowField, TextContext],
												fieldsBuilder: VectorBuilder[(String, ManagedField[RowField])])
											   (implicit context: FieldRowContext) =
	{
		val fieldNameComesFirst = blueprint.fieldAlignment.horizontal != Alignment.Left
		createFieldAndName(factories, blueprint, fieldsBuilder, fieldNameComesFirst, expandLabel = false)
	}
	
	private def createFieldAndName(factories: => ContextualMixed[ColorContext],
								   blueprint: InputRowBlueprint[RowField, TextContext],
								   fieldsBuilder: VectorBuilder[(String, ManagedField[RowField])],
								   fieldNameIsFirst: Boolean, expandLabel: Boolean)
								  (implicit context: FieldRowContext) =
	{
		// TODO: Possibly add a way for the blueprint to edit a) label creation context and b) label during or after
		//  creation (E.g. by adding mouse listeners)
		val labelFactory =
		{
			val base = factories.withContext(context.nameContext)(TextLabel)
			if (expandLabel)
				base.mapContext { _.expandingHorizontally }
			else
				base
		}
		val fieldNameLabel = labelFactory(blueprint.displayName)
		val field = blueprint(factories.parentHierarchy, context.fieldContext)
		fieldsBuilder += blueprint.key -> field
		// Field ordering depends on the blueprint alignment
		val components = if (fieldNameIsFirst) Vector(fieldNameLabel, field.field) else Vector(field.field, fieldNameLabel)
		
		// Attaches field visibility pointer as a result
		ComponentCreationResult.many(components, blueprint.visibilityPointer)
	}
}

private case class FieldRowContext(nameContext: TextContext, fieldContext: TextContext)