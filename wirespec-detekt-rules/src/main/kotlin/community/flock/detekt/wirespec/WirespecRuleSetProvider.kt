package community.flock.detekt.wirespec

import community.flock.detekt.wirespec.rules.ControllerMustImplementWirespecHandler
import community.flock.detekt.wirespec.rules.NoManualSpringMappingAnnotations
import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetProvider

class WirespecRuleSetProvider : RuleSetProvider {
    override val ruleSetId: RuleSet.Id = RuleSet.Id("wirespec")

    override fun instance(): RuleSet = RuleSet(
        ruleSetId,
        listOf(
            ::ControllerMustImplementWirespecHandler,
            ::NoManualSpringMappingAnnotations,
        )
    )
}
