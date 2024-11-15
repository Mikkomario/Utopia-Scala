package utopia.firmament.context.window

import utopia.firmament.model.enumeration.WindowResizePolicy
import utopia.genesis.handling.action.ActorHandler
import utopia.genesis.image.Image
import utopia.paradigm.shape.shape2d.insets.Insets

/**
  * Common trait for context items that specify information for window-creation.
  * This trait only provides read (not copy) access to window-creation properties.
  * It does not limit the implementation to either a static or a variable approach.
  * @author Mikko Hilpinen
  * @since 14.11.2024, v1.4
  */
trait WindowContextPropsView
{
	// ABSTRACT -----------------------
	
	/**
	  * @return Actor handler used for delivering action events for window event generation
	  */
	def actorHandler: ActorHandler
	/**
	  * @return Resize logic to use for constructed windows
	  */
	def windowResizeLogic: WindowResizePolicy
	/**
	  * @return Empty space to place around the screen borders, where windows should not enter
	  */
	def screenBorderMargins: Insets
	/**
	  * @return Window icon used
	  */
	def icon: Image
	
	/**
	  * @return Whether the created windows should have the OS borders (i.e. headers)
	  */
	def windowBordersEnabled: Boolean
	/**
	  * @return Whether the created windows should fill the whole screen whenever possible
	  */
	def fullScreenEnabled: Boolean
	/**
	  * @return Whether the created windows should be focusable
	  */
	def focusEnabled: Boolean
	/**
	  * @return Whether the created windows should avoid overlap with the screen insets, i.e. the OS tool bar
	  */
	def screenInsetsEnabled: Boolean
	/**
	  * @return Whether transparent windows should be enabled, when possible
	  */
	def transparencyEnabled: Boolean
	
	
	// COMPUTED ---------------------
	
	// TODO: Possibly rename these properties, since they look like copy functions
	/**
	  * @return Whether created windows are borderless (i.e. undecorated; don't contain OS decorations / header)
	  */
	def windowBordersDisabled = !windowBordersEnabled
	/**
	  * @return Whether created windows shall not attempt to fill the whole screen by default
	  */
	def fullScreenDisabled = !fullScreenEnabled
	/**
	  * @return Whether the created windows shall not receive focus ever
	  */
	def focusDisabled = !focusEnabled
	/**
	  * @return Whether the created windows should ignore screen insets, such as the OS toolbar
	  */
	def screenInsetsDisabled = !screenInsetsEnabled
	/**
	  * @return Whether window transparency should always be disabled
	  */
	def transparencyDisabled = !transparencyEnabled
}
