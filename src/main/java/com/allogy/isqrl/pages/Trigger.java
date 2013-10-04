package com.allogy.isqrl.pages;

import com.allogy.isqrl.entities.Blip;
import com.allogy.isqrl.services.CrossRoads;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.util.TextStreamResponse;

/**
 * Used to test Poll for latency, only works against a single hardcoded X value.
 *
 * User: robert
 * Date: 2013/10/04
 * Time: 2:07 AM
 */
public class Trigger
{

    @Inject
    private CrossRoads crossRoads;

    Object onActivate()
    {
        Blip blip=crossRoads.getOrCreateBlip("x123");
        String what;

        synchronized (blip)
        {
            if (blip.getZ()==null)
            {
                blip.setZ("zzzzzzzzzzzzzzzz");
                what="triggered\n";
            }
            else
            {
                blip.setZ(null);
                what="reset\n";
            }
            blip.notifyAll();
        }

        return new TextStreamResponse("text/plain", what);
    }
}
