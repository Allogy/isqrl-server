package com.allogy.isqrl.helpers;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * User: robert
 * Date: 2013/10/07
 * Time: 5:24 PM
 */
public class PollTime
{
    private final int maxThreads;
    private final long maxMilliseconds;

    private final double slope;

    private Long staticWaitTime;

    public
    PollTime(int maxThreads, long maxMilliseconds)
    {
        this.maxThreads = maxThreads;
        this.maxMilliseconds = maxMilliseconds;
        this.slope = (maxMilliseconds/(1.0-maxThreads));
    }

    public
    long getMaxWaitTimeForNumberOfThreads(int n)
    {
        if (staticWaitTime!=null) return staticWaitTime;

        /*
        Ekk!!! Math!!! :-)
        Basically, we want longest-wait-time when we are idle, and zero-wait-time when we are at maximum capacity.
        So...              [maxMilliseconds]         (at n=1), and [zero]         when [ n >= maxThreads].
         */
        if (n>=maxThreads)
        {
            return 0;
        }
        else
        if ( n <=1 )
        {
            return maxMilliseconds;
        }
        else
        {
            return (long)(slope*(n-1)+maxMilliseconds);
        }
    }

    public static
    void main(String[] argArray)
    {
        List<String> args=Arrays.asList(argArray);
        Iterator<String> i=args.iterator();

        PollTime p=new PollTime(Integer.parseInt(i.next()), Long.parseLong(i.next()));

        while (i.hasNext())
        {
            int numThreads=Integer.parseInt(i.next());
            long milliseconds=p.getMaxWaitTimeForNumberOfThreads(numThreads);
            System.out.println(String.format("%10d -> %d", numThreads, milliseconds));
        }
    }

    public
    void setStaticWaitTime(long l)
    {
        staticWaitTime=l;
    }
}
