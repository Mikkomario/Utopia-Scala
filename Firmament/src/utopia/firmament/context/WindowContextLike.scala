package utopia.firmament.context

import utopia.firmament.model.enumeration.WindowResizePolicy
import utopia.flow.operator.ScopeUsable
import utopia.genesis.handling.action.ActorHandler2
import utopia.genesis.image.Image
import utopia.paradigm.shape.shape2d.insets.Insets

/**
  * Common trait for context items that specify information for window-creation
  * @author Mikko Hilpinen
  * @since 12.4.2023, v1.0
  */
trait WindowContextLike[+Repr] extends ScopeUsable[Repr]
{
	// ABSTRACT -----------------------
	
	/**
	  * @return Actor handler used for delivering action events for window event generation
	  */
	def actorHandler: ActorHandler2
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
	
	def withResizeLogic(logic: WindowResizePolicy): Repr
	def withScreenBorderMargins(margins: Insets): Repr
	def withIcon(icon: Image): Repr
	
	def withWindowBordersEnabled(enabled: Boolean): Repr
	def withFullScreenEnabled(enabled: Boolean): Repr
	def withFocusEnabled(enabled: Boolean): Repr
	def withScreenInsetsEnabled(enabled: Boolean): Repr
	def withTransparencyEnabled(enabled: Boolean): Repr
	
	
	// COMPUTED ---------------------
	
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
	
	/**
	  * @return A copy of this context that creates windows which can be resized by the user
	  */
	def resizable = mapResizeLogic { _.allowingResizeByUser }
	/**
	  * @return A copy of this context that creates windows that can be resized by the program
	  */
	def allowingProgrammaticSizeChanges = mapResizeLogic { _.allowingResizeByProgram }
	/**
	  * @return A copy of this context where user-initiated window-resizing is disabled
	  */
	def nonResizable = mapResizeLogic { _.disallowingResizeByUser }
	/**
	  * @return A copy of this context where the program is not allowed to resize windows,
	  *         unless strictly necessary
	  */
	def withoutProgrammaticSizeChanges = mapResizeLogic { _.disallowingResizeByProgram }
	
	/**
	  * @return A copy of this context that doesn't place any additional margins on the screen edges.
	  *         Screen insets may still be respected.
	  */
	def withoutScreenBorderMargins = withScreenBorderMargins(Insets.zero)
	
	/**
	  * @return A copy of this context that creates borderless (i.e. undecorated) windows
	  */
	def borderless = withWindowBordersEnabled(false)
	/**
	  * @return A copy of this context that creates decorated windows,
	  *         meaning that the windows will contain the OS header, etc.
	  */
	def decorated = withWindowBordersEnabled(true)
	
	/**
	  * @return A copy of this context that creates full-screen windows
	  */
	def fullScreen = withFullScreenEnabled(true)
	/**
	  * @return A copy of this context that creates windows that don't attempt to fill the whole screen
	  */
	def windowed = withFullScreenEnabled(false)
	
	/**
	  * @return A copy of this context that creates windows that can receive focus
	  */
	def focusable = withFocusEnabled(true)
	/**
	  * @return A copy of this context that creates windows that can't receive focus
	  */
	def nonFocusable = withFocusEnabled(false)
	
	/**
	  * @return A copy of this context that takes account screen insets (i.e. OS toolbar)
	  */
	def respectingScreenInsets = withScreenInsetsEnabled(true)
	/**
	  * @return A copy of this context that ignores screen insets (i.e. OS toolbar)
	  */
	def ignoringScreenInsets = withScreenInsetsEnabled(false)
	
	/**
	  * @return A copy of this context that allows transparent window creation, when possible and appropriate
	  */
	def transparent = withTransparencyEnabled(true)
	/**
	  * @return A copy of this context that only creates non-transparent windows
	  */
	def nonTransparent = withTransparencyEnabled(false)
	
	
	// OTHER    ---------------------
	
	def mapResizeLogic(f: WindowResizePolicy => WindowResizePolicy) =
		withResizeLogic(f(windowResizeLogic))
	def mapScreenBorderMargins(f: Insets => Insets) = withScreenBorderMargins(f(screenBorderMargins))
	def mapIcon(f: Image => Image) = withIcon(f(icon))
}
