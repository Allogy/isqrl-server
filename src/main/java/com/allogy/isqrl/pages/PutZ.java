package com.allogy.isqrl.pages;

import com.allogy.isqrl.entities.Blip;
import com.allogy.isqrl.services.CrossRoads;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.Response;
import org.apache.tapestry5.util.TextStreamResponse;
import org.slf4j.Logger;

/**
 * User: robert
 * Date: 2013/11/04
 * Time: 10:53 AM
 */
public class PutZ
{
    private static final boolean LOW_MEMORY_MODE = Boolean.getBoolean("LOW_MEMORY_MODE");

    @Inject
    private Logger log;

    @Inject
    private Request request;

    @Inject
    private Response response;

    @Inject
    private CrossRoads crossRoads;

    private
    Object message(int status, String message)
    {
        log.debug(message);
        response.setStatus(status);
        return new TextStreamResponse("text/plain", message);
    }

    Object onActivate()
    {
        String x =request.getParameter("x");
        String hy=request.getParameter("hy");
        String hz=request.getParameter("hz");
        String error=null;

        if (x ==null) { error="missing 'x' (via post variable)" ; }
        if (hy==null) { error="missing 'hy' (via post variable)"; }
        if (hz==null) { error="missing 'hz' (via post variable)"; }

        if (request.getHeader(Poll.HTTP_MISSPELLED_REFERRER)!=null)
        {
            error="putz does not allow the presence of a referer header";
        }

        if (error!=null) { return message(400, error); }

        Blip blip=crossRoads.getOrCreateBlip(x);

        if (blip.isVoided())
        {
            if (LOW_MEMORY_MODE)
            {
                error=blip.getVoidMessage();
            }
            else
            {
                error="voided: "+blip.getVoidMessage();
            }
        }

        if (blip.getHashY()==null)
        {
            error="putZ on blip without hashY set";
        }

        if (!hy.equals(blip.getHashY()))
        {
            error="invalid hashY (hy) value";
        }

        if (blip.getHashZ()!=null)
        {
            error="blip already has received a Z value";
        }

        if (error!=null)
        {
            blip.setVoidMessage(error);
            return message(400, error);
        }

        synchronized (blip)
        {
            if (blip.isVoided() || blip.getHashZ()!=null)
            {
                return message(400, "try again for error message");
            }

            blip.setHashZ(hz, true);
            blip.notifyAll();
        }

        return new TextStreamResponse("text/plain", "accepted");
    }

}

