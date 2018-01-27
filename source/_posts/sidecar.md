---
title: Service Mesh - Spring Cloud [Netflix|Consul] Sidecar
date: 2018-1-27 20:46:25
categories:
- 分布式&云计算
- 分布式技术架构
tags:
  - 分布式
  - 架构设计
---


## 简介
Spring Cloud是目前非常流行的微服务化解决方案，它将Spring Boot的便捷开发和Netflix OSS的丰富解决方案结合起来。Spring Cloud不同于Dubbo，使用的是基于HTTP(s)的Rest服务来构建整个服务体系。

那么有没有可能使用一些非JVM语言，例如熟悉的Node.js来开发一些Rest服务呢？当然是可以的。但是如果只有Rest服务，还不能接入Spring Cloud系统。此外，还想使用起Spring Cloud提供的Eureka进行服务发现，使用Config Server做配置管理，使用Ribbon做客户端负载均衡。这个时候Spring sidecar就可以大显身手了。

Sidecar起源于Netflix Prana。它提供了一个可以获取既定服务所有实例的信息(例如host，端口等)的http api。也可以通过一个嵌入的Zuul，代理服务到从Eureka获取的相关路由节点。Spring Cloud Config Server可以直接通过主机查找或通过代理Zuul进行访问。

需要注意的是你所开发的Node.js应用，必须去实现一个健康检查接口，来让Sidecar可以把这个服务实例的健康状况报告给Eureka。
非jvm应用应该实现一个健康检查，Sidecar能够以此来报告给Eureka注册中心该应用是up还是down状态。

## 使用spring cloud netflix sidecar
为了使用Sidecar，你可以创建一个带有@EnableSidecar注解的Spring Boot程序。在项目中使用Sidecar，需要添加依赖，其group为 org.springframework.cloud ，artifact id为 spring-cloud-netflix-sidecar 。

启用Sidecar，创建一个Spring Boot应用程序，并在在应用主类上加上@EnableSidecar 注解。该注解包含 @EnableCircuitBreaker , @EnableDiscoveryClient 以及 @EnableZuulProxy 。

配置Sidecar，在application.yml中添加 sidecar.port 和 sidecar.health-uri 。 sidecar.port 属性是非jre程序监听的端口号，这就是Sidecar可以正确注册应用到Eureka的原因。

### 健康检查
sidecar.health-uri 是非jre应用提供的一个对外暴露的可访问uri地址，在该地址对应的接口中需要实现一个模仿Spring Boot健康检查指示器的功能。
```
{
    "status": "UP"
}
```
### DiscoveryClient
API DiscoveryClient.getInstances() 所对应的访问方式是 /hosts/{serviceId} ，这是访问 /hosts/consul-sidecar-myapp 后的响应示例，它返回了一个或多个不同主机上的实例.

```
[
    {
        "serviceId": "consul-sidecar-myapp",
        "host": "192.168.43.153",
        "port": 8092,
        "secure": false,
        "metadata": {},
        "uri": "http://192.168.43.153:8092"
    }
]
```

### Zuul代理
Zuul代理会自动为每个在Eureka注册中心上的服务添加路由到 /serviceId 上，所以上面那个consul-sidecar-myapp的服务可以通过 /consul-sidecar-myapp 访问。非Jre应用可以通过 http://localhost:port}/consul-sidecar-myapp 来访问Service.

如果你用的不是Eureka，譬如使用了consul, 实现有所不同。

## Spring Cloud Consul Sidecar
官方并没有提供spring cloud consul Sidecar, 下面会讲述一下如何定制实现该功能。
直接上代码先。

![](/images/spring-cloud-consul-sidecar-project.png)

代码地址：https://github.com/osswangxining/spring-cloud-consul/spring-cloud-consul-sidecar

使用该Sidecar的例子： https://github.com/osswangxining/spring-cloud-consul/spring-cloud-consul-sample

下面不打算写太多文字了，直接看代码吧。

### 实现@EnableSidecar
```
package org.springframework.cloud.consul.sidecar;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.annotation.Import;

/**
 * @author Xi Ning Wang
 */
@EnableCircuitBreaker
@EnableZuulProxy
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(SidecarConfiguration.class)
public @interface EnableSidecar {

}

```

