package utopia.firmament.component.stack

import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.Axis2D
import utopia.reflection.shape.stack.modifier.{StackLengthModifier, StackSizeModifier}

/**
  * A common trait for stackable instances that can apply stack size constraints
  * @author Mikko Hilpinen
  * @since 15.3.2020, Reflection v1
  */
trait Constrainable extends StackSizeCalculating
{
	// ABSTRACT	-----------------------
	
	/**
	  * @return Stack size modifiers applied to this component's stack size
	  */
	def constraints: Vector[StackSizeModifier]
	/**
	  * @param newConstraints New modifiers for this component's stack size
	  */
	def constraints_=(newConstraints: Vector[StackSizeModifier]): Unit
	
	
	// COMPUTED	-----------------------
	
	/**
	  * @return This component's calculated stack size after constraints have been applied
	  */
	def calculatedStackSizeWithConstraints = constraints.foldLeft(calculatedStackSize) { (size, mod) => mod(size) }
	
	
	// OTHER	-----------------------
	
	/**
	  * Adds a new stack size constraint
	  * @param constraint Constraint to add
	  */
	def addConstraint(constraint: StackSizeModifier) = constraints :+= constraint
	
	/**
	  * Adds a new stack length constraint for specific axis
	  * @param axis Targeted axis
	  * @param constraint Stack length constraint to add
	  */
	def addConstraintOver(axis: Axis2D)(constraint: StackLengthModifier) = addConstraint(constraint.over(axis))
	
	/**
	  * Adds a new constraint over stack size width
	  * @param constraint Constraint to add
	  */
	def addWidthConstraint(constraint: StackLengthModifier) = addConstraintOver(X)(constraint)
	
	/**
	  * Adds a new constraint over stack size height
	  * @param constraint Constraint to add
	  */
	def addHeightConstraint(constraint: StackLengthModifier) = addConstraintOver(Y)(constraint)
	
	/**
	  * Removes a specific stack size constraint (NB: Will not work with functional constraints)
	  * @param constraint constraint to remove
	  */
	def removeConstraint(constraint: Any) = constraints = constraints.filterNot { _ == constraint }
	
	/**
	  * Clears all stack size constraints
	  */
	def clearConstraints() = constraints = Vector()
}
