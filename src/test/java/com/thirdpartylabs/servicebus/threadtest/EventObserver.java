/*
 * ServiceBus inter-component communication bus
 *
 * Copyright (c) 2021- Rob Ruchte, rob@thirdpartylabs.com
 *
 * Licensed under the License specified in file LICENSE, included with the source code.
 * You may not use this file except in compliance with the License.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thirdpartylabs.servicebus.threadtest;

import com.thirdpartylabs.servicebus.ServiceBus;
import com.thirdpartylabs.servicebus.events.ApplicationActivityServiceBusEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Simple listener for ApplicationActivityServiceBusEvent
 * Received events are collected for comparison against events collected by the worker
 */
public class EventObserver
{
    /*
     * Since this handler will be called by different worker threads, we need to make the list synchronized
     */
    private final List<String> receivedEvents =  Collections.synchronizedList(new ArrayList<>());

    public EventObserver()
    {
        // Set up an event listener - listener is executed in the generator thread
        ServiceBus.getInstance().events.listenFor(ApplicationActivityServiceBusEvent.class, this::handleApplicationActivityEvent);
    }

    /**
     * Handle the incoming ApplicationActivityServiceBusEvent and populate receivedEventMessage with the message string
     *
     * @param event to process
     */
    private void handleApplicationActivityEvent(ApplicationActivityServiceBusEvent event)
    {
        receivedEvents.add(event.getMessage());
    }

    public List<String> getReceivedEvents()
    {
        return receivedEvents;
    }
}
