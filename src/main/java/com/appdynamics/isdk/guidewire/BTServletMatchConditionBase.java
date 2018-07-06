package com.appdynamics.isdk.guidewire;

import java.util.Map;

/**
 * Created by louis.nieuwoudt on 11/06/2018.
 */
public abstract class BTServletMatchConditionBase {
    public abstract boolean Match(Map<String, String[]> params ) ;
}
