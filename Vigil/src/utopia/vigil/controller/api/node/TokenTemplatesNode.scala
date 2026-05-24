package utopia.vigil.controller.api.node

import utopia.nexus.controller.api.node.LeafNode
import utopia.vigil.controller.api.context.AuthContext
import utopia.vigil.model.cached.scope.ScopeTarget

/**
 * An API node for creating new token templates.
 * Intended for developer access only. Intended to appear as /tokens/templates.
 * @author Mikko Hilpinen
 * @since 22.05.2026, v0.1
 */
class TokenTemplatesNode(requiredScope: ScopeTarget) // extends LeafNode[AuthContext[_]]
