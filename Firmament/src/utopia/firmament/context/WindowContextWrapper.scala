package utopia.firmament.context
import utopia.firmament.model.enumeration.WindowResizePolicy
import utopia.genesis.handling.action.ActorHandler2
import utopia.genesis.image.Image
import utopia.paradigm.shape.shape2d.insets.Insets

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
	def windowContext: WindowContext
	
	/**
	  * @param base A new window context to wrap
	  * @return A copy of this context, wrapping the specified window context
	  */
	def withWindowContext(base: WindowContext): Repr
	
	
	// IMPLEMENTED  -----------------
	
	override def actorHandler: ActorHandler2 = windowContext.actorHandler
	override def windowResizeLogic: WindowResizePolicy = windowContext.windowResizeLogic
	override def screenBorderMargins: Insets = windowContext.screenBorderMargins
	override def icon: Image = windowContext.icon
	
	override def windowBordersEnabled: Boolean = windowContext.windowBordersEnabled
	override def fullScreenEnabled: Boolean = windowContext.fullScreenEnabled
	override def focusEnabled: Boolean = windowContext.focusEnabled
	override def screenInsetsEnabled: Boolean = windowContext.screenInsetsEnabled
	override def transparencyEnabled: Boolean = windowContext.transparencyEnabled
	
	override def withResizeLogic(logic: WindowResizePolicy): Repr = mapWindowContext { _.withResizeLogic(logic) }
	override def withScreenBorderMargins(margins: Insets): Repr = mapWindowContext { _.withScreenBorderMargins(margins) }
	override def withIcon(icon: Image): Repr = mapWindowContext { _.withIcon(icon) }
	override def withWindowBordersEnabled(enabled: Boolean): Repr = mapWindowContext { _.withWindowBordersEnabled(enabled) }
	override def withFullScreenEnabled(enabled: Boolean): Repr = mapWindowContext { _.withFullScreenEnabled(enabled) }
	override def withFocusEnabled(enabled: Boolean): Repr = mapWindowContext { _.withFocusEnabled(enabled) }
	override def withScreenInsetsEnabled(enabled: Boolean): Repr = mapWindowContext { _.withScreenInsetsEnabled(enabled) }
	override def withTransparencyEnabled(enabled: Boolean): Repr = mapWindowContext { _.withTransparencyEnabled(enabled) }
	
	
	// OTHER    ------------------
	
	/**
	  * @param f A function for mapping the wrapped window context
	  * @return A copy of this context with mapped window context
	  */
	def mapWindowContext(f: WindowContext => WindowContext) = withWindowContext(f(windowContext))
}
