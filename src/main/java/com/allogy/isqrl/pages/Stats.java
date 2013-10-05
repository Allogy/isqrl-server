package com.allogy.isqrl.pages;

import com.allogy.isqrl.services.CrossRoads;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.util.TextStreamResponse;

/**
 * User: robert
 * Date: 2013/10/05
 * Time: 2:37 AM
 */
public class Stats
{
    @Inject
    private CrossRoads crossRoads;

    Object onActivate()
    {
        StringBuilder sb=new StringBuilder();

        sb.append("blips=").append(crossRoads.activeBlipCount()).append('\n');

        return new TextStreamResponse("text/plain", sb.toString());
    }

}
