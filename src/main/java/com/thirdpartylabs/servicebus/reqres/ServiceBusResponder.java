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
package com.thirdpartylabs.servicebus.reqres;

/**
 * Interface for request responders
 *
 * @param <T> ServiceBusRequest type that the responder handles
 * @param <A> Type of value that the request requires
 */
public interface ServiceBusResponder<T extends ServiceBusRequest<A>, A>
{
    /**
     * Handle the request
     * <p>
     * A ServiceBusResponse should be created and returned with the appropriate type set as the value
     *
     * @param request ServiceBusRequest object
     */
    ServiceBusResponse<A> respond(T request);
}
