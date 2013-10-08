package com.allogy.isqrl.pages;

import com.allogy.isqrl.entities.Blip;
import com.allogy.isqrl.helpers.DomainName;
import com.allogy.isqrl.helpers.PollTime;
import com.allogy.isqrl.services.CrossRoads;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.Response;
import org.apache.tapestry5.util.TextStreamResponse;
import org.slf4j.Logger;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: robert
 * Date: 2013/10/04
 * Time: 1:15 AM
 */
public class Poll
{
    private static final boolean LOW_MEMORY_MODE = Boolean.getBoolean("LOW_MEMORY_MODE");
    private static final long NO_THRASHING_DELAY_MS = TimeUnit.SECONDS.toMillis(4);
    private static final boolean ISQRL_DOWN=Boolean.getBoolean("ISQRL_DOWN") || "true".equals(System.getenv("ISQRL_DOWN"));

    /**
     * This value should be short enough that sane infrastructure would cut the line, nor that the user would notice if a funky web browser held the page open, etc.
     * This value should be long enough that the TCP/IP & HTTP overhead is small compared to the window of waiting, and that it will easily satisfy the stale blip value in CrossRoads.
     */
    private static final long MAX_POLLING_PERIOD_MS = TimeUnit.SECONDS.toMillis(7);
    private static final String HTTP_MISSPELLED_REFERRER = "Referer";

    @Inject
    private CrossRoads crossRoads;

    @Inject
    private Request request;

    @Inject
    private Response response;

    Object onActivate() throws InterruptedException
    {
        if (hashY==null || x==null)
        {
            Thread.sleep(NO_THRASHING_DELAY_MS);

            //Even the error messages won't get through without...
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setStatus(400);
            return new TextStreamResponse("text/plain", "missing one or more arguments");
        }

        return null;
    }

    private String hashY;
    private String x;

    private static PollTime pollTime;

    private static final AtomicInteger numThreadsWaiting=new AtomicInteger(0);

    //@PostInjection does not work?
    //@PostConstruct does not work?
    /*
    I'll just make it static...
    public
    void pageConstructed()
    */
    static
    {
        String numThreads=System.getProperty("ISQRL_MAX_POLLING_THREADS");
        if (numThreads==null)
        {
            numThreads=System.getenv("ISQRL_MAX_POLLING_THREADS");
        }
        int maxTargetPollingThreads;
        if (numThreads==null)
        {
            /*
            I reluctantly set this default to 200, b/c Apache's default is 256 and Apache is very popular.
            One might think, "you gotta leave some threads left"... in reality this only causes us to have
            a zero-delay (no-wait) at that load.
             */
            maxTargetPollingThreads=256;
        }
        else
        {
            maxTargetPollingThreads=Integer.parseInt(numThreads);
        }
        pollTime=new PollTime(maxTargetPollingThreads, MAX_POLLING_PERIOD_MS);

        //Used to test "fastest possible" & "highest load" (maximum wait)...
        String staticWaitTime=System.getProperty("ISQRL_FIXED_WAIT_TIME");
        if (staticWaitTime==null)
        {
            staticWaitTime=System.getenv("ISQRL_FIXED_WAIT_TIME");
        }
        if (staticWaitTime!=null)
        {
            //log.warn("using static wait time: {}ms", staticWaitTime);
            pollTime.setStaticWaitTime(Long.parseLong(staticWaitTime));
        }
    }

    private Blip blip;

