<h1 align="center">Rate Limit Spring Boot Starter</h1> 

[![Build Status](https://travis-ci.org/sajjaadalipour/ratelimit-spring-boot-starter.svg)](https://travis-ci.org/sajjaadalipour/ratelimit-spring-boot-starter) 
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=sajjaadalipour_ratelimit-spring-boot-starter&metric=alert_status)](https://sonarcloud.io/dashboard?id=sajjaadalipour_ratelimit-spring-boot-starter)
[![codecov](https://codecov.io/gh/sajjaadalipour/ratelimit-spring-boot-starter/branch/master/graph/badge.svg)](https://codecov.io/gh/sajjaadalipour/ratelimit-spring-boot-starter)
## Table of Contents

  * [Introduction](#introduction)
  * [Getting Started](#getting-started)
    + [Usage](#usage)
    + [Prerequisites](#prerequisites)
    + [Configuration](#configuration)
    + [Common Application Properties](#common-application-properties)
    + [Rate Limiter](#rate-limiter)
      + [Custom Rate Limiter](#custom-rate-limiter)
    + [Rate Limit Key Generator](#rate-limit-key-generator)
      + [Custom Rate Limit Key Generator](#custom-rate-limit-key-generator)
    + [Too Many Request Error](#too-many-request-error)
  * [License](#license)

## Introduction
Built on top of Spring Boot to limit the rate of access to your REST APIs.
 The benefit of this project is the configuration via Spring Boot properties or YAML files.

## Getting Started

### Usage

Add the dependency on pom.xml

```xml
<dependency>
    <groupId>com.github.sajjaadalipour</groupId>
    <artifactId>ratelimit-spring-boot-starter</artifactId>
</dependency>
```

or Gradle:
```
compile "com.github.sajjaadalipour:ratelimit-spring-boot-starter:{release}"
```

In order yo use `1.0.6-SNAPSHOT` version, you should define the following snapshot repository:
```xml
<repositories>
    <repository>
        <id>Sonatype</id>
        <url>https://oss.sonatype.org/content/repositories/snapshots/</url>
    </repository>
</repositories>
```

or:

```groovy
repositories {
    maven {
      url 'https://oss.sonatype.org/content/repositories/snapshots/'
    }
}
```

Add the following dependency accordingly to the chosen data storage:

```xml

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

### Prerequisites
The main dependency is JDK 8+. Tested with:
 - JDK 8, JDK 9, JDK 10 and JDK 11 on Linux.
 - Spring Boot `2.2.3.RELEASE` (Also, should work with any spring boot versions)

### Configuration
The configuration can be done in the application.properties / application.yml.
```yaml
rate-limit:
  enabled: true
  filterOrder: 0
  repository: "IN_MEMORY"
  policies:
    - duration: 5s
      count: 3
      keyGenerator: BY_IP
      block:
        duration: 1h
      routes:
        - uri: "/**"
          method: GET
  keyGenerators:
    - name: BY_IP
      generator: com.github.sajjaadalipour.ratelimit.generators.HeaderBasedKeyGenerator
      params: ["X-FORWARD-FOR"]
```

Sample Properties configuration

```properties
rate-limit.enabled=true
rate-limit.filterOrder=0
rate-limit.repository="IN_MEMORY"
rate-limit.policies[0].duration=5s
rate-limit.policies[0].count=3
rate-limit.policies[0].keyGenerator= BY_IP
rate-limit.policies[0].block.duration= 1d
rate-limit.policies[0].routes[0].uri="/**"
rate-limit.policies[0].routes[0].method=GET
rate-limit.keyGenerators[0].name=BY_IP
rate-limit.keyGenerators[0].generator=com.github.sajjaadalipour.ratelimit.generators.HeaderBasedKeyGenerator
rate-limit.keyGenerators[0].params[0]="X-FORWARD-FOR"
```

The 'duration', can be expressed with [Spring Boot’s duration formats](https://docs.spring.io/spring-boot/docs/current/reference/html/spring-boot-features.html#boot-features-external-config-conversion-duration):

- The standard ISO-8601 format used by java.time.Duration (e.g. PT30M means 30 minutes)

- A more readable format where the value, and the unit are coupled (e.g. 10s means 10 seconds)

### Common Application Properties
Property namespace: ratelimit

| Property name |  Value | Default |
|:-------------:|:------:|:-------:|
| enabled | true/false | false |
| filterOrder | int | 0 |
| repository | redis/in_memory | , |
| policies | List of Policy | , |
| keyGenerators | List of KeyGenerator | , |

**Policy** properties:

| Property name |  Value | Default |
|:-------------:|:------:|:-------:|
| duration | [Duration](https://docs.spring.io/spring-boot/docs/current/reference/html/spring-boot-features.html#boot-features-external-config-conversion-duration) | , |
| count | int (number of calls) | , |
| keyGenerator | String | , |
| block | Block | , |
| routes | List of Route | , |

**Block** properties:

| Property name |  Value | Default |
|:-------------:|:------:|:-------:|
| duration | [Duration](https://docs.spring.io/spring-boot/docs/current/reference/html/spring-boot-features.html#boot-features-external-config-conversion-duration) | , | | , |

- **Note**: In order to block the requester for a specified duration, after exceed policy limitation.

**Route** properties:

| Property name |  Value | Default |
|:-------------:|:------:|:-------:|
| uri | String | , |
| method | GET/POST/PUT/DELETE | `null` value means all HTTP methods |

**KeyGenerator** properties:

| Property name |  Value | Default |
|:-------------:|:------:|:-------:|
| name | String | , |
| generator | Class of generator | , |
| params | List of string | , |


### Rate Limiter
Available `RateLimiter` implementations:

|     Implementation    |        Data Storage        |
|:---------------------:|:--------------------------:|
| `InMemoryRateCache`   |        java hash map       |
| `RedisRateCache`      | [Redis](https://redis.io/) |

### Custom Rate Limiter
In order to replace your custom `RateLimiter`, just implements `RateLimiter` interface and register as a bean.
```java
@Component
public class CustomRateLimiter implements RateLimiter {
    
    @Override
     synchronized Rate consume(RatePolicy ratePolicy){
        return new Rate();
    }
}

```

### Rate Limit Key Generator
In order to identify the requester to rate limiting, we should generate an identity key and cache the details of 
the rate limit with the generated key.
By default we provide some implementation of `RateLimitKeyGenerator`s:

- `HeaderBaseKeyGenerator`: Generates an identity key based on HTTP request headers parameters.

#### Custom Rate Limit Key Generator
In order to provide a custom key generator for a specific policy, just implement the `RateLimitKeyGenerator` interface
 for that exception and add to properties file:
```java
public class CustomKeyGenerator implements RateLimitKeyGenerator {
    
    @Override
    String generateKey(HttpServletRequest servletRequest, Policy policy){
        return servletRequest.getHeader("X-FORWARDED-FOR");
    }
}

```

### Too Many Request Error
In order to handle your custom too many request error, just implement `TooManyRequestErrorHandler` interface.

```java
public class CustomTooManyRequestErrorHandler implements TooManyRequestErrorHandler {

    @Override
    public void handle(HttpServletResponse httpServletResponse) throws IOException {
        httpServletResponse.setStatus(429);
        httpServletResponse.getWriter().append("{\"message\":\"Exceeded.\"}");
    }
}
```

## License
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
