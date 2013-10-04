package com.allogy.isqrl.pages;

import com.allogy.isqrl.entities.Blip;
import com.allogy.isqrl.helpers.DomainName;
import com.allogy.isqrl.services.CrossRoads;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.Response;
import org.apache.tapestry5.util.TextStreamResponse;
import org.slf4j.Logger;

import java.util.concurrent.TimeUnit;

/**
 * User: robert
 * Date: 2013/10/04
 * Time: 1:15 AM
 */
public class Poll
{
    private static final boolean LOW_MEMORY_MODE = Boolean.getBoolean("LOW_MEMORY_MODE");
    private static final long NO_THRASHING_DELAY_MS = TimeUnit.SECONDS.toMillis(4);

    /**
     * This value should be short enough that sane infrastructure would cut the line, nor that the user would notice if a funky web browser held the page open, etc.
     * This value should be long enough that the TCP/IP & HTTP overhead is small compared to the window of waiting, and that it will easily satisfy the stale blip value in CrossRoads.
     */
    private static final long STANDARD_POLLING_PERIOD_MS = TimeUnit.SECONDS.toMillis(7);

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
            response.setStatus(400);
            return new TextStreamResponse("text/plain", "missing one or more arguments");
        }

        return null;
    }

    private String hashY;
    private String x;

    private Blip blip;

    Object onActivate(String hashY, String x) throws InterruptedException
    {
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

            String referrer = request.getHeader("Referrer");
            String domainName = DomainName.fromReferrer(referrer);

            log.debug("extract domain from referrer: '{}' -> '{}'", referrer, domainName);

            if (domainName==null)
            {
                response.setStatus(400);
                return new TextStreamResponse("text/plain", "invalid or missing referrer header");
            }
            else
            if (blip.getDomainName()==null)
            {
                //Signals a late initial poll(), or a sub-millisecond-eager snap() request... I suppose.
                synchronized (blip)
                {
                    blip.setDomainName(domainName);
                    blip.notifyAll();
                }
            }
            else
            if (!blip.getDomainName().equals(domainName))
            {
                if (LOW_MEMORY_MODE)
                {
                    blip.setVoidMessage("conflicting domain name polling");
                }
                else
                {
                    blip.setVoidMessage("blip had domain name of '"+blip.getDomainName()+"', but was then accessed by '"+domainName+"'");
                }
            }
        }

        /*
        Wait at most one standard polling period for the blip to be confirmed or voided, but wake *instantly* if it is...
         */
        if (!blip.isVoided())
        {
            long wakeTime=System.currentTimeMillis()+STANDARD_POLLING_PERIOD_MS;

            synchronized (blip)
            {
                while(blip.getZ()==null && !blip.isVoided())
                {
                    long timeToSleep=wakeTime-System.currentTimeMillis();
                    if (timeToSleep<=0) break;
                    blip.wait(timeToSleep);
                }
            }
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

        if (blip.getZ()==null)
        {
            return new TextStreamResponse("application/json", "false");
        }
        else
        {
            return new TextStreamResponse("application/json", "true");
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
