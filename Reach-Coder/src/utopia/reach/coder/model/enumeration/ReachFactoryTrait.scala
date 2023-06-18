package utopia.reach.coder.model.enumeration

import utopia.coder.model.scala.code.CodePiece
import utopia.coder.model.scala.datatype.{Extension, Reference, ScalaType}
import utopia.flow.util.StringExtensions._
import utopia.reach.coder.model.data.Property
import utopia.reach.coder.util.ReachReferences.Reach._
import utopia.reach.coder.util.ReachReferences._

/**
  * Enumeration for different existing factory traits from Reach
  * @author Mikko Hilpinen
  * @since 19.5.2023, v1.0
  */
sealed trait ReachFactoryTrait
{
	// ABSTRACT -----------------------
	
	/**
	  * @return The keyword used for identifying this trait in user input
	  */
	def keyword: String
	/**
	  * @return Reference to this trait
	  */
	def reference: Reference
	/**
	  * @return Property controlled by this trait
	  */
	def property: Property
	
	
	// OTHER ---------------------
	
	/**
	  * @param repr The implementing factory type
	  * @return This trait as an extension
	  */
	def toExtension(repr: ScalaType): Extension = reference(repr)
}

object ReachFactoryTrait
{
	// ATTRIBUTES   --------------------
	
	val values = Vector[ReachFactoryTrait](FramedFactory, CustomDrawableFactory, FocusListenableFactory)
	
	
	// OTHER    ------------------------
	
	/**
	  * @param input An input string that is supposed to represent a Reach factory trait
	  * @return Trait that matches that input. None if no trait matched that input.
	  */
	def apply(input: String) = values.find { f => input.containsIgnoreCase(f.keyword) }
	
	
	// VALUES   ------------------------
	
	/**
	  * Common factory for components with insets
	  */
	case object FramedFactory extends ReachFactoryTrait
	{
		override val keyword: String = "frame"
		override lazy val property: Property = Property.simple("insets", firmament.stackInsets,
			CodePiece("StackInsets.any", Set(firmament.stackInsets)),
			description = "Insets to place around created components")
		
		override def reference: Reference = framedFactory
	}
	/**
	  * Common factory for components that support custom drawing
	  */
	case object CustomDrawableFactory extends ReachFactoryTrait
	{
		override val keyword: String = "draw"
		override lazy val property: Property = Property("customDrawers", ScalaType.vector(firmament.customDrawer),
			"withCustomDrawers", "drawers", "Vector.empty",
			description = "Custom drawers to assign to created components")
		
		override def reference: Reference = customDrawableFactory
	}
	/**
	  * Common factory for components that support focus-listening
	  */
	case object FocusListenableFactory extends ReachFactoryTrait
	{
		override val keyword: String = "focus"
		override lazy val property: Property = Property("focusListeners", ScalaType.vector(focusListener),
			"withFocusListeners", "listeners", "Vector.empty",
			description = "Focus listeners to assign to created components")
		
		override def reference: Reference = focusListenableFactory
	}
	/*
	case object FromAlignmentFactory extends ReachFactoryTrait
	{
		override val keyword: String = "align"
		override val property: Property = Property("alignment", paradigm.alignment, "apply", "alignment")
		
		override def reference: Reference = fromAlignmentFactory
	}*/
}
