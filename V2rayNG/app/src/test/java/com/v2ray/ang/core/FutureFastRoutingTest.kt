package com.v2ray.ang.core

import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.FutureFastAiService
import com.v2ray.ang.dto.V2rayConfig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FutureFastRoutingTest {
    private fun config() = V2rayConfig(log = V2rayConfig.LogBean(), inbounds = arrayListOf(),
        outbounds = arrayListOf(V2rayConfig.OutboundBean(protocol = "socks")),
        routing = V2rayConfig.RoutingBean("AsIs", rules = arrayListOf()))

    @Test fun `healthy targets remain independent and have an observed default fallback`() {
        val config = config()
        FutureFastAiService.entries.forEach {
            config.outbounds.add(V2rayConfig.OutboundBean(tag = it.tag, protocol = "socks"))
            config.routing.rules.add(V2rayConfig.RoutingBean.RulesBean(domain = it.domains, outboundTag = it.tag))
        }
        FutureFastRouting.apply(config, "https://example.com/generate_204")
        val balancers = config.routing.balancers!!
        assertEquals(3, balancers.size)
        FutureFastAiService.entries.forEach { service ->
            val rule = config.routing.rules.first { it.domain == service.domains }
            assertNull(rule.outboundTag)
            val balancer = balancers.single { it.tag == rule.balancerTag }
            assertEquals(listOf(service.tag), balancer.selector)
            assertEquals(AppConfig.TAG_PROXY, balancer.fallbackTag)
            assertEquals("random", balancer.strategy?.type)
        }
        assertEquals(3, (config.observatory as V2rayConfig.ObservatoryObject).subjectSelector.size)
    }

    @Test fun `empty or missing targets do not introduce phantom balancers or probes`() {
        val config = config()
        config.routing.rules.add(V2rayConfig.RoutingBean.RulesBean(outboundTag = AppConfig.TAG_PROXY))
        FutureFastRouting.apply(config, "https://example.com")
        assertNull(config.routing.balancers)
        assertNull(config.observatory)
        assertEquals(AppConfig.TAG_PROXY, config.routing.rules.single().outboundTag)
    }

    @Test fun `default policy group is reached through loopback without losing existing probes`() {
        val config = config()
        val tag = FutureFastAiService.CHATGPT.tag
        config.outbounds.add(V2rayConfig.OutboundBean(tag = tag, protocol = "socks"))
        config.routing.rules.add(V2rayConfig.RoutingBean.RulesBean(outboundTag = tag))
        config.observatory = V2rayConfig.ObservatoryObject(listOf("existing"), "https://existing.test", "20s")
        config.routing.balancers = listOf(V2rayConfig.RoutingBean.BalancerBean("default-group", listOf("member")))
        FutureFastRouting.apply(config, "https://example.com", "default-group")
        val fallback = config.routing.balancers!!.last().fallbackTag
        val loopback = config.outbounds.single { it.tag == fallback }
        assertEquals("loopback", loopback.protocol)
        val rule = config.routing.rules.first()
        assertEquals(listOf(loopback.settings!!.inboundTag), rule.inboundTag)
        assertEquals("default-group", rule.balancerTag)
        val obs = config.observatory as V2rayConfig.ObservatoryObject
        assertEquals(listOf("existing", tag), obs.subjectSelector)
        assertEquals("https://existing.test", obs.probeUrl)
        assertEquals("20s", obs.probeInterval)
    }
}
