package community.flock.detekt.arrow

import community.flock.detekt.arrow.rules.DomainServiceMustUseRaise
import community.flock.detekt.arrow.rules.ErrorTypeMustBeSealedInterface
import community.flock.detekt.arrow.rules.NoThrowInDomainOrAdapters
import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

class ArrowRuleSetProvider : RuleSetProvider {
    override val ruleSetId: RuleSetId = RuleSetId("arrow")

    override fun instance(): RuleSet = RuleSet(
        ruleSetId,
        listOf(
            ::NoThrowInDomainOrAdapters,
            ::DomainServiceMustUseRaise,
            ::ErrorTypeMustBeSealedInterface
        )
    )
}
