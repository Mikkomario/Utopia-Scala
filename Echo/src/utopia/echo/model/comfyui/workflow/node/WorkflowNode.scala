package utopia.echo.model.comfyui.workflow.node

import utopia.echo.model.comfyui.workflow.OutputRef
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Constant, Model}

import scala.language.implicitConversions

object WorkflowNode
{
	// IMPLICIT ------------------------
	
	implicit def nodeAsOutput(node: WorkflowNode): OutputRef = node.output
	
	
	// OTHER    ------------------------
	
	/**
	 * @param classType Type of this node
	 * @param name Name of this node
	 * @param input Inputs of this node as a (JSON) object
	 * @return A new workflow node
	 */
	def apply(classType: NodeClass, name: String, input: Model): WorkflowNode = _WorkflowNode(classType, name, input)
	
	
	// NESTED   ------------------------
	
	private case class _WorkflowNode(classType: NodeClass, name: String, input: Model) extends WorkflowNode
}

/**
 * Used for defining a workflow by connecting ComfyUI nodes and inputs into a graph
 *
 * @author Mikko Hilpinen
 * @since 04.08.2025, v1.4
 */
trait WorkflowNode
{
	// ABSTRACT ----------------------
	
	/**
	 * @return Type of this node
	 */
	def classType: NodeClass
	/**
	 * @return Name given to this node
	 */
	def name: String
	/**
	 * @return Inputs of this node as a JSON object
	 */
	def input: Model
	
	
	// COMPUTED --------------------
	
	/**
	 * @return A value used for referencing the default output of this node, i.e. the output at index 0.
	 *         Note: Some nodes have multiple outputs.
	 * @see [[outputAt]]
	 */
	def output = outputAt(0)
	
	/**
	 * @return A JSON graph constant representing this node
	 */
	def toConstant = Constant(name, Model.from("class_type" -> classType.identifier, "inputs" -> input))
	
	
	// OTHER    --------------------
	
	/**
	 * @param index Targeted output index. 0-based.
	 *              The role of each output depends on the [[classType]]
	 * @return A value used for referencing that output
	 */
	def outputAt(index: Int) = OutputRef(name, index)
}
