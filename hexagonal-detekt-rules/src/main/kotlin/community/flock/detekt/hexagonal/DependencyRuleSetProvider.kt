package community.flock.detekt.hexagonal

import community.flock.detekt.hexagonal.rules.dependency.ApiCannotDependOnAdapters
import community.flock.detekt.hexagonal.rules.dependency.ApiCannotDependOnPorts
import community.flock.detekt.hexagonal.rules.dependency.DomainCannotDependOnAdapters
import community.flock.detekt.hexagonal.rules.dependency.DomainCannotDependOnApi
import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

class DependencyRuleSetProvider : RuleSetProvider {
    override val ruleSetId: RuleSetId = RuleSetId("hexagonal-dependency")

    override fun instance(): RuleSet = RuleSet(
        ruleSetId,
        listOf(
            ::DomainCannotDependOnAdapters,
            ::DomainCannotDependOnApi,
            ::ApiCannotDependOnAdapters,
            ::ApiCannotDependOnPorts,
        )
    )
}
