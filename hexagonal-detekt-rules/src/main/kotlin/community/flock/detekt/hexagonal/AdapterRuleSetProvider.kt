package community.flock.detekt.hexagonal

import community.flock.detekt.hexagonal.rules.adapter.AdapterCannotDependOnAdapter
import community.flock.detekt.hexagonal.rules.adapter.AdapterMustImplementPort
import community.flock.detekt.hexagonal.rules.adapter.AdapterNamingConvention
import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetProvider

class AdapterRuleSetProvider : RuleSetProvider {
    override val ruleSetId: RuleSet.Id = RuleSet.Id("hexagonal-adapter")

    override fun instance(): RuleSet = RuleSet(
        ruleSetId,
        listOf(
            ::AdapterMustImplementPort,
            ::AdapterNamingConvention,
            ::AdapterCannotDependOnAdapter,
        )
    )
}
