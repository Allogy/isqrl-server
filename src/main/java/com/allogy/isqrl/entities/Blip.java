package com.allogy.isqrl.entities;

/**
 * User: robert
 * Date: 2013/10/04
 * Time: 12:48 AM
 */
public final class Blip
{
    final private String x;

    private long activityTime;
    private String voidMessage;
    private String fullDomainName;

    private String hashY;

    volatile
    private String z;

    public
    Blip(String x)
    {
        this.x = x;
    }

    @Override
    public
    int hashCode()
    {
        return x.hashCode();
    }

    @Override
    public
    boolean equals(Object o)
    {
        if (o instanceof Blip)
        {
            return this.x.equals(((Blip)o).x);
        }
        else
        {
            return false;
        }
    }

    public String getX()
    {
        return x;
    }

    public String getZ()
    {
        return z;
    }

    public void setZ(String z)
    {
        this.z = z;
    }

    public String getFullDomainName()
    {
        return fullDomainName;
    }

    public void setFullDomainName(String fullDomainName)
    {
        this.fullDomainName = fullDomainName;
    }

    public String getVoidMessage()
    {
        return voidMessage;
    }

    public void setVoidMessage(String voidMessage)
    {
        this.voidMessage = voidMessage;
    }

    public long getActivityTime()
    {
        return activityTime;
    }

    public void setActivityTime(long activityTime)
    {
        this.activityTime = activityTime;
    }

    public boolean isVoided()
    {
        return (voidMessage!=null);
    }

    public boolean isExpired(long oldestAcceptable)
    {
        return (activityTime<oldestAcceptable);
    }

    public String getHashY()
    {
        return hashY;
    }

    public void setHashY(String hashY)
    {
        this.hashY = hashY;
    }
}
