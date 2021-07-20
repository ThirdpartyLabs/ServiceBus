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

/**
 * Thread plumbing for the workers
 */
public abstract class AbstractWorker implements Runnable
{
    private final Thread t;
    protected boolean ready = false;
    protected boolean working = false;

    public AbstractWorker()
    {
        t = new Thread(this);
    }

    /**
     * Start the thread
     */
    public void start()
    {
        t.start();
    }

    /**
     * Workers should override this method
     */
    @Override
    public void run()
    {
        ready = true;

        // Block until we're told to stop
        while (true)
        {
            try
            {
                Thread.sleep(100);
            }
            catch (Exception ignored)
            {
            }
        }
    }

    /**
     * Stop the running thread
     */
    public void stop()
    {
        t.interrupt();
    }

    /**
     * @return flag indicating that the worker has completed initialization after the thread is started
     */
    public boolean isReady()
    {
        return ready;
    }

    /**
     * @return flag indicating that the worker is currently doing work
     */
    public boolean isWorking()
    {
        return working;
    }
}
