package utopia.echo.model.llm

import utopia.echo.model.enumeration.ModelParameter
import utopia.flow.generic.model.immutable.Value

/**
  * Common trait for interfaces that specify model settings
  * @author Mikko Hilpinen
  * @since 25.09.2024, v1.1
  */
trait HasModelSettings
{
	// ABSTRACT -----------------------
	
	/**
	  * @return The applied model settings
	  */
	def settings: ModelSettings
	
	
	// OTHER    -----------------------
	
	/**
	  * @param setting A setting
	  * @return Whether that setting has been defined
	  */
	def specifies(setting: ModelParameter) = settings.contains(setting)
	
	/**
	  * @param setting Targeted parameter
	  * @return Value of that parameter. Empty if this value has not been specifically defined.
	  */
	def get(setting: ModelParameter) = settings.get(setting)
	/**
	  * @param setting Targeted parameter
	  * @return Value of that parameter
	  */
	def apply(setting: ModelParameter): Value = settings(setting)
}
