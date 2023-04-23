# Spring Boot KeepassXC Property Agent

A Java agent that can be used on Spring Boot applications to add additional configuration properties
from a paired running instance of KeepassXC.

This agent can be used when security-sensitive properties are required for running and testing 
your Spring Boot applications on your local machine, but you don't want to have these properties saved
anywhere in plaintext.  Properties are instead pulled from a KeepassXC instance that is running on your
desktop, after being paired.  Pairing KeepassXC with the agent is as easy as pairing KeepassXC with a 
browser as it used the same API.

## Usage

When running a Spring Boot application, use an additional VM command line argument:

```
-javaagent:<path to this agent's JAR file>
```

and when the application starts up an additional property source will be injected that reads from KeepassXC.
The first time it runs, KeepassXC will ask in a dialog box whether you want to pair the agent and the
name to give the connection.

The agent uses an entry in KeepassXC with URL `spring://app` by default.  Any additional attributes of the
entry that is prefixed with 'KPH: spring:' will be added as a property to the Spring Boot application.

---

For example, to set the 
[Spring Security default password](https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html#application-properties.security.spring.security.user.password)
you would:

- create a new entry in KeepassXC with whatever name you like and with URI of `spring://app`.
- in the 'Advanced' section, add an attribute named `KPH: spring:spring.security.user.password` and give it some value. 
  The space after 'KPH: ' is important - and is a KeepassXC restriction.
- start your Spring Boot application with an additional command line argument: 

  `-javaagent:/home/auser/.m2/repository/au/net/causal/spring-boot-keepassxc-property-agent/spring-boot-keepassxc-property-agent/1.0/spring-boot-keepassxc-property-agent-1.0.jar`
  
  
- perform the pairing steps from the prompts in KeepassXC.

## Options

Agent options are specified after an '=' sign and are separated by ','s.

### entryUri

The default URI of `spring://app` used for lookup can be changed by adding an additional argument to the
agent command line.  Use `=entryUri=spring://myapp` to use and entry with URL `spring://myapp` instead.
There is no restriction for the `spring://` prefix, though it's a good idea to keep these URLs separate from
your other URLs used for storing your browser passwords.

This option may be specified multiple times to add properties from multiple entries in KeepassXC.

Example:

```
-javaagent:/home/auser/.m2/repository/au/net/causal/spring-boot-keepassxc-property-agent/spring-boot-keepassxc-property-agent/1.0/spring-boot-keepassxc-property-agent-1.0.jar=entryUri=spring://defaults,entryUri=spring://myapp
```
