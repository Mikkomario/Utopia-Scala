package utopia.reach.window

import utopia.firmament.context.text.StaticTextContext
import utopia.firmament.localization.LocalizedString
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.template.eventful.Changing
import utopia.flow.view.template.eventful.Flag.wrap
import utopia.paradigm.enumeration.{Alignment, HorizontalDirection}
import utopia.reach.component.factory.FromContextComponentFactoryFactory.Ccff
import utopia.reach.component.hierarchy.ComponentHierarchy

object InputRowBlueprint
{
	/**
	  * Creates a new input row blueprint utilizing a component creation factory
	  * @param factory A factory used for creating the field components
	  * @param key Unique key used for this row
	  * @param displayName Field name displayed on this row (default = empty = no name displayed)
	  * @param fieldAlignment Alignment used when placing the field component (relative to the field name) (default = right)
	  * @param visibilityPointer A pointer to this row's visibility state (default = always visible)
	  * @param isScalable Whether the field can be scaled horizontally (default = true)
	  * @param createField A function for creating a new managed field when component creation context is known.
	  *                    Accepts component creation factory and produces a managed field.
	  * @tparam F Type of contextual component factory version
	  * @return A new input row blueprint
	  */
	def using[F](factory: Ccff[StaticTextContext, F], key: String, displayName: LocalizedString  = LocalizedString.empty,
	             fieldAlignment: Alignment = Alignment.Right, visibilityPointer: Changing[Boolean] = AlwaysTrue,
	             isScalable: Boolean = true)(createField: F => InputField) =
		apply(key, displayName, fieldAlignment, visibilityPointer, isScalable) { (hierarchy, context) =>
			createField(factory.withContext(hierarchy, context))
		}
}

/**
  * Used in creating input rows for input windows
  * @author Mikko Hilpinen
  * @since 26.2.2021, v0.1
  * @param key Unique key used for this row
  * @param displayName Field name displayed on this row (default = empty = no name displayed)
  * @param fieldAlignment Alignment used when placing the field component (relative to the field name) (default = right)
  * @param visibilityPointer A pointer to this row's visibility state (default = always visible)
  * @param isScalable Whether the field can be scaled horizontally (default = true)
  * @param createField A function for creating a new managed field when component creation context is known.
  *                    Accepts component creation hierarchy and context.
  */
case class InputRowBlueprint(key: String, displayName: LocalizedString = LocalizedString.empty,
                             fieldAlignment: Alignment = Alignment.Right,
                             visibilityPointer: Changing[Boolean] = AlwaysTrue, isScalable: Boolean = true)
                            (createField: (ComponentHierarchy, StaticTextContext) => InputField)
{
	// COMPUTED	-----------------------------
	
	/**
	  * @return Whether this row should always be displayed
	  */
	def isAlwaysVisible = visibilityPointer.isAlwaysTrue
	
	/**
	  * @return Whether this blueprint specifies a field name to display
	  */
	def displaysName: Boolean = displayName.nonEmpty
	
	/**
	  * @return The side on which the input field component would be placed. None if the component is not
	  *         contained to either side.
	  */
	def fieldSegmentSide = if (displaysName) fieldAlignment.horizontalDirection else None
	
	/**
	  * @return Whether this row enables placing of the field name and input field to separate segments
	  */
	def usesSegmentLayout = fieldSegmentSide.isDefined
	
	
	// OTHER	-----------------------------
	
	/**
	  * Creates a new managed field
	  * @param hierarchy Component creation hierarchy
	  * @param context Component creation context
	  * @return A new managed field
	  */
	def apply(hierarchy: ComponentHierarchy, context: StaticTextContext) =
		createField(hierarchy, context)
	
	/**
	  * Creates a new managed field
	  * @param hierarchy Component creation hierarchy
	  * @param context Component creation context (implicit)
	  * @return A new managed field
	  */
	def contextual(hierarchy: ComponentHierarchy)(implicit context: StaticTextContext) =
		apply(hierarchy, context)
	
	/**
	  * @param direction Segment side direction
	  * @return Whether that side can use Fit layout type for this component
	  */
	def allowsFitSegmentLayoutForSide(direction: HorizontalDirection) =
		isScalable || !fieldSegmentSide.contains(direction)
}
