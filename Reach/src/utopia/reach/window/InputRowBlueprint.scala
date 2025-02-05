package utopia.reach.window

import utopia.firmament.context.text.{StaticTextContext, VariableTextContext}
import utopia.firmament.localization.LocalizedString
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.template.eventful.Flag.wrap
import utopia.flow.view.template.eventful.{Changing, Flag}
import utopia.paradigm.enumeration.{Alignment, FromAlignmentFactory, HorizontalDirection}
import utopia.reach.component.factory.FromContextComponentFactoryFactory.Ccff
import utopia.reach.component.hierarchy.ComponentHierarchy

object InputRowBlueprint
{
	// OTHER    -----------------------------
	
	/**
	  * Creates a factory for constructing an input row blueprint
	  * @param key Unique key used for this row
	  * @param displayName Field name displayed on this row (default = empty = no name displayed)
	  * @param fieldAlignment Alignment used when placing the field component (relative to the field name) (default = right)
	  * @param visibleFlag A pointer to this row's visibility state (default = always visible)
	  * @param scalable Whether the field can be scaled horizontally (default = true)
	  * @return A new input row blueprint factory
	  */
	def apply(key: String, displayName: LocalizedString  = LocalizedString.empty,
	          fieldAlignment: Alignment = Alignment.Right, visibleFlag: Flag = AlwaysTrue, scalable: Boolean = true) =
		InputRowBlueprintFactory(key, displayName, fieldAlignment, visibleFlag, scalable)
	
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
	@deprecated("Deprecated for removal. Please use .apply(...).using(...) instead", "v1.5")
	def using[F](factory: Ccff[StaticTextContext, F], key: String, displayName: LocalizedString  = LocalizedString.empty,
	             fieldAlignment: Alignment = Alignment.Right, visibilityPointer: Changing[Boolean] = AlwaysTrue,
	             isScalable: Boolean = true)(createField: F => InputField) =
		apply(key, displayName, fieldAlignment, visibilityPointer, isScalable) { (hierarchy, context) =>
			createField(factory.withContext(hierarchy, context))
		}
		
		
	// NESTED   -------------------------------
	
	case class InputRowBlueprintFactory(key: String, displayName: LocalizedString = LocalizedString.empty,
	                                    fieldAlignment: Alignment = Alignment.Right,
	                                    visibleFlag: Flag = AlwaysTrue, isScalable: Boolean = false)
		extends FromAlignmentFactory[InputRowBlueprintFactory]
	{
		// COMPUTED ----------------------------
		
		/**
		  * @return Copy of this factory that constructs horizontally scalable fields
		  */
		def scalable = copy(isScalable = true)
		
		
		// IMPLEMENTED  ------------------------
		
		override def apply(alignment: Alignment): InputRowBlueprintFactory = copy(fieldAlignment = alignment)
		
		
		// OTHER    ----------------------------
		
		/**
		  * @param displayName Name to display for the constructed field
		  * @return Copy of this factory that includes the specified field name
		  */
		def withName(displayName: LocalizedString) = copy(displayName = displayName)
		/**
		  * @param flag A flag that contains true while this field should be displayed
		  * @return Copy of this factory that utilizes the specified visibility flag
		  */
		def withVisibleFlag(flag: Flag) = copy(visibleFlag = flag)
		@deprecated("Renamed to withVisibleFlag", "v1.6")
		def withVisibilityFlag(flag: Flag) = withVisibleFlag(flag)
		
		/**
		  * @param createField A function for creating the input field.
		  *                    Accepts component creation hierarchy, and the component context.
		  * @return A new input row blueprint utilizing the specified constructor
		  */
		def apply(createField: (ComponentHierarchy, StaticTextContext) => InputField) =
			new InputRowBlueprint(key, displayName, fieldAlignment, visibleFlag, isScalable)(createField)
		/**
		  * @param factory Component factory to utilize in field construction
		  * @param createField A function that accepts an initialized component creation factory and
		  *                    yields the input field to place on this row
		  * @tparam F Type of the initialized factory used
		  * @return A new input row blueprint using the specified constructor
		  */
		def using[F](factory: Ccff[StaticTextContext, F])(createField: F => InputField) =
			apply { (hierarchy, context) => createField(factory.withContext(hierarchy, context)) }
		/**
		  * @param factory Component factory to utilize in field construction.
		  *                This factory expects a variable context instance.
		  * @param createField A function that accepts an initialized component creation factory and
		  *                    yields the input field to place on this row
		  * @tparam F Type of the initialized factory used
		  * @return A new input row blueprint using the specified constructor
		  */
		def usingVariable[F](factory: Ccff[VariableTextContext, F])(createField: F => InputField) =
			apply { (hierarchy, context) => createField(factory.withContext(hierarchy, context.toVariableContext)) }
	}
}

/**
  * Used in creating input rows for input windows
  * @author Mikko Hilpinen
  * @since 26.2.2021, v0.1
  * @param key Unique key used for this row
  * @param displayName Field name displayed on this row (default = empty = no name displayed)
  * @param fieldAlignment Alignment used when placing the field component (relative to the field name) (default = right)
  * @param visibleFlag A pointer to this row's visibility state (default = always visible)
  * @param isScalable Whether the field can be scaled horizontally (default = true)
  * @param createField A function for creating a new managed field when component creation context is known.
  *                    Accepts component creation hierarchy and context.
  */
class InputRowBlueprint(val key: String, val displayName: LocalizedString = LocalizedString.empty,
                        val fieldAlignment: Alignment = Alignment.Right,
                        val visibleFlag: Flag = AlwaysTrue, val isScalable: Boolean = true)
                       (createField: (ComponentHierarchy, StaticTextContext) => InputField)
{
	// COMPUTED	-----------------------------
	
	/**
	  * @return Whether this row should always be displayed
	  */
	def isAlwaysVisible = visibleFlag.isAlwaysTrue
	
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
	
	@deprecated("Deprecated for removal. Please use .visibleFlag instead", "v1.6")
	def visibilityPointer = visibleFlag
	
	
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
