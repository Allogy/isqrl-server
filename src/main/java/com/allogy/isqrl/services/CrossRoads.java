package com.allogy.isqrl.services;

import com.allogy.isqrl.entities.Blip;

/**
 * User: robert
 * Date: 2013/10/04
 * Time: 12:57 AM
 */
public interface CrossRoads
{
    Blip getOrCreateBlip(String x);

    void remove(Blip blip);

    int activeBlipCount();
}
