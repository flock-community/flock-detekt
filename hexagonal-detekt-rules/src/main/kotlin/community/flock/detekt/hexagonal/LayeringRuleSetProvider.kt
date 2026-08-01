package community.flock.detekt.hexagonal

import community.flock.detekt.hexagonal.rules.layering.DtoOnlyInAdaptersOrApi
import community.flock.detekt.hexagonal.rules.layering.NoServiceInApiOrAdapter
import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

class LayeringRuleSetProvider : RuleSetProvider {
    override val ruleSetId: RuleSetId = RuleSetId("hexagonal-layering")

    override fun instance(): RuleSet = RuleSet(
        ruleSetId,
        listOf(
            ::DtoOnlyInAdaptersOrApi,
            ::NoServiceInApiOrAdapter,
        )
    )
}
