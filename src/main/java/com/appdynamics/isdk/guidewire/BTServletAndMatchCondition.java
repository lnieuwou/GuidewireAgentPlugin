package com.appdynamics.isdk.guidewire;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by louis.nieuwoudt on 11/06/2018.
 */
public class BTServletAndMatchCondition extends BTServletMatchConditionBase
{
    protected List<BTServletMatchConditionBase> conditions ;

    public BTServletAndMatchCondition()
    {
        conditions = new ArrayList<BTServletMatchConditionBase>() ;
    }

    public void addCondition( BTServletMatchConditionBase c )
    {
        conditions.add( c ) ;
    }

    @Override
    public boolean Match(Map<String, String[]> params )
    {
        for ( BTServletMatchConditionBase c : conditions )
        {
            if ( ! c.Match( params ))
                return false ;
        }

        return true ;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder() ;
        String sep = "   " ;
        for( BTServletMatchConditionBase m : conditions )
        {
            sb.append( sep + m.toString() ) ;
            sep = " AND\n   " ;
        }
        return sb.toString() ;
    }
}
