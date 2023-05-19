package utopia.coder.model.scala.declaration

import utopia.coder.model.scala.declaration.DeclarationPrefix.{Abstract, Case, Implicit, Lazy, Override, Sealed}
import utopia.coder.model.scala.declaration.DeclarationTypeCategory.Instance

/**
  * An enumeration for different declaration types (e.g. class or function)
  * @author Mikko Hilpinen
  * @since 1.11.2021, v1.3
  */
sealed trait DeclarationType
{
	/**
	  * @return Scala keyword used to declare an item of this type
	  */
	def keyword: String
	/**
	  * @return Prefixes allowed with this declaration type
	  */
	def availablePrefixes: Set[DeclarationPrefix]
	/**
	  * @return Whether this declaration type allows a parameter list to be included
	  */
	def acceptsParameterList: Boolean
	/**
	  * @return Whether this declaration type allows generic types to be used
	  */
	def acceptsGenericTypes: Boolean
	/**
	  * @return Category where this declaration belongs
	  */
	def category: DeclarationTypeCategory
}

/**
  * An enumeration for different instance declaration types (class, object, trait)
  */
sealed trait InstanceDeclarationType extends DeclarationType
{
	override def category = Instance
}
/**
  * An enumeration for different function declaration types (val, var, def)
  */
sealed trait FunctionDeclarationType extends DeclarationType
{
	override def category = DeclarationTypeCategory.Function
}

object DeclarationType
{
	// ATTRIBUTES   -------------------------
	
	/**
	  * All available declaration types
	  */
	val values = InstanceDeclarationType.values ++ FunctionDeclarationType.values
}

object InstanceDeclarationType
{
	// ATTRIBUTES   -------------------------
	
	/**
	  * All available declaration types
	  */
	val values = Vector[InstanceDeclarationType](ClassD, ObjectD, TraitD)
	
	
	// NESTED   ------------------------------
	
	/**
	  * Used for declaring classes
	  */
	case object ClassD extends InstanceDeclarationType
	{
		override val keyword = "class"
		override val availablePrefixes = Set(Case, Implicit, Abstract)
		override val acceptsParameterList = true
		override val acceptsGenericTypes = true
	}
	/**
	  * Used for declaring singular instances / objects
	  */
	case object ObjectD extends InstanceDeclarationType
	{
		override val keyword = "object"
		override val availablePrefixes = Set(Case, Implicit)
		override val acceptsParameterList = false
		override val acceptsGenericTypes = false
	}
	/**
	  * Used for declaring abstract traits
	  */
	case object TraitD extends InstanceDeclarationType
	{
		override val keyword = "trait"
		override val availablePrefixes = Set(Sealed)
		override val acceptsParameterList = false
		override val acceptsGenericTypes = true
	}
}

object FunctionDeclarationType
{
	// ATTRIBUTES   -------------------------
	
	/**
	  * All available declaration types
	  */
	val values = Vector[FunctionDeclarationType](FunctionD, ValueD, VariableD)
	
	
	// NESTED   ------------------------------
	
	/**
	  * Used for declaring methods and computed properties
	  */
	case object FunctionD extends FunctionDeclarationType
	{
		override val keyword = "def"
		override val availablePrefixes = Set(Override, Implicit)
		override val acceptsParameterList = true
		override val acceptsGenericTypes = true
	}
	/**
	  * Used for declaring immutable values
	  */
	case object ValueD extends FunctionDeclarationType
	{
		override val keyword = "val"
		override val availablePrefixes = Set(Override, Implicit, Lazy)
		override val acceptsParameterList = false
		override val acceptsGenericTypes = false
	}
	/**
	  * Used for declaring mutable variables
	  */
	case object VariableD extends FunctionDeclarationType
	{
		override val keyword = "var"
		override val availablePrefixes = Set(Override)
		override val acceptsParameterList = false
		override val acceptsGenericTypes = false
	}
}