package community.flock.detekt.hexagonal

import community.flock.detekt.hexagonal.rules.adapter.AdapterCannotDependOnAdapter
import community.flock.detekt.hexagonal.rules.adapter.AdapterMustImplementPort
import community.flock.detekt.hexagonal.rules.adapter.AdapterNamingConvention
import community.flock.detekt.hexagonal.rules.dependency.ApiCannotDependOnAdapters
import community.flock.detekt.hexagonal.rules.dependency.DomainCannotDependOnAdapters
import community.flock.detekt.hexagonal.rules.dependency.DomainCannotDependOnApi
import community.flock.detekt.hexagonal.rules.domain.DomainMustBeImmutable
import community.flock.detekt.hexagonal.rules.domain.DomainNoFrameworkImports
import community.flock.detekt.hexagonal.rules.domain.DomainNoPrimitiveObsession
import community.flock.detekt.hexagonal.rules.domain.ValueClassMustHaveJvmInline
import community.flock.detekt.hexagonal.rules.layering.DtoOnlyInAdaptersOrApi
import community.flock.detekt.hexagonal.rules.layering.NoServiceInApiOrAdapter
import community.flock.detekt.hexagonal.rules.port.PortMustBeInterface
import community.flock.detekt.hexagonal.rules.port.PortNamingConvention
import community.flock.detekt.hexagonal.rules.port.PortsInDomainOnly
import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetProvider

class HexagonalRuleSetProvider : RuleSetProvider {
    override val ruleSetId: RuleSet.Id = RuleSet.Id("hexagonal")

    override fun instance(): RuleSet = RuleSet(
        ruleSetId,
        listOf(
            // Domain rules
            ::DomainNoPrimitiveObsession,
            ::DomainNoFrameworkImports,
            ::DomainMustBeImmutable,
            ::ValueClassMustHaveJvmInline,
            // Port rules
            ::PortMustBeInterface,
            ::PortNamingConvention,
            ::PortsInDomainOnly,
            // Adapter rules
            ::AdapterMustImplementPort,
            ::AdapterNamingConvention,
            ::AdapterCannotDependOnAdapter,
            // Dependency rules
            ::DomainCannotDependOnAdapters,
            ::DomainCannotDependOnApi,
            ::ApiCannotDependOnAdapters,
            // Layering rules
            ::DtoOnlyInAdaptersOrApi,
            ::NoServiceInApiOrAdapter,
        )
    )
}