### Healthcheck
```

package org.springframework.cloud.consul.sidecar;

import java.net.URI;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.web.client.RestTemplate;

/**
 * @author Xi Ning Wang
 */
public class LocalApplicationHealthIndicator extends AbstractHealthIndicator {

	@Autowired
	private SidecarProperties properties;

	@SuppressWarnings("unchecked")
	@Override
	protected void doHealthCheck(Health.Builder builder) throws Exception {
		URI uri = this.properties.getHealthUri();
		if (uri == null) {
			builder.up();
			return;
		}
		Map<String, Object> map = new RestTemplate().getForObject(uri, Map.class);
		Object status = map.get("status");
		if (status != null && status instanceof String) {
			builder.status(status.toString());
		}
		else if (status != null && status instanceof Map) {
			Map<String, Object> statusMap = (Map<String, Object>) status;
			Object code = statusMap.get("code");
			if (code != null) {
				builder.status(code.toString());
			}
			else {
				getWarning(builder);
			}
		}
		else {
			getWarning(builder);
		}
	}

	private Health.Builder getWarning(Health.Builder builder) {
		return builder.unknown().withDetail("warning", "no status field in response");
	}

}

```
###  SidecarConfiguration
```
package org.springframework.cloud.consul.sidecar;

import java.net.URI;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.actuator.HasFeatures;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.consul.discovery.ConsulDiscoveryProperties;
import org.springframework.cloud.consul.discovery.HeartbeatProperties;
import org.springframework.cloud.consul.serviceregistry.ConsulAutoRegistration;
import org.springframework.cloud.consul.serviceregistry.ConsulRegistrationCustomizer;
import org.springframework.cloud.consul.serviceregistry.ConsulServiceRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.StringUtils;

import com.ecwid.consul.v1.agent.model.NewService;

/**
 * Sidecar Configuration
 * <p>
 * Depends on {@link SidecarProperties} property.
 * </p>
 * @author Xi Ning Wang
 *
 */
@Configuration
@EnableConfigurationProperties
@ConditionalOnProperty(value = "spring.cloud.consul.sidecar.enabled", matchIfMissing = true)
public class SidecarConfiguration {

	@Bean
	public HasFeatures Feature() {
		return HasFeatures.namedFeature("Consul Sidecar", SidecarConfiguration.class);
	}

	@Bean
	public SidecarProperties sidecarProperties() {
		return new SidecarProperties();
	}

	@Bean
	@ConditionalOnMissingBean
	public ConsulAutoRegistration consulRegistration(ConsulDiscoveryProperties properties, ApplicationContext applicationContext,
			ObjectProvider<List<ConsulRegistrationCustomizer>> registrationCustomizers, HeartbeatProperties heartbeatProperties) {
		return registration(properties, applicationContext, registrationCustomizers.getIfAvailable(), heartbeatProperties);
	}


	@Autowired
	private InetUtils inetUtils;

	@Value("${consul.instance.hostname:${CONSUL_INSTANCE_HOSTNAME:}}")
	private String hostname;

	@Autowired
	private ConfigurableEnvironment env;

	private  ConsulAutoRegistration registration(ConsulDiscoveryProperties properties, ApplicationContext context,
			List<ConsulRegistrationCustomizer> registrationCustomizers,
			HeartbeatProperties heartbeatProperties) {
		RelaxedPropertyResolver propertyResolver = new RelaxedPropertyResolver(context.getEnvironment());

		SidecarProperties sidecarProperties = sidecarProperties();
		int port = sidecarProperties.getPort();
		String hostname = sidecarProperties.getHostname();
		String ipAddress = sidecarProperties.getIpAddress();
		if (!StringUtils.hasText(hostname) && StringUtils.hasText(this.hostname)) {
			hostname = this.hostname;
		}
		if(!StringUtils.hasText(hostname)) {
			hostname = properties.getHostname();
		}
		URI healthUri = sidecarProperties.getHealthUri();
		URI homePageUri = sidecarProperties.getHomePageUri();
		NewService service = new NewService();

		String appName = ConsulAutoRegistration.getAppName(properties, propertyResolver);
		service.setId(ConsulAutoRegistration.getInstanceId(properties, context));
		if(!properties.isPreferAgentAddress()) {
			service.setAddress(hostname);
		}
		service.setName(ConsulAutoRegistration.normalizeForDns(appName));
		service.setTags(ConsulAutoRegistration.createTags(properties));

		if (properties.getPort() != null) {
			service.setPort(properties.getPort());
			// we know the port and can set the check
			ConsulAutoRegistration.setCheck(service, properties, context, heartbeatProperties);
		}

		ConsulAutoRegistration registration = new ConsulAutoRegistration(service, properties, context, heartbeatProperties);
		ConsulAutoRegistration.customize(registrationCustomizers, registration);
		return registration;
	}

	@Bean
	public LocalApplicationHealthIndicator localApplicationHealthIndicator() {
		return new LocalApplicationHealthIndicator();
	}

	@Bean
	public SidecarController sidecarController() {
		return new SidecarController();
	}

}

```
## 附录1 - RetryLoadBalancerInterceptor
org.springframework.cloud.client.loadbalancer.RetryLoadBalancerInterceptor

```
@Override
	public ClientHttpResponse intercept(final HttpRequest request, final byte[] body,
										final ClientHttpRequestExecution execution) throws IOException {
		final URI originalUri = request.getURI();
		final String serviceName = originalUri.getHost();
		Assert.state(serviceName != null, "Request URI does not contain a valid hostname: " + originalUri);
		final LoadBalancedRetryPolicy retryPolicy = lbRetryPolicyFactory.create(serviceName,
				loadBalancer);
		RetryTemplate template = this.retryTemplate == null ? new RetryTemplate() : this.retryTemplate;
		BackOffPolicy backOffPolicy = backOffPolicyFactory.createBackOffPolicy(serviceName);
		template.setBackOffPolicy(backOffPolicy == null ? new NoBackOffPolicy() : backOffPolicy);
		template.setThrowLastExceptionOnExhausted(true);
		template.setRetryPolicy(
				!lbProperties.isEnabled() || retryPolicy == null ? new NeverRetryPolicy()
						: new InterceptorRetryPolicy(request, retryPolicy, loadBalancer,
						serviceName));
		return template
				.execute(new RetryCallback<ClientHttpResponse, IOException>() {
					@Override
					public ClientHttpResponse doWithRetry(RetryContext context)
							throws IOException {
						ServiceInstance serviceInstance = null;
						if (context instanceof LoadBalancedRetryContext) {
							LoadBalancedRetryContext lbContext = (LoadBalancedRetryContext) context;
							serviceInstance = lbContext.getServiceInstance();
						}
						if (serviceInstance == null) {
							serviceInstance = loadBalancer.choose(serviceName);
						}
						ClientHttpResponse response = RetryLoadBalancerInterceptor.this.loadBalancer.execute(
								serviceName, serviceInstance,
								requestFactory.createRequest(request, body, execution));
						int statusCode = response.getRawStatusCode();
						if(retryPolicy != null && retryPolicy.retryableStatusCode(statusCode)) {
							response.close();
							throw new RetryableStatusCodeException(serviceName, statusCode);
						}
						return response;
					}
				});
	}
```
