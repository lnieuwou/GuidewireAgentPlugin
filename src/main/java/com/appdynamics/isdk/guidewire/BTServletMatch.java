package com.appdynamics.isdk.guidewire;

import java.util.Map;

/**
 * Created by louis.nieuwoudt on 11/06/2018.
 */
public class BTServletMatch {
    public String btName ;
    protected BTServletMatchConditionBase condition ;

    public BTServletMatch(String name, BTServletMatchConditionBase c  )
    {
        btName = name ;
        condition = c ;
    }


    public boolean Match(Map<String, String[]> params )
    {
        return condition.Match( params ) ;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder() ;
        sb.append( "BT Name : " + btName + "\n") ;
        sb.append( "Condition : \n") ;
        sb.append( condition.toString() ) ;
        return sb.toString() ;
    }
}
