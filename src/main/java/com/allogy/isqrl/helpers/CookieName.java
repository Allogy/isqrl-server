package com.allogy.isqrl.helpers;

/**
 * Newer version of javax.servlet further restrict cookie names, they cannot include any of: "/()<>@,;:\\\"[]?={} \t";
 *
 * User: robert
 * Date: 2013/10/04
 * Time: 2:50 PM
 */
public class CookieName
{
    public static
    String forDomainTrust(String domainName)
    {
        return "isqrl-hy-"+domainName.toLowerCase();
    }

    /**
     * Cookies named after this function are intended to store a server-signed Z values.
     *
     * @param domainName
     * @param id_number
     * @return
     */
    public static
    String forZValue(String domainName, int id_number)
    {
        return "isqrl-z-"+domainName.toLowerCase()+"-"+id_number;
    }

    private CookieName() {}

    public static
    String globalSpoofDetector()
    {
        return "isqrl-unspoofed";
    }
}
