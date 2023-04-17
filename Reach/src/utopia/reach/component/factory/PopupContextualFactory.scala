package utopia.reach.component.factory

import utopia.reach.context.{PopupContext, PopupContextWrapper}

/**
  * Common trait for factories which wrap and use a popup creation context
  * @author Mikko Hilpinen
  * @since 17.4.2023, v1.0
  */
trait PopupContextualFactory[+Repr] extends ContextualFactory[PopupContext, Repr] with PopupContextWrapper[Repr]
{
	override def popupContext: PopupContext = context
	override def withPopupContext(base: PopupContext): Repr = withContext(base)
	
	override def *(mod: Double): Repr = mapContext { _ * mod }
}
