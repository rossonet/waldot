# WaldOT

[![WaldOT logo](https://raw.githubusercontent.com/rossonet/waldot/refs/heads/master/artwork/logo.png)](https://github.com/rossonet/waldot)

experimental integration between [Apache TinkerPop](https://tinkerpop.apache.org/gremlin.html) and [Eclipse Milo OPCUA library](https://projects.eclipse.org/projects/iot.milo)

[Docker Hub page](https://hub.docker.com/r/rossonet/waldot)

## Logbook

### January 2025

I started from [a project template that I use practically everywhere](https://github.com/rossonet/TemplateConsoleApplication).

I integrated the [on memory backend of Gremlin](https://github.com/rossonet/tinkerpop/tree/master/tinkergraph-gremlin).

I started refactoring the code to replace the hashmaps used as the backend for Gremlin with the structure used by Milo's server sdk.

My first goal is to pass TinkerPop's compliance tests, I want the logo...

> At the core of TinkerPop 3.x is a Java API. The implementation of this core API and its validation via the gremlin-test suite is all that is required of a graph system provider wishing to provide a TinkerPop-enabled graph engine. Once a graph system has a valid implementation, then all the applications provided by TinkerPop (e.g. Gremlin Console, Gremlin Server, etc.) and 3rd-party developers (e.g. Gremlin-Scala, Gremlin-JS, etc.) will integrate properly. Finally, please feel free to use the logo on the left to promote your TinkerPop implementation.

<small>[rif: https://tinkerpop.apache.org/docs/current/dev/provider/#graph-system-provider-requirements](https://tinkerpop.apache.org/docs/current/dev/provider/#graph-system-provider-requirements)</small>

## Maven

[WaldOT](https://mvnrepository.com/artifact/net.rossonet.waldot)

## Code tools

[![Gitpod ready-to-code](https://img.shields.io/badge/Gitpod-ready--to--code-blue?logo=gitpod)](https://gitpod.io/#https://github.com/rossonet/waldot)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/b00164ee3a36444b920764db52634ebb)](https://app.codacy.com/gh/rossonet/waldot/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)

## GitHub Actions

[![Gradle Test](https://github.com/rossonet/waldot/actions/workflows/test-on-master-with-gradle.yml/badge.svg?branch=master)](https://github.com/rossonet/waldot/actions/workflows/test-on-master-with-gradle.yml)
[![Build and publish WaldOT docker image to Docker Hub](https://github.com/rossonet/waldot/actions/workflows/publish-to-docker-hub.yml/badge.svg?branch=master)](https://github.com/rossonet/waldot/actions/workflows/publish-to-docker-hub.yml)
[![Publish Java artifacts to Maven Central](https://github.com/rossonet/waldot/actions/workflows/publish-to-maven.yml/badge.svg)](https://github.com/rossonet/waldot/actions/workflows/publish-to-maven.yml)

## Reference

[TinkerPop Provider Documentation](https://tinkerpop.apache.org/docs/current/dev/provider/)

[OPCUA - AddressSpace concepts](https://reference.opcfoundation.org/Core/Part3/v104/docs/4)

[OPC 30270: Industry 4.0 Asset Administration Shell document](https://reference.opcfoundation.org/I4AAS/v100/docs/)


### Project sponsor 

[![Rossonet s.c.a r.l.](https://raw.githubusercontent.com/rossonet/images/main/artwork/rossonet-logo/png/rossonet-logo_280_115.png)](https://www.rossonet.net)



