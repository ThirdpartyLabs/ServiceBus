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
package com.thirdpartylabs.servicebus;

/**
 * Singleton to hold instances of the three communication busses
 */
public class ServiceBus
{
    public final CommandBus commands = CommandBus.getInstance();
    public final EventBus events = EventBus.getInstance();
    public final RequestResponseBus reqRes = RequestResponseBus.getInstance();

    private static final ServiceBus instance = new ServiceBus();

    private ServiceBus()
    {
    }

    /**
     * Return the single instance of the ServiceBus
     *
     * @return ServiceBus instance
     */
    public static ServiceBus getInstance()
    {
        return instance;
    }

    /**
     * Shut down the thread executors
     */
    public void shutdown()
    {
        commands.shutdown();
        events.shutdown();
        reqRes.shutdown();
    }
}