    Object onActivate(String hashY, String x) throws InterruptedException
    {
        //Even the error messages won't get through without...
        response.setHeader("Access-Control-Allow-Origin", "*");

        if (ISQRL_DOWN)
        {
            //no sleep (consumes threads, which is probably what we are lacking in a down situation)
            response.setStatus(500);
            return new TextStreamResponse("text/plain", "iSQRL temporarily down");
        }

        this.hashY=hashY;
        this.x=x;

        this.blip=crossRoads.getOrCreateBlip(x);

        if (!blip.isVoided())
        {
            if (blip.getHashY()==null)
            {
                blip.setHashY(hashY);
            }
            else
            if (!blip.getHashY().equals(hashY))
            {
                if (LOW_MEMORY_MODE)
                {
                    blip.setVoidMessage("conflicting hashY values for same x value");
                }
                else
                {
                    blip.setVoidMessage("blip had hashY of '"+blip.getHashY()+"', but now polling with '"+hashY+"'");
                }
            }

            String referrer = request.getHeader(HTTP_MISSPELLED_REFERRER);
            String domainName = DomainName.fromReferrer(referrer);

            log.trace("extract domain from referrer: '{}' -> '{}'", referrer, domainName);

            if (domainName==null)
            {
                response.setStatus(400);
                return new TextStreamResponse("text/plain", "invalid or missing referrer header");
            }
            else
            if (blip.getFullDomainName()==null)
            {
                //Signals a late initial poll(), or a sub-millisecond-eager snap() request... I suppose.
                synchronized (blip)
                {
                    blip.setFullDomainName(domainName);
                    blip.notifyAll();
                }
            }
            else
            if (!blip.getFullDomainName().equals(domainName))
            {
                if (LOW_MEMORY_MODE)
                {
                    blip.setVoidMessage("conflicting domain name polling");
                }
                else
                {
                    blip.setVoidMessage("blip had domain name of '"+blip.getFullDomainName()+"', but was then accessed by '"+domainName+"'");
                }
            }
        }

        /*
        Wait at most one standard polling period for the blip to be confirmed or voided, but wake *instantly* if it is...
         */
        if (!blip.isVoided())
        {
            maybeWaitForZToAppear(blip);
        }

        if (blip.isVoided())
        {
            response.setStatus(400);
            if (LOW_MEMORY_MODE)
            {
                return new TextStreamResponse("text/plain", blip.getVoidMessage());
            }
            else
            {
                return new TextStreamResponse("text/plain", "void: "+blip.getVoidMessage());
            }
        }

        if (blip.getHashZ()==null)
        {
            return new TextStreamResponse("application/json", "false");
        }
        else
        {
            return new TextStreamResponse("application/json", "true");
        }
    }

    private
    void maybeWaitForZToAppear(final Blip blip) throws InterruptedException
    {
        /*
        Dirty is fast (no contention), but not accurate (or atomic). The double-checking will
        ensure that we will breeze right through if we are under high load. Remember, synchronizations
        (and even to a lesser degree, the AtomicInteger functions) are *expensive* (esp. with multiple
        processors).
        */

        int  dirtyNumThreads=numThreadsWaiting.get();
        long dirtyWaitTime=pollTime.getMaxWaitTimeForNumberOfThreads(dirtyNumThreads);

        if (dirtyWaitTime>0)
        {
            int numThreads=numThreadsWaiting.incrementAndGet();
            try
            {
                long thisPollingPeriod=pollTime.getMaxWaitTimeForNumberOfThreads(numThreads);
                long wakeTime=System.currentTimeMillis()+thisPollingPeriod;
                //NB: thisPollingPeriod could still be < 0, putting wakeTime in the past (we'll just break out first go-around)

                synchronized (blip)
                {
                    while(blip.getHashZ()==null && !blip.isVoided())
                    {
                        long timeToSleep=wakeTime-System.currentTimeMillis();
                        if (timeToSleep<=0) break;
                        blip.wait(timeToSleep);
                    }
                }
            }
            finally
            {
                numThreadsWaiting.getAndDecrement();
            }
        }
        else
        {
                /*
                Don't worry too much about skipping the synchronization block.
                getZ() reads from a volatile so we won't get ever-stale processor
                cache if the write happens off of "our" processor.
                */
        }
    }

    @Inject
    private Logger log;

    public
    void withHashYAndX(String hashY, String x)
    {
        this.hashY=hashY;
        this.x=x;
    }

    String[] onPassivate()
    {
        return new String[]{
                hashY,
                x
        };
    }
}
