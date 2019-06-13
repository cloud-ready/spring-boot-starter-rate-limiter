# spring-boot-starter-rate-limiter

[![Sonar](https://sonarcloud.io/api/project_badges/measure?project=top.infra%3Aspring-boot-starter-rate-limiter&metric=alert_status)](https://sonarcloud.io/dashboard?id=top.infra%3Aspring-boot-starter-rate-limiter)  
[Maven Site (github.io)](https://cloud-ready.github.io/cloud-ready/snapshot/spring-boot-starter-rate-limiter/index.html)  
[Maven site (infra.top)](https://maven-site.infra.top/cloud-ready/snapshot/staging/spring-boot-starter-rate-limiter/index.html)  
[Source Repository](https://github.com/cloud-ready/spring-boot-starter-rate-limiter/tree/develop)  
[![Build Status](https://travis-ci.org/cloud-ready/spring-boot-starter-rate-limiter.svg?branch=develop)travis-ci](https://travis-ci.org/cloud-ready/spring-boot-starter-rate-limiter)  
[![Build status](https://ci.appveyor.com/api/projects/status/any0kvwcxs5b6s8c?svg=true)(appveyor)](https://ci.appveyor.com/project/chshawkn/spring-boot-starter-rate-limiter)    


spring-boot-starter-rate-limiter

A rate limiter based on redisson (top.infra:spring-boot-starter-redisson)

### Usage:

Just put it into classpath.  

Maven:
```xml
<dependency>
    <groupId>top.infra</groupId>
    <artifactId>spring-boot-starter-rate-limiter</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <scope>runtime</scope>
</dependency>
```

### Build this project

```bash
JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk1.8.0_201.jdk/Contents/Home" \
    mvn -Dmaven.artifacts.skip=true -Dskip-quality=true help:active-profiles \
    clean install spotbugs:spotbugs spotbugs:check pmd:pmd pmd:check
```
