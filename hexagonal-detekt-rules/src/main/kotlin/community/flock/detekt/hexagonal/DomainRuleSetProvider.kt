package community.flock.detekt.hexagonal

import community.flock.detekt.hexagonal.rules.domain.DomainModelMustBeStandalone
import community.flock.detekt.hexagonal.rules.domain.DomainMustBeImmutable
import community.flock.detekt.hexagonal.rules.domain.DomainNoFrameworkImports
import community.flock.detekt.hexagonal.rules.domain.DomainNoPrimitiveObsession
import community.flock.detekt.hexagonal.rules.domain.ValueClassMustHaveJvmInline
import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

class DomainRuleSetProvider : RuleSetProvider {
    override val ruleSetId: RuleSetId = RuleSetId("hexagonal-domain")

    override fun instance(): RuleSet = RuleSet(
        ruleSetId,
        listOf(
            ::DomainNoPrimitiveObsession,
            ::DomainNoFrameworkImports,
            ::DomainModelMustBeStandalone,
            ::DomainMustBeImmutable,
            ::ValueClassMustHaveJvmInline,
        )
    )
}
