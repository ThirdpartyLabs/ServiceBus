# Overview

**ServiceBus is a light-weight Java library that enables the communication between decoupled components via events, commands, and request-response.**
<!---
### Status
[![Build Status](https://github.com/ThirdpartyLabs/ServiceBus/actions/workflows/maven.yml/badge.svg)](https://github.com/ThirdpartyLabs/ServiceBus/blob/main/.github/workflows/maven.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.thirdpartylabs/servicebus?color=45bf17)](https://search.maven.org/artifact/com.thirdpartylabs/servicebus)
[![Javadoc](https://javadoc.io/badge/com.thirdpartylabs/servicebus.svg)](http://www.javadoc.io/doc/com.thirdpartylabs/servicebus)
--->
## Use Case
In most applications that are composed of multiple decoupled or loosely coupled components (a typical user interface, for example), 
communication between those components is a necessity. Communication needs can usually be broken down into three categories:

* **Events** - Something happens in a component that other factes of the application need to know about
* **Commands** - A component needs to initiate an action somewhere else in the application, possibly with parameters 
* **Request-Response** - A component needs data from a provider somewhere within the application

Without a well-defined and decoupled way to facilitate these communications, your code quickly can quickly become a jumbled mess of 
references. ServiceBus is a simple implementation of an event bus, command bus, and request-response bus, all wrapped up 
in one easy-to-use package, with more advanced capabilities available if you need them. 
###Events
```java
// Create your custom event types by implementing the ServiceBusEvent interface
public class AppEvent implements ServiceBusEvent
{
    public final String message;

    public AppEvent(String message)
    {
        this.message = message;
    }
}

// Register a listener for the custom event type
EventBus.getInstance().listenFor(AppEvent.class, e -> System.out.println(e.message));

// Emit an event
AppEvent event = new AppEvent("Something happened");
ServiceBus.getInstance().emit(event);
```
###Commands
```java
// Create your custom commands types by implementing the ServiceBusCommand interface
public class CloseApplicationCommand implements ServiceBusCommand
{
}

// Register a command executor for the custom command type
CommandBus.getInstance().assignExecutor(CloseApplicationCommand.class, e -> Platform.exit());

// Issue a command
CloseApplicationCommand command = new CloseApplicationCommand();
CommandBus.getInstance().issue(command);
```
###Request-Response

```java
import com.thirdpartylabs.servicebus.RequestResponseBus;

// Create your custom request types by implementing the ServiceBusRequest interface
public class AdditionServiceBusRequest extends ServiceBusRequest<Integer>
{
    private final int a;
    private final int b;

    public AdditionServiceBusRequest(int a, int b)
    {
        this.a = a;
        this.b = b;
    }

    public int getA()
    {
        return a;
    }

    public int getB()
    {
        return b;
    }
}

// Create responders be implementing the ServiceBusResponder interface
public class AdditionServiceBusResponder implements ServiceBusResponder<AdditionServiceBusRequest, Integer>
{
    @Override
    public ServiceBusResponse<Integer> respond(AdditionServiceBusRequest request)
    {
        int value = request.getA() + request.getB();

        return new ServiceBusResponse<>(value);
    }
}

int a = 10;
int b = 13;
int expectedResult = a + b;

// Create and submit a request
AdditionServiceBusRequest request = new AdditionServiceBusRequest(a, b);
ServiceBusResponse<Integer> response = RequestResponseBus.getInstance().submit(request);

// Verify that expected results were returned
assertEquals(ServiceBusResponse.Status.SUCCESS,response.getStatus(),"Status should match expected result");
assertEquals(expectedResult,response.getValue().intValue(),"Response should match expected result");
```


In addition to these basic cases, ServiceBus supports various blocking and non-blocking, synchronous and asynchronous modes, 
and subscriptions for commands and requests that enable support for pooled workers. See the tests for a full rundown of capabilities.

## Maven

Use Maven (or Ivy) to add as a dependency from Maven Central repository:

* Group id: `com.thirdpartylabs`
* Artifact id: `servicebus`
* Latest published version: 0.0.3 (2021-07-19)

## Requirements

Requires Java 9 (JDK 1.9)+

## License

XMLScalpel is licensed under [Apache 2](http://www.apache.org/licenses/LICENSE-2.0.txt) license.
