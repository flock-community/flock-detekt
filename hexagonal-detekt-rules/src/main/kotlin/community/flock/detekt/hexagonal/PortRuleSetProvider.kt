package community.flock.detekt.hexagonal

import community.flock.detekt.hexagonal.rules.port.PortMustBeInterface
import community.flock.detekt.hexagonal.rules.port.PortNamingConvention
import community.flock.detekt.hexagonal.rules.port.PortsInDomainOnly
import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetProvider

class PortRuleSetProvider : RuleSetProvider {
    override val ruleSetId: RuleSet.Id = RuleSet.Id("hexagonal-port")

    override fun instance(): RuleSet = RuleSet(
        ruleSetId,
        listOf(
            ::PortMustBeInterface,
            ::PortNamingConvention,
            ::PortsInDomainOnly,
        )
    )
}
