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
package com.thirdpartylabs.servicebus.events;

/**
 * Interface for event listeners
 *
 * @param <T> ServiceBusEvent type
 */
public interface ServiceBusEventListener<T extends ServiceBusEvent>
{
    /**
     * Handle the provided event
     *
     * @param event Event to handle
     */
    void handle(T event);
}
