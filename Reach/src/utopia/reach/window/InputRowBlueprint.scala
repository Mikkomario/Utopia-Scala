package utopia.reach.window

import utopia.flow.event.{AlwaysTrue, ChangingLike}
import utopia.reach.component.factory.{ContextInsertableComponentFactoryFactory, ContextualComponentFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reflection.container.template.window.ManagedField
import utopia.reflection.localization.LocalizedString
import utopia.reflection.shape.Alignment

object InputRowBlueprint
{
	/**
	  * Creates a new input row blueprint utilizing a component creation factory
	  * @param factory A factory used for creating the field components
	  * @param key Unique key used for this row
	  * @param displayName Field name displayed on this row
	  * @param fieldAlignment Alignment used when placing the field component (relative to the field name) (default = right)
	  * @param visibilityPointer A pointer to this row's visibility state (default = always visible)
	  * @param isScalable Whether the field can be scaled horizontally (default = true)
	  * @param createField A function for creating a new managed field when component creation context is known.
	  *                    Accepts component creation factory and produces a managed field.
	  * @tparam C Type of wrapped field
	  * @tparam N Type of utilized component creation context
	  * @tparam F Type of contextual component factory version
	  * @return A new input row blueprint
	  */
	def using[C, N, F[X <: N] <: ContextualComponentFactory[X, _ >: N, F]]
	(factory: ContextInsertableComponentFactoryFactory[_ >: N, _, F], key: String, displayName: LocalizedString,
	 fieldAlignment: Alignment = Alignment.Right, visibilityPointer: ChangingLike[Boolean] = AlwaysTrue,
	 isScalable: Boolean = true)(createField: F[N] => ManagedField[C]) =
		apply[C, N](key, displayName, fieldAlignment, visibilityPointer, isScalable) { (hierarchy, context) =>
			createField(factory.withContext(hierarchy, context))
		}
}

/**
  * Used in creating input rows for input windows
  * @author Mikko Hilpinen
  * @since 26.2.2021, v1
  * @param key Unique key used for this row
  * @param displayName Field name displayed on this row
  * @param fieldAlignment Alignment used when placing the field component (relative to the field name) (default = right)
  * @param visibilityPointer A pointer to this row's visibility state (default = always visible)
  * @param isScalable Whether the field can be scaled horizontally (default = true)
  * @param createField A function for creating a new managed field when component creation context is known.
  *                    Accepts component creation hierarchy and context.
  */
case class InputRowBlueprint[+C, -N](key: String, displayName: LocalizedString,
									 fieldAlignment: Alignment = Alignment.Right,
									 visibilityPointer: ChangingLike[Boolean] = AlwaysTrue, isScalable: Boolean = true)
									(createField: (ComponentHierarchy, N) => ManagedField[C])
{
	// COMPUTED	-----------------------------
	
	/**
	  * @return Whether this row should always be displayed
	  */
	def isAlwaysVisible = visibilityPointer.isAlwaysTrue
	
	
	// OTHER	-----------------------------
	
	/**
	  * Creates a new managed field
	  * @param hierarchy Component creation hierarchy
	  * @param context Component creation context
	  * @return A new managed field
	  */
	def apply(hierarchy: ComponentHierarchy, context: N) = createField(hierarchy, context)
	
	/**
	  * Creates a new managed field
	  * @param hierarchy Component creation hierarchy
	  * @param context Component creation context (implicit)
	  * @return A new managed field
	  */
	def contextual(hierarchy: ComponentHierarchy)(implicit context: N) = apply(hierarchy, context)
}
