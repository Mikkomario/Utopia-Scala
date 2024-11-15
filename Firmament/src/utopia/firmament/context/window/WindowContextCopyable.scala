package utopia.firmament.context.window

import utopia.firmament.model.enumeration.WindowResizePolicy
import utopia.flow.operator.ScopeUsable
import utopia.genesis.image.Image
import utopia.paradigm.shape.shape2d.insets.Insets

/**
  * Common trait for copyable context items that specify information for window-creation.
  * @author Mikko Hilpinen
  * @since 14.11.2024, v1.4
  */
trait WindowContextCopyable[+Repr] extends WindowContextPropsView with ScopeUsable[Repr]
{
	// ABSTRACT -----------------------
	
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
