package utopia.firmament.context
import utopia.firmament.model.enumeration.WindowResizePolicy
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.image.Image
import utopia.paradigm.shape.shape2d.Insets

/**
  * A context instance that wraps a window context
  * @author Mikko Hilpinen
  * @since 13.4.2023, v1.0
  */
trait WindowContextWrapper[+Repr] extends WindowContextLike[Repr]
{
	// ABSTRACT ----------------------
	
	/**
	  * @return The wrapped window context
	  */
	def wrapped: WindowContext
	
	/**
	  * @param base A new window context to wrap
	  * @return A copy of this context, wrapping the specified window context
	  */
	def withBase(base: WindowContext): Repr
	
	
	// IMPLEMENTED  -----------------
	
	override def actorHandler: ActorHandler = wrapped.actorHandler
	override def windowResizeLogic: WindowResizePolicy = wrapped.windowResizeLogic
	override def screenBorderMargins: Insets = wrapped.screenBorderMargins
	override def icon: Image = wrapped.icon
	
	override def windowBordersEnabled: Boolean = wrapped.windowBordersEnabled
	override def fullScreenEnabled: Boolean = wrapped.fullScreenEnabled
	override def focusEnabled: Boolean = wrapped.focusEnabled
	override def screenInsetsEnabled: Boolean = wrapped.screenInsetsEnabled
	override def transparencyEnabled: Boolean = wrapped.transparencyEnabled
	
	override def withResizeLogic(logic: WindowResizePolicy): Repr = mapBase { _.withResizeLogic(logic) }
	override def withScreenBorderMargins(margins: Insets): Repr = mapBase { _.withScreenBorderMargins(margins) }
	override def withIcon(icon: Image): Repr = mapBase { _.withIcon(icon) }
	override def withWindowBordersEnabled(enabled: Boolean): Repr = mapBase { _.withWindowBordersEnabled(enabled) }
	override def withFullScreenEnabled(enabled: Boolean): Repr = mapBase { _.withFullScreenEnabled(enabled) }
	override def withFocusEnabled(enabled: Boolean): Repr = mapBase { _.withFocusEnabled(enabled) }
	override def withScreenInsetsEnabled(enabled: Boolean): Repr = mapBase { _.withScreenInsetsEnabled(enabled) }
	override def withTransparencyEnabled(enabled: Boolean): Repr = mapBase { _.withTransparencyEnabled(enabled) }
	
	
	// OTHER    ------------------
	
	private def mapBase(f: WindowContext => WindowContext) = withBase(f(wrapped))
}
