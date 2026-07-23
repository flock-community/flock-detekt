package community.flock.detekt.wirespec

import community.flock.detekt.wirespec.rules.ControllerMustImplementWirespecHandler
import community.flock.detekt.wirespec.rules.NoManualSpringMappingAnnotations
import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

class WirespecRuleSetProvider : RuleSetProvider {
    override val ruleSetId: RuleSetId = RuleSetId("wirespec")

    override fun instance(): RuleSet = RuleSet(
        ruleSetId,
        listOf(
            ::ControllerMustImplementWirespecHandler,
            ::NoManualSpringMappingAnnotations,
        )
    )
}
