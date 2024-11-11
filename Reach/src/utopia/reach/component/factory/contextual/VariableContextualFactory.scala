package utopia.reach.component.factory.contextual

/**
  * Common trait for component factories that use a variable context parameter (i.e. a context pointer)
  * @author Mikko Hilpinen
  * @since 31.5.2023, v1.1
  */
@deprecated("Now that variable context classes have been introduced, this trait is replaced with ContextualFactory", "v1.5")
trait VariableContextualFactory[N, +Repr] extends ContextualFactory[N, Repr]
