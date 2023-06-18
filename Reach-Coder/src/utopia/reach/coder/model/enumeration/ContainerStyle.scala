package utopia.reach.coder.model.enumeration

import utopia.coder.model.scala.datatype.Reference
import utopia.reach.coder.util.ReachReferences.Reach._

/**
  * An enumeration for different styles to approaching containers and container creation
  * @author Mikko Hilpinen
  * @since 2.6.2023, v1.0
  */
sealed trait ContainerStyle
{
	// ABSTRACT -------------------------
	
	/**
	  * @return A keyword used for distinguishing this style from user input
	  */
	def keyword: String
	
	/**
	  * @return The common trait for all container factories of this type
	  */
	def factoryTrait: Reference
	/**
	  * @return Common trait for all context-using container factories of this type
	  */
	def contextualFactoryTrait: Reference
	/**
	  * @return Common trait for all non-contextual container factories of this type
	  */
	def nonContextualFactoryTrait: Reference
}

object ContainerStyle
{
	// ATTRIBUTES   ---------------------
	
	/**
	  * All values of this enumeration
	  */
	val values = Vector[ContainerStyle](Wrapper, Combining, View)
	
	
	// OTHER    -------------------------
	
	/**
	  * @param input User input
	  * @return A container style mentioned in that input. None if no style was mentioned.
	  */
	def apply(input: String) = {
		val lower = input.toLowerCase
		values.find { v => lower.contains(v.keyword) }
	}
	
	
	// VALUES   -------------------------
	
	/**
	  * Type of immutable container that wraps a single component
	  */
	case object Wrapper extends ContainerStyle
	{
		override val keyword: String = "wrap"
		
		override def factoryTrait: Reference = wrapperContainerFactory
		override def contextualFactoryTrait: Reference = contextualWrapperContainerFactory
		override def nonContextualFactoryTrait: Reference = nonContextualWrapperContainerFactory
	}
	/**
	  * Type of immutable container that wraps multiple components
	  */
	case object Combining extends ContainerStyle
	{
		override val keyword: String = "comb"
		
		override def factoryTrait: Reference = combiningContainerFactory
		override def contextualFactoryTrait: Reference = contextualCombiningContainerFactory
		override def nonContextualFactoryTrait: Reference = nonContextualCombiningContainerFactory
	}
	/**
	  * Type of view-based container that wraps multiple components
	  */
	case object View extends ContainerStyle
	{
		override val keyword: String = "view"
		
		override def factoryTrait: Reference = viewContainerFactory
		override def contextualFactoryTrait: Reference = contextualViewContainerFactory
		override def nonContextualFactoryTrait: Reference = nonContextualViewContainerFactory
	}
}
