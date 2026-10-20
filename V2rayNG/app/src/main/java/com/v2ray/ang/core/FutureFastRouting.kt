package com.v2ray.ang.core

import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.FutureFastAiService
import com.v2ray.ang.dto.V2rayConfig

/** Runtime failover belongs to core configuration; saved AI references remain unchanged. */
internal object FutureFastRouting {
    fun apply(config: V2rayConfig, probeUrl: String, defaultBalancer: String? = null) {
        val aiTags = FutureFastAiService.entries.map { it.tag }.toSet()
        val targets = config.routing.rules.mapNotNull { it.outboundTag }
            .filter { it in aiTags && config.outbounds.any { outbound -> outbound.tag == it } }.distinct()
        if (targets.isEmpty()) return
        val fallback = if (defaultBalancer == null) AppConfig.TAG_PROXY else {
            // Loop back with a dedicated inbound tag to preserve the selected default policy group.
            val tag = "future-fast-default-fallback"
            val inbound = "future-fast-default-in"
            config.outbounds.add(V2rayConfig.OutboundBean(tag = tag, protocol = "loopback",
                settings = V2rayConfig.OutboundBean.OutSettingsBean(inboundTag = inbound)))
            config.routing.rules.add(0, V2rayConfig.RoutingBean.RulesBean(
                inboundTag = listOf(inbound), balancerTag = defaultBalancer))
            tag
        }
        val balancers = config.routing.balancers.orEmpty().toMutableList()
        targets.forEach { tag ->
            val balancerTag = "$tag-failover"
            balancers.add(V2rayConfig.RoutingBean.BalancerBean(tag = balancerTag, selector = listOf(tag),
                fallbackTag = fallback, strategy = V2rayConfig.RoutingBean.StrategyObject(type = "random")))
            config.routing.rules.filter { it.outboundTag == tag }.forEach {
                it.outboundTag = null
                it.balancerTag = balancerTag
            }
        }
        config.routing.balancers = balancers
        val existing = config.observatory as? V2rayConfig.ObservatoryObject
        config.observatory = V2rayConfig.ObservatoryObject(
            subjectSelector = (existing?.subjectSelector.orEmpty() + targets).distinct(),
            probeUrl = existing?.probeUrl ?: probeUrl,
            probeInterval = existing?.probeInterval ?: "10s", enableConcurrency = true)
    }
}
