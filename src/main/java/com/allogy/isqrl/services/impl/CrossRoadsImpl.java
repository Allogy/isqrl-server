package com.allogy.isqrl.services.impl;

import com.allogy.isqrl.entities.Blip;
import com.allogy.isqrl.services.CrossRoads;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.PostInjection;
import org.apache.tapestry5.ioc.services.cron.IntervalSchedule;
import org.apache.tapestry5.ioc.services.cron.PeriodicExecutor;
import org.slf4j.Logger;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * User: robert
 * Date: 2013/10/04
 * Time: 12:58 AM
 */
public class CrossRoadsImpl implements CrossRoads
{

    private Map<String,Blip> map=new ConcurrentHashMap<String, Blip>();

    private Object createLock=new Object();

    public
    Blip getOrCreateBlip(String x)
    {
        Blip blip=map.get(x);

        //NB: double-checked locking is considered an anti-pattern, use sparingly...
        if (blip==null)
        {
            synchronized (createLock)
            {
                blip=map.get(x);
                if (blip==null)
                {
                    blip=new Blip(x);
                    map.put(x, blip);
                }
            }
        }

        blip.setActivityTime(System.currentTimeMillis());
        return blip;
    }

    public
    void remove(Blip blip)
    {
        map.remove(blip.getX());
    }

    public
    int activeBlipCount()
    {
        return map.size();
    }

    private static final long STALE_BLIP_TIMEOUT_MS= TimeUnit.SECONDS.toMillis(20);

    @PostInjection
    public
    void serviceStarts(PeriodicExecutor periodicExecutor)
    {
        periodicExecutor.addJob(new IntervalSchedule(STALE_BLIP_TIMEOUT_MS/2), "blip-fade", new Runnable()
        {
            public
            void run()
            {
                cleanupStaleBlips();
            }
        });
    }

    @Inject
    private Logger log;

    private
    void cleanupStaleBlips()
    {
        int numRemoved=0;
        long oldestAcceptable=System.currentTimeMillis()-STALE_BLIP_TIMEOUT_MS;

        Iterator<Blip> i=map.values().iterator();

        while (i.hasNext())
        {
            Blip blip=i.next();
            if (blip.isExpired(oldestAcceptable))
            {
                numRemoved++;
                i.remove();
            }
        }

        if (numRemoved>0)
        {
            log.debug("removed {} stale blips", numRemoved);
        }
    }

}
